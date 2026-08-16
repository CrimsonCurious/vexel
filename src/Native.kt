// ============================================================
//                         Native.kt
// ============================================================
package app.pie.vexel

import java.io.File

data class ClangTarget(
	val compiler: String,
	val target: String
)

private fun getNdk(): String {
    return Vexel.Config.ndk
        ?: vexelThrow(ErrorCode.TOOL_NOT_FOUND,
        "NDK not configured.",
        "Configure NDK path in config.toml"
        )
}

fun checkNdk() {
    val ndk = getNdk()
    if (!checkDir(ndk)) {
        vexelThrow(ErrorCode.TOOL_NOT_FOUND,
        "NDK installation not found",
        "Check that the NDK path is correct."
        )
        return
    }
    return
}

fun clangTarget(api: Any, abi: String, cpp: Boolean = true): ClangTarget {
    val ndk = getNdk()
    val clang = path(ndk, "bin", "clang")
    val clangpp = path(ndk, "bin", "clang++")
    val compiler = if (cpp) clangpp else clang
    
    val targets = mapOf(
        "arm64-v8a" to "aarch64-linux-android$api",
        "armeabi-v7a" to "armv7a-linux-androideabi$api",
        "x86" to "i686-linux-android$api",
        "x86_64" to "x86_64-linux-android$api"
    )

    if (!targets.containsKey(abi)) {
        vexelThrow(ErrorCode.INVALID_ABI,
        "Invalid ABI name: $abi",
        "Check that the ABI name is correct"
        )
    }

    return ClangTarget(compiler, targets[abi]!!)
}

fun resolveSources(srcField: String, projectDir: String, findCpp: Boolean = true): List<String> {
    val sources = mutableListOf<String>()

    for (raw in srcField.split(Regex("\\s+"))) {

        val item = path(projectDir, raw)
        val file = File(item)
        
        // Directory 
        if (file.isDirectory) {
            sources.addAll(findFiles(item, ".c"))
            if (findCpp) { sources.addAll(findFiles(item, ".cpp"))}
        }

        // Wildcard
        else if ("*" in item) {
            val parent = file.parentFile ?: continue
            val regex = file.name.split("*").joinToString(".*") { Regex.escape(it) }.toRegex()
            parent.listFiles()?.forEach {
            	if (it.isFile && regex.matches(it.name)) {
                        sources.add(it.absolutePath)
                    }
                }
        }

        // Single file
        else if (file.isFile) { sources.add(file.absolutePath) }
        else {
        	vexelThrow(ErrorCode.FILE_NOT_FOUND,
        	"Source file not found: $item",
        	"Check that the source file exits."
        	)
        }
    }

    return sources.distinct().sorted()
}

fun buildNative() {
    val vexelB = loadConfig(Vexel.Project.vexelBuild)
    val cfgndk = vexelB["ndk"] as? Map<String, Any> ?: emptyMap()
    val enabled = cfgndk["enabled"] as? Boolean ?: false

    if (!enabled) {
    	vexelLog("No C/C++ sources found.", "INFO")
    	return
    }
    checkNdk()

    vexelLog("C/C++ sources found", "INFO")
	vexelLog("Compiling C/C++ sources...", "BUILD")
	
    makeDir(Vexel.Output.dynLib)

    val buildScript = cfgndk["build_script"] ?.toString() ?: return
    val cfg = loadConfig(buildScript)

    for ((section, libCfgAny) in cfg) {
        if (!section.startsWith("lib-")) { continue }
        
        val libCfg = libCfgAny as? Map<String, Any> ?: continue
        val libname = section.split("-", limit = 2)[1]

        vexelLog("Compile Library: $libname", "BUILD")
        buildLibrary(libname, libCfg, cfgndk)
    }
}

fun buildLibrary(libname: String, cfg: Map<String, Any>, cfgndk: Map<String, Any>) {
    val ndk = getNdk()
    
    val api = cfgndk["api"] ?: error("Missing API")
    val abis = cfgndk["abi"].toString().split(" ")
    val cpp = cfg["cpp"] as? Boolean ?: true
    val sources = resolveSources(cfg["src"].toString(), Vexel.Project.dir, cpp)
    val includes = cfg["include"]?.toString()?.split(" ")?.filter{ 
    	it.isNotBlank() }?.map {"-I${path(Vexel.Project.dir, it)}"} ?: emptyList()

    val linkLib = cfg["link"]?.toString()?.split(" ")?.filter { it.isNotBlank() }?.map { "-l$it" }?: emptyList()

    val flags = cfg["flags"]?.toString()?.split(" ")?.filter { it.isNotBlank() }?: emptyList()

    for (abi in abis) {
        val target = clangTarget(api, abi, cpp)
        val userLibDir = path(Vexel.Project.dir, "lib", abi)
        val outLibDir = path(Vexel.Output.dynLib, abi)
        val outFile = path(outLibDir, "lib$libname.so")

        makeDir(outLibDir)

        val cmd = mutableListOf<String>()

        cmd.add(target.compiler)
        cmd.add("--target=${target.target}")
        cmd.add("--sysroot")
        cmd.add(path(ndk, "sysroot"))

        if (cpp) { cmd.add("-static-libstdc++") }

        cmd.addAll(listOf("-shared", "-fPIC", "-o", outFile))

        if (checkDir(userLibDir)) { cmd.add("-L$userLibDir") }
        
        cmd.add("-L$outLibDir")
        cmd.addAll(includes)
        cmd.addAll(sources)
        cmd.addAll(flags)
        cmd.addAll(linkLib)

        runCmd(cmd)
    }
}
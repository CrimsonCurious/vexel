// ============================================================
//				     BuildTools/BuildTools.kt
// ============================================================
package app.cc.vexel

import java.io.File
import java.io.PrintStream
import java.io.FileNotFoundException
import java.io.ByteArrayOutputStream

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import java.security.SecureRandom
import java.net.URLClassLoader

import com.android.tools.r8.D8
import com.android.apksigner.ApkSignerTool

object BuildTools {
	fun runAapt2Compile() {
		AAPT2.runAapt2Compile()
	}

	fun runAapt2Link() {
		AAPT2.runAapt2Link()
	}

	fun runApksigner() {
		Apksigner.signApk()
	}

	fun runClangCompile() {
		Clang.runClangCompile()
	}

	fun runD8() {
		D8Tool.convertDex()
	}

	fun runJavaCompile() {
		Javac.runJavaCompile()
	}

	fun runKotlinCompile() {
		Kotlinc.runKotlincCompile()
	}

	fun runZipalign() {
		Zipalign.alignApk()
	}
}

// ============================================================
// BuildUtils
// ============================================================

private object BuildUtils {
	fun findJavaTool(name: String): String {
		val javaHome = Vexel.Config.javaHome
		
		if (javaHome != null) {
			val exe = if (Vexel.Env.isWindows) "$name.exe" else name
			val tool = path(javaHome, "bin", exe)
			if (checkFile(tool)) { return tool }
			else {
				vexelThrow(ErrorCode.TOOL_NOT_FOUND,
				"Configured JAVA_HOME is invalid.\nMissing binary: $name",
				"Verify that the path of JAVA_HOME is correct.",
				debugInfo = "Java Home: $javaHome"
				)
			}
		}
		
		try {
			runCmd(listOf(name, "-version"))
			return name
		} catch (e: Exception) {
			vexelThrow(ErrorCode.TOOL_NOT_FOUND,
			"Unable to find $name.",
			"Install JDK 17+ or configure JAVA_HOME.",
			cause = e
			)
		}
	}
	
	fun ensureCached(pathInJar: String, outputName: String): String {
		val output = path(Vexel.Cache.buildTools, outputName)

		if (!checkFile(output)) {
    		extractFile(Vexel.Env.currentJarPath, pathInJar, Vexel.Cache.buildTools)
		}

		return output
	}
	
	fun makeExecutable(path: String) {
		if (!Vexel.Env.isWindows) {
			val binary = File(path)
			if (!binary.canExecute() && !binary.setExecutable(true, false)) {
    			vexelThrow(ErrorCode.FILE_PERMISSION_FAILED,
    			"Failed to add execute permission: $path",
    			"Check that you own the file and have permission to modify its attributes."
    			)
			}
		}
	}
	
	fun getBundledTool(name: String): String {
		val exe = if (Vexel.Env.isWindows) "$name.exe" else name
    	val path = ensureCached(path("vexel-tools", Vexel.Env.platform, exe), exe)
    	makeExecutable(path)
    	return path
	}
	
	fun resolveTool(path: String?, name: String): String {
	    path?.let {
            if (!checkFile(it)) {
                vexelThrow(ErrorCode.FILE_NOT_FOUND,
                "Configured file $name not found",
                "verify that the file path is correct and have permission to read"
                )
                
            }
            return it
        }
    	return getBundledTool(name)
	}
	
	fun getSdk(): String {
		val path: String? = Vexel.Config.sdk
		path?.let {
            if (!checkFile(it)) {
                vexelThrow(ErrorCode.FILE_NOT_FOUND,
                "Configured file $path not found",
                "verify that the file path is correct and have permission to read"
                )
            }
            return it
        }
		return ensureCached(path("vexel-tools", "android-34.jar"), "android-34.jar")
	}
}

// ============================================================
// AAPT2
// ============================================================

private object AAPT2 {
	fun runAapt2(args: List<String>) = runCmd(listOf(BuildUtils.resolveTool(Vexel.Config.aapt2, "aapt2")) + args)

	fun runAapt2Compile() {
		runAapt2(listOf("compile", "--dir", Vexel.Project.res, "-o", Vexel.Output.res))
	}

	fun runAapt2Link() {
		val flatFiles = findFiles(Vexel.Output.res, ".flat")
		if (flatFiles.isEmpty()) { return }
	
		runAapt2(listOf("link",
			"-o", path(Vexel.Output.apk, "base.apk"),
			"-I", BuildUtils.getSdk(),
			"--manifest", Vexel.Output.manifest,
			"--java", Vexel.Output.gen
			) + flatFiles
		)
	}
}

// ============================================================
// Apksigner
// ============================================================

private object Apksigner {
    private const val buildType = "debug"

    fun runApkSigner(args: List<String>) {
        ApkSignerTool.main(args.toTypedArray())
    }

    fun signApk() {
        val result = Keytool.getKeystore(buildType)

        vexelLog("Signing APK...", "BUILD")

        runApkSigner(
            listOf(
                "sign",
                "--ks", result.keystorePath,
                "--ks-key-alias", "vexelkey",
                "--ks-pass", "pass:${result.password}",
                "--key-pass", "pass:${result.password}",
                path(Vexel.Output.apk, "final.apk")
            )
        )

        vexelLog("APK signed successfully.", "INFO")
    }
}


// ============================================================
// Keytool
// ============================================================

private data class KeyStoreResult(
    val keystorePath: String,
    val password: String
)

private object Keytool {
    private val keytool by lazy {
        BuildUtils.findJavaTool("keytool")
    }

    fun runKeytool(args: List<String>) {
        runCmd(listOf(keytool) + args)
    }

    private fun generatePassword(length: Int = 24): String {
        val chars =
            "abcdefghijklmnopqrstuvwxyz" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "0123456789"

        val random = SecureRandom()

        return buildString {
            repeat(length) {
                append(chars[random.nextInt(chars.length)])
            }
        }
    }

    private fun readSigningConfig(
        buildType: String
    ): Map<*, *> {
        val configFile = File(Vexel.Project.vexelVcl)

        if (!configFile.isFile) {
            vexelThrow(
                ErrorCode.FILE_NOT_FOUND,
                "vexel.vcl was not found",
                "Create a vexel.vcl file in the project directory."
            )
        }

        val config = ConfigParser.parse(configFile.readText())

        val build = config["build"] as? Map<*, *>
            ?: return emptyMap<String, Any>()
			
		val signing = build["signing"] as? Map<*, *>
            ?: return emptyMap<String, Any>()

        return signing[buildType] as? Map<*, *>
            ?: emptyMap<String, Any>()
    }

    private fun resolvePassword(
        cfg: Map<*, *>,
        buildType: String
    ): String {
        // 1. Prefer a configured password file when it exists.
        val configuredPasswordFile =
            cfg["passwordFile"] as? String

        if (!configuredPasswordFile.isNullOrBlank()) {
            val file = File(
                path(Vexel.Env.projectDir, configuredPasswordFile)
            )

            if (file.isFile) {
                val password = file.readText().trim()

                if (password.isNotEmpty()) {
                    vexelLog(
                        "Using password from configured password file.",
                        "INFO"
                    )
                    return password
                }
            }
        }
        // 2. Otherwise, reuse the generated password file.
        val passwordFile = File(
            path(Vexel.Output.sign, "$buildType.password")
        )

        if (passwordFile.isFile) {
            val password = passwordFile.readText().trim()

            if (password.isNotEmpty()) {
                vexelLog(
                    "Using existing generated signing password.",
                    "INFO"
                )
                return password
            }
        }

        // 3. No password exists: generate and persist one.
        vexelLog("Generating signing password...", "INFO")

        val password = generatePassword()

        makeDir(Vexel.Output.sign)
        writeFile(passwordFile.path, password)

        return password
    }

    private fun verifyKeystore(keystorePath: String, password: String): Boolean {
    	if (!File(keystorePath).isFile) {
        	return false
    	}

    	val result = runCmdResult(
        	listOf(
            	keytool,
            	"-list",
            	"-keystore", keystorePath,
            	"-storepass", password
        	)
    	)

    	return result.exitCode == 0
	}

    private fun createKeystore(
        keystorePath: String,
        password: String
    ) {
        vexelLog("Generating debug keystore...", "INFO")

        val file = File(keystorePath)
        file.parentFile?.mkdirs()

        runKeytool(
            listOf(
                "-genkeypair",
                "-v",
                "-keystore", keystorePath,
                "-storepass", password,
                "-keypass", password,
                "-alias", "vexelkey",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-dname", "CN=Vexel,O=Dev,C=US"
            )
        )

        vexelLog("Keystore generated successfully.", "INFO")
    }

    fun getKeystore(
        buildType: String
    ): KeyStoreResult {
        vexelLog("Checking $buildType signing credentials...", "INFO")

        val cfg = readSigningConfig(buildType)
        val password = resolvePassword(cfg, buildType)
        val keystorePath = path(Vexel.Output.sign, "${buildType}.keystore")

        makeDir(Vexel.Output.sign)

        val keystore = File(keystorePath)

        if (!keystore.isFile) {
            createKeystore(keystorePath, password)
        } else if (verifyKeystore(keystorePath, password)) {
            vexelLog(
                "Existing keystore and password verified.",
                "INFO"
            )
        } else {
            vexelLog(
                "WARNING: The signing password does not match the existing keystore. " +
                "The existing signing identity will be replaced; previously signed ",
                "WARN"
            )

            if (!keystore.delete()) {
                vexelThrow(
                    ErrorCode.INVALID_CONFIG,
                    "Could not replace the existing keystore",
                    "Check file permissions for $keystorePath"
                )
            }

            createKeystore(keystorePath, password)
        }

        return KeyStoreResult(
            keystorePath = keystorePath,
            password = password
        )
    }
}

// ============================================================
// Clang
// ============================================================

private object Clang {
	private data class ClangTarget(
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

	private fun stringList(value: Any?): List<String> {
    	val list = value as? List<*> ?: return emptyList()

    	return list.map {
        	it as? String ?: vexelThrow(
            	ErrorCode.INVALID_CONFIG,
            	"Expected a string in configuration array",
            	"Check your VCL configuration."
        	)
    	}
	}

	private fun clangTarget(api: Any, abi: String, cpp: Boolean = true): ClangTarget {
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

	private fun resolveSources(
		srcField: List<String>,
		projectDir: String,
		findCpp: Boolean = true
	): List<String> {
    	val sources = mutableListOf<String>()
    	for (raw in srcField) {

        	val item = path(projectDir, raw)
        	val file = File(item)

        	// Directory
        	if (file.isDirectory) {
            	sources.addAll(findFiles(item, ".c"))

            	if (findCpp) {
                	sources.addAll(findFiles(item, ".cpp"))
            	}
        	}

        	// Wildcard
        	else if ("*" in item) {
            	val parent = file.parentFile ?: continue

            	val regex = file.name
                	.split("*")
                	.joinToString(".*") { Regex.escape(it) }
                	.toRegex()

            	parent.listFiles()?.forEach {
                	if (it.isFile && regex.matches(it.name)) {
                    	sources.add(it.absolutePath)
                	}
            	}
        	}

        	// Single file
        	else if (file.isFile) {
            	sources.add(file.absolutePath)
        	}

        	// Doesn't exist
        	else {
            	vexelThrow(
                	ErrorCode.FILE_NOT_FOUND,
                	"Source file not found: $item",
                	"Check that the source file exists."
            	)
        	}
    	}
    	return sources.distinct().sorted()
	}

	private fun checkNdk() {
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
	
	fun runClangCompile() {
    	val vexelB = ConfigParser.parse(
        	File(Vexel.Project.vexelVcl).readText()
    	)

    	val cfgndk = asConfigMap(vexelB["native"]) ?: emptyMap()

    	val enabled = cfgndk["enabled"] as? Boolean ?: false

    	if (!enabled) {
        	vexelLog(
            	"C/C++ compilation not enabled, skipping C/C++ compilation",
            	"INFO"
        	)
        	return
    	}
    	checkNdk()

    	val totalFiles =
        	findFiles(Vexel.Project.cpp, ".c") +
        	findFiles(Vexel.Project.cpp, ".cpp")

    	vexelLog(
        	"Found ${totalFiles.size} C/C++ source(s).",
        	"INFO"
    	)
		
    	vexelLog("Compiling C/C++ sources...", "BUILD")

    	makeDir(Vexel.Output.dynLib)

    	val buildScript = cfgndk["buildScript"] as? String
        	?: vexelThrow(
            	ErrorCode.INVALID_CONFIG,
            	"Missing native.build_script",
            	"Set build_script to the path of your native build configuration."
        	)

    	val buildScriptPath = path(Vexel.Project.dir, buildScript)

    	val cfg = ConfigParser.parse(
        	File(buildScriptPath).readText()
    	)

    	for ((section, libCfgAny) in cfg) {
        	if (!section.startsWith("lib-")) continue

        	val libCfg = asConfigMap(libCfgAny) ?: continue
        	val libname = section.removePrefix("lib")

        	vexelLog("Compile Library: $libname", "BUILD")
        	buildLibrary(libname, libCfg, cfgndk)
    	}
	}

	fun buildLibrary(
    	libname: String,
    	cfg: Map<String, Any>,
    	cfgndk: Map<String, Any>
	) {
    	val ndk = getNdk()

    	val api = cfgndk["api"] ?: error("Missing API")
    	val cpp = cfg["cpp"] as? Boolean ?: true

    	val abis = stringList(cfgndk["abi"])

    	val sources = resolveSources(
        	stringList(cfg["src"]),
        	Vexel.Project.dir,
        	cpp
    	)

    	val staticLibs = stringList(cfg["staticLibs"])
        	.map { path(Vexel.Project.dir, it) }

    	val includes = stringList(cfg["include"])
        	.map { "-I${path(Vexel.Project.dir, it)}" }

    	val linkLib = stringList(cfg["link"])
        	.map { "-l$it" }

    	val flags = stringList(cfg["flags"])

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

        	if (cpp) {
            	cmd.add("-static-libstdc++")
        	}

        	cmd.addAll(listOf("-shared", "-fPIC", "-o", outFile))

        	if (checkDir(userLibDir)) {
            	cmd.add("-L$userLibDir")
        	}

        	cmd.add("-L$outLibDir")
        	cmd.addAll(includes)
        	cmd.addAll(sources)
        	cmd.addAll(staticLibs)
        	cmd.addAll(flags)
        	cmd.addAll(linkLib)

        	runCmd(cmd)
    	}
	}
}

// ============================================================
// D8
// ============================================================

private object D8Tool {
	fun runD8(args: List<String>) = D8.main(args.toTypedArray())

	fun convertDex() {
		val classFiles = findFiles(Vexel.Output.classes, ".class")
		if (classFiles.isEmpty()) {
			vexelLog("No class files found, skipping DEX compilation.","INFO")
			return
		}
		
		vexelLog("Compiling classes to DEX...", "BUILD")
		
		val jarFiles = mutableListOf<String>()
		
		if (checkDir(Vexel.Project.jarLib)) {
			jarFiles.addAll(findFiles(Vexel.Project.jarLib, ".jar"))
		}
		
		val kotlinFiles =
    		if (checkDir(path(Vexel.Project.dir, "kotlin")))
        		findFiles(path(Vexel.Project.dir, "kotlin"), ".kt", true)
    		else
        		emptyList()

		val useKotlinStdlib = kotlinFiles.isNotEmpty()
		val dexInputs = mutableListOf<String>()

		dexInputs.addAll(classFiles)
		dexInputs.addAll(jarFiles)

		if (useKotlinStdlib) {
    		dexInputs.add(Vexel.Env.kotlinStdlib)
		}
		
		runD8(listOf(
			"--lib", BuildUtils.getSdk(),
			"--min-api", "21",
			"--output", Vexel.Output.dex
			) + dexInputs
		)
		
		vexelLog("Add classes.dex...", "BUILD")
		
		// Add DEX
    	for (dex in findFiles(Vexel.Output.dex, ".dex")) {
        	addToZip(path(Vexel.Output.apk, "unaligned.apk"), dex, File(dex).name, false)
		}
	}
}

// ============================================================
// Javac
// ============================================================

private object Javac {
	private val javac by lazy { BuildUtils.findJavaTool("javac") }

	fun runJavac(args: List<String>) = runCmd(listOf(javac) + args)

	fun runJavaCompile() {
		val javaDir = path(Vexel.Project.dir, "java")
		val javaFiles = if (checkDir(javaDir))
			{ findFiles(javaDir, ".java") }
			else { emptyList() }

		val genFiles = if (checkDir(Vexel.Output.gen)) 
			{ findFiles(Vexel.Output.gen, ".java") } 
			else { emptyList() }
		  
		if (javaFiles.isEmpty() && genFiles.isEmpty()) {
    		vexelLog("No Java sources found, skipping Java compilation.", "INFO")
    		return
		}
		
		if (javaFiles.isEmpty()) {
			vexelLog("No project Java sources found.", "INFO")
		}
		
		if (javaFiles.isNotEmpty()) {
    		vexelLog("Found ${javaFiles.size} Java source(s).", "INFO")
		}

		if (genFiles.isNotEmpty()) {
    		vexelLog("Found ${genFiles.size} generated Java source(s).", "INFO")
		}
		
		vexelLog("Compiling Java sources...", "BUILD")
		
		val jarFiles = mutableListOf<String>()
		
		if (checkDir(Vexel.Project.jarLib)) {
			jarFiles.addAll(findFiles(Vexel.Project.jarLib, ".jar"))
			for (jar in jarFiles) {
				vexelLog("Loaded ${File(jar).name}", "INFO")
			}
		}
		val separator = File.pathSeparator
		val classpath = (listOf(BuildUtils.getSdk()) + jarFiles).joinToString(separator)
		
		runJavac(listOf(
			"-d", Vexel.Output.classes,
			"-classpath", classpath
			) + genFiles + javaFiles
		)
	}
}

// ============================================================
// Kotlinc
// ============================================================

private object Kotlinc {
	fun runKotlinc(args: Array<String>): String {
		val launcherName = path(Vexel.Env.kotlinHome, "bin", "kotlinc")
		val launcher = if (Vexel.Env.isWindows) "${launcherName}.bat" else launcherName
		return runCmd(listOf(launcher) + args)
	}

	fun runKotlincCompile() {
    	val kotlinFiles = findFiles(path(Vexel.Project.dir, "kotlin"), ".kt", true)
    	
    	if (kotlinFiles.isEmpty()) {
    		vexelLog("No Kotlin sources found, skipping Kotlin compilation.", "INFO")
        	return
    	}
    	vexelLog("Found ${kotlinFiles.size} Kotlin source(s).", "INFO")
    	vexelLog("Compiling Kotlin sources...", "BUILD")
    	
    	val jarFiles = mutableListOf<String>()

    	if (checkDir(Vexel.Project.jarLib)) {
        	jarFiles.addAll(findFiles(Vexel.Project.jarLib, ".jar"))

        	for (jar in jarFiles) {
            	vexelLog("Loaded ${File(jar).name}", "INFO")
        	}
    	}

    	val separator = File.pathSeparator

    	val classpath = (listOf(
        	BuildUtils.getSdk(),
        	Vexel.Output.classes
    	) + jarFiles ).joinToString(separator)

    	val args = arrayOf(
        	"-d", Vexel.Output.classes,
        	"-classpath", classpath,
        	*kotlinFiles.toTypedArray()
    	)

    	val error = runKotlinc(args)

    	if (error.isNotEmpty()) {
        	vexelThrow(ErrorCode.INTERNAL_ERROR, error)
        	return
    	}
	}
}

// ============================================================
// Zipalign
// ============================================================

internal object Zipalign {
	fun runZipalign(args: List<String>) = runCmd(listOf(BuildUtils.resolveTool(Vexel.Config.zipalign, "zipalign")) + args)

	fun alignApk() {
		runZipalign(listOf("-f", "4",
			path(Vexel.Output.apk, "unaligned.apk"),
			path(Vexel.Output.apk, "final.apk")
			)
		)
	}
}
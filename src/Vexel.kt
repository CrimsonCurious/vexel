// ============================================================
//				          Vexel.kt
// ============================================================
package app.cc.vexel

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.FileNotFoundException

// ============================================================
// Default Configuration 
// ============================================================

fun loadDefaultConfig() {
    val configFile = when {
        checkFile(Vexel.Config.file) -> Vexel.Config.file
        else -> null
    }

    if (configFile == null) {
        return
    }

    val config = ConfigParser.parse(
        File(configFile).readText()
    )

    val tools = config["tools"] as? Map<*, *> ?: emptyMap<String, Any>()
    val java = config["java"] as? Map<*, *> ?: emptyMap<String, Any>()
    val logs = config["logs"] as? Map<*, *> ?: emptyMap<String, Any>()

    fun Map<*, *>.string(key: String): String? =
        (this[key] as? String)
            ?.takeUnless { it.equals("default", ignoreCase = true) }

    fun Map<*, *>.boolean(key: String): Boolean? =
        this[key] as? Boolean

    Vexel.Config.apply {
        aapt2 = tools.string("aapt2")
        zipalign = tools.string("zipalign")
        sdk = tools.string("androidJar")
        ndk = tools.string("ndk")

        javaHome =
            java.string("home")
                ?: System.getenv("JAVA_HOME")

        enableColor =
            logs.boolean("enableColor") ?: false

        enableTimeStamp =
            logs.boolean("enableTimeStamp") ?: false

        enableDebugMode =
            logs.boolean("enableDebugMode") ?: false
    }
}

// ============================================================
// Builder
// ============================================================

private fun makeFileSystem() {
    if (checkDir(Vexel.Output.dir)) {
        vexelLog("Cleaning build directory...", "INFO")
        removeDir(Vexel.Output.dir)
    }

    makeDir(Vexel.Output.res)
    makeDir(Vexel.Output.dex)
    makeDir(Vexel.Output.classes)
    makeDir(Vexel.Output.gen)
    makeDir(Vexel.Output.sign)
    makeDir(Vexel.Output.apk)
}

private fun mergeNativeLibraries(srcDir: String, dstDir: String) {
    val source = File(srcDir)
    val dest = File(dstDir)

    if (!source.exists()) { return }
    dest.mkdirs()

    source.walkTopDown().filter { it.isFile }.forEach { file ->
        val relative = file.relativeTo(source)
        val outFile = File(dest, relative.path)

        outFile.parentFile?.mkdirs()

        if (outFile.exists()) {
            vexelThrow(
                ErrorCode.FILE_ALREADY_EXISTS,
                "Native library conflict: ${relative.path}",
                "Two libraries with the same name were found. Remove one of them.",
                debugInfo = "Source: ${file.absolutePath}\nDestination: ${outFile.absolutePath}"
            )
        }

        file.copyTo(outFile)
    }
}

internal fun buildProject() {
    val start = System.currentTimeMillis()

	val config = try {
	    ConfigParser.parse(File(Vexel.Project.vexelVcl).readText())
	} catch (e: FileNotFoundException) {
    	vexelThrow(
			ErrorCode.FILE_NOT_FOUND, 
			"Unable to find ${Vexel.Project.vexelVcl}"
		)
	}
	
	val build = config["build"] as? Map<*, *> ?: emptyMap<String, Any>()
	val app = build["app"] as? Map<*, *> ?: emptyMap<String, Any>()
	val appName = app["name"] as? String ?: ""
	
    makeFileSystem()

    updateManifest(Vexel.Project.manifest, Vexel.Output.manifest, config)

    vexelLog("Compiling resources...", "BUILD")
    BuildTools.runAapt2Compile()

    vexelLog("Linking resources...", "BUILD")
    BuildTools.runAapt2Link()
	
    Files.copy(
        File(path(Vexel.Output.apk, "base.apk")).toPath(),
        File(path(Vexel.Output.apk, "unaligned.apk")).toPath(),
        StandardCopyOption.REPLACE_EXISTING
    )
	
    vexelLog("Checking Java sources...", "INFO")
    BuildTools.runJavaCompile()
    
    vexelLog("Checking Kotlin sources...", "INFO")
    BuildTools.runKotlinCompile()

    vexelLog("Checking C/C++ sources...", "INFO")
    BuildTools.runClangCompile()
	
    vexelLog("Checking Class files..", "INFO")
    BuildTools.runD8()

    // Add User Dynamic Libraries
	mergeNativeLibraries(
    	Vexel.Project.dynLib,
    	Vexel.Output.dynLib
	)

    // Add Dynamic Libraries
    addToZip(
        path(Vexel.Output.apk, "unaligned.apk"),
        Vexel.Output.dynLib
    )

    // Add Assets
	addToZip(
        path(Vexel.Output.apk, "unaligned.apk"),
        Vexel.Project.assets
    )

    vexelLog("Aligning APK...", "BUILD")
    BuildTools.runZipalign()

    BuildTools.runApksigner()

    makeDir(Vexel.Output.finalApk)

    Files.copy(
        File(path(Vexel.Output.apk, "final.apk")).toPath(),
        File(path(Vexel.Output.finalApk, "${appName}-debug.apk")).toPath(),
        StandardCopyOption.REPLACE_EXISTING
    )

    val elapsed = (System.currentTimeMillis() - start) / 1000.0

    vexelLog("Build Successful in ${textFormat(elapsed.toString(), Vexel.Color.Blue)} sec.", "INFO")
    vexelLog("APK saved at: ${path(Vexel.Output.finalApk,"${appName}-debug.apk")}", "INFO")
}

// ============================================================
// Variables
// ============================================================

data class CheckupResult(
    val version: String,
    val platform: String,
    val supported: Boolean,
    val javaVersion: String,
    val javaHome: String?,
    val aapt2: String,
    val zipalign: String,
    val androidJar: String,
    val ndk: String,
    val config: String
)

object Vexel {
	fun Build() {
		buildProject()
	}
	
	fun Create(projectName: String, packageName: String, Language: String) {
		writeTemplate(Project(projectName, packageName, 24, 34, Language))
	}

	fun Checkup(): CheckupResult {
    	return CheckupResult(
        	version = Version,
        	platform = Env.platform,
        	supported = Env.platform != "unknown",
        	javaVersion = getJavaVersion(),
        	javaHome = Config.javaHome,
        	aapt2 = Config.aapt2 ?: "Embedded (Default)",
        	zipalign = Config.zipalign ?: "Embedded (Default)",
        	androidJar = Config.sdk ?: "Embedded (API 34)",
        	ndk = Config.ndk ?: "Not Configured",
        	config =
            if (checkFile(Config.file)) "Present"
			else "Not Present"
    	)
	}
	
	fun GetVersion(): String {
		return Version
	}

	fun LoadDefaultConfig() {
		loadDefaultConfig()
	}
	
	const val Version = "0.5.0-beta"
	
	// Environment Paths
	object Env {
		val currentJar = File(object {}.javaClass.protectionDomain.codeSource.location.toURI())
        val currentJarPath = currentJar.absolutePath
        val currentJarDir = currentJar.parentFile.absolutePath
        
		val userHome: String = System.getenv("HOME") ?: currentJarDir
		val home: String = path(userHome, ".vexel")
		val projectDir: String = System.getProperty("user.dir")
		val platform by lazy { getPlatform() }
		val isWindows by lazy { platform == "x64-windows" }
		val projectSrc: String = path(projectDir, "src")
		
		val kotlinHome: String = path(currentJarDir, "..", "tools", "kotlin")
		val kotlinStdlib: String = path(kotlinHome, "lib", "kotlin-stdlib.jar")
		val kotlinReflect: String = path(kotlinHome, "lib", "kotlin-reflect.jar")
	}
	
	// Vexel Config
	object Config {
		val file: String = path(Env.currentJarDir, "config.vcl")
		
		var aapt2: String? = null
		var zipalign: String? = null
		var sdk: String? = null
		var ndk: String? = null
		var javaHome: String? = System.getenv("JAVA_HOME")
		
		var enableColor: Boolean = false
		var enableTimeStamp: Boolean = false
		var enableDebugMode: Boolean = false
	}
	
	// Cache
	object Cache {
		val buildTools: String = path(Env.home, "cache", "buildTools")
		val defaultIcons: String = path(buildTools, "default_icons.zip")
	}
	
	// Project Paths
	object Project {
		val dir: String = Env.projectSrc
		val dynLib: String = path(Env.projectSrc, "lib")
  	    val assets: String = path(Env.projectSrc, "assets")
		val res: String = path(Env.projectSrc, "res")
 	    val manifest: String = path(Env.projectSrc, "AndroidManifest.xml")
 	    val cpp: String = path(Env.projectSrc, "cpp")
  	    val vexelVcl: String = path(Env.projectDir, "vexel.vcl")
 	    val jarLib: String = path(Env.projectDir, "libs")
	}
	
	// Output Paths
	object Output {
		val dir: String = path(Env.projectDir, "build", "intermediates")
		val sign: String = path(Env.projectDir, "build", "signing")
		
		val res: String = path(dir, "resources")
		val dex: String = path(dir, "dex")
		val classes: String = path(dir, "classes")
		val gen: String = path(dir, "generated")
		val dynLib: String = path(dir, "lib")
		val manifest: String = path(dir, "generated", "AndroidManifest.xml")
		val apk: String = path(dir, "apk")
		val finalApk: String = path(Env.projectDir, "build", "output")
	}

	// Colors
	object Color {
		const val Reset   = "\u001B[0m"

		const val Red = "\u001B[31m"
		const val Green   = "\u001B[32m"
		const val Yellow  = "\u001B[33m"
		const val Blue	= "\u001B[34m"
		const val Magenta = "\u001B[35m"
		const val Cyan	= "\u001B[36m"
		const val White   = "\u001B[37m"

		const val Bold	= "\u001B[1m"

		val tagColors = mapOf(
			"INFO" to Cyan,
			"BUILD" to Green,
			"WARN" to Yellow,
			"CRASH" to Red,
			"DEBUG" to Magenta,
			"NONE" to White
		)
	}
}
// ============================================================
//						 Variables.kt
// ============================================================
package app.pie.vexel

import java.io.File

// ============================================================
// ANSI Colors
// ============================================================

object ANSI {
	const val RESET   = "\u001B[0m"

	const val RED	 = "\u001B[31m"
	const val GREEN   = "\u001B[32m"
	const val YELLOW  = "\u001B[33m"
	const val BLUE	= "\u001B[34m"
	const val MAGENTA = "\u001B[35m"
	const val CYAN	= "\u001B[36m"
	const val WHITE   = "\u001B[37m"

	const val BOLD	= "\u001B[1m"

	val tagColors = mapOf(
		"INFO" to CYAN,
		"BUILD" to GREEN,
		"WARN" to YELLOW,
		"CRASH" to RED,
		"DEBUG" to MAGENTA,
		"NONE" to WHITE
	)
}

object ErrorCode {
    const val FILE_NOT_FOUND = "FILE_NOT_FOUND"
    const val DIR_NOT_FOUND = "DIRECTORY_NOT_FOUND"
    
    const val FILE_ALREADY_EXISTS = "FILE_ALREADY_EXISTS"
    
    const val FILE_WRITE_FAILED = "FILE_WRITE_FAILED"
    const val FILE_PERMISSION_FAILED = "FILE_PERMISSON_FAILED"
    const val DIR_WRITE_FAILED = "DIRECTORY_WRITE_FAILED"
    const val DIR_DELETE_FAILED = "DIRECTORY_DELETE_FAILED"

    const val INVALID_CONFIG = "INVALID_CONFIG"
    const val INVALID_MANIFEST = "INVALID_MANIFEST"

    const val TOOL_NOT_FOUND = "TOOL_NOT_FOUND"
    const val PROCESS_FAILED = "PROCESS_FAILED"

    const val ZIP_ERROR = "ZIP_ERROR"
    const val INVALID_ABI = "INVALID_ABI"
    const val INVALID_INPUT = "INVALID_INPUT"
    const val INTERNAL_ERROR = "INTERNAL_ERROR"
}

object Vexel {
	// Environment Paths
	object Env {
		val currentJar = File(object {}.javaClass.protectionDomain.codeSource.location.toURI())
        val currentJarPath = currentJar.absolutePath
        val currentJarDir = currentJar.parentFile.absolutePath
        
		val userHome: String = System.getenv("HOME") ?: currentJarDir
		val home: String = path(userHome, ".vexel")
		val projectDir: String = System.getProperty("user.dir")
		val platform by lazy { getPlatform() }
		val projectSrc: String = path(projectDir, "src")
		
		val kotlinHome: String = path(currentJarDir, "..", "tools", "kotlin")
		val kotlinStdlib: String = path(kotlinHome, "lib", "kotlin-stdlib.jar")
		val kotlinReflect: String = path(kotlinHome, "lib", "kotlin-reflect.jar")
	}
	
	// Vexel Config
	object Config {
		val file: String = path(Env.currentJarDir, "config.toml")
		val fileFallBack: String = path(Env.home, "config.toml")
		
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
  	  val vexelBuild: String = path(Env.projectDir, "vexel.build")
 	   val jarLib: String = path(Env.projectDir, "libs")
	}
	
	// Output Paths
	object Output {
		val dir: String = path(Env.projectDir, "build", "intermediates")
		val key: String = path(Env.projectDir, "build", "signing")
		
		val res: String = path(dir, "resources")
		val dex: String = path(dir, "dex")
		val classes: String = path(dir, "classes")
		val gen: String = path(dir, "generated")
		val dynLib: String = path(dir, "lib")
		val manifest: String = path(dir, "generated", "AndroidManifest.xml")
		val apk: String = path(dir, "apk")
		val finalApk: String = path(Env.projectDir, "build", "output")
	}
}
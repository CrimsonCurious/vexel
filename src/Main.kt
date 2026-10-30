// ============================================================
//                         Main.kt
// ============================================================
@file:JvmName("Main")
package app.cc.vexel

import java.io.File

private const val UNKNOWN_COMMAND =
    """vexel: Unknown command [type "vexel help"]"""

private const val NO_ARGUMENT =
    """vexel: Needs one Argument [type "vexel help"]"""

val help = """
Vexel $Vexel.Version
A Minimal Android APK Builder

Usage:
  vexel <option>

Options:
  build        Builds the project
  checkup      Checks config.toml
  clean        Remove build cache
  config       Writes default config.toml
  help         Display this help page
  new          Create a new project
  version      Display current version

CrimsonCurious © 2026
""".trimIndent()

// ============================================================
// Interactive Creator
// ============================================================

fun CreateProjectInteractive() {
	// Ask for Template
	println("Choose Template:")
	println("  [1] Java Activity")
	println("  [2] Kotlin Activity")
	println("  [3] Native Activity (java)")
	println("  [4] Native Activity (kotlin)")
	
	val template = inputInt("Template: ")
	  
	// Convert Number into String
	val language = when {
		template == 1 -> Template.PROJECT_JAVA
		template == 2 -> Template.PROJECT_KOTLIN
		template == 3 -> Template.PROJECT_CPP_JAVA
		template == 4 -> Template.PROJECT_CPP_KOTLIN
		else -> vexelThrow(ErrorCode.INVALID_INPUT, "Invalid template number selected")
	}

	// Ask Project Details
	val appName = input("Project Name: ")
	if (appName.isEmpty()) {
		vexelThrow(ErrorCode.INVALID_INPUT, "Empty project name provided")
	}

	val packageName = cleanPackageName(input("Package Name: "))
	if (packageName.isEmpty()) {
		vexelThrow(ErrorCode.INVALID_INPUT, "Empty package name provided")
	}

	Vexel.Create(appName, packageName, language)
}

// ============================================================
// Config Writer
// ============================================================

private fun WriteConfig() {
	val configFile = """
# Vexel Configuration
java: {
	# Optional. Falls back to JAVA_HOME if omitted.
	home: "default"
}

tools: {
	# Use "default" to use the bundled tool.
	aapt2: "default"
	zipalign: "default"
	androidJar: "default"

	# Path to the LLVM prebuilt directory.
	# This is only for example may you have to change this.
	ndk: "/data/data/com.termux/files/ndk/android-r29/toolchains/llvm/prebuilt/linux-arm64" 
}

logs: {
	enableColor: true
	enableTimeStamp: true
	enableDebugMode: false
}
		""".trimIndent()
	writeFile(Vexel.Config.file, configFile)
}

// ============================================================
// Printing Checkup
// ============================================================

fun PrintCheckup() {
	val result = Vexel.Checkup()

	println("Version      : ${result.version}")
	println("Platform     : ${result.platform}")
	println("Supported    : ${if (result.supported) "Yes" else "No"}")
	println()
	println("Java Version : ${result.javaVersion}")
	println("JAVA_HOME    : ${result.javaHome ?: "Not Set"}")
	println()
	println("AAPT2        : ${result.aapt2}")
	println("Zipalign     : ${result.zipalign}")
	println("Android.jar  : ${result.androidJar}")
	println()
	println("NDK          : ${result.ndk}")
	println("Config       : ${result.config}")
	
}

// ============================================================
// Utils
// ============================================================

private fun cleanPackageName(packageName: String): String {
	val allowed = "abcdefghijklmnopqrstuvwxyz0123456789._"
	val cleaned = buildString {
		for (char in packageName.lowercase()) {
			if (char in allowed) { append(char) }
			else {
				vexelLog("Warning: '$char' removed from package name", "WARN")
			}
		}
	}

	return cleaned.split(".").filter{ it.isNotEmpty() }.joinToString(".")
}

// Clean
private fun Clean() {
    vexelLog("Cleaning...", "INFO")
    removeDir(Vexel.Output.dir)
}

// ============================================================
// Main Entry
// ============================================================

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println(NO_ARGUMENT)
        return
    }
	
    Vexel.LoadDefaultConfig()

    val arg = args[0].trimStart('-').lowercase()

    when (arg) {
        "new" -> {
            val template = args.getOrNull(1)
			if (template == null) {
            	CreateProjectInteractive()
			}
			else {
				val appName = "Hello World"
				val packageName = "com.example.helloworld"
				
				val projectTemplate = when (template) {
    				"1", "java" -> Template.PROJECT_JAVA
    				"2", "kotlin" -> Template.PROJECT_KOTLIN
    				"3", "java_cpp" -> Template.PROJECT_CPP_JAVA
    				"4", "kotlin_cpp" -> Template.PROJECT_CPP_KOTLIN
    				else -> null // Handle invalid templates if necessary
				}

				if (projectTemplate != null) {
    				Vexel.Create(appName, packageName, projectTemplate)
				} else {
					vexelThrow(ErrorCode.INVALID_INPUT, "Invalid Template selected")
				}
			}
        }

        "build" -> {
			Vexel.Build()
		}
		
        "clean" -> {
			Clean()
		}
		
        "version" -> {
			println("Vexel ${Vexel.Version}")
		}
		
        "checkup" -> {
			PrintCheckup()
		}
		
        "help" -> {
			println(help)
		}
		
        "config" -> {
			WriteConfig()
			vexelLog("Default config written to: ${Vexel.Config.file}", "INFO")
		}
		
        else -> {
			println(UNKNOWN_COMMAND)
		}
    }
}
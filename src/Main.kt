// ============================================================
//                         Main.kt
// ============================================================
package app.pie.vexel

import java.io.File

const val Version = "0.3.0"
val InstallMarker = path(Vexel.Env.home, ".installed")
const val unkcmd = """vexel: Unknown command [type "vexel help"]"""
const val narg = """vexel: Needs one Argument [type "vexel help"]"""

val help = """
Vexel $Version
A Minimal Android APK Builder

Usage:
  vexel <option>

Options:
  new          Create a new project
  build        Builds the project
  clean        Remove build cache
  version      Display current version
  checkup      Checks config.toml
  help         Display this help page

PieStudios © 2026
""".trimIndent()

fun clean() {
    vexelLog("Cleaning...", "INFO")
    removeDir(Vexel.Output.dir)
}

fun getJavaVersion(): String {
    return try {
        val version = System.getProperty("java.version")
        version ?: "Unknown"
    } catch (_: Exception) {
        "Not Found"
    }
}

fun checkup() {
    var supported: String = "Yes"
    var config: String = "Not Found"
    val ndk = Vexel.Config.ndk ?: "Not Installed"
    val aapt2 = Vexel.Config.aapt2 ?: "Embedded (Default)"
    val zipalign = Vexel.Config.zipalign ?: "Embedded (Default)"
    val androidJar = Vexel.Config.sdk ?: "Embedded (API 34)"
    
    if (Vexel.Env.platform == "unknown") {
    	supported = "No"
    }
    if (checkFile(Vexel.Config.file)) {
    	config = "Present"
    }
    
    println("Version      : $Version")
    println("Platform     : ${Vexel.Env.platform}")
    println("Supported    : $supported")

    println()

    println("Java Version : ${getJavaVersion()}")
    println("JAVA_HOME    : ${Vexel.Config.javaHome ?: "Not Set"}")

    println()

    println("AAPT2        : $aapt2")
    println("Zipalign     : $zipalign")
    println("Android.jar  : $androidJar")

    println()

    println("NDK          : $ndk")
    println("Config       : $config")
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println(narg)
        return
    }
    loadConfig()

    val arg = args[0].trimStart('-').lowercase()

    when (arg) {
        "new" -> {
            val template = args.getOrNull(1)
            create(template)
        }

        "build" -> { build() }
        "clean" -> { clean() }
        "version" -> { println("Vexel $Version") }
        "checkup" -> { checkup() }
        "help" -> { println(help) }
        else -> { println(unkcmd) }
    }
}
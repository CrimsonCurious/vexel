// ============================================================
//                         Builder.kt
// ============================================================
package app.pie.vexel

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun makeFileSystem() {
    if (checkDir(Vexel.Output.dir)) {
        vexelLog("Cleaning build directory...", "INFO")
        removeDir(Vexel.Output.dir)
    }

    makeDir(Vexel.Output.res)
    makeDir(Vexel.Output.dex)
    makeDir(Vexel.Output.classes)
    makeDir(Vexel.Output.gen)
    makeDir(Vexel.Output.key)
    makeDir(Vexel.Output.apk)
}

fun build() {
    val start = System.currentTimeMillis()

    val config = loadConfig(Vexel.Project.vexelBuild)
	val app = config["app"] ?: mutableMapOf()
	val appName = app.get("name")
	
    makeFileSystem()

    Build.updateManifest(Vexel.Project.manifest, Vexel.Output.manifest, config)

    vexelLog("Compiling resources...", "BUILD")
    Build.runAapt2Compile()

    vexelLog("Linking resources...", "BUILD")
    Build.runAapt2Link()

    vexelLog("Checking Java sources...", "INFO")
    Build.runJavaCompile()
    
    vexelLog("Checking Kotlin sources...", "INFO")
    Build.runKotlinCompile()

    vexelLog("Checking C/C++ sources...", "INFO")
    buildNative()

    vexelLog("Convert classes to DEX...", "BUILD")
    Build.convertDex()

    vexelLog("Add classes.dex...", "BUILD")

    Files.copy(
        File(path(Vexel.Output.apk, "base.apk")).toPath(),
        File(path(Vexel.Output.apk, "unaligned.apk")).toPath(),
        StandardCopyOption.REPLACE_EXISTING
    )

    // Add DEX
    for (dex in findFiles(Vexel.Output.dex, ".dex")) {
        Build.addToZip(path(Vexel.Output.apk, "unaligned.apk"), dex, File(dex).name, false)
    }

    // Add User Dynamic Libraries
	Build.mergeNativeLibraries(
    	Vexel.Project.dynLib,
    	Vexel.Output.dynLib
	)

    // Add Dynamic Libraries
    Build.addToZip(
        path(Vexel.Output.apk, "unaligned.apk"),
        Vexel.Output.dynLib
    )

    // Add Assets
    Build.addToZip(
        path(Vexel.Output.apk, "unaligned.apk"),
        Vexel.Project.assets
    )

    vexelLog("Aligning APK...", "BUILD")
    Build.alignApk()

    Build.signApk()

    makeDir(Vexel.Output.finalApk)

    Files.copy(
        File(path(Vexel.Output.apk, "final.apk")).toPath(),
        File(path(Vexel.Output.finalApk, "${appName}-debug.apk")).toPath(),
        StandardCopyOption.REPLACE_EXISTING
    )

    val elapsed = (System.currentTimeMillis() - start) / 1000.0

    vexelLog("Build Successful in %.2f sec.".format(elapsed), "INFO")
    vexelLog("APK saved at: ${path(Vexel.Output.finalApk,"${appName}-debug.apk")}", "INFO")
}
// ============================================================
//                         Builder.kt
// ============================================================
package app.pie.vexel

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun makeFileSystem() {
    if (checkDir(Vexel.Output.dir)) {
        vexelLog("Cleaning...", "INFO")
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

    makeFileSystem()

    Build.updateManifest(Vexel.Project.manifest, Vexel.Output.manifest, config)

    vexelLog("Compile resources...", "BUILD")
    Build.runAapt2Compile()

    vexelLog("Link resources...", "BUILD")
    Build.runAapt2Link()

    vexelLog("Compile Java...", "BUILD")
    Build.runJavaCompile()

    // Build native
    buildNative()

    vexelLog("Convert to DEX...", "BUILD")
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
    Build.addToZip(
        path(Vexel.Output.apk, "unaligned.apk"),
        Vexel.Project.dynLib
	)

    // Add Compiled Dynamic Libraries
    Build.addToZip(
        path(Vexel.Output.apk, "unaligned.apk"),
        Vexel.Output.dynLib
    )

    // Add Assets
    Build.addToZip(
        path(Vexel.Output.apk, "unaligned.apk"),
        Vexel.Project.assets
    )

    vexelLog("Align APK...", "BUILD")
    Build.alignApk()

    vexelLog("Sign APK...", "BUILD")
    Build.signApk()

    makeDir(Vexel.Output.finalApk)

    Files.copy(
        File(path(Vexel.Output.apk, "final.apk")).toPath(),
        File(path(Vexel.Output.finalApk, "Debug.apk")).toPath(),
        StandardCopyOption.REPLACE_EXISTING
    )

    val elapsed = (System.currentTimeMillis() - start) / 1000.0

    vexelLog("Build Successful in %.2f sec".format(elapsed), "INFO")
    vexelLog("APK saved at: ${path(Vexel.Output.finalApk,"Debug.apk")}", "INFO")
}
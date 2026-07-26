// ============================================================
//						  BuildTools.kt
// ============================================================
package app.pie.vexel

import com.android.tools.r8.D8
import com.android.apksigner.ApkSignerTool

import java.io.File

object BuildTools {
	private val isWindows = Vexel.Env.platform == "x64-windows"
    private val javac by lazy { findJavaTool("javac") }
	private val keytool by lazy { findJavaTool("keytool") }
	
	private fun findJavaTool(name: String): String {
		val javaHome = Vexel.Config.javaHome
		
		if (javaHome != null) {
			val exe = if (isWindows) "$name.exe" else name
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
	
	private fun ensureCached(pathInJar: String, outputName: String): String {
		val output = path(Vexel.Cache.buildTools, outputName)

		if (!checkFile(output)) {
    		extractFile(Vexel.Env.currentJarPath, pathInJar, Vexel.Cache.buildTools)
		}

		return output
	}
	
	private fun makeExecutable(path: String) {
		if (!isWindows) {
			val binary = File(path)
			if (!binary.canExecute() && !binary.setExecutable(true, false)) {
    			vexelThrow(ErrorCode.FILE_PERMISSION_FAILED,
    			"Failed to add execute permission: $path",
    			"Check that you own the file and have permission to modify its attributes."
    			)
			}
		}
	}
	
	private fun getBundledTool(name: String): String {
    	val exe = if (isWindows) "$name.exe" else name
    	val path = ensureCached(path("vexel-tools", Vexel.Env.platform, exe), exe)
    	makeExecutable(path)
    	return path
	}
	
	private fun resolveTool(path: String?, name: String): String {
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
	
	fun runAapt2(args: List<String>) =
    	runCmd(listOf(resolveTool(Vexel.Config.aapt2, "aapt2")) + args)
	
	fun runApkSigner(args: List<String>) =
		ApkSignerTool.main(args.toTypedArray())
		
	fun runD8(args: List<String>) =
		D8.main(args.toTypedArray())
	
	fun runZipalign(args: List<String>) =
    	runCmd(listOf(resolveTool(Vexel.Config.zipalign, "zipalign")) + args)

	fun runJavac(args: List<String>) =
		runCmd(listOf(javac) + args)
		
	fun runKeytool(args: List<String>) =
		runCmd(listOf(keytool) + args)
		
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
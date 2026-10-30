// ============================================================
//						    Utils.kt
// ============================================================
package app.cc.vexel

import java.io.File
import java.io.IOException
import java.io.FileNotFoundException

import java.io.FileWriter
import java.io.FileOutputStream

import java.nio.file.Path

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import kotlin.system.exitProcess

import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters

// ============================================================
// Path Utils
// ============================================================

fun path(vararg parts: String): String =
	Path.of(parts.first(), *parts.drop(1).toTypedArray()).toString()

// ============================================================
// Logging
// ============================================================

// ErrorCodes
internal object ErrorCode {
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

fun textFormat(message: String, color: String): String {
	if (Vexel.Config.enableColor == true) {
		return "${color}${message}${Vexel.Color.Reset}"
	} else {
		return message
	}
}

fun vexelLog(message: String, tag: String = "NONE", exit: Boolean = true) {
	val upperTag = tag.uppercase()
	
	val zoneId = java.util.TimeZone.getDefault().id
	val zone = java.time.ZoneId.of(zoneId)
	val time = java.time.ZonedDateTime.now(zone)
      .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
	
	val parts = mutableListOf<String>()

	if (Vexel.Config.enableTimeStamp) { parts.add("[$time] ") }
	if (Vexel.Config.enableColor) { parts.add("${Vexel.Color.Bold}${Vexel.Color.Blue}[VEXEL]${Vexel.Color.Reset} ") }
	else { parts.add("[VEXEL] ") }

	if (upperTag != "NONE") {
		if (Vexel.Config.enableColor) {
			val color = Vexel.Color.tagColors[upperTag] ?: Vexel.Color.White
			parts.add("${color}[$upperTag]${Vexel.Color.Reset} ")
		} else {
			parts.add("[$upperTag] ")
		}
	}

	parts.add(message)
	println(parts.joinToString(""))

	if (upperTag == "CRASH" && exit) {
		kotlin.system.exitProcess(1)
	}
}

fun vexelThrow(
	errorCode: String, errorMessage: String, solution: String? = null,
	cause: Throwable? = null, debugInfo: String? = null
): Nothing {
    val useColor = Vexel.Config.enableColor
    
    val zoneId = java.util.TimeZone.getDefault().id
    val zone = java.time.ZoneId.of(zoneId)
    val time = java.time.ZonedDateTime.now(zone)
      .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
      
    val timeStamp = if (Vexel.Config.enableTimeStamp) {
    	"[${time}] "
	} else {
    	""
	}
    
    val reset = if (useColor) Vexel.Color.Reset else ""
    val red = if (useColor) Vexel.Color.Red else ""
    val yellow = if (useColor) Vexel.Color.Yellow else ""
    val bold = if (useColor) Vexel.Color.Bold else ""

    println("${timeStamp}${red}${bold}[VEXEL] [CRASH] [$errorCode]$reset")
    println()

    println("${bold}Error:$reset")
    println(errorMessage)

    if (!solution.isNullOrBlank()) {
        println()
        println("${yellow}${bold}Possible solution:$reset")
        println(solution)
    }

    if (Vexel.Config.enableDebugMode) {
        debugInfo?.let {
            println()
            println("${red}${bold}Debug Information:$reset")
            println(debugInfo)
        }

        cause?.let {
            println()
            println("${red}${bold}Debug Stack Trace:$reset")
            cause.printStackTrace()
        }
    }

    exitProcess(1)
}

// ============================================================
// Process Execution
// ============================================================

data class ProcessResult(
    val output: String,
    val exitCode: Int
)

fun runCmd(cmd: List<String>, cwd: String? = null): String {
	var output = ""
	val debugCmdInfo = """
Command:
${cmd.joinToString(" ")}
	""".trimIndent()
	try {
		if (cmd.isEmpty()) {
			vexelThrow(ErrorCode.PROCESS_FAILED, "Command list cannot be empty")
		}

		val processBuilder = ProcessBuilder(cmd)
			.redirectErrorStream(true)

		if (cwd != null) {
			val directory = File(cwd)
			if (!directory.isDirectory) {
				vexelThrow(ErrorCode.DIR_NOT_FOUND, "Invalid working directory: $cwd")
			}
			processBuilder.directory(directory)
		}

		val process = processBuilder.start()
		val output = process.inputStream
			.bufferedReader()
			.use { it.readText() }

		val exitCode = process.waitFor()
		if (exitCode != 0) {
			vexelThrow(ErrorCode.PROCESS_FAILED,
				output.ifBlank { "External command failed. Exit Code: $exitCode" },
    			"Enable debug mode to see the executed command.",
    			debugInfo = debugCmdInfo
    		)
		}

		return output

	} catch (e: Exception) {
		vexelThrow(ErrorCode.PROCESS_FAILED,
			output.ifBlank { "External command failed." },
    		"Enable debug mode to see the executed command.",
    		cause = e, debugInfo = debugCmdInfo
		)
	}
}

fun runCmdResult(cmd: List<String>, cwd: String? = null): ProcessResult {
    if (cmd.isEmpty()) {
        vexelThrow(
            ErrorCode.PROCESS_FAILED,
            "Command list cannot be empty"
        )
    }

    val processBuilder = ProcessBuilder(cmd)
        .redirectErrorStream(true)

    if (cwd != null) {
        val directory = File(cwd)

        if (!directory.isDirectory) {
            vexelThrow(
                ErrorCode.DIR_NOT_FOUND,
                "Invalid working directory: $cwd"
            )
        }

        processBuilder.directory(directory)
    }

    val process = processBuilder.start()

    val output = process.inputStream
        .bufferedReader()
        .use { it.readText() }

    val exitCode = process.waitFor()

    return ProcessResult(output, exitCode)
}

// ============================================================
// Platform Detection
// ============================================================

fun getPlatform(): String {
	val arch = System.getProperty("os.arch").lowercase()
	val system = System.getProperty("os.name").lowercase()
	
	val is64Bit = arch.contains("64")
	val isAndroid = System.getenv("ANDROID_ROOT") != null
	val isArm = arch.contains("arm") || arch.contains("aarch")
	
	return when {
		isAndroid && isArm && is64Bit -> "aarch64-android"
		isAndroid && isArm -> "aarch32-android"
		system.contains("linux") && isArm && is64Bit -> "aarch64-linux"
		system.contains("linux") && is64Bit -> "x64-linux"
		system.contains("windows") -> "x64-windows"
		
		else -> "unknown"
	}

}

fun getJavaVersion(): String {
    return try {
        val version = System.getProperty("java.version")
        version ?: "Unknown"
    } catch (_: Exception) {
        "Not Found"
    }
}

// ============================================================
// Input helpers
// ============================================================

fun input(prompt: String): String {
    print(prompt)
    return readln().trim()
}

fun inputInt(prompt: String): Int {
    return input(prompt).toIntOrNull() ?: vexelThrow(ErrorCode.INVALID_INPUT, "Expected an integer")
}

// ============================================================
//	File Utilities
// ============================================================

fun writeFile(path: String, content: String) {
	try {
		val file = File(path)
		file.parentFile?.mkdirs()
		file.writeText(content)
		
	} catch (e: IOException) {
		vexelThrow(ErrorCode.FILE_WRITE_FAILED,
			"Unable to write file: $path",
			"Check that the directory exists and you have write permission.", cause = e
		)
	}

}

fun makeDir(path: String) {
	val dir = File(path)
	
	if (!dir.exists() && !dir.mkdirs()) {
		vexelThrow(ErrorCode.DIR_WRITE_FAILED,
			"Failed to create directory: $path",
			"Verify if a directory with the same name exists  and you have write permission"
		)
	}

}

fun removeDir(path: String) {
	val file = File(path)
	if (!file.exists()) { return }
	
	val success = file.deleteRecursively()
	if (!success) {
		vexelThrow(ErrorCode.DIR_DELETE_FAILED, 
			"Failed to remove directory: $path",
			"Verify you have delete permission"
		)
	}

}

fun findFiles(dir: String, ext: String, allowMissingDir: Boolean = false): List<String> {
	val result = mutableListOf<String>()
	val directory = File(dir)
	
	if (!directory.isDirectory) {
		if (allowMissingDir) { return result }
		vexelThrow(ErrorCode.DIR_NOT_FOUND, 
			"Directory does not exists: $dir",
			"Verify the directory exists and you have read permission"
		)
	}
	
	val normalizedExt = ext.removePrefix(".")
	
	directory.walkTopDown().forEach {
		if (it.isFile && it.extension == normalizedExt) {
			result.add(it.absolutePath)
		}
	}
	return result
}

fun checkDir(path: String): Boolean {
	return File(path).isDirectory
}

fun checkFile(path: String): Boolean {
	return File(path).isFile
}

// ============================================================
//	Manifest
// ============================================================

fun updateManifest(
    manifestPath: String,
    outManifestPath: String,
    cfg: Map<String, Any>
) {
    if (!checkFile(manifestPath)) {
        vexelThrow(
            ErrorCode.FILE_NOT_FOUND,
            "Failed to load AndroidManifest.xml",
            "Check that AndroidManifest.xml exists."
        )
        return
    }
	
	val build = cfg["build"] as? Map<*, *> ?: emptyMap<String, Any>()

    val androidNs = Namespace.getNamespace(
        "android",
        "http://schemas.android.com/apk/res/android"
    )

    try {
        val document = SAXBuilder().build(File(manifestPath))
        val root = document.rootElement

        // ---------------- APP ----------------

        val app = build["app"] as? Map<*, *>

        app?.let {
            it["id"]?.let { value ->
                root.setAttribute(
                    "package",
                    value.toString()
                )
            }

            it["buildCode"]?.let { value ->
                root.setAttribute(
                    "versionCode",
                    value.toString(),
                    androidNs
                )
            }

            it["version"]?.let { value ->
                root.setAttribute(
                    "versionName",
                    value.toString(),
                    androidNs
                )
            }

            val application = root.getChild("application")

            if (application != null) {
                it["name"]?.let { value ->
                    application.setAttribute(
                        "label",
                        value.toString(),
                        androidNs
                    )
                }
            }
        }

        // ---------------- SDK ----------------

        val sdk = build["sdk"] as? Map<*, *>

        sdk?.let {
            val usesSdk = root.getChild("uses-sdk")
                ?: Element("uses-sdk").also {
                    root.addContent(it)
                }

            it["minSdk"]?.let { value ->
                usesSdk.setAttribute(
                    "minSdkVersion",
                    value.toString(),
                    androidNs
                )
            }

            it["targetSdk"]?.let { value ->
                usesSdk.setAttribute(
                    "targetSdkVersion",
                    value.toString(),
                    androidNs
                )
            }
        }

        // ---------------- SAVE ----------------

        val outputter = XMLOutputter(
            Format.getPrettyFormat().apply {
                omitDeclaration = false
                encoding = "UTF-8"
            }
        )

        val outFile = File(outManifestPath)
        outFile.parentFile?.mkdirs()

        FileWriter(outFile).use { writer ->
            outputter.output(document, writer)
        }

    } catch (e: Exception) {
        vexelThrow(
            ErrorCode.INVALID_MANIFEST,
            "Failed to parse AndroidManifest.xml",
            "Verify that the manifest is valid XML and contains the required elements.",
            cause = e
        )
    }
}

// ============================================================
//	Zip Utilities
// ============================================================

fun addToZip(zipPath: String, filePath: String, innerPath: String = "", okIfMissing: Boolean = true) {
	val sourceFile = File(filePath)
		
	// Check source file
	if (!checkFile(filePath) && !checkDir(filePath)) {
		if (okIfMissing) { return }
		else {
			vexelThrow(ErrorCode.FILE_NOT_FOUND,
			"Failed to find source file: $filePath",
			"Check that the file path is correct and have permission to read it."
			)
		}
	}
		
	if (!File(zipPath).exists()) {
		vexelThrow(ErrorCode.FILE_NOT_FOUND,
		"Failed to find zip file: $zipPath",
		"Check that the file path is correct and have permission to read it."
		)
	}
		
	try {
		val zipFile = ZipFile(zipPath)
		if (sourceFile.isDirectory) {
			zipFile.addFolder(sourceFile)
		} else {
			zipFile.addFile(sourceFile)
		}
			
	} catch (e: ZipException) {
		vexelThrow(ErrorCode.ZIP_ERROR,
		"Failed to add file to zip",
		"Check that both zip and file exists.",
		debugInfo = "ZipPath: $zipPath \nFilePath: $filePath"
		)
	}
}

fun extractZip(zipPath: String,outputDirectory: String) {
    if (!checkFile(zipPath)) {
        vexelThrow(ErrorCode.FILE_NOT_FOUND,
        	"Failed to find zip archive: $zipPath",
        	"Verfiy that the zip archive exists and it have read permission"
        )
    }
   
    val normalizedOutput = File(outputDirectory).canonicalFile
    try {
        // Create output directory
        val outDir = File(outputDirectory)
		if (!outDir.exists() && !outDir.mkdirs()) {
    		vexelThrow(
        		ErrorCode.DIR_WRITE_FAILED,
        		"Failed to create directory: $outputDirectory",
        		"Check write permissions."
    		)
		}
		
        val zipFile = ZipFile(zipPath)

        // Validate entries before extraction (Zip Slip protection)
        for (header in zipFile.getFileHeaders()) {
            val entryName = header.fileName
            val resolvedPath = File(outputDirectory, entryName)
            
            if (!resolvedPath.canonicalPath.startsWith(normalizedOutput.path)) {
                vexelThrow(ErrorCode.ZIP_ERROR,
                	"Unsafe zip entry detected: $entryName",
                	"The Vexel archive may be corrupted. Reinstall Vexel or download a fresh copy."
                )
            }

            // Prevent overwriting existing files
            if (checkFile(resolvedPath.path)) {
                vexelThrow(ErrorCode.ZIP_ERROR,
                	"File already exists: $resolvedPath.path",
                	"Remove or rename the existing file before extracting archive"
                )
            }
        }

        // Extraction
        zipFile.extractAll(outputDirectory)

    }
    catch (e: Exception) {
    	vexelThrow(
        	ErrorCode.ZIP_ERROR,
        	"Failed to extract ZIP archive.",
        	"Check that the destination directory is writable and has enough free space.",
        	cause = e
    	)
	}

	catch (e: Exception) {
    	vexelThrow(
        	ErrorCode.ZIP_ERROR,
        	"Failed to extract ZIP archive: $zipPath",
        	"The archive may be corrupted or unreadable. Try downloading it again.",
        	cause = e
    	)
	}
}


fun extractFile(zipPath: String, insideZipPath: String, extractPath: String) {
    if (!checkFile(zipPath)) {
    	vexelThrow(
        	ErrorCode.FILE_NOT_FOUND,
        	"ZIP archive not found: $zipPath",
        	"Verify that the archive exists and you have permission to read it."
    	)
	}
	
    val zip = File(zipPath)
    
    if (!checkFile(zipPath)) {
    	vexelThrow(
        	ErrorCode.FILE_NOT_FOUND,
        	"ZIP archive not found: $zipPath",
        	"Verify that the archive exists and you have permission to read it."
    	)
	}

    val destination = File(extractPath)
    destination.mkdirs()

    try {
        val zipFile = ZipFile(zip)
        val header = zipFile.getFileHeader(insideZipPath)
            ?: vexelThrow(ErrorCode.ZIP_ERROR,
            "Path not found inside zip: $insideZipPath",
            "Verify that the path inside zip archive exists."
        )

        zipFile.extractFile(header, destination.absolutePath)
        
        val extracted = File(destination, insideZipPath)
        val finalFile = File(destination, extracted.name)
        val root = insideZipPath.substringBefore('/')
        
        extracted.copyTo(finalFile, overwrite = true)
        extracted.delete()
        File(destination, root).deleteRecursively()
        
    } catch (e: ZipException) {
        vexelThrow(ErrorCode.ZIP_ERROR,
        	"Failed to extract $insideZipPath from $zipPath",
        	"The archive may be corrupted or unreadable. Try downloading it again.", cause = e
        )
    }
}

fun extractFolder(zipPath: String, folderInZip: String, extractPath: String) {
    try {
    	if (!checkFile(zipPath)) {
    		vexelThrow(
        		ErrorCode.FILE_NOT_FOUND,
        		"ZIP archive not found: $zipPath",
        		"Verify that the archive exists and you have permission to read it."
    		)
		}
    	val zipFile = ZipFile(zipPath)
    	File(extractPath).mkdirs()
    	val prefix = folderInZip.trimEnd('/') + "/"

    	zipFile.fileHeaders
        	.filter { it.fileName.startsWith(prefix) && !it.isDirectory }
        	.forEach { header -> zipFile.extractFile(header, extractPath) }
    } catch (e: ZipException) {
    	vexelThrow(ErrorCode.ZIP_ERROR,
        	"Failed to extract $folderInZip from $zipPath",
        	"The archive may be corrupted or unreadable. Try downloading it again.", cause = e
        )
    }
}

// ============================================================
//	Internal Resolver
// ============================================================

internal fun asConfigMap(value: Any?): Map<String, Any>? {
    val map = value as? Map<*, *> ?: return null

    if (map.keys.any { it !is String }) {
        return null
    }

    return map.entries.associate { (key, value) ->
        key as String to value as Any
    }
}
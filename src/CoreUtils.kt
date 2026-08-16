// ============================================================
//						 CoreUtils.kt
// ============================================================
package app.pie.vexel

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import kotlin.system.exitProcess

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException

import org.tomlj.Toml

// ============================================================
// Path Utils
// ============================================================

fun path(vararg parts: String): String =
	Path.of(parts.first(), *parts.drop(1).toTypedArray()).toString()

// ============================================================
// Logging
// ============================================================

fun vexelLog(message: String, tag: String = "NONE", exit: Boolean = true) {
	val upperTag = tag.uppercase()
	
	val zoneId = java.util.TimeZone.getDefault().id
	val zone = java.time.ZoneId.of(zoneId)
	val time = java.time.ZonedDateTime.now(zone)
      .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
	
	val parts = mutableListOf<String>()

	if (Vexel.Config.enableTimeStamp) { parts.add("[$time] ") }
	if (Vexel.Config.enableColor) { parts.add("${ANSI.BOLD}${ANSI.BLUE}[VEXEL]${ANSI.RESET} ") }
	else { parts.add("[VEXEL] ") }

	if (upperTag != "NONE") {
		if (Vexel.Config.enableColor) {
			val color = ANSI.tagColors[upperTag] ?: ANSI.WHITE
			parts.add("${color}[$upperTag]${ANSI.RESET} ")
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
    
    val reset = if (useColor) ANSI.RESET else ""
    val red = if (useColor) ANSI.RED else ""
    val yellow = if (useColor) ANSI.YELLOW else ""
    val bold = if (useColor) ANSI.BOLD else ""

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

// ============================================================
// File Utilities
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

// ============================================================
// Config Loader (TOML)
// ============================================================

fun loadConfig(path: String): MutableMap<String, MutableMap<String, Any>> {
	val file = File(path)
	
	if (!file.isFile) {
		vexelThrow(ErrorCode.FILE_NOT_FOUND,
			"Unable to find configuration file: $path",
			"Verify the file exists and you have permission to read it."
		)
	}
	
	val toml = try {
		Toml.parse(file.toPath())
	} catch (e: Exception) {
		vexelThrow(ErrorCode.INVALID_CONFIG, 
			"Unable to parse configuration file: $path",
			"Verify that the config file is correct",
			cause = e
		)
	}
	
	if (toml.hasErrors()) {
		val errors = toml.errors().joinToString("\n")
		vexelThrow(ErrorCode.INVALID_CONFIG,
			"Unable to parse configuration file: $path",
			"Verify that the config file is correct",
			debugInfo = errors
		)
	}
	
	val result = mutableMapOf<String, MutableMap<String, Any>>()
	
	for (key in toml.keySet()) {
		val table = toml.getTable(key) ?: continue
		val sectionMap = mutableMapOf<String, Any>()

		for (entryKey in table.keySet()) {
			val value = table.get(entryKey)
			if (value != null) {
    			sectionMap[entryKey] = value
			}
		}
		
		result[key] = sectionMap
	}
	return result
}

// ============================================================
// Zip Extractor
// ============================================================

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
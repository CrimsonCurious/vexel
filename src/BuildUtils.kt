// ============================================================
//						  BuildUtils.kt
// ============================================================
package app.pie.vexel

import java.io.File
import java.io.FileWriter
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.FileNotFoundException

import java.security.SecureRandom

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters

import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter

data class KeyStoreResult(
	val keystorePath: String,
	val password: String
)

object Build {
	// AAPT2
	fun runAapt2Compile() {
		BuildTools.runAapt2(listOf("compile", "--dir", Vexel.Project.res, "-o", Vexel.Output.res))
	}
	fun runAapt2Link() {
		val flatFiles = findFiles(Vexel.Output.res, ".flat")
		if (flatFiles.isEmpty()) { return }
		
		BuildTools.runAapt2(listOf("link",
			"-o", path(Vexel.Output.apk, "base.apk"),
			"-I", BuildTools.getSdk(),
			"--manifest", Vexel.Output.manifest,
			"--java", Vexel.Output.gen
			) + flatFiles
		)
	}
	
	// Java
	fun runJavaCompile() {
		val javaFiles = findFiles(path(Vexel.Project.dir, "java"), ".java")
		val genFiles = findFiles(Vexel.Output.gen, ".java")
		val jarFiles = mutableListOf<String>()
		
		if (checkDir(Vexel.Project.jarLib)) {
			jarFiles.addAll(findFiles(Vexel.Project.jarLib, ".jar"))
			for (jar in jarFiles) {
				vexelLog("Loaded ${File(jar).name}", "INFO")
			}
		}
		val separator = File.pathSeparator
		val classpath = (listOf(BuildTools.getSdk()) + jarFiles).joinToString(separator)
		
		BuildTools.runJavac(listOf(
			"-d", Vexel.Output.classes,
			"-classpath", classpath
			) + javaFiles + genFiles
		)
	}
	
	// Dex
	fun convertDex() {
		val classFiles = findFiles(Vexel.Output.classes, ".class")
		if (classFiles.isEmpty()) {
			vexelThrow(ErrorCode.FILE_NOT_FOUND,
			"Unable to find class files",
			"Verify that the java code exists and it is compiled successfully"
			)
		}
		
		val jarFiles = mutableListOf<String>()
		
		if (checkDir(Vexel.Project.jarLib)) {
			jarFiles.addAll(findFiles(Vexel.Project.jarLib, ".jar"))
		}
		
		BuildTools.runD8(listOf(
			"--lib", BuildTools.getSdk(),
			"--min-api", "21",
			"--output", Vexel.Output.dex
			) + classFiles + jarFiles
		)
	}
	
	// AddToZip
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
	
	// Zipalign
	fun alignApk() {
		BuildTools.runZipalign(listOf("-f", "4",
			path(Vexel.Output.apk, "unaligned.apk"),
			path(Vexel.Output.apk, "final.apk")
			)
		)
	}
	
	// Generate KeyStore
	fun generateKeystore(outKeyDir: String, key: String? = null, length: Int = 24): KeyStoreResult {
		val keystorePath = path(outKeyDir, "debug.keystore")
		
		if (checkFile(path(outKeyDir, "debug.keystore.pass"))) {
			val file = File(path(outKeyDir, "debug.keystore.pass"))
			val key: String = file.readText() 
		}
		
		val password = if (key != null) { key } else {
			vexelLog("Generating new password...", "INFO")
			
			val chars = "abcdefghijklmnopqrstuvwxyz" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789"
			val random = SecureRandom()
			
			buildString {
				repeat(length) {
					append(chars[random.nextInt(chars.length)])
				}
			}
		}
		
		vexelLog("Generating keystore...", "INFO")
		
		BuildTools.runKeytool(listOf(
			"-genkeypair", "-v",
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
		
		writeFile(path(outKeyDir, "debug.keystore.pass"), password)
		vexelLog("Keystore created", "INFO")
		
		return KeyStoreResult(keystorePath, password)
	}
	
	// ApkSigner
	fun signApk() {
		val result = generateKeystore(Vexel.Output.key)
		
		BuildTools.runApkSigner(listOf(
			"sign", "--ks", result.keystorePath,
			"--ks-pass", "pass:${result.password}",
			path(Vexel.Output.apk, "final.apk")
			)
		)
	}
	
	// Generate Manifest
	fun updateManifest(manifestPath: String, outManifestPath: String, cfg: Map<String, Any>) {
		if (!checkFile(manifestPath)) {
			vexelThrow(ErrorCode.FILE_NOT_FOUND,
			"Failed to load AndroidManifest.xml",
			"Check that AndroidManifest.xml exists."
			)
			return
		}

		val androidNs = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android")
	
		try {
			val document = SAXBuilder().build(File(manifestPath))
			val root = document.rootElement
		
			// ---------------- APP ----------------
			val app = cfg["app"] as? Map<*, *>
		
			app?.let {
				it["package"]?.let { value ->
				root.setAttribute("package", value.toString()) }

				it["version_code"]?.let { value ->
				root.setAttribute("versionCode", value.toString(), androidNs) }

				it["version_name"]?.let { value ->
				root.setAttribute("versionName", value.toString(), androidNs) }

				val application = root.getChild("application")

				if (application != null) {
					it["name"]?.let { value -> application.setAttribute("label", value.toString(), androidNs) }
				}
			}

			// ---------------- SDK ----------------
			val sdk = cfg["sdk"] as? Map<*, *>

			sdk?.let {
				val usesSdk = root.getChild("uses-sdk")
				?: Element("uses-sdk").also { root.addContent(it) }

				it["min"]?.let { value ->
				usesSdk.setAttribute("minSdkVersion", value.toString(), androidNs) }

				it["target"]?.let { value ->
				usesSdk.setAttribute("targetSdkVersion", value.toString(), androidNs) }
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
		
			FileWriter(outFile).use { writer -> outputter.output(document, writer) }
			
		} catch (e: Exception) {
			vexelThrow(ErrorCode.INVALID_MANIFEST,
			"Failed to parse AndroidManifest.xml",
			"Verify that the manifest is valid XML and contains the required elements.",
			cause = e
			)
		}
	}
}
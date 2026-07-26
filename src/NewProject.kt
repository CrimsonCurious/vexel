// ============================================================
//					  NewProject.kt
// ============================================================
package app.pie.vexel

data class ProjectConfig(
	val template: Int, val appName: String,
	val packageName: String, val sdkMin: Int, val sdkTarget: Int
) {
	val nativePackage = packageName.replace(".", "_")
	val packageDir = path(
		Vexel.Env.projectDir,
		"src",
		"java",
		*packageName.split(".").toTypedArray()
	)
}

// ============================================================
// Template Writer
// ============================================================

fun writeTemplate(cfg: ProjectConfig) {
	writeFile(Vexel.Project.manifest, DefaultData.getManifest(cfg.appName))
	writeFile(path(Vexel.Project.res, "layout", "activity_main.xml"), DefaultData.getXMLMain())
	
	if (!checkFile(Vexel.Cache.defaultIcons)) {
		extractFile(Vexel.Env.currentJarPath, "vexel-tools/default_icons.zip", Vexel.Cache.buildTools)
	}
	extractZip(Vexel.Cache.defaultIcons, Vexel.Project.res)
	when (cfg.template) {

		// Native Activity
		1 -> {
			writeFile(Vexel.Project.vexelBuild, DefaultData.getVexelBuild(1, cfg.appName, cfg.packageName, cfg.sdkMin, cfg.sdkTarget))
			writeFile(path(cfg.packageDir, "MainActivity.java"), DefaultData.getJavaActivity(1, cfg.packageName))
			makeDir(Vexel.Project.cpp)

			writeFile(path(Vexel.Project.cpp, "src", "main.cpp"), DefaultData.getCppMain(cfg.nativePackage))

			writeFile(path(Vexel.Project.cpp, "native.build"), DefaultData.getNativeBuild())
		}

		// Java Activity
		2 -> {
			writeFile(Vexel.Project.vexelBuild, DefaultData.getVexelBuild(2, cfg.appName, cfg.packageName, cfg.sdkMin, cfg.sdkTarget))
			writeFile(path(cfg.packageDir, "MainActivity.java"), DefaultData.getJavaActivity(2, cfg.packageName))
		}

		else -> {
			vexelThrow(ErrorCode.INVALID_INPUT,
			"Invalid temple selected"
			)
		}
	}
}

// ============================================================
// Package Cleaner
// ============================================================

fun cleanPackageName(packageName: String): String {
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

// ============================================================
// Interactive Creator
// ============================================================

fun askProjectConfig(template: Int): ProjectConfig {
	print("Project Name: ")
	val appName = readln().trim()

	if (appName.isEmpty()) {
		vexelLog("Empty Project Name provided", "CRASH")
	}

	print("Package Name: ")
	val packageName = cleanPackageName(readln().trim())

	if (packageName.isEmpty()) {
		vexelLog("Invalid package name", "CRASH")
	}

	print("Minimum SDK: ")
	val sdkMin = readln().trim().toIntOrNull()
		?: vexelThrow(ErrorCode.INVALID_INPUT,
		   "Minimum SDK must be integer."
		   )
	require(sdkMin > 0)

	print("Target SDK: ")
	val sdkTarget = readln().trim().toIntOrNull()
		?: vexelThrow(ErrorCode.INVALID_INPUT,
		   "Target SDK must be integer."
		   )
	require(sdkTarget >= sdkMin)
	
	return ProjectConfig(template, appName, packageName, sdkMin, sdkTarget)
}

// ============================================================
// Entry
// ============================================================

fun create(arg: String? = null) {
	vexelLog("Creating project...", "INFO")

	val templates = mapOf("1" to 1, "2" to 2, "native" to 1, "java" to 2)
	val template: Int
	val cfg: ProjectConfig
	
	if (arg != null) {
		template = templates[arg.lowercase()]
		  ?: vexelThrow(ErrorCode.INVALID_INPUT,
			"Invalid temple selected"
			)
		cfg = ProjectConfig(template, "Hello World", "com.example.app", 29, 34)
		
	} else {
		println("Choose Template:")
		println("  [1] Native Activity")
		println("  [2] Java Activity")

		print("Template: ")

		template = readln().trim().toIntOrNull()
		  ?: vexelThrow(ErrorCode.INVALID_INPUT,
			"Invalid template number selected"
			)
		cfg = askProjectConfig(template)
	}

	if (template !in listOf(1, 2)) {
		vexelThrow(ErrorCode.INVALID_INPUT,
			"Invalid temple selected"
		)
	}
	makeDir(Vexel.Project.jarLib)
	writeTemplate(cfg)
	vexelLog("Project created successfully", "INFO")
}
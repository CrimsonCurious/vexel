// ============================================================
//				        NewProject.kt
// ============================================================
package app.cc.vexel

data class Project(
	val projectName: String, val packageName: String,
	val minSDK: Int, val targetSDK: Int, val language: String
) {
	val nativePackage = packageName.replace(".", "_")
	val MainLanguage = if (language.contains("java", ignoreCase = true)) { "java" } else { "kotlin" }
	val packageDir = path(
		Vexel.Env.projectDir, "src", MainLanguage,
		*packageName.split(".").toTypedArray()
	)
}

object Template {
	const val PROJECT_JAVA = "JAVA"
	const val PROJECT_KOTLIN = "KOTLIN"
	const val PROJECT_CPP_JAVA = "CPP_JAVA"
	const val PROJECT_CPP_KOTLIN = "CPP_KOTLIN"
}

// ============================================================
// Template Writer
// ============================================================

fun writeTemplate(project: Project) {
	makeDir(Vexel.Project.jarLib)

    writeFile(
        Vexel.Project.manifest,
        TemplatesFiles.getManifest(project.projectName)
    )

    writeFile(
        path(Vexel.Project.res, "layout", "activity_main.xml"),
        TemplatesFiles.getXMLMain()
    )

    writeDefaultIcons()

    when (project.language) {
        Template.PROJECT_JAVA -> writeJavaProject(project, false)
        Template.PROJECT_KOTLIN -> writeKotlinProject(project, false)
        Template.PROJECT_CPP_JAVA -> writeJavaProject(project, true)
        Template.PROJECT_CPP_KOTLIN -> writeKotlinProject(project, true)

        else -> vexelThrow(
            ErrorCode.INVALID_INPUT,
            "Invalid project template"
        )
	}

	vexelLog("Successfully created project", "INFO")
}

private fun writeJavaProject(project: Project, native: Boolean) {
    writeFile(
        Vexel.Project.vexelVcl,
        TemplatesFiles.getVexelBuild(
            if (native) 1 else 2,
            project.projectName,
            project.packageName,
            project.minSDK,
            project.targetSDK
        )
    )

    writeFile(
        path(project.packageDir, "MainActivity.java"),
        TemplatesFiles.getJavaActivity(
            if (native) 1 else 2,
            project.packageName
        )
    )

    if (native) {
        writeNativeProject(project)
    }
}

private fun writeKotlinProject(project: Project, native: Boolean) {
    writeFile(
        Vexel.Project.vexelVcl,
        TemplatesFiles.getVexelBuild(
            if (native) 1 else 2,
            project.projectName,
            project.packageName,
            project.minSDK,
            project.targetSDK
        )
    )

    writeFile(
        path(project.packageDir, "MainActivity.kt"),
        TemplatesFiles.getKotlinActivity(project.packageName)
    )

    if (native) {
        writeNativeProject(project)
    }
}

private fun writeNativeProject(project: Project) {
    makeDir(Vexel.Project.cpp)

    writeFile(
        path(Vexel.Project.cpp, "src", "main.cpp"),
        TemplatesFiles.getCppMain(project.nativePackage)
    )

    writeFile(
        path(Vexel.Project.cpp, "native.vcl"),
        TemplatesFiles.getNativeBuild()
    )
}

private fun writeDefaultIcons() {
    if (!checkFile(Vexel.Cache.defaultIcons)) {
        extractFile(
            Vexel.Env.currentJarPath,
            "vexel-tools/default_icons.zip",
            Vexel.Cache.buildTools
        )
    }

    extractZip(
        Vexel.Cache.defaultIcons,
        Vexel.Project.res
    )
}

// ============================================================
// Templates
// ============================================================

private object TemplatesFiles {
	fun getManifest(appName: String): String {
		val androidManifest = """
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
    	android:icon="@mipmap/ic_launcher"
        android:label="$appName"
        android:theme="@android:style/Theme.NoTitleBar">

        <activity
            android:name=".MainActivity"
            android:exported="true">

            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>

        </activity>
    </application>
</manifest>
		""".trimIndent()
		
		return androidManifest
	}
	fun getJavaActivity(type: Int, packageName: String): String {
		return when(type) {
			1 -> """
package $packageName;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("native");
    }

    public native String getMessage();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        TextView text =
            findViewById(R.id.text);

        text.setText(getMessage());
    }
}
		""".trimIndent()
			2 -> """
package $packageName;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        TextView text =
            findViewById(R.id.text);

        text.setText("Hello, World!");
    }
}
		""".trimIndent()
       	 else -> error("Invalid activity type")
		}
	}
	
	fun getKotlinActivity(packageName: String): String {
		val MainActivity = """
package $packageName

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val text: TextView = findViewById(R.id.text)

        text.text = "Hello, World!"
    }
}
		""".trimIndent()
		return MainActivity
	}
	
	fun getXMLMain(): String {
	   val activityMain = """
<?xml version="1.0" encoding="utf-8"?>

<RelativeLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello, World!"
        android:textSize="24sp"
        android:layout_centerInParent="true"/>

</RelativeLayout>
		""".trimIndent()
		return activityMain
	}
	fun getCppMain (nativePackage: String): String {
	   val mainCpp = """
#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_${nativePackage}_MainActivity_getMessage(
    JNIEnv* env,
    jobject thiz
) {
    return env->NewStringUTF("Hello World");
}
		""".trimIndent()
		return mainCpp
	}
	fun getVexelBuild(type: Int, appName: String, packageName: String, sdkMin: Int, sdkTarget: Int): String {
	   return when(type) {
	   	1 -> """
build: {
    app: {
		id: "$packageName
        name: "$appName"
        version: "1.0.0"
        buildCode: 1
    }

    sdk: {
        minSdk: $sdkMin
        targetSdk: $sdkTarget
    }

    native: {
        enabled: true
		api: 29
		abi: [
			"arm64-v8a"
			"armeabi-v7a"
			"x86"
			"x86_64"
		]
		
		buildScript: "src/cpp/native.vcl"
	}
}
			""".trimIndent()
			2 -> """
build: {
    app: {
		id: "$packageName"
        name: "$appName"
        version: "1.0.0"
        buildCode: 1
    }

    sdk: {
        minSdk: $sdkMin
        targetSdk: $sdkTarget
    }

    native: {
        enabled: false
	}
}
			""".trimIndent()
       	 else -> error("Invalid template type")
		}
	}
	fun getNativeBuild(): String {
		val nativeBuild = """
libnative: {
	# Source files or directories.
	# Supported formats:
	#   cpp/src/
	#   cpp/src/*.c
	#   cpp/Main.c
	src: [
		"cpp/src/main.cpp"
	]
	
	# Static libraries files
	# staticLibs: ["cpp/src/libfoo.a"]

	# Additional header search directories.
	include: [
		"cpp/src"
	]

	# Enable C++ compilation.
	# Set to false to compile as C.
	cpp: true

	# System or user libraries to link.
	link: [
		"android"
		"log"
	]

	# Additional compiler and linker flags.
	# The flags -shared and -fPIC are added automatically.
	flags: [
		"-O2"
		"-Wall"
		"-s"
	]
}
		""".trimIndent()
		return nativeBuild
	}
}
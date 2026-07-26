// ============================================================
//                      DefaultData.kt
// ============================================================
package app.pie.vexel

object DefaultData {
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
[app]
name = "$appName"
package = "$packageName"
version_code = 1
version_name = 1.0

[sdk]
min = $sdkMin
target = $sdkTarget

[ndk]
enabled = true
api = 29
abi = "arm64-v8a armeabi-v7a x86_64"
build_script = "src/cpp/native.build"
			""".trimIndent()
			2 -> """
[app]
name = "$appName"
package = "$packageName"
version_code = 1
version_name = 1.0

[sdk]
min = $sdkMin
target = $sdkTarget

[ndk]
enabled = false
			""".trimIndent()
       	 else -> error("Invalid template type")
		}
	}
	
	fun getNativeBuild(): String {
		val nativeBuild = """
["lib-native"]
# Source files or directories.
# Supported formats:
#   cpp/src/
#   cpp/src/*.c
#   cpp/Main.c
src = "cpp/src"

# Additional header search directories.
include = "cpp/"

# Enable C++ compilation.
# Set to false to compile as C.
cpp = true

# System or user libraries to link.
link = "android log"

# Additional compiler and linker flags.
# The flags -shared and -fPIC are added automatically.
flags = "-O2 -Wall -s"

		""".trimIndent()
		return nativeBuild
	}
}
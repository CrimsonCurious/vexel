# ============================================================
#						 new_project.py
# ============================================================
from core_utils import *
from variables import *
	
def create():
	VexelLog("Creating a project...", "INFO")
	App_Name = input("Enter App Name: ")
	if App_Name == "":
		raise ValueError("Empty App Name provided")
		
	Package_Name = input("Enter Package Name: ").lower()
	
	Allowed = "abcdefghijklmnopqrstuvwxyz0123456789._"
	Cleaned = ""
	
	for Char in Package_Name:
		if Char in Allowed: Cleaned += Char
		else: VexelLog(f"Warning: '{Char}' is not allowed and was removed", "WARN")
		
	Parts = Cleaned.split(".")
	Parts = [p for p in Parts if p != ""]
	Package_Name = ".".join(Parts)
	

	if Package_Name == "":
		raise ValueError("Empty Package Name provided")
		
	package_path = Package_Name.replace(".", "/")
	Package_dir = path(Project_dir, "src", "java", *Package_Name.split("."))
	Native_Package = Package_Name.replace(".", "_")
	
	# Default Version Set Can be changed in vexel.build
	Version_Code = 1  
	Version_Name = "1.0"
	
	sdk_min = int(input("Enter Minimum SDK: "))
	if sdk_min == None:
		raise ValueError("Empty SDK minimum version provided")
		
	sdk_target = int(input("Enter Target SDK: "))
	if sdk_target == None:
		raise ValueError("Empty SDK target version provided")
	
	AndroidManifest = f"""
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <application
	android:label="{App_Name}"
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
	"""

	MainActivity = f"""
package {Package_Name};

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {{

    static {{
        System.loadLibrary("native");
    }}

    public native String getMessage();

    @Override
    protected void onCreate(Bundle savedInstanceState) {{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView text = findViewById(R.id.text);

        text.setText(getMessage());
    }}
}}
	"""

	Activity_Main = """
<?xml version="1.0" encoding="utf-8"?>

<RelativeLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <TextView
        android:id="@+id/text"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Loading..."
        android:textSize="24sp"
        android:layout_centerInParent="true"/>

</RelativeLayout>
	"""
	Main_Cpp = f"""
#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_{Native_Package}_MainActivity_getMessage(
    JNIEnv* env,
    jobject thiz
) {{
    return env->NewStringUTF("Hello World");
}}
	"""

	Vexel_Build_Template = f"""
[app]
name = {App_Name}
package = {Package_Name}
version_code = 1
version_name = 1.0

[sdk]
min = {sdk_min}
target = {sdk_target}

[ndk]
enabled = true
api = 29
abi = arm64-v8a armeabi-v7a x86_64
build_script = src/cpp/native.build
	"""
	Native_Build = f"""
[lib:native]
# Source files or directories.
# Supported formats:
#   cpp/src/
#   cpp/src/*.c
#   cpp/Main.c
src = cpp/src

# Additional header search directories.
include = cpp/

# Enable C++ compilation.
# Set to false to compile as C.
cpp = true

# System or user libraries to link.
link = android log

# Additional compiler and linker flags.
# The flags -shared and -fPIC are added automatically.
flags = -O2 -Wall -s
	"""
	mkdir(project['cpp'])
	mkdir(project['jar_lib'])
	write_file(project['manifest'], AndroidManifest)
	write_file(path(project['cpp'],"src", "main.cpp"), Main_Cpp)
	write_file(path(project['cpp'], "native.build"), Native_Build)
	write_file(project['vexel_build'], Vexel_Build_Template)
	write_file(path(Package_dir, "MainActivity.java"),MainActivity)
	write_file(path(project['res'], "layout", "activity_main.xml"), Activity_Main)
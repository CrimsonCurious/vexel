# ============================================================
#						 new_project.py
# ============================================================
from core_utils import *
from variables import *

temp_file = {
  1: path(Vexel_Home, "templates", "Native_Activity_Basic.zip"),
  2: path(Vexel_Home, "templates", "Java_Activity_Basic.zip"),
  3: path(Vexel_Home, "templates", "Java_Activity_Assets.zip"),
  4: path(Vexel_Home, "templates", "Java_Activity_Jar_Plugin.zip"),
  5: path(Vexel_Home, "templates", "Java_Activity_DL.zip"),
  6: path(Vexel_Home, "templates", "Native_Activity_GLES_Blue_Screen.zip"),
  7: path(Vexel_Home, "templates", "Native_Activity_GLES_Triangle.zip")
}

def get_template(value):
	temp = temp_file.get(value)
	if temp is None: VexelLog("Invalid template", "CRASH")
		
	cwd = project['dir']
	import zipfile
	
	with zipfile.ZipFile(temp, "r") as zip_ref:
		zip_ref.extractall(cwd)

def create_basic(template):
	App_Name = input("Project Name: ")
	if App_Name == "": VexelLog("Empty Project Name provided", "CRASH")
		
	Package_Name = input("Package Name: ").lower()
	if Package_Name == "": VexelLog("Empty Package Name provided", "CRASH")
	
	Allowed = "abcdefghijklmnopqrstuvwxyz0123456789._"
	Cleaned = ""
	for Char in Package_Name:
		if Char in Allowed: Cleaned += Char
		else: VexelLog(f"Warning: '{Char}' is not allowed and was removed", "WARN")
		if not Package_Name: VexelLog("Empty package name", "CRASH")
		
	Parts = Cleaned.split(".")
	Parts = [p for p in Parts if p != ""]
	Package_Name = ".".join(Parts)
			
	package_path = Package_Name.replace(".", "/")
	Package_dir = path(Project_dir, "src", "java", *Package_Name.split("."))
	Native_Package = Package_Name.replace(".", "_")
			
	# Default Version Set Can be changed in vexel.build
	Version_Code = 1  
	Version_Name = "1.0"	
			
	value = input("Minimum SDK: ").strip()
	if not value: VexelLog("Empty SDK minimum version provided", "CRASH")
	try: sdk_min = int(value) 
	except ValueError: VexeLog("Minimum SDK must be a integer", "CRASH")
	
	value = input("Target SDK: ")
	if not value: VexelLog("Empty SDK target version provided", "CRASH")
	try: sdk_target = int(value)
	except ValueError: VexeLog("Target SDK must be a integer", "CRASH")
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
	
	MainActivity2 = f"""
package {Package_Name};

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {{
	@Override
	protected void onCreate(Bundle savedInstanceState) {{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		TextView text = findViewById(R.id.text);

		text.setText("Hello, World!");
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
key = Auto
# You can enter your own key or use Auto to automatically generated it

[sdk]
min = {sdk_min}
target = {sdk_target}

[ndk]
enabled = true
api = 29
abi = arm64-v8a armeabi-v7a x86_64
build_script = src/cpp/native.build
	"""
	
	Vexel_Build_Template2 = f"""
[app]
name = {App_Name}
package = {Package_Name}
version_code = 1
version_name = 1.0
key = Auto
# You can enter your own key or use Auto to automatically generated it

[sdk]
min = {sdk_min}
target = {sdk_target}

[ndk]
enabled = false
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
	mkdir(project['jar_lib'])
	write_file(project['manifest'], AndroidManifest)
	write_file(path(project['res'], "layout", "activity_main.xml"), Activity_Main)
	
	if template == 1:
		write_file(project['vexel_build'], Vexel_Build_Template)
		write_file(path(Package_dir, "MainActivity.java"), MainActivity)
		mkdir(project['cpp'])
		write_file(path(project['cpp'],"src", "main.cpp"), Main_Cpp)
		write_file(path(project['cpp'], "native.build"), Native_Build)
	elif template == 2:
		write_file(project['vexel_build'], Vexel_Build_Template2)
		write_file(path(Package_dir, "MainActivity.java"), MainActivity2)

def create(arg2=None):
	VexelLog("Creating a project...", "INFO")
	template_map = {
		"1": 1,
		"2": 2,
		"3": 3,
		"4": 4,
		"5": 5,
		"6": 6,
		"7": 7,
		
		"native": 1,
		"java": 2,
		"java-assets": 3,
		"java-jar": 4,
		"java-dl": 5,
		"native-blue": 6,
		"native-triangle": 7,
	}
	if arg2 is not None:
		try: get_template(template_map[arg2.lower()])
		except KeyError: VexelLog("Invalid Template!", "CRASH")
	else:
		print("Choose Template:")
		print("Simple:")
		print("  [1] Native Activity")
		print("  [2] Java Activity")
		print("\nExamples:")
		print("  [3] Java Activity Assets")
		print("  [4] Java Activity Jar Plugin")
		print("  [5] Java Activity Shared library")
		print("  [6] Native Activity Blue screen")
		print("  [7] Native Activity Triangle")
	
		value = input("Template: ")
		if not value: VexelLog("Empty Template Value Provided", "CRASH")
		try: template = int(value)
		except ValueError: VexelLog("input must be a number", "CRASH")
		if template > 2: get_template(template)
		else: create_basic(template)
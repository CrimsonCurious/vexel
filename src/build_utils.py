# ==============================================================
#						 build_utils.py
# ==============================================================
import os, zipfile
import xml.etree.ElementTree as ET
import string
import secrets

from variables import *
from core_utils import *

""" ------------------ Functions ------------------
  run_aapt2_compile()
  run_aapt2_link()
  run_java_complie()
  convert_dex()
  add_to_apk()
  align_apk()
  sign_apk()
  update_manifest()
  generate_keystore()
  
"""

def run_aapt2_compile():
	run([tool['aapt2'], "compile", "--dir", project['res'], "-o", output['res']])

def run_aapt2_link():
	flat_files = find_files(output['res'], ".flat")

	run([
		tool['aapt2'], "link", "-o", 
		path(output['dir'], "temp.apk"), "-I", tool['platform'],
		"--manifest", output['manifest'], "--java", output['gen'],
		*flat_files
	])

def run_java_compile():
	java_files = find_files(path(Project_dir, "src"), ".java")
	jar_files = []
	if os.path.isdir(project['jar_lib']):
		jar_files = find_files(project['jar_lib'], ".jar")
		for jar in jar_files:
			VexelLog(f"Loaded {os.path.basename(jar)}", "INFO")
			
	gen_files = find_files(output['gen'], ".java")
	# Line [current + 2] uses a Linux/Android only feature this 
	# has to be changed later for window capability
	classpath = ":".join([tool['platform']] + jar_files)

	run([
		tool['javac'],
		"-d", output['classes'],
		"-classpath", classpath,
		*(java_files + gen_files)
	])

def convert_dex():
	class_files = find_files(output['classes'], ".class")
	if not class_files:
		raise CodeNotFoundError("Unable to find Class files")
	jar_files = []
	if os.path.isdir(project['jar_lib']):
		jar_files.extend(find_files(project['jar_lib'], ".jar"))
		
	run([
		tool['java'], "-cp", tool['d8'], "com.android.tools.r8.D8",
		"--lib", tool['platform'], "--min-api", "21", "--output", 
		output['dex'], *class_files, *jar_files
	])

def add_to_apk(apk_path, source_path, apk_inner_path="", not_exists_ok=True, zip_level=6):
	if not os.path.exists(source_path):
		if not_exists_ok: return
		raise FileNotFoundError(f"Path not found: {source_path}")

	temp_apk = apk_path + ".tmp"

	with zipfile.ZipFile(apk_path, "r") as zin:
		with zipfile.ZipFile(temp_apk, "w",
			compression=zipfile.ZIP_DEFLATED, compresslevel=zip_level
		) as zout:

			# Copy old APK files
			for item in zin.infolist():
				zout.writestr(item, zin.read(item.filename), compress_type=item.compress_type)
				
			if os.path.isfile(source_path):
				if apk_inner_path:
					apk_file_path = apk_inner_path
				else:
					apk_file_path = os.path.basename(source_path)

				with open(source_path, "rb") as f:
					zout.writestr(apk_file_path, f.read())

				VexelLog(f"Added {os.path.basename(apk_file_path)}", "INFO")
				
			elif os.path.isdir(source_path):
				for root, _, files in os.walk(source_path):
					for file in files:

						full_path = os.path.join(root, file)
						rel_path = os.path.relpath(full_path, source_path)
						if apk_inner_path:
							apk_file_path = path(apk_inner_path, rel_path).replace("\\", "/")
						else:
							apk_file_path = rel_path.replace("\\", "/")
						zout.write(full_path, apk_file_path)
						VexelLog(f"Added {apk_file_path}", "INFO")
	os.replace(temp_apk, apk_path)
	
def align_apk():
	run([
		tool['zipalign'], "-f", "4",
		path(output['dir'], "temp_unaligned.apk"),
		path(output['dir'], "Debug.apk")
	])

def sign_apk():
	keystore_path, password = generate_keystore(output['key'])
		
	run([
		tool['java'], "-jar", tool['apksigner'], "sign",
		"--ks", keystore_path,
		"--ks-pass", f"pass:{password}",
		path(output['dir'], "Debug.apk")
	])

def update_manifest(manifest_path, out_manifest_path, cfg):
	if not check_file(manifest_path):
		VexelLog("Failed to load AndroidManifest.xml", "CRASH")
		
	tree = ET.parse(manifest_path)
	root = tree.getroot()

	android_ns = "http://schemas.android.com/apk/res/android"
	ET.register_namespace('android', android_ns)

	def android_attr(name):
		return f"{{{android_ns}}}{name}"

	# ---- APP ---- #
	app = cfg.get("app", {})
	if app:
		if "package" in app:
			root.set("package", app["package"])

		if "version_code" in app:
			root.set(android_attr("versionCode"), str(app["version_code"]))

		if "version_name" in app:
			root.set(android_attr("versionName"), str(app["version_name"]))

		application = root.find("application")
		if application is not None and "name" in app:
			application.set(android_attr("label"), app["name"])

	# ---- SDK ---- #
	sdk = cfg.get("sdk", {})
	if sdk:
		uses_sdk = root.find("uses-sdk")
		if uses_sdk is None:
			uses_sdk = ET.SubElement(root, "uses-sdk")

		if "min" in sdk:
			uses_sdk.set(android_attr("minSdkVersion"), str(sdk["min"]))

		if "target" in sdk:
			uses_sdk.set(android_attr("targetSdkVersion"), str(sdk["target"]))

	tree.write(out_manifest_path, encoding="utf-8", xml_declaration=True)
	
def generate_keystore(Out_Key_dir, length=24):
	keystore_path = path(Out_Key_dir, "debug.keystore")

	if os.path.exists(keystore_path):
		with open(path(Out_Key_dir, "debug.keystore.pass")) as f:
			password = f.read().strip()
			return keystore_path, password

	VexelLog("Generating new keystore...", "INFO")

	chars = string.ascii_letters + string.digits
	password = ''.join(secrets.choice(chars) for _ in range(length))

	run([
		tool['keytool'],
		"-genkeypair",
		"-v",
		"-keystore", keystore_path,
		"-storepass", password,
		"-keypass", password,
		"-alias", "vexelkey",
		"-keyalg", "RSA",
		"-keysize", "2048",
		"-validity", "10000",
		"-dname", "CN=Vexel,O=Dev,C=US"
	])

	# Save password (IMPORTANT)
	with open(path(Out_Key_dir, "debug.keystore.pass"), "w") as f:
		f.write(password)
	VexelLog("Keystore created", "INFO")

	return keystore_path, password
# ============================================================
#						 builder.py
# ============================================================
import os
import time
import shutil

from native import *
from variables import *
from core_utils import *
from build_utils import *

def MakeFileSystem():
	if check_dir(output['dir']):
		VexelLog("Cleaning...", "INFO")
		rmdir(output['dir'])
	
	mkdir(output['res'])
	mkdir(output['dex'])
	mkdir(output['classes'])
	mkdir(output['gen'])
	mkdir(output['key'])
	mkdir(output['apk'])


def build(dir=None):
	start = time.time()
	
	config = load_config(project['vexel_build'])
	MakeFileSystem()
	update_manifest(project['manifest'], output['manifest'], config)
	VexelLog("Compile resources...", "BUILD")
	run_aapt2_compile()

	VexelLog("Link resources...", "BUILD")
	run_aapt2_link()
	
	VexelLog("Compile Java...", "BUILD")
	run_java_compile()
	
	# Build native
	build_native()

	VexelLog("Convert to DEX...", "BUILD")
	convert_dex()

	VexelLog("Add classes.dex...", "BUILD")
	shutil.copy(path(output['apk'], "base.apk"), path(output['apk'], "unaligned.apk"))
	
	# Add Dex
	for dex in find_files(output['dex'], ".dex"):
		add_to_apk(path(output['apk'], "unaligned.apk"), dex, os.path.basename(dex), False, 0)
	
	# Add User Dynamic Libraries
	add_to_apk(path(output['apk'], "unaligned.apk"), project['dyn_lib'], "lib")
	
	# Add Compiled Dynamic Libraries
	add_to_apk(path(output['apk'], "unaligned.apk"), output['dyn_lib'], "lib")
	
	# Add Java Libraries
	add_to_apk(path(output['apk'], "unaligned.apk"), project['assets'], "assets")
	
	VexelLog("Align APK...", "BUILD")
	align_apk()

	VexelLog("Sign APK...", "BUILD")
	sign_apk(config)
	
	mkdir(output['final'])
	shutil.copy(path(output['apk'], "final.apk"), path(output['final'], "Debug.apk"))
	
	elapsed = round(time.time() - start, 2)
	VexelLog(f"Build Successful in {elapsed} sec", "INFO")
	VexelLog(f"Apk saved at: {path(output['final'], 'Debug.apk')}", "INFO")
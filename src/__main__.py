# ============================================================
#                            __main__.py
# ============================================================
import sys
import os
import stat
import new_project
import builder

from variables import *
from core_utils import *

def clean():
	VexelLog("Cleaning...", "INFO")
	rmdir(output['dir'])

def checkup():
	for name, file in tool.items():
		if file == tool['ndk']:
			if not check_dir(file):
				VexelLog("NDK installation not found", "WARN")
			continue

		if not check_file(file):
			VexelLog(f"Tool not found: {name} -> {file}", "CRASH")

	VexelLog("All Build Tools Checked", "INFO")

def add_execute_permission():
	runtime_ = find_files(RT_Bin, "")
	tool_ = [tool['aapt2'], tool['zipalign']]
	if check_dir(tool['ndk']):
		ndk_ = find_files(path(tool['ndk'], "bin"), "")
	else: ndk_ = []
	
	final = runtime_ + tool_ + ndk_
	for file in final:
		mode = os.stat(file).st_mode
		os.chmod(
			file, mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH
		)
	
# ======================
#        CLI
# ======================

Version = "1.0"
Install_Marker = path(Vexel_Home, ".installed")

unkcmd = 'vexel: Unknown command [type "vexel help"]'
narg = 'vexel: Needs one Argument [type "vexel help"]'
help = f"""Vexel {Version}
A Minimal Android APK Builder for Termux

Usage:
  vexel <command>

Commands:
  create       Create a new project
  build        Build current project
  clean        Remove build cache
  checkup      Check build environment
  version      Display current version
  help         Display this help page

PieStudios © 2026"""

if __name__ == "__main__":
    if not check_file(Install_Marker):
    	add_execute_permission()
    	with open(Install_Marker, "w") as f: f.write("1")
    	
    if len(sys.argv) < 2:
        print(narg)
        exit()

    if sys.argv[1] == "create":
        new_project.create()
        
    elif sys.argv[1] == "build":
        builder.build()
        	
    elif sys.argv[1] == "checkup":
    	checkup()
    	
    elif sys.argv[1] == "clean":
    	clean()
    
    elif sys.argv[1] == "version":
    	print(f"Vexel {Version}")
    	
    elif sys.argv[1] == "help":
    	print(help)
    	
    else: print(unkcmd)
# ============================================================
#						 core_utils.py
# ============================================================
import os
import sys
import shutil
import subprocess
import platform

from datetime import datetime

""" ------------------- Function -----------------------
  run(cmd) -> result
  path(*path) -> Path
  mkdir(path) -> None
  rmdir(path) -> None
  get_arm_abi() -> ABI
  check_dir(path) -> Bool
  check_file(path) -> Bool
  load_config(path) -> Config
  VexelLog(message, tag) -> None
  write_file(path, content) -> None
  find_files(path, extension) -> *Files

# -------------------- Custom Errors --------------------
  CodeNotFoundError
  OSNotSupportedError
  
"""

RESET   = "\033[0m"

RED	 = "\033[31m"
GREEN   = "\033[32m"
YELLOW  = "\033[33m"
BLUE	= "\033[34m"
MAGENTA = "\033[35m"
CYAN	= "\033[36m"
WHITE   = "\033[37m"

BOLD	= "\033[1m"

def path(*parts): return os.path.join(*parts)
	
Home_dir = os.getenv('HOME', '/data/data/com.termux/files/home')
Vexel_Home = path(Home_dir, ".vexel")

def VexelLog(message="", tag="NONE", exit=True, stamp_time=True):
	tag = tag.upper()
	time = datetime.now().strftime("%H:%M:%S")

	tag_colors = {
		"INFO": CYAN,
		"BUILD": GREEN,
		"WARN": YELLOW,
		"CRASH": RED,
		"DEBUG": MAGENTA,
		"NONE": WHITE
	}

	parts = []

	if stamp_time: parts.append(f"[{time}]")
	parts.append(f"{BOLD}{BLUE}[VEXEL]{RESET}")

	if tag != "NONE":
		color = tag_colors.get(tag, WHITE)
		parts.append(f"{color}[{tag}]{RESET}")

	parts.append(message)
	print(*parts, flush=True)
	
	if tag == "CRASH" and exit: sys.exit(1)

def run(cmd, cwd=None):
	env = os.environ.copy()
	rt_lib = path(Vexel_Home, "runtime", "lib:")
	env["LD_LIBRARY_PATH"] = rt_lib + env.get("LD_LIBRARY_PATH","")
	
	result = subprocess.run(
		cmd, cwd=cwd,
  	  capture_output=True,
  	  text=True, env=env
	)
	
	if result.returncode != 0:
		VexelLog("Command failed", "CRASH", exit=False)
		VexelLog(f"Error:\n{result.stderr}", "CRASH")
		exit(1)

	return result.stdout
	
def write_file(path, content):
	os.makedirs(os.path.dirname(path), exist_ok=True)
	with open(path, "w") as f:
		f.write(content.strip() + "\n")

def mkdir(path):
	os.makedirs(path, exist_ok=True)

def rmdir(path):
	shutil.rmtree(path, ignore_errors=True)

def find_files(dir, ext):
	result = []
	for base, _, files in os.walk(dir):
		for f in files:
			if f.endswith(ext):
				result.append(os.path.join(base, f))
	return result
	
def check_dir(path):
	if os.path.exists(path): return True
	else: return False

def check_file(path):
	if os.path.isfile(path): return True
	else: return False

def get_arm_abi():
    arch = platform.machine().lower()
    is_arm = any(keyword in arch for keyword in ["arm", "aarch"])
    
    if not is_arm: raise OSNotSupportedError("This OS is not supported by Vexel yet")
    is_64bit = sys.maxsize > 2**32
    
    if is_64bit: return "arm64"
    else: return "arm"

def load_config(path):
	import configparser
	
	config = configparser.ConfigParser()
	if not check_file(path):
		VexelLog(f"Config file not found: {path}", "CRASH")
		
	with open(path, "r") as f: config.read_file(f)
	result = {}

	for section in config.sections():
		result[section] = {}
		for key, value in config[section].items():
			if value.lower() == "true":
				value = True
			elif value.lower() == "false":
				value = False
			elif value.isdigit():
				value = int(value)
			else:
				try: value = float(value)
				except: pass
				
			result[section][key] = value
	return result

class CodeNotFoundError(Exception): pass
class OSNotSupportedError(Exception): pass
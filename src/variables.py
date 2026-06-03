# ============================================================
#                         Variables.py
# ============================================================
import os, sys
from core_utils import *

""" -------------------- Variables ----------------
  Home_dir -> Home
  Vexel_Home -> Vexel's Home
  Project_dir -> Current Folder
  
# ----------------------- Dicts -------------------
  project[] -> Project Folders/Files
  output[] -> Output Folders
  tool[] -> Tool binaries
  
# ---------------------- Shortcuts ----------------
  RT_Bin -> Vexel Runtime Binray folder
  Pro_Src -> Project Script Folder
  
"""

Home_dir = os.getenv('HOME', '/data/data/com.termux/files/home')
Vexel_Home = path(Home_dir, ".vexel")
Project_dir = os.getcwd()
os.environ['VEXEL_HOME'] = Vexel_Home

# Shortcut Paths
RT_Bin = path(Vexel_Home, "runtime", "bin")
Pro_Src = path(Project_dir, "src")
abi = get_arm_abi()

# Project Dirs
project = {
	"dir": Pro_Src,
	"dyn_lib": path(Pro_Src, "lib"),
	"assets": path(Pro_Src, "assets"),
	"res": path(Pro_Src, "res"),
	"manifest": path(Pro_Src, "AndroidManifest.xml"),
	"cpp": path(Pro_Src, "cpp"),
	"vexel_build": path(Project_dir, "vexel.build"),
	"jar_lib": path(Project_dir, "libs")
}

# Output Dirs
output = {"dir": path(Project_dir, "out")}
output["res"] = path(output["dir"], "res")
output["dex"] = path(output["dir"], "dex")
output["classes"] = path(output["dir"], "classes")
output["gen"] = path(output["dir"], "gen")
output["key"] = path(output["dir"], "key")
output["dyn_lib"] = path(output["dir"], "lib")
output["manifest"] = path(output["gen"], "AndroidManifest.xml")

tool = {
    "platform": path(Vexel_Home, "platforms", "android-33","android.jar"),
    "java": path(RT_Bin, "java"),
    "javac": path(RT_Bin, "javac"),
    "keytool": path(RT_Bin, "keytool"),
    "aapt2": path(Vexel_Home, "tools", abi, "aapt2"),
    "zipalign": path(Vexel_Home, "tools", abi, "zipalign"),
    "d8": path(Vexel_Home, "tools", "d8.jar"),
    "apksigner": path(Vexel_Home, "tools", "apksigner.jar"),
    "ndk": path(Vexel_Home, "ndk-arm64")
}
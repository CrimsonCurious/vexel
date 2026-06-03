# ============================================================
#						 native.py
# ============================================================
import glob
import os

from core_utils import *
from variables import *

def clang_target(api, abi, cpp=True):
	clang = path(tool['ndk'], "bin", "clang")
	clangpp = path(tool['ndk'], "bin", "clang++")
	
	compiler = clangpp if cpp else clang
	targets = {
		"arm64-v8a": f"aarch64-linux-android{api}",
		"armeabi-v7a": f"armv7a-linux-androideabi{api}",
		"x86_64": f"x86_64-linux-android{api}"
	}

	if abi not in targets:
		raise ValueError(f"Unsupported ABI: {abi}")
	apis = [21, 24, 29, 35, "21", "24", "29", "35"]
	if api not in apis:
		raise ValueError(f"Unsupported API: {api}")
	return compiler, targets[abi]

def resolve_sources(src_field, project_dir, find_cpp=True):
	sources = []

	for item in src_field.split():
		item = path(project_dir, item)

		# Directory
		if os.path.isdir(item):
			sources.extend(find_files(item, ".c"))

			if find_cpp:
				sources.extend(find_files(item, ".cpp"))

		# Wildcard
		elif "*" in item:
			for match in glob.glob(item):
				if os.path.isfile(match):
					sources.append(match)

		# Single file
		elif os.path.isfile(item):
			sources.append(item)

		else:
			VexelLog(f"Source not found: {item}", "CRASH")

	return sorted(set(sources))
	
def build_native():
	vexel_b = load_config(project["vexel_build"])
	ndk = vexel_b.get("ndk", {})

	if not ndk.get("enabled"): return
	VexelLog("Compile C++...", "BUILD")
	mkdir(output['dyn_lib'])

	cfg = load_config(ndk['build_script'])

	for section, lib_cfg in cfg.items():
		if section.startswith("lib:"):
			libname = section.split(":", 1)[1]
			VexelLog(f"Compile Library: {libname}", "BUILD")
			build_library(libname, lib_cfg, ndk)
			
def build_library(libname, cfg, ndk):
	api = ndk["api"]
	abis = ndk["abi"].split()
	cpp = cfg["cpp"]
	
	sources = resolve_sources(cfg["src"], project["dir"], cpp)

	includes = [
		f"-I{path(project['dir'], x)}"
		for x in cfg.get("include", "").split()
	]

	link_lib = [f"-l{x}" for x in cfg.get("link", "").split()]
	flags = cfg.get('flags', '').split()

	for abi in abis:
		compiler, target = clang_target(api, abi, cpp)
		
		user_lib_dir = path(project['dir'], "lib", abi)
		out_lib_dir = path(output["dyn_lib"], abi)
		out_file = path(out_lib_dir, f"lib{libname}.so")
		
		mkdir(out_lib_dir)

		cmd = [
			compiler, f"--target={target}",
			"--sysroot", path(tool["ndk"], "sysroot"),
			"-shared", "-fPIC", "-o", out_file,
		]
		if os.path.isdir(user_lib_dir):
			cmd.append(f"-L{user_lib_dir}")
		if os.path.isdir(out_lib_dir):
			cmd.append(f"-L{out_lib_dir}")
		cmd.extend([*includes, *sources, *flags, *link_lib])

		if cpp: cmd.insert(4, "-static-libstdc++")

		run(cmd)
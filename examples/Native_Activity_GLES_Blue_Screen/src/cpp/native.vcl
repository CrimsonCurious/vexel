libnative: {
	# Source files or directories.
	# Supported formats:
	#   cpp/src/
	#   cpp/src/*.c
	#   cpp/Main.c
	src: [
		"cpp/src"
	]
	
	# Static libraries files
	# staticLibs: ["cpp/src/libfoo.a"]

	# Additional header search directories.
	include: [
		"cpp/src/include"
	]

	# Enable C++ compilation.
	# Set to false to compile as C.
	cpp: false

	# System or user libraries to link.
	link: [
		"android"
		"log"
		"EGL"
		"GLESv3"
	]

	# Additional compiler and linker flags.
	# The flags -shared and -fPIC are added automatically.
	flags: [
		"-O2"
		"-Wall"
		"-s"
	]
}
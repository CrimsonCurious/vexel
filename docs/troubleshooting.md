# Troubleshooting

## NDK installation not found

Error:

```text
[VEXEL] [WARN] NDK installation not found
```

Cause:

The Android NDK package is not installed.

Fix:

Install the Vexel NDK package and makesure this folder exits at:

```text
~/.vexel/ndk-arm64/
```

---

## Tool not found

Error:

```text
[VEXEL] [CRASH] Tool not found
```

Cause:

A required tool is missing from the Vexel runtime.

Fix:

Run:

```text
vexel checkup
```

Verify that:

```text
~/.vexel/runtime/ ~/.vexel/tools/ ~/.vexel/platforms/
```

exist and contain the required files.


---

## Config file not found

Error:

```text
Config file not found
```

Cause:

Vexel could not find a required configuration file.

Fix:

Verify that your project contains:

```text
vexel.build AndroidManifest.xml
```

If native support is enabled:

```text
src/cpp/native.build
```

---

## Source not found

Error:

```text
Source not found: ...
```

Cause:

A source file or directory listed in `native.build`
does not exist.

Example:

```toml
src = cpp/src
```

but:

```text
project/
└── cpp/
```

does not exist.
Fix:
Check every path listed in:

```text
src/cpp/native.build
```

# Unsupported ABI

Error:

```text
Unsupported ABI
```

Cause:

An unsupported ABI was specified.

Supported values:

```text
arm64-v8a
armeabi-v7a
x86_64
```
Example:

```toml
abi = arm64-v8a armeabi-v7a x86_64
```

# Unsupported API
Error:

```text
Unsupported API
```

Cause:

The configured Android API level is not available in the bundled NDK.

Supported values:

```text
21
24
29
35
```

Example:

```toml
api = 26
```

# Failed to load AndroidManifest.xml

Error:

```text
Failed to load AndroidManifest.xml
```

Cause:

The manifest file is missing.

Fix:

Verify:

```text
AndroidManifest.xml
```

exists in the project root.

# Unable to find Class files

Error:

```text
Unable to find Class files
```

Cause:

Java compilation failed or no Java source files were found.

Fix:

Verify:

```text
src/java/
```

contains Java source files.
Check previous build output for Java compiler errors.

# Command failed
Error:

```text
[VEXEL] [CRASH] Command failed
```

Cause:

One of the build tools returned a non-zero exit code.

Examples:

```text
javac compilation failure
aapt2 resource error
d8 dex conversion error
clang compilation error
```

Fix:

Read the error text printed directly below the message. The actual tool output is displayed after:

```text
Error:
```

and usually contains the exact cause.

# Build successful but application crashes

Cause:

The APK built successfully, but the application contains runtime errors.

Common causes:

```text
Incorrect JNI method name
Missing native library
Invalid AndroidManifest configuration
Java exceptions
```

Fix:

Use Logcat or LogFox and inspect runtime logs. Vexel only verifies build-time errors.
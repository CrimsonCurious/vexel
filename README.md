# Vexel

Vexel is a lightweight, self-contained Android APK builder written in Kotlin. It is designed to build Android applications directly on Android devices without requiring Android Studio, Gradle, or a desktop computer.

Vexel focuses on simplicity, portability, and speed while remaining powerful enough to support Java, Kotlin, and native C/C++ development.

---

Why Vexel?

Most Android build systems are designed for desktop operating systems and require large dependencies such as Android Studio and Gradle.

Vexel takes a different approach:

  - Runs directly on Android.
  - No Android Studio required.
  - No Gradle required.
  - Supports Java, Kotlin and Native C/C++.
  - Lightweight installation size.
  - Fast incremental build pipeline.
  - Built-in Kotlin compiler.
  - Java/Kotlin mixed-project support.

The goal of Vexel is to provide a minimal but practical Android build environment that can run almost anywhere.

---

Features
--------

**Android Application Building**

  - Android APK generation
  - AndroidManifest.xml processing
  - Resource compilation using AAPT2
  - Java compilation using Javac
  - Kotlin compilation using the bundled Kotlin compiler
  - Java/Kotlin mixed-project support
  - DEX generation using D8
  - APK alignment
  - APK signing
  - Automatic/Manual debug keystore generation
  - Persistent debug signing credentials

Bundled Android platform:

- Android API 34

A different `android.jar` can be configured through `config.toml`.

Native Development (NDK)

  - Native C support
  - Native C++ support
  - Multi-ABI builds
  - Shared library generation (.so)
  - Multiple native libraries
  - Custom include directories
  - Custom linker flags
  - Custom compiler flags
  - Custom libraries
  - User libraries
  - Automatic APK native library packaging

Supported ABIs:

  - arm64-v8a
  - armeabi-v7a
  - x86
  - x86_64

Self-Contained Runtime

Vexel bundles its own dependencies reducing host system dependencies on the host environment.

Benefits:

  - Consistent builds
  - Easier installation
  - Better portability
  - Reduced configuration requirements

Portable Design

Vexel primarily targets:

  - Termux

Experimental support:

  - Other Android terminal environments

Also supported:

  - Linux
  - Windows

---

**Installation**

1. Download the latest release.
2. Extract the archive.
3. Add the `bin` directory to your PATH.
4. Ensure Java 17 or newer is installed.
5. Run:

vexel version

---

**Project Structure**

Typical project layout:

```
MyApp/
├── src/
│   ├── java/
│   ├── kotlin/
│   ├── res/
│   ├── cpp/
│   │   └── native.build
│   └── AndroidManifest.xml
│
├── vexel.build
└── build/
```

---

**Build Configuration**

`vexel.build`

Controls application-level settings:

```toml
[app]
name = "My App"
package = "com.example.app"
version_code = 1
version_name = "1.0"

[sdk]
min = 21
target = 34

[ndk]
enabled = true
cpp = true
api = 29
abi = "arm64-v8a armeabi-v7a x86_64"
```

`native.build`

Controls native library compilation:

```toml
[lib-native]

src = "cpp/src"
include = "cpp/include"

cpp = true

link = "android log"

flags = "-O2 -Wall -s"
```

---

**Usage**

*Creating a New Project*

To create a new project interactively, run:

```text
vexel new
```

Then list your actual template names.

For example:

```text
Java Activity
Kotlin Activity
Native Activity (Java)
Native Activity (Kotlin)
```

A specific template can also be selected directly:

```text
vexel new <template>
```

Select the desired project template when prompted.

Configuring the Project

After creating the project:

1. Edit the "vexel.build" file to configure your project settings.
2. If your project contains C or C++ code, edit the "native.build" file as needed.
3. Implement your application code.

Building the Application

Once your project is ready, build it using:

```text
vexel build
```

The build process will generate an Android APK from your project source code.
The final generated APK is placed in:

```text
build/output/
```

The APK filename is generated from the project/application name.

---

**How Vexel Works**

The build process is intentionally straightforward:
  1. Compile Android resources using AAPT2.
  2. Link resources and generate R.java.
  3. Compile Java sources and generated Java sources.
  4. Compile Kotlin sources, if present.
  5. Compile native source files using Clang, if enabled.
  6. Convert class files into DEX using D8.
  7. Package resources, DEX files, and native libraries into the APK.
  8. Align the APK using ZipAlign.
  9. Sign the APK using APKSigner.
  10. Produce the final APK.

---

**Design Goals**

  - Lightweight
  - Portable
  - Fast
  - Self-contained
  - Android-first
  - Easy to understand
  - Easy to modify

Vexel is intended to be approachable for developers who want direct control over the Android build process.

---

**Credits**

Vexel would not be possible without the following projects and tools.

`Java Runtime`
  - JDK 17

`Android Platform`
  - Android SDK Platform (`android.jar`)
  - Android Open Source Project (AOSP)

`Kotlin`
  - Kotlin Compiler
  - Kotlin Standard library

`Android Build Tools`
  - AAPT2
  - D8
  - APKSigner
  - ZipAlign

Provided by Android Build Tools and AOSP.

`Toml parser`
  - tomlj

`XML parser`
  - jdom2

`Zip editor`
  - zip4j

`Native Toolchain`

  - LLVM Project
  - Clang
  - Android NDK
  - Android Open Source Project (AOSP)

Special thanks to:

  - [SuperAppMan](https://github.com/SuperAppMan)

for providing Android-compatible Android NDK builds used by Vexel.

---

**License**

See LICENSE for licensing information.

---

**Status**

Vexel is an actively developed experimental project.

Current release: v0.4.0

While stable for many use cases, APIs, project layouts, and build features may change between releases.

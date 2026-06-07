**Vexel**

Vexel is a lightweight, self-contained Android APK builder written in Python. It is designed to build Android applications directly on Android devices without requiring Android Studio, Gradle, or a desktop computer.

Vexel focuses on simplicity, portability, and speed while remaining powerful enough to support Java, JNI, and native C/C++ development.

---

Why Vexel?

Most Android build systems are designed for desktop operating systems and require large dependencies such as Android Studio and Gradle.

Vexel takes a different approach:

  - Runs directly on Android.
  - No Android Studio required.
  - No Gradle required.
  - Self-contained runtime.
  - Supports Java and Native C/C++.
  - Small installation size.
  - Fast build process.

The goal of Vexel is to provide a minimal but practical Android build environment that can run almost anywhere.

---

Features
--------

**Android Application Building**

  - Android APK generation
  - AndroidManifest.xml processing
  - Resource compilation using AAPT2
  - Java compilation using Javac
  - DEX generation using D8
  - APK alignment
  - APK signing
  - Automatic debug keystore generation

Supported APIs:
  - 33

*NOTE: The API 33 and older APIs are only supported yet.*

Native Development (NDK)

  - Native C support
  - Native C++ support
  - JNI support
  - Multi-ABI builds
  - Shared library generation (.so)
  - Multiple native libraries
  - Custom include directories
  - Custom compiler flags
  - Automatic APK native library packaging

Supported ABIs:

  - arm64-v8a
  - armeabi-v7a
  - x86_64

Supported Android APIs:

  - 21
  - 24
  - 29
  - 35

Multi-Dex

Vexel supports Multi-Dex builds for projects exceeding the 65,536 method limit.

Self-Contained Runtime

Vexel bundles its own runtime and dependencies, reducing dependency on the host environment.

Benefits:

  - Consistent builds
  - Easier installation
  - Better portability
  - Reduced configuration requirements

Portable Design

Vexel primarily targets:

  - Termux

Experimental support:

  - Cxxdroid
  - Pydroid
  - Other Android terminal environments

---

**Project Structure**

Typical project layout:

```
MyApp/
├── src/
│   ├── java/
│   ├── res/
│   ├── cpp/
│   │   └── native.build   
│   └── AndroidManifest.xml
│
├── vexel.build
└── out/
```

---

**Build Configuration**

`vexel.build`

Controls application-level settings:

```toml
[app]
name = My App
package = com.example.app
version_code = 1
version_name = 1.0

[sdk]
min = 21
target = 35

[ndk]
enabled = true
cpp = true
api = 29
abi = arm64-v8a armeabi-v7a x86_64
```

`native.build`

Controls native library compilation:

```toml
[lib:native]

src = cpp/src
include = cpp/include

cpp = true

link = android log

flags = -O2 -Wall -s
```

---

**How Vexel Works**

The build process is intentionally straightforward:

  1. Compile Android resources using AAPT2.
  2. Link resources and generate R.java.
  3. Compile Java source files using Javac.
  4. Compile native source files using Clang (optional).
  5. Convert class files into DEX using D8.
  6. Package resources and DEX files into APK.
  7. Add native libraries.
  8. Align APK using ZipAlign.
  9. Sign APK using APKSigner.
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
    - Java
    - Javac
    - Keytool

`Python Runtime`

  - CPython 3.14

`Android Platform`

  - Android SDK Platform ("android.jar")
  - Android Open Source Project (AOSP)

`Android Build Tools`

  - AAPT2
  - D8
  - APKSigner
  - ZipAlign

Provided by Android Build Tools and AOSP.

`Native Toolchain`

  - LLVM Project
  - Clang
  - Android NDK
  - Android Open Source Project (AOSP)

Special thanks to:

  - [SuperAppMan](https://github.com/SuperAppMan)

for providing Android-compatible NDK builds used by Vexel.

---

**License**

See LICENSE for licensing information.

---

**Status**

Vexel is an actively developed experimental project.

While stable for many use cases, APIs, project layouts, and build features may change between releases.

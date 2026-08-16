# Getting Started

Vexel is a lightweight Android APK builder designed to run directly on Android devices.

## Create a Project

```shell
vexel new
```

Follow the prompts and enter:

- Template
- Application Name
- Package Name
- Minimum SDK
- Target SDK

## Build

```shell
vexel build
```

After a successful build:

```text
build/output/Debug.apk
```

will be generated.

## Clean Build Files

```shell
vexel clean
```

Removes the output directory.

## Check Toolchain

```shell
vexel checkup
```

Verifies that all required build tools are installed.

## Project Files

Main project configuration:

```text
vexel.build
native.build
```

Java source:

```text
src/java/
```

Kotlin source:

```text
src/Kotlin
```

Resources:

```text
src/res/
```

Native code:

```text
src/cpp/
```
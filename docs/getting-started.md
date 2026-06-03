# Getting Started

Vexel is a lightweight Android APK builder designed to run directly on Android devices.

## Create a Project

```sh
vexel create
```

Follow the prompts and enter:

- Application Name
- Package Name
- Minimum SDK
- Target SDK

## Build

```sh
vexel build
```

After a successful build:

```text
out/Debug.apk
```

will be generated.

## Clean Build Files

```sh
vexel clean
```

Removes the output directory.

## Check Toolchain

```sh
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

Resources:

```text
src/res/
```

Native code:

```text
src/cpp/
```
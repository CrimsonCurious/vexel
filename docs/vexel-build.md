# vexel.build

Main application configuration file.

Example:

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

## [app]

### name

Application display name. (Only for new projects else it doesn't automatically set in manifest)

### package

Java package name.

### version_code

Integer version code.

### version_name

Human-readable version.

## [sdk]

### min

Minimum Android API.

### target

Target Android API.

## [ndk]

### enabled

Enable native compilation.

### cpp

Compile as C++.

### api

NDK API level. (only API 21, 24, 28, 35 are supported due to extended size)

### abi

Target architectures.

Supported:

- arm64-v8a
- armeabi-v7a
- x86_64
# Changelog

## v0.1.0
- Initial release
- Java application support
- Android resource compilation
- APK signing
- Native C/C++ support
- Multi-library support
- Multi-DEX support
- JAR library support
- Full APK build pipeline

## v0.1.1
- Added support for commands with a leading dash (`-`)
- Fixed version detection
- Added missing dependency: `libz.so.1`

## v0.2.0
- Added additional project templates
- Added instant project creation with `vexel new <template>`
- Renamed `vexel create` to `vexel new`
- Reorganized build output
- Final APK is now located at `build/output/Debug.apk`
- Added Debian package distribution
- Fixed various minor bugs

## v0.3.0
### Major changes
- Complete rewrite from Python to Kotlin
- Portable distribution for Android, Linux, and Windows
- Bundled Android build tools into `vexel-main.jar`
- Removed Python runtime dependency
- Removed bundled JRE
- Requires OpenJDK 17 or newer
- `JAVA_HOME` is now required
- Introduced consistent Vexel error reporting
- Bundled tools are extracted lazily and cached automatically.
- Large internal refactoring and code cleanup.

### Platform support
- Added Android support (armeabi, arm64)
- Added support for overriding bundled build tools

### Configuration
- Added `config.toml` support
- Added support for overriding bundled tools:
  - `aapt2`
  - `zipalign`
  - `android.jar`
  - Android NDK
  - `JAVA_HOME`
- Configuration file is searched next to `vexel-main.jar` or in `$HOME/.vexel/`

### Runtime
- Build tools are extracted on demand and cached automatically
- `$HOME/.vexel/` is now used only for cache files
- `vexel-main.jar` is installed to `/usr/lib/`

### Other
- Removed example template
- Improved project generation
- Improved build performance
- Fixed numerous bugs
- Internal codebase cleanup and refactoring

## v0.3.1
- Fixed packaging of user-provided native libraries together with Vexel-compiled libraries.
- Updated D8 and APK Signer to Android 16 Build Tools.
- Fixed launcher failing to locate vexel-main.jar when invoked through a symbolic link.

## v0.4.0

### Kotlin support

- Added Kotlin project support.
- Added bundled Kotlin compiler.
- Added automatic Kotlin compiler setup and execution.
- Added support for Kotlin source files in `src/kotlin/`.
- Added support for building mixed Java/Kotlin projects.

### Project templates

- Added new Kotlin-based project templates.
- Added Native Activity templates for both Java and Kotlin.
- Fixed incorrect package names in project templates.
- Improved generated project structure.

### Build system

- Improved Java, Kotlin, and C/C++ source detection.
- Improved build logging and build-step reporting.
- Added clearer messages when source types are not present.
- Improved generated Java source handling.
- Improved APK signing and debug signing-key handling.
- Added persistent debug signing credentials.
- Added dedicated `build/signing/` directory for signing files.
- Improved build output organization.
- APK output now uses the project/app name in the filename.

### Diagnostics

- Improved build and process error reporting.
- Improved compiler error output and debugging information.

### Documentation

- Added and expanded Vexel documentation.
- Added better contribution guidelines.
- Updated examples and templates.

### Other

- Fixed numerous bugs discovered in project templates and the build pipeline.
- Improved overall build reliability and project generation.
- General cleanup and internal improvements.
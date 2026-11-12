# Building

## Requirements

- **JDK 17** (`java -version`)
- **Android SDK** with platform 34 installed at `~/android-sdk`
- **Gradle 8.7** (`gradle --version`)

The Android SDK, JDK and Gradle are already available in the Termux environment.

## Debug build

```bash
cd me2008/alarm
./build.sh debug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

Or manually:

```bash
cd me2008/alarm
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk
gradle assembleDebug
```

## Release build

```bash
./build.sh release
```

## Install on device

```bash
./build.sh install
# or manually:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Clean

```bash
./build.sh clean       # remove build/ directory
./build.sh nuke        # clean + delete .gradle and .build-cache
```

## CPU limiting (Termux / laptops)

To avoid overheating on limited hardware:

```bash
CPU_CORES=4 ./build.sh debug
```

This pins Gradle to 4 cores via `taskset`. Adjust the number as needed.

## Termux notes

The Android SDK is at `~/android-sdk`. If `gradle` complains about `aapt2`, add this to `~/.gradle/gradle.properties`:

```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

## Versions

| | |
|---|---|
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24 |
| Compose compiler extension | 1.5.14 |
| Compose BOM | 2024.06.00 |
| `compileSdk` / `targetSdk` / `minSdk` | 34 / 34 / 26 |
| Application id | `me2008.alarm` |

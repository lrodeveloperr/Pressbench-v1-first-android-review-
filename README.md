# PressBench APK Compiler

This repository builds the reviewed PressBench v14 native Android application.

- Runtime UI: Kotlin and Jetpack Compose
- Persistence: Jetpack DataStore
- Web runtime: none
- Output: `PressBench-v14-Native-Jetpack.apk`

Every push to `main` runs source compilation and unit tests without producing an
APK. The APK workflow is manual and runs only when explicitly requested.

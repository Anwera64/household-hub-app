# Household Hub app (Kotlin Multiplatform + Compose Multiplatform)

Shared Kotlin Multiplatform (KMP) client for **Household Hub**: domain logic, Ktor networking,
reactive ViewModels, dependency injection, and the Compose Multiplatform phone UI.

Targets: **JVM** (tests), **Android**, and **iOS** (`iosArm64`, `iosSimulatorArm64`). `commonMain`
stays free of platform-only APIs, and a test enforces it.

```bash
git config core.hooksPath .githooks   # once per clone: ktlint and the architecture guards before every commit
```

```powershell
.\gradlew.bat jvmTest                  # everything that runs on the JVM
.\gradlew.bat :androidApp:installDebug # the dev app on a phone or an emulator
```

Modules keep three layers, `presentation -> domain <- data`, and tests guard the boundaries. Merging
a bump of `app.version` in `gradle.properties` releases it, tagged `v<version>`.

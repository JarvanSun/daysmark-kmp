# Build

## Toolchain

| Tool | Version | Note |
|---|---|---|
| Kotlin | 2.4.21 | Compiler plugins below are tied to it |
| Android Gradle Plugin | 9.5.1 | Built-in Kotlin; KMP libraries use `com.android.kotlin.multiplatform.library` |
| XcodeGen | 2.46.0, pinned in `iosApp/.xcodegen-version` | `scripts/verify.sh` refuses another version; CI installs exactly this one |
| Xcode | 27.1 | CI selects it (`setup-xcode`); CI runs on the `xcode-27` runner image (preview) |
| Gradle | 9.9.1 (wrapper) | Must **run on JDK 21 or later**, because Metro's Gradle plugin requires it |
| JDK for Gradle | 25, pinned by `gradle/gradle-daemon-jvm.properties` (Gradle's daemon toolchain) | Android Studio's bundled JBR is 25, so locally nothing is downloaded; CI sets up 25 too. Bytecode still targets Java 17. |
| Metro | 1.6.0 | Compiler plugin. Its compatibility table lists Kotlin up to 2.4.20; 2.4.21 was verified by building and testing here. Check the [table](https://zacsweers.github.io/metro/latest/compatibility/) before changing Kotlin. |
| KMP-NativeCoroutines | 1.0.7 | Built for exactly Kotlin 2.4.21 |
| KSP | 2.3.14 | Room's annotation processor |
| detekt | 2.0.0-alpha.6 | detekt 1.23.8 failed to run on the JDK 25 Gradle uses here (it could not parse the Java version), so 2.0 is used |

**Upgrading Kotlin** means upgrading Metro, KMP-NativeCoroutines (Kotlin and Swift package
together) and KSP in the same change, and only to versions whose compatibility tables list the new
Kotlin version.

## Layout

- `build-logic/convention` holds three plugins: `daysmark.kmp.library` (targets, explicit API,
  warnings as errors, iOS test device), `daysmark.android.application` and `daysmark.quality`
  (ktfmt, detekt, wired into `check`).
- `gradle/libs.versions.toml` is the only place versions are written.
- Module namespaces come from the module path: `:core:domain` →
  `com.example.daysmark.core.domain`.
- Every module targets Android, `iosArm64`, `iosSimulatorArm64` and a JVM target used only for
  fast tests.

## Commands

```bash
scripts/verify.sh                               # everything CI runs (Kotlin and iOS)
./gradlew check                                 # the Kotlin half, the fast loop
./gradlew :androidApp:installDebug             # build and install on a running emulator
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
cd iosApp && xcodegen && open DaysmarkKMP.xcodeproj
```

Command-line Xcode builds put DerivedData under `$XCODE_BUILD_ROOT/DaysmarkKMP-cli` (or Xcode's
default location), never inside the repository.

## Local configuration

- `local.properties` (`sdk.dir`) is generated per machine and ignored by Git.
- No absolute path to a machine appears in the repository. Paths come from the environment or
  from the per-machine files listed above.

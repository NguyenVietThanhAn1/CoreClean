# CoreClean

![Android CI](https://github.com/NguyenVietThanhAn1/CoreClean/actions/workflows/android-ci.yml/badge.svg)
[![codecov](https://codecov.io/gh/NguyenVietThanhAn1/CoreClean/branch/master/graph/badge.svg)](https://codecov.io/gh/NguyenVietThanhAn1/CoreClean)

A modern Android device cleaner app built with Clean Architecture, Jetpack Compose, and Kotlin.

## Overview

CoreClean provides tools to analyze and clean your Android device — without fake metrics or scam patterns.

| Module                | Status          |
|-----------------------|-----------------|
| Media Scanner         | Implemented     |
| Storage Analyzer      | Implemented     |
| Battery Monitor       | Implemented     |
| App Usage             | Implemented     |
| Contacts              | Implemented     |
| Junk Cleaner          | Implemented     |
| Permission Onboarding | Implemented     |
| Settings              | Implemented     |
| RAM Monitor           | Implemented     |
| APK Analyzer          | Implemented     |
| Privacy Dashboard     | Implemented     |
| Smart Suggestions     | Implemented     |
| Auto-cleaning         | Implemented     |
| Battery Prediction    | Implemented     |
| Accessibility (A11y)  | Implemented     |
| AI Dedupe (pHash)     | Implemented     |
| Tablet Two-Pane       | Implemented     |

## Screenshots

<!-- docs/images/home.png -->
<!-- docs/images/storage.png -->
<!-- docs/images/app_usage.png -->

## Languages

- 🇻🇳 Vietnamese (default)
- 🇬🇧 English

## Crash Reporting

Opt-in Sentry integration. Disabled by default. Enable in Settings → Crash reporting.

## Tech Stack

Kotlin 2.1.10 · Jetpack Compose · Hilt · Room · WorkManager · Coroutines + Flow · Navigation (type-safe) · Coil 3 · DataStore Preferences · AppCompat · Sentry

## Build variants: gms / foss

CoreClean ships two product flavors:

| Flavor | Description | Sentry crash reporting |
|--------|-------------|------------------------|
| `gms` (default) | Standard build for Google Play | Opt-in (disabled by default) |
| `foss` | FOSS build for F-Droid | Disabled (no proprietary SDK) |

```bash
./gradlew :app:assembleGmsDebug      # GMS debug build (default)
./gradlew :app:assembleFossRelease   # FOSS release build (for F-Droid)
```

## Build

```bash
./gradlew assembleDebug          # build (gms flavor by default)
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # lint
```

## Docs

**Start here:**
- [Architecture](docs/Architecture.md) — layering, module folders, key flows
- [Contributing](docs/Contributing.md) — commit convention, branch naming, PR checklist
- [AI Context](docs/AI%20Context.md) — context for LLM coding assistants

**Product:**
- [Development Roadmap](docs/DevelopmentRoadmap.md)
- [Planned Features](docs/PlannedFeatures.md)
- [Known Issues](docs/knownIssues.md)
- [Anti-Patterns](docs/AntiPatterns.md) — what we never do (scareware, fake numbers, …)
- [UX Guidelines](docs/UX_Guidelines.md)

**Security & privacy:**
- [Privacy Policy](docs/PrivacyPolicy.md)
- [Permissions](docs/Permissions.md)
- [Security](docs/Security.md) — threat model
- [Telemetry](docs/Telemetry.md)

**Engineering:**
- [Testing](docs/Testing.md)
- [Performance](docs/Performance.md)
- [Release](docs/Release.md)
- [i18n](docs/i18n.md)
- [Supported Devices](docs/SupportedDevices.md)
- [Changelog](CHANGELOG.md)

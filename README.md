# CoreClean

![Android CI](https://github.com/NguyenVietThanhAn1/CoreClean/actions/workflows/android-ci.yml/badge.svg)

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

## Build

```bash
./gradlew assembleDebug          # build
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # lint
```

## Docs

- [Architecture](docs/Architecture.md)
- [Development Roadmap](docs/DevelopmentRoadmap.md)
- [Known Issues](docs/knownIssues.md)
- [Contributing](docs/Contributing.md)
- [Permissions](docs/Permissions.md)
- [Planned Features](docs/PlannedFeatures.md)

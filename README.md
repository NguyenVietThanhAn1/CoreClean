# CoreClean

A modern Android device cleaner app built with Clean Architecture, Jetpack Compose, and Kotlin.

## Overview

CoreClean provides tools to analyze and clean your Android device — without fake metrics or scam patterns.

| Module            | Status          |
|-------------------|-----------------|
| Media Scanner     | Implemented     |
| Storage Analyzer  | Implemented     |
| Battery Monitor   | Implemented     |
| App Usage         | Coming soon     |
| Contacts          | Coming soon     |

## Tech Stack

Kotlin 2.1.10 · Jetpack Compose · Hilt · Room · WorkManager · Coroutines + Flow · Navigation (type-safe) · Coil 3

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
- [Planned Features](docs/PlannedFeatures.md)

# F-Droid Metadata — CoreClean

## Build Recipe (metadata/com.coreclean.app.yml)

This file describes how to build CoreClean for F-Droid submission.

### Flavor: foss
The `foss` product flavor excludes proprietary SDKs (Sentry crash reporting).
Build with: `./gradlew :app:assembleFossRelease`

### Anti-features:
- None (FOSS flavor has no proprietary dependencies)

### Build flags:
```yaml
Builds:
  - versionName: '1.0.0'
    versionCode: 1
    commit: master
    subdir: app
    gradle:
      - foss
```

## FOSS vs GMS flavors
| Feature | GMS flavor | FOSS flavor |
|---------|-----------|-------------|
| Crash reporting | Sentry (opt-in) | Disabled |
| All other features | Yes | Yes |

## F-Droid Categories
- System
- Utilities

## F-Droid Description (en-US)
CoreClean is a transparent Android device optimizer. No fake metrics, no scareware.

Features: duplicate image detection (MD5 + AI pHash), junk cleaner, storage analyzer,
battery monitor, app usage stats, contacts dedup, privacy dashboard.

All processing is local. No data sent without explicit opt-in.

## Source Code
https://github.com/annguyn/CoreClean

## Issue Tracker
https://github.com/annguyn/CoreClean/issues

## Changelog (latest)
See CHANGELOG.md in the repository root.

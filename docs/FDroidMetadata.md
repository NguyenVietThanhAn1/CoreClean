# F-Droid Submission Checklist — CoreClean

## Build Recipe (metadata/com.coreclean.app.yml)

Placed in the F-Droid `fdroiddata` repository as `metadata/com.coreclean.app.yml`.

### Flavor: foss
The `foss` product flavor excludes proprietary SDKs (Sentry crash reporting).
Build command: `./gradlew :app:assembleFossRelease`

### Anti-features
- None — FOSS flavor has zero proprietary dependencies.

### Build YAML snippet
```yaml
Builds:
  - versionName: '1.0.0'
    versionCode: 1
    commit: v1.0.0          # tag the release commit
    subdir: app
    gradle:
      - foss
```

## FOSS vs GMS flavors
| Feature | GMS flavor | FOSS flavor |
|---------|-----------|-------------|
| Crash reporting | Sentry (opt-in) | Fully disabled (NoOpCrashReporter) |
| All other features | Yes | Yes |
| Proprietary SDKs | io.sentry | None |

## Fastlane / Triple-T Metadata

All store listing content lives in `metadata/` at the repo root (fastlane format):

```
metadata/
  en-US/
    short_description.txt   ≤80 chars
    full_description.txt
    changelogs/1.txt         versionCode = 1
    images/phoneScreenshots/ (add real PNGs before submission)
  vi-VN/
    short_description.txt
    full_description.txt
    changelogs/1.txt
```

## Submission Checklist

### Before opening a Merge Request on fdroiddata

- [ ] Tag release commit: `git tag v1.0.0 && git push --tags`
- [ ] Verify FOSS APK is Sentry-free:
  ```bash
  unzip -l app/build/outputs/apk/foss/release/*.apk | grep 'io/sentry'
  # must return nothing
  ```
- [ ] Verify no `MANAGE_EXTERNAL_STORAGE` in FOSS manifest:
  ```bash
  unzip -p app/build/outputs/apk/foss/release/*.apk AndroidManifest.xml \
    | strings | grep -i manage_external
  # must return nothing
  ```
- [ ] `minSdk = 26`, `targetSdk ≤ current stable API` — no Play-Store-only APIs used.
- [ ] Add real screenshots (≥2) to `metadata/en-US/images/phoneScreenshots/`.
- [ ] Fill `metadata/com.coreclean.app.yml` — see build recipe above.
- [ ] Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata), add metadata YAML.
- [ ] Run `fdroid lint metadata/com.coreclean.app.yml` locally to check syntax.
- [ ] Submit Merge Request to fdroiddata with title:
  `New app: CoreClean — transparent Android optimizer (FOSS, no tracking)`

### Merge Request description template
```
## Summary
CoreClean is a transparent Android device optimizer.

- Package: com.coreclean.app
- License: Apache-2.0
- Source: https://github.com/annguyn/CoreClean
- No anti-features: FOSS flavor strips all proprietary SDKs.
- Build flavor: `foss` (gms flavor excluded from F-Droid)

## Verification
- [ ] fdroid build com.coreclean.app:1 succeeds
- [ ] APK contains no io.sentry classes
```

### After acceptance
- [ ] Monitor F-Droid build bot results (gitlab.com/fdroid/fdroid-bot).
- [ ] Ensure future releases update `metadata/com.coreclean.app.yml` Builds list
  and bump versionCode in `app/build.gradle.kts`.
- [ ] Update `metadata/en-US/changelogs/<versionCode>.txt` for each release.

## F-Droid Categories
- System
- Utilities

## Source Code
https://github.com/annguyn/CoreClean

## Issue Tracker
https://github.com/annguyn/CoreClean/issues

## Changelog
See `CHANGELOG.md` and `metadata/en-US/changelogs/` in the repository root.

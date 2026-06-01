# Performance Notes

## Cold Start Target

- **Goal:** < 800 ms on mid-tier device (Snapdragon 665, 4 GB RAM)
- **Measured via:** Logcat `ActivityManager: Displayed` or Android Vitals

## Baseline Profile (planned)

When a `:baselineprofile` module is added:

```bash
./gradlew :baselineprofile:generateBaselineProfile
```

The generated `baseline-prof.txt` is placed in `app/src/main/` and picked up by R8 at release build time.

### Macrobenchmark flow for baseline generation:
1. Cold launch → `HomeScreen` renders
2. Tap "Media Scanner" → `MediaScreen` loads
3. Trigger duplicate scan

## R8 Release Config

- Minification: enabled (`isMinifyEnabled = true`)
- Resource shrinking: enabled (`isShrinkResources = true`)
- ProGuard rules in `app/proguard-rules.pro`:
  - Keeps Kotlin, Serialization, Room, Hilt, Coil, Sentry, WorkManager
  - Source file / line number attributes preserved for crash symbolication

## Sentry Performance Tracing

`tracesSampleRate = 0.1` in `CleanerApp` → 10% of sessions send performance traces.
Review in Sentry dashboard: filter by transaction name `HomeActivity`.

## Known Bottlenecks

- `MediaScanWorker`: MD5 hashing 256 KB per file — runs only when `BATTERY_NOT_LOW`
- `AppUsageDataSource`: `queryUsageStats` can be slow (>500 ms) on first call
- `JunkScanner.scanAppCache`: iterates all installed packages — O(n) with StorageStatsManager

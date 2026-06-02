# Performance Notes

## Cold Start Target

- **Goal:** < 800 ms on mid-tier device (Snapdragon 665, 4 GB RAM)
- **Measured via:** Logcat `ActivityManager: Displayed` or Android Vitals

## Baseline Profile (pending hardware run)

`:baselineprofile` module added with `BaselineProfileGenerator` using UiAutomator.

The hand-crafted `baseline-prof.txt` was removed in Sprint 8.1 because the signatures did not match compiled Kotlin bytecode (wrong arg types for `CleanerNavGraph`, wrong parameter order for `HomeScreen`). A real profile must be generated on a physical device or managed emulator (API 33+):

```bash
./gradlew :baselineprofile:connectedGmsReleaseAndroidTest
# Copy output from:
# baselineprofile/build/outputs/managed_device_android_test_additional_output/
# into app/src/main/generated/baselineProfiles/baseline-prof.txt
```

`androidx.profileinstaller` is included so the profile is installed on API 28+ devices once the file is committed.

### Macrobenchmark flow for baseline generation:
1. Cold launch (StartupMode.COLD) → `HomeScreen` renders
2. Tap "Media Scanner" → `MediaScreen` loads
3. Scroll one page in media grid
4. Trigger duplicate scan

### Last measured numbers (Pixel 6, API 34 emulator — no valid profile):
- Cold start without profile: ~680 ms

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

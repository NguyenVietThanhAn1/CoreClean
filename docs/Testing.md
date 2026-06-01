# Testing Strategy

## Pyramid

```
        /\
       /  \    Macrobenchmark (1 flow: cold start → Home → Media)
      /----\
     /      \  Instrumentation (Hilt + WorkManager + ContentResolver thật)
    /--------\
   /          \ Robolectric (DataSource có ContentResolver / PackageManager)
  /------------\
 /              \ Unit (Domain UseCase, ViewModel, DuplicateDetector, ...)
/________________\
```

## Tier 1 — Unit Tests (`app/src/test`)

**Mục tiêu:** ≥ 70% coverage cho `domain/` + `data/datasource/`.

| File | Cover |
|---|---|
| `DuplicateDetectorTest` | Group by size, normalize name, MD5 hash |
| `MediaViewModelTest` | Loading → Success transition, scan flag |
| `BatteryRepositoryImplTest` | BroadcastReceiver fake → Flow emit |
| `SettingsViewModelTest` | DataStore round-trip cho theme/lang |
| `AppUsageRepositoryImplTest` | Filter system app, sort, edge case updated-system-app |
| `MergeContactsUseCaseTest` | Aggregation logic |
| `MainDispatcherRule` | Helper |

**Tools:**
- JUnit 4 (Android default).
- `kotlinx-coroutines-test` cho `runTest { }`.
- `Turbine` cho Flow assertion.
- Robolectric cho test cần `Context` (DataSource ContentResolver, ContactsContract).

**Convention:** `<name>Test.kt`. Method name dạng `methodName_condition_expectedResult()`.

## Tier 2 — Instrumented Tests (`app/src/androidTest`)

**Mục tiêu:** verify integration điểm khó mock — Room migration, WorkManager scheduling, real ContentResolver.

| File | Cover |
|---|---|
| `MediaScanWorkerInstrumentationTest` | Worker `Result.success()` + DB populated |
| `AppDatabaseMigrationTest` (Sprint 6) | v1 → v2 migration with seeded data |
| `OnboardingFlowTest` (Sprint 6) | Espresso/Compose UI: navigate 3 steps → HomeScreen |

**Tools:**
- `androidx.test.ext:junit`, `espresso-core`, `compose-ui-test-junit4`.
- `work-testing` cho `TestListenableWorkerBuilder`.
- `HiltAndroidTest` + `HiltAndroidRule` cho DI trong test.

## Tier 3 — Macrobenchmark (`:baselineprofile` module, Sprint 6)

**Mục tiêu:** đo + sinh baseline profile cho R8.

```bash
./gradlew :baselineprofile:connectedBenchmarkAndroidTest
./gradlew :baselineprofile:generateBaselineProfile
```

Flow:
1. Cold launch.
2. HomeScreen rendered.
3. Tap "Media Scanner".
4. Tap "Tất cả ảnh" tab.

Target: cold start < 800ms mid-tier (xem [`Performance.md`](Performance.md)).

## CI Integration

`.github/workflows/android-ci.yml` chạy:

```yaml
- run: ./gradlew :app:testDebugUnitTest          # tier 1
- run: ./gradlew :app:lintDebug                  # static check
- run: ./gradlew :app:jacocoTestReport           # coverage
- uses: codecov/codecov-action@v4               # upload to Codecov
```

Instrumentation (tier 2) chạy trên Firebase Test Lab hoặc emulator runner.
Macrobenchmark (tier 3): `:baselineprofile` module implemented Sprint 6 — chạy trên physical device hoặc emulator API 28+.

## JaCoCo Coverage

Sprint 6: JaCoCo plugin added. Report generated at `app/build/reports/jacoco/jacocoTestReport/`.
Codecov badge hiện trên README.

Target coverage (domain/ + datasource/): ≥ 70%.

## What NOT to test

- Compose preview functions (`@Preview`).
- DI module wiring (Hilt verify ở compile time + chạy thực tế là đủ).
- 3rd-party library internals (Room, Hilt — đã được test bởi Google).

## Fake vs Mock

- **Prefer Fake** (in-memory implementation) cho Repository.
- **Mock** (Mockito / Mockk) chỉ khi fake quá phức tạp (vd: `PackageManager`).
- Không mock data class — luôn dùng instance thật.

## Test Doubles Location

```
app/src/test/java/com/coreclean/app/testdoubles/
  FakeMediaRepository.kt
  FakeBatteryRepository.kt
  FakeAppPreferences.kt
```

## Coverage Reporting (Sprint 6)

- JaCoCo plugin trong `app/build.gradle.kts`.
- CI upload coverage XML → Codecov badge trong README.

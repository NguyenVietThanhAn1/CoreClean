# Changelog

## [Sprint 5] - 2026-06-01

### Added
- RAM Monitor module: reactive 2s-tick flow via `ActivityManager.MemoryInfo`, top-process list
- APK Analyzer module: `PackageManager` + `StorageStatsManager`, sort/filter/uninstall flow
- Contacts Merge UI: `MergeContactDialog` + `MergeContactsUseCase` (AggregationException)
- SAF (Storage Access Framework) support for Junk Cleaner: `OpenDocumentTree` folder picker
- Privacy Dashboard: permission status, stored data stats, clear history, open app settings
- Sentry crash reporting: `CrashReporter` interface, `SentryCrashReporter` adapter, opt-in toggle
- i18n: `values/strings.xml` (Vietnamese default), `values-en/strings.xml` (English)
- AppCompat locale switcher in Settings via `AppCompatDelegate.setApplicationLocales()`
- Language selector (System / Vietnamese / English) in Settings
- Crash reporting toggle in Settings
- Privacy Dashboard link in Settings
- `RamRoute`, `AppAnalyzerRoute`, `PrivacyRoute` in navigation
- `count()` queries added to `ScanResultDao` and `PendingReviewDao`
- `SAF_FOLDER_URIS`, `APP_LANGUAGE`, `CRASH_REPORTING` keys in `AppPreferenceKeys`
- `AppLanguage` enum in `AppPreferences`
- `BuildConfig.SENTRY_DSN` from `local.properties`
- Signing config in `build.gradle.kts` from `local.properties` (optional, skipped in CI)
- `gradleLocalProperties` import for reading local.properties
- AppCompat dependency (`libs.appcompat`)
- Sentry dependency (`libs.sentry.android`)
- `MockK Android` + `androidx.test:core` for androidTest
- ProGuard rules: Kotlin, Serialization, Room, Hilt, Coil, Sentry, WorkManager
- `docs/i18n.md`: guide for adding new locales
- `docs/Performance.md`: cold-start target, baseline profile notes, R8 config
- Tests: `ContactDataSourceTest`, `ContactRepositoryImplTest`, `MergeContactsUseCaseTest`
- Tests: `MediaScanWorkerInstrumentationTest` (androidTest, `TestListenableWorkerBuilder`)
- CI matrix `java-version: [17, 21]`; added `release-check` job

### Changed
- `ContactViewModel`: added `startMerge`, `confirmMerge`, `cancelMerge`, `dismissMergeMessage`
- `ContactScreen`: merge button in Duplicate tab, `MergeContactDialog` shown inline
- `SettingsViewModel`: added `setLanguage`, `setCrashReporting`
- `SettingsScreen`: Language selector, Crash reporting toggle, Privacy Dashboard link
- `CleanerApp.onCreate`: Sentry init when DSN present
- `AppPreferences`: new keys + `AppLanguage` enum
- `HomeScreen`: added RAM Monitor and App Analyzer cards
- `CleanerNavGraph`: 3 new routes (Ram, AppAnalyzer, Privacy)
- `ScanResultDao` / `PendingReviewDao`: `count()` query added
- `.gitignore`: sentry.properties, keystore.properties, baselineprofile/build/
- `proguard-rules.pro`: comprehensive rules for all major dependencies
- CI workflow: matrix java versions, release-check job

### Fixed
- Sprint 4 debt: `AppUsageRepositoryImplTest` gains "user-updated system app" case
- Sprint 4 debt: contacts tests coverage (DataSource, Repository, MergeUseCase)
- Sprint 4 debt: `MediaScanWorkerInstrumentationTest` added to androidTest

---

## [Sprint 4] - 2026-06-01

### Added
- Permission Onboarding flow (3-step HorizontalPager: Storage, Usage Stats, Notifications)
- `OnboardingRoute` as conditional startDestination via DataStore flag `onboarding_done`
- App Usage module: `AppUsageDataSource`, `AppUsageRepositoryImpl`, `AppUsageViewModel`, `AppUsageScreen`
  - 7d/30d range selector, system app filter, NoPermission empty state
- Contacts module (lite): `ContactDataSource`, `ContactRepositoryImpl`, `ContactViewModel`, `ContactScreen`
  - 3 tabs: All / Duplicates / Incomplete; detection only (no merge UI)
- Junk Cleaner module: `JunkScanner`, `ScanJunkUseCase`, `CleanJunkUseCase`, `JunkViewModel`, `JunkScreen`
  - APP_CACHE (guide), TEMP_FILES, RESIDUAL_APK, EMPTY_FOLDERS scan + delete
- Settings screen: `SettingsViewModel`, `SettingsScreen`
  - Theme (System/Light/Dark), Dynamic Color toggle (API 31+), Background scan + period, Reset onboarding
- Large File detector: `FindLargeFilesUseCase` (MediaStore >50 MB), shown in StorageScreen with multi-select → SafetyReview
- `DataStoreModule` + `androidx.datastore:datastore-preferences:1.1.3` dependency
- WorkManager provided via Hilt (`AppModule.provideWorkManager`)
- `buildConfig = true` in `buildFeatures` for `BuildConfig.VERSION_NAME`
- `AppPreferenceKeys` + `ThemeMode` enum in `core/preferences/`
- `CoreCleanTheme` gains `dynamicColor: Boolean` parameter
- CI: `.github/workflows/android-ci.yml` — build + test + lint + upload reports
- CI: `.github/PULL_REQUEST_TEMPLATE.md` — checklist
- Tests: `AppUsageRepositoryImplTest`, `JunkScannerTest`, `SettingsViewModelTest`

### Changed
- `HomeScreen`: AppUsage + Contacts `enabled = true`; new Junk Cleaner card; Settings icon wired to `SettingsRoute`
- `CleanerNavGraph`: added `OnboardingRoute`, `JunkRoute`, `SettingsRoute`; `startDestination` parameter
- `CoreCleanApp`: reads DataStore onboarding flag + SettingsViewModel to apply theme/dynamicColor
- `MainActivity`: injects `DataStore<Preferences>`, passes to `CoreCleanApp`
- `StorageViewModel`: also loads large files via `FindLargeFilesUseCase`
- `AppModule`: provides `WorkManager` singleton
- `.gitignore`: added Untitled drafts, CI artifacts, Compose Compiler metrics, KSP cache

### Removed
- `CoreClean/CoreClean/` nested directory (Obsidian vault artifact)

---

## [Sprint 3] - 2026-06-01

### Added
- HomeScreen dashboard (2-column grid, FeatureCard with enabled/disabled state)
- Storage Analyzer module: StatFs + MediaStore breakdown, Canvas donut chart
- Battery Monitor module: reactive BroadcastReceiver Flow, Canvas level circle
- `PendingReviewEntity` + `PendingReviewDao` (Room v2) for large selection fallback
- `work-testing` + `robolectric` test dependencies
- New tests: StorageDataSourceTest, BatteryRepositoryImplTest, MediaScanWorkerTest
- docs/Architecture.md (ASCII layer diagram + Safety Delete flow)
- docs/Contributing.md (commit convention, branch naming, merge checklist)
- README.md at project root

### Changed
- `ReviewRoute` now carries `imageIds: List<Long>`; > 200 ids fallback to Room `pending_review`
- `SafetyReviewViewModel` reads images via `SavedStateHandle.toRoute<ReviewRoute>()`
- `MediaViewModel` removes `SelectedImagesHolder`, injects `PendingReviewDao`
- `themes.xml` updated to `NoActionBar` + transparent status/nav bars
- `MainActivity` calls `setTheme()` before `enableEdgeToEdge()` for splash sync
- All docs updated: note.md, roadmap, knownIssues, AI Context, PlannedFeatures

### Removed
- `core/SelectedImagesHolder.kt` (replaced by SavedStateHandle + Room)
- `ExampleUnitTest.kt` (template artifact)
- Stale `.idea/` files from git tracking

### Fixed
- Full Android `.gitignore` replaces minimal template

---

## [Sprint 2] - 2026-06-01

- Safety Review screen + Android 11+ `MediaStore.createDeleteRequest`
- Content-hash duplicate detection (MD5 first 256 KB, parallel Semaphore)
- Permission denied recovery (PermissionDenied state + Retry/Settings buttons)
- `CoreCleanTheme` with dynamic colors (Android 12+)
- `MediaScanWorker` periodic background scan (12h, BATTERY_NOT_LOW)
- Unit tests: DuplicateDetectorTest, MediaViewModelTest (Turbine)

---

## [Sprint 1] - 2026-04-xx

- Initial project scaffold
- Media Scanner with MediaStore query
- Basic duplicate detection (size + normalized name)
- Room schema, Hilt DI, WorkManager setup

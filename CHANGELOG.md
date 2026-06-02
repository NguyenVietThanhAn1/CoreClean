# Changelog

## [Sprint 8] - 2026-06-02

### Added
- `NoOpCrashReporter` in `src/foss/` — implements `CrashReporter` with all no-ops; no io.sentry dependency
- `SentryInitializer.kt` in `src/gms/` — extension function `CleanerApp.initializeSentry(dataStore)` that reads DataStore `crash_reporting` flag on IO before calling `SentryAndroid.init`; Sentry is never initialized if user has not opted in
- `SentryInitializer.kt` in `src/foss/` — no-op counterpart; foss builds compile with zero Sentry references
- `SentryCrashReporterTest` in `src/testGms/` (moved from shared `src/test/`)
- `NoOpCrashReporterTest` in `src/testFoss/` — verifies no-throw and no external side-effects

### Fixed
- **FOSS flavor Sentry leak**: `gmsImplementation(libs.sentry.android)` replaces `implementation`; foss APK no longer contains the Sentry SDK
- **Privacy claim**: `CleanerApp` now reads `crash_reporting` DataStore flag via `runBlocking(IO)` before any `SentryAndroid.init`; Sentry is skipped by default (opt-out was previously impossible before first Settings open)
- `SettingsViewModel.setCrashReporting(false)` now calls `crashReporter.setEnabled(false)` through the domain interface instead of `io.sentry.Sentry.close()` directly; foss builds no longer reference io.sentry in presentation layer

### Changed
- `SentryCrashReporter` and `CrashReporterModule` moved from `src/main/` to `src/gms/` source set
- `CleanerApp` injects `DataStore<Preferences>` (previously only `HiltWorkerFactory`)
- `SettingsViewModel` injects `CrashReporter` (new parameter after `WorkManager`)
- Removed empty directories: `core/base`, `core/extensions`, `core/utils`, `data/datasource/file`, `data/mapper`, `domain/usecase/appusage`, `presentation/appusage`

---

## [Sprint 7] - 2026-06-08

### Added
- AI-based perceptual duplicate detection (pHash, toggleable via Settings — slower but more accurate)
- AMOLED black mode in Settings (Dark theme only, reduces power on OLED displays)
- Tablet two-pane adaptive layout (Expanded width class — NavigationRail + content pane)
- F-Droid FOSS build flavor (`foss` product flavor — Sentry disabled, all other features intact)
- GitHub Pages workflow for Privacy Policy hosting (annguyn.github.io/CoreClean)
- Room database proper migrations (v1→v2→v3 via addMigrations(), no data loss)
- `PerceptualHasher` class (DCT-based pHash, 32×32 grayscale, 8×8 DCT, 64-bit hash)
- `PERCEPTUAL_DEDUPE` preference key in AppPreferenceKeys
- Play Store listing documentation (docs/PlayStoreListing.md)
- F-Droid metadata documentation (docs/FDroidMetadata.md)
- Screenshot specifications (docs/images/screenshots/README.md)
- Sentry source-map upload step in android-ci.yml

### Fixed
- i18n debt: all hardcoded strings in 7 screens replaced with string resources
- `ui/media/` moved to `presentation/media/` (architecture cleanup)
- `@Serializable` annotations added to `Frequency` and `JunkCategory` enums
- Privacy Policy URL updated to GitHub Pages URL (annguyn.github.io/CoreClean/PrivacyPolicy)

### Changed
- BaselineProfileGenerator skips onboarding screen for faster cold-start profiling
- Sentry runtime toggle works via close() call
- DevelopmentRoadmap.md: Phase 3 complete, Phase 4 complete, Phase 5 (Distribution) added

---

## [Sprint 6] - 2026-06-01

### Added
- **Smart Cleaning Suggestions**: `GenerateSuggestionsUseCase` with 5 rule-based suggestions (duplicates, unused apps, oversized downloads, stale screenshots, low storage); displayed as LazyRow on HomeScreen
- **Scheduled Auto-Cleaning**: `AutoCleanWorker` + `ScheduleConfig` data class; safe categories only (TEMP_FILES, EMPTY_FOLDERS, RESIDUAL_APK); Settings section with frequency picker + category multi-select
- **Notification Recommendations**: `RecommendationNotifier` with 7-day rate limit; triggers on <5% storage or >2 GB duplicate detection; `recommendations` notification channel; opt-out toggle in Settings
- **Battery Usage Prediction**: `BatteryHistoryEntity` + `BatteryHistoryDao`; `BatteryHistoryRecorder` periodic worker (15 min); `PredictBatteryRemainingUseCase` (linear regression, 24 h window, ≥4 samples); prediction card on BatteryScreen with "ước tính" disclaimer
- **Baseline Profile**: `:baselineprofile` module with `BaselineProfileGenerator` (UiAutomator cold-start → Home → Media Scanner flow); `baseline-prof.txt` committed
- **Accessibility (A11y)**: All informative icons now have `contentDescription`; Canvas composables (BatteryCircle, StorageDonutChart) wrapped in `semantics { contentDescription }`; `cd_*` strings in `strings.xml`
- **Tablet/Foldable**: `WindowSizeClass` passed from `MainActivity` → `CoreCleanApp` → `HomeScreen`; compact=2 columns, medium/expanded=3 columns
- **Dependabot**: `.github/dependabot.yml` for Gradle (weekly) + GitHub Actions (monthly)
- **JaCoCo coverage**: `jacocoTestReport` task in `app/build.gradle.kts`; Codecov upload in CI
- **Privacy Dashboard**: "Clear battery history" button; privacy policy link (GitHub raw URL)
- `BatteryHistoryDao.count()` for privacy data display
- `SCHEDULE_CONFIG_JSON`, `RECOMMENDATIONS_ENABLED`, `NOTIF_LAST_*` keys in `AppPreferenceKeys`
- `ScheduleConfig` + `Frequency` domain models (kotlinx.serialization)
- `HomeViewModel` to load suggestions on home screen
- `material3-window-size-class` + `profileinstaller` dependencies

### Changed
- `AppDatabase` bumped to version 3 (adds `battery_history` table; `fallbackToDestructiveMigration`)
- `BatteryViewModel` now injects `PredictBatteryRemainingUseCase` and exposes `prediction` StateFlow
- `SettingsState` + `SettingsViewModel` extended with `scheduleConfig` + `recommendationsEnabled`
- `CoreCleanApp` + `CleanerNavGraph` accept `WindowSizeClass?` parameter
- `PrivacyViewModel` + `PrivacyDashboardScreen` include battery history count + clear action
- `BatteryDataSource` gains `getBatteryInfo()` one-shot snapshot method
- `HomeScreen` uses string resources for all text (was hardcoded)
- `SettingsScreen` uses string resources throughout

### Fixed
- Removed `ExampleInstrumentedTest.kt` (wrong package, wrong app ID)
- `app/lint-baseline.xml` created and configured in `lint { baseline = ... }`

### Removed
- `ExampleInstrumentedTest.kt` + empty `com.example.coreclean` directory

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

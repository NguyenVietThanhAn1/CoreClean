# Changelog

## [Sprint 9.2] - 2026-07-17 — Crash-report privacy hardening

### Fixed
- Crash reports (for users who opt into crash reporting) no longer leak the tail of a file name that contains a space — e.g. a WhatsApp download or a human-typed file name — the whole path is now redacted instead of stopping at the first space
- A domain name starting with a digit (e.g. `1.bp.blogspot.com`) is now fully redacted instead of leaking a chopped prefix or, in some cases, passing through completely untouched
- A crash report can no longer slip a real installed-package name past redaction by prefixing it with a fake "http"-like scheme (e.g. `shttp://`)
- Phone numbers written with no punctuation (just spaces between the digits) are intentionally left unredacted, to avoid mistakenly redacting unrelated numeric data such as image dimensions

## [Sprint 9.1] - 2026-06-03 — Post-review: French locale reachable + metadata fix

### Added
- `AppLanguage.FRENCH("fr")` enum value — French is now selectable in the in-app language switcher
- `settings_lang_fr` string in all three locales (`values/`, `values-en/`, `values-fr/`)
- `app/src/main/res/xml/locales_config.xml` — per-app language support (Android 13+, lists vi/en/fr)
- `android:localeConfig="@xml/locales_config"` attribute on `<application>` in `AndroidManifest.xml`
- `androidResources { localeFilters += listOf("vi","en","fr") }` in `build.gradle.kts` — pins supported locales and reduces APK size

### Fixed
- `SettingsScreen.kt`: exhaustive `when (lang)` now covers `AppLanguage.FRENCH`; compile-time guard if a future locale is added without a label
- `metadata/vi-VN/short_description.txt`: trimmed from 83 → 80 characters ("bộ nhớ" → "RAM"); Play Store / F-Droid limit is 80 chars

---

## [Sprint 8.3] - 2026-06-02 — Sprint 8.2 follow-up: 3 fix sót

### Fixed
- `SettingsViewModel.setCrashReporting`: completes fix from Sprint 8.2 — B2: previously only called `crashReporter.setEnabled(false)`; now applies both directions at runtime via `withContext(Dispatchers.IO)` so enabling crash reporting no longer requires a restart
- `HomeViewModel`: completes fix from Sprint 8.2 — B8: `NOTIF_PERMISSION_NEEDED` flag was written but never read; now exposed as `notifPermissionNeeded: StateFlow<Boolean>` with `clearNotifPermissionFlag()` to clear it
- `HomeViewModel.loadSuggestions`: new race condition fix: concurrent calls now guarded by `Mutex.tryLock()` — second and third concurrent invocations return immediately instead of overwriting a fresh result with stale cache or wasting heavy IO

### Added
- `HomeScreen`: notification permission banner displayed when `notifPermissionNeeded == true`; "Enable" launches `POST_NOTIFICATIONS` system dialog (API 33+), "Dismiss" clears the flag without requesting
- String resources `notif_permission_banner_title / _action / _dismiss` (vi + en)
- `SettingsViewModelTest`: `setCrashReporting(true/false)` → `verify { crashReporter.setEnabled(true/false) }`
- `HomeViewModelTest`: `notifPermissionNeeded` reflects DataStore flag; `clearNotifPermissionFlag()` resets it; concurrent `loadSuggestions()` calls invoke `appListRepository` exactly once

---

## [Sprint 8.2] - 2026-06-02 — Post-review hot-fixes round 2

### Fixed
- `SentryInitializer` (gms): replaced `runBlocking(Dispatchers.IO)` with `MainScope().launch(Dispatchers.IO)` — Sentry init no longer blocks the main thread at startup
- `SentryCrashReporter`: inject `@ApplicationContext`; `setEnabled(true)` now calls `SentryAndroid.init` at runtime so toggling crash reporting on works without a restart
- `SettingsScreen`: removed restart hint for crash-reporting off→on toggle (runtime re-init makes it unnecessary)
- `AutoCleanWorker` + `SettingsViewModel`: both scheduling paths now call `enqueueUniqueWork("auto_clean", REPLACE, ...)` with `setRequiresBatteryNotLow + setRequiresDeviceIdle` constraints; `scheduleNext` is also called in the `getOrElse` branch so the chain never breaks on a failed run
- `JunkScanner.scanResidualApks`: now also walks user-granted SAF trees for `.apk` files in addition to the app's own external cache directory
- `GenerateSuggestionsUseCase` Rule 2: added `usageStatsAvailable: Boolean` param — when `false` the rule is skipped entirely to avoid false positives from `installTime` fallback
- `HomeViewModel`: passes `usageStatsAvailable = appUsageRepository.hasUsageAccessPermission()` to suggestions use case
- `HomeViewModel`: caches `List<CleaningSuggestion>` as JSON in DataStore (`HOME_SUGGESTIONS_CACHE_JSON`); on init the cached suggestions are emitted immediately before heavy IO runs
- `SettingsViewModel`: `AppLanguage.valueOf(...)` wrapped in `runCatching { }.getOrDefault(SYSTEM)` to prevent crash on unknown stored value
- `AppPreferences`: `APP_LANGUAGE` comment corrected to `// SYSTEM | VIETNAMESE | ENGLISH`
- `RecommendationNotifier` + `AutoCleanWorker.showResultNotification`: POST_NOTIFICATIONS permission check on API 33+ before `notify()`; sets `NOTIF_PERMISSION_NEEDED = true` pref for Home banner when missing
- `AutoCleanWorker.doWork()`: reads `RECOMMENDATIONS_ENABLED` pref; skips `showResultNotification` when false (cleaning and rescheduling still run)

### Added
- `CleaningSuggestion` sealed class: all subclasses annotated `@Serializable` with `@SerialName` discriminators
- `JunkItem`: annotated `@Serializable`
- `AppPreferenceKeys`: `HOME_SUGGESTIONS_CACHE_JSON`, `NOTIF_PERMISSION_NEEDED` keys
- `SentryCrashReporterTest`: `setEnabled(true)` after `setEnabled(false)` re-init test
- `AutoCleanWorkerTest` (Robolectric): verifies `enqueueUniqueWork` with REPLACE is called for each `setAutoCleanEnabled(true)` invocation
- `JunkScannerTest`: SAF tree containing `.apk` files is picked up under RESIDUAL_APK
- `GenerateSuggestionsUseCaseTest`: `usageStatsAvailable=false` → no `UnusedApp` suggestions
- `HomeViewModelTest`: cold load reads cached JSON and emits suggestions before heavy IO completes

---

## [Sprint 8.1] - 2026-06-02 — Post-review hot-fixes

### Fixed
- `JunkScanner.clean()`: EMPTY_FOLDERS items (content URIs) now deleted via `DocumentFile.fromSingleUri().delete()` instead of `File(path).delete()` which always returned false for SAF paths
- `AutoCleanWorker.doWork()`: reads `SAF_FOLDER_URIS` from DataStore and passes them to `ScanJunkUseCase` so empty-folder scanning works in background jobs

### Changed
- `baseline-prof.txt` removed — hand-crafted signatures did not match Kotlin bytecode; baseline profile pending hardware run (see docs/Performance.md)
- Removed `buildConfigField SENTRY_ENABLED` from both product flavors (dead code — never read at runtime)
- Sentry source-map upload step removed from CI (`sentryUploadProguardMappingsGmsRelease` task does not exist without the Sentry Gradle plugin)
- `SettingsScreen`: crash-reporting section hidden in FOSS flavor; restart hint shown in GMS flavor when toggle transitions from disabled to enabled

### Added
- `JunkScannerTest`: two new tests covering EMPTY_FOLDERS SAF deletion path (mock `DocumentFile`) and non-existent SAF document

---

## [Sprint 8] - 2026-06-02 — Distribution blockers

### Added
- `NoOpCrashReporter` in `src/foss/` — implements `CrashReporter` with all no-ops; no io.sentry dependency
- `SentryInitializer.kt` in `src/gms/` — extension function reads DataStore `crash_reporting` flag on IO before `SentryAndroid.init`; Sentry never initializes without user opt-in
- `SentryInitializer.kt` in `src/foss/` — no-op; foss builds compile with zero Sentry references
- `SentryCrashReporterTest` in `src/testGms/`, `NoOpCrashReporterTest` in `src/testFoss/`
- `HomeViewModel` now injects `AppListRepository`, `AppUsageRepository`, `ScanJunkUseCase`, `DataStore<Preferences>` — all 6 parameters of `GenerateSuggestionsUseCase` are populated
- 6-hour suggestion cache: heavy repos (app list, usage stats, junk scan) are rate-limited; cache timestamp stored in DataStore `home_suggestions_cache_ts`
- `baseline-prof.txt` committed to `app/src/main/generated/baselineProfiles/` with startup-class hints for `profileinstaller`
- `QUERY_ALL_PACKAGES` permission in manifest with justification in `docs/Permissions.md`

### Fixed
- **FOSS Sentry leak**: `"gmsImplementation"(libs.sentry.android)` — foss APK contains zero Sentry SDK bytes
- **Privacy opt-in**: `CleanerApp` reads `crash_reporting` DataStore flag before any Sentry init
- **Play Store blocker**: removed `MANAGE_EXTERNAL_STORAGE`; `JunkScanner.scanEmptyFolders()` now walks DocumentFile trees from user-granted SAF URIs only
- `SettingsViewModel.setCrashReporting(false)` uses `crashReporter.setEnabled(false)` via domain interface (no direct `io.sentry.*` in `src/main/`)
- **Rule 3 bug**: `GenerateSuggestionsUseCase` filter corrected from `DOWNLOAD` substring (matched nothing) to `RESIDUAL_APK` category
- CI `testDebugUnitTest` ambiguity resolved: separate `testGmsDebugUnitTest` and `testFossDebugUnitTest` steps

### Changed
- `SentryCrashReporter` and `CrashReporterModule` moved from `src/main/` to `src/gms/` source set
- `ScanJunkUseCase.invoke()` accepts `safFolderUriStrings: Set<String> = emptySet()`; `JunkViewModel.scan()` passes current SAF URIs
- `CleanerApp` injects `DataStore<Preferences>` (previously only `HiltWorkerFactory`)
- `SettingsViewModel` injects `CrashReporter` (new parameter after `WorkManager`)
- CI `android-ci.yml`: explicit FOSS build step added; `assembleRelease` split into `assembleGmsRelease` + `assembleFossRelease`
- Removed 3 unused Material3 Adaptive deps (`adaptive`, `adaptive-navigation`, `adaptive-layout`) — tablet layout uses `WindowSizeClass` only
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

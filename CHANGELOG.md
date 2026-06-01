# Changelog

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

# Changelog

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

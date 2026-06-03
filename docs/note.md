# CoreClean - Android Cleaner App

## Tong quan du an
**CoreClean** la mot ung dung ho tro toi uu hoa thiet bi Android, duoc xay dung voi muc tieu
cung cap cac cong cu don dep va quan ly he thong hieu qua, bao mat va hien dai.

- **Package Name:** `com.coreclean.app`
- **Kien truc:** Clean Architecture (Domain, Data, Presentation layers)
- **UI Framework:** Jetpack Compose (Modern UI)

## Trang thai module (Sprint 7 - thang 6/2026)

| Module                  | Trang thai          | Ghi chu                                                  |
|-------------------------|---------------------|----------------------------------------------------------|
| Media Scanner           | Implemented         | Scan, dedup (MD5), SafetyReview, Worker                  |
| Storage Analyzer        | Implemented         | StatFs + MediaStore + Large File detector                 |
| Battery Monitor         | Implemented (basic) | BroadcastReceiver reactive Flow                          |
| App Usage               | Implemented         | UsageStatsManager, 7d/30d range, NoPermission state      |
| Contacts                | Implemented (full)  | Detect + Merge UI (AggregationException)                 |
| Junk Cleaner            | Implemented         | APP_CACHE guide, TEMP/APK/EMPTY scan & delete, SAF       |
| Permission Onboarding   | Implemented         | HorizontalPager 3 step, DataStore flag                   |
| Settings                | Implemented         | Theme, DynamicColor, Language, CrashReporting, Privacy   |
| RAM Monitor             | Implemented         | ActivityManager reactive flow, top process list          |
| APK Analyzer            | Implemented         | PackageManager + StorageStatsManager, sort/filter/uninstall |
| Privacy Dashboard       | Implemented         | Permissions, stored data, clear history                  |
| Crash Reporting         | Implemented         | Sentry opt-in, CrashReporter interface                   |
| i18n                    | Implemented         | values/ (vi), values-en/ (en), locale switcher            |

## Cong nghe su dung (Tech Stack)
- **Kotlin 2.1.10**, **Jetpack Compose**, **Hilt**, **Room 2**, **Coroutines & Flow**
- **Navigation Compose (Type-safe)** voi Kotlin Serialization
- **Coil 3**, **WorkManager**, **Material Icons Extended**

## Cau truc du an
```
app/src/main/java/com/coreclean/app/
  core/di/        # Hilt modules (App, Database, Media, Storage, Battery)
  data/           # Repository impls, DataSources, Room entities/DAOs, Worker
  domain/         # Models (MediaImage, StorageInfo, BatteryInfo), Repositories, UseCases
  presentation/   # Screens + ViewModels (home, storage, battery, review, theme)
  ui/media/       # MediaScreen + MediaViewModel
```

## Huong dan cai dat & Build
1. **Yeu cau:** Android Studio Hedgehog+, JDK 17+
2. **Build:** `./gradlew assembleDebug`
3. **Test:** `./gradlew testDebugUnitTest`

## Sprint History
- **Sprint 1 (init):** Project scaffold, Hilt + Room + WorkManager setup
- **Sprint 2:** Media Scanner full (SafetyReview Android 11+, content-hash dedup, Worker scan)
- **Sprint 3:** Storage Analyzer (basic), Battery Monitor (reactive), HomeScreen dashboard,
  SelectedImagesHolder removed (SavedStateHandle + Room pending_review fallback)
- **Sprint 4:** Permission Onboarding flow, App Usage module, Contacts module (lite),
  Junk Cleaner (basic), Settings screen, Large File detector, CI workflow GitHub Actions,
  DataStore Preferences, WorkManager DI, BuildConfig enabled
- **Sprint 5:** RAM Monitor, APK Analyzer, Contacts Merge UI, SAF Junk, Privacy Dashboard,
  Sentry crash reporting (opt-in), i18n vi/en, AppCompat locale switcher, Settings expanded
  (Language/CrashReporting), ProGuard rules updated, CI matrix (Java 17+21), release-check job,
  ContactDataSourceTest, ContactRepositoryImplTest, MergeContactsUseCaseTest,
  MediaScanWorkerInstrumentationTest, updated AppUsageRepositoryImplTest
- **Sprint 6:** Smart Suggestions, AutoClean, Battery Prediction, A11y, Tablet (WindowSizeClass),
  Baseline Profile, Dependabot, JaCoCo + Codecov, Privacy Dashboard expanded, RecommendationNotifier
- **Sprint 7:** AI Dedupe (pHash) ✅, Dark AMOLED ✅, Two-pane Expanded ✅, FOSS flavor ✅,
  GitHub Pages ✅, Room proper migrations ✅, i18n debt resolved ✅

## Sprint 9 — Launch (2026-06-03)

| Task | Status | Ghi chu |
|------|--------|---------|
| F-Droid fastlane metadata (en-US + vi-VN) | Done | metadata/en-US + vi-VN, changelogs/1.txt |
| FOSS APK Sentry-free verification | Done | CI step: unzip grep io/sentry → empty |
| docs/FDroidMetadata.md → real submit checklist | Done | fdroiddata MR template included |
| Play Store Data Safety form mapping | Done | PlayStoreListing.md — Sentry opt-in only, EU DSN |
| Content rating questionnaire answers | Done | Everyone / 3+, no sensitive content |
| bundleGmsRelease (AAB) build verified | Done | CI release-check job |
| Signing config reads from env vars | Done | signingProp() tries local.properties then System.getenv() |
| crowdin.yml — Crowdin project config | Done | source: values-en/strings.xml → values-%android_code%/ |
| values-fr/strings.xml — French sample locale | Done | Full translation, proves locale-switcher pipeline |
| DevelopmentRoadmap.md — Phase 5 ticked | Done | All 3 distribution tasks marked complete |

## Sprint 8.3 — Notif banner + crash toggle + race guard (2026-06-03)

| Task | Status | Ghi chu |
|------|--------|---------|
| notifPermissionNeeded StateFlow + clearNotifPermissionFlag | Done | HomeViewModel |
| HomeScreen notif permission banner | Done | POST_NOTIFICATIONS dialog (API 33+) |
| setCrashReporting both directions at runtime | Done | SettingsViewModel |
| loadSuggestions Mutex race guard | Done | HomeViewModel |
| String resources notif_permission_banner_* | Done | vi + en |
| HomeViewModelTest + SettingsViewModelTest | Done | missing import kotlinx.coroutines.launch fixed |
| .gitattributes line-ending rules | Done | *.bat CRLF, *.kt LF, etc. |
| .claude/ added to .gitignore | Done | |

## Sprint 8 — Distribution blockers (2026-06-02)
| Task | Status | Ghi chu |
|------|--------|---------|
| FOSS Sentry-free (gmsImplementation, NoOpCrashReporter) | Done | foss APK: zero io.sentry bytes |
| MANAGE_EXTERNAL_STORAGE removed | Done | scanEmptyFolders -> SAF DocumentFile |
| HomeViewModel passes all 6 params to GenerateSuggestionsUseCase | Done | + 6h cache via DataStore |
| QUERY_ALL_PACKAGES added to manifest | Done | Justified in Permissions.md |
| baseline-prof.txt committed | Done | app/src/main/generated/baselineProfiles/ |
| CI builds both flavors | Done | assembleFossDebug + assembleGmsDebug |
| 3 unused Material3 Adaptive deps removed | Done | adaptive, adaptive-navigation, adaptive-layout |
| Rule 3 bug fixed (DOWNLOAD filter -> RESIDUAL_APK) | Done | GenerateSuggestionsUseCase |

## Sprint 7 Features
| Feature | Status |
|---------|--------|
| AI Dedupe (pHash) | Done |
| Dark AMOLED mode | Done |
| Two-pane Expanded layout | Done |
| FOSS build flavor | Done |
| GitHub Pages hosting | Done |
| Room proper migrations | Done |
| i18n debt resolved | Done |

---
*Cap nhat: thang 6/2026 - Sprint 8*

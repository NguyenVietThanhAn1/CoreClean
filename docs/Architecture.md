# Architecture

## Overview

CoreClean dùng **Clean Architecture 3 tầng** (Domain / Data / Presentation) + **MVVM** ở presentation. Mỗi feature module có pattern lặp lại: `Screen → ViewModel → UseCase → Repository → DataSource`.

## Clean Architecture Layers

```
┌──────────────────────────────────────────────────────────────────┐
│ PRESENTATION LAYER                                                │
│ ─────────────────────────────────────────────────────────────────│
│ HomeScreen        MediaScreen       StorageScreen   BatteryScreen │
│ AppUsageScreen    ContactScreen     JunkScreen      RamScreen     │
│ AppAnalyzerScreen PrivacyDashboard  SettingsScreen  Onboarding    │
│ SafetyReviewScreen                                                │
│ (Compose + ViewModel + Type-safe Navigation + DataStore prefs)    │
└─────────────────────────────┬────────────────────────────────────┘
                              │  observes Flow / invokes UseCase
┌─────────────────────────────▼────────────────────────────────────┐
│ DOMAIN LAYER (pure Kotlin, no Android dependency)                 │
│ ─────────────────────────────────────────────────────────────────│
│ Models:    MediaImage, StorageInfo, BatteryInfo, AppUsageInfo,    │
│            Contact, JunkItem, RamInfo, InstalledApp               │
│                                                                   │
│ Repositories (interfaces):                                        │
│   MediaRepo, StorageRepo, BatteryRepo, AppUsageRepo,              │
│   ContactRepo, RamRepo, AppListRepo, CrashReporter                │
│                                                                   │
│ UseCases:  GetAllImages, FindDuplicateImages, GetStorageInfo,     │
│            FindLargeFiles, GetAppUsage, ScanJunk, CleanJunk,      │
│            MergeContacts                                          │
└─────────────────────────────┬────────────────────────────────────┘
                              │  implements
┌─────────────────────────────▼────────────────────────────────────┐
│ DATA LAYER                                                        │
│ ─────────────────────────────────────────────────────────────────│
│ Repository impls (bind via Hilt @Binds)                           │
│                                                                   │
│ DataSources (Android-specific):                                   │
│   MediaDataSource       MediaStore.Images                         │
│   StorageDataSource     StatFs + MediaStore + StorageStatsManager │
│   BatteryDataSource     BroadcastReceiver(ACTION_BATTERY_CHANGED) │
│   AppUsageDataSource    UsageStatsManager.queryUsageStats         │
│   ContactDataSource     ContactsContract                          │
│   JunkScanner           StorageStatsManager + DocumentFile (SAF)  │
│   RamDataSource         ActivityManager.MemoryInfo                │
│   AppListDataSource     PackageManager + StorageStatsManager      │
│                                                                   │
│ Helpers:                                                          │
│   DuplicateDetector     MD5 256KB partial hash, parallel semaphore│
│   SentryCrashReporter   Adapter for io.sentry SDK                 │
│   MediaScanWorker       CoroutineWorker (Hilt) — 12h periodic     │
│                                                                   │
│ Local persistence:                                                │
│   Room v2: scan_results, pending_review                           │
│   DataStore Preferences: settings, onboarding flag, SAF URIs      │
└──────────────────────────────────────────────────────────────────┘
```

## Module Folders

```
app/src/main/java/com/coreclean/app/
├── core/
│   ├── di/                # Hilt modules per feature
│   └── preferences/       # AppPreferences, ThemeMode, AppLanguage
├── data/
│   ├── crash/             # SentryCrashReporter
│   ├── datasource/
│   │   ├── media/         # MediaDataSource, DuplicateDetector
│   │   ├── storage/       # StorageDataSource
│   │   ├── battery/       # BatteryDataSource
│   │   ├── usage/         # AppUsageDataSource
│   │   ├── contact/       # ContactDataSource
│   │   ├── junk/          # JunkScanner
│   │   ├── ram/           # RamDataSource
│   │   └── applist/       # AppListDataSource
│   ├── local/
│   │   ├── dao/           # ScanResultDao, PendingReviewDao
│   │   ├── entity/        # ScanResultEntity, PendingReviewEntity
│   │   └── AppDatabase.kt
│   ├── repository/        # *RepositoryImpl (one per domain repo)
│   └── worker/            # MediaScanWorker
├── domain/
│   ├── CrashReporter.kt
│   ├── model/             # data class only
│   ├── repository/        # interfaces only
│   └── usecase/           # one folder per feature
├── presentation/
│   ├── home/              # HomeScreen
│   ├── media/             # (ở ui/media/ — legacy)
│   ├── storage/, battery/, usage/, contact/, junk/, ram/, appanalyzer/
│   ├── privacy/           # PrivacyDashboardScreen
│   ├── review/            # SafetyReviewScreen
│   ├── settings/          # SettingsScreen
│   ├── onboarding/        # PermissionOnboardingScreen
│   ├── theme/             # CoreCleanTheme
│   └── navigation/        # CleanerNavGraph + type-safe routes
├── CleanerApp.kt          # @HiltAndroidApp, WorkManager init
└── MainActivity.kt
```

## Layering Rules

1. **Domain không import Android SDK** (trừ những class chuyên về Uri/data — chấp nhận).
2. **Presentation không gọi DataSource trực tiếp** — phải qua UseCase / Repository.
3. **Data có thể dùng Android SDK thoải mái** (MediaStore, PackageManager, …).
4. **DI scope:** Repository = `@Singleton`. UseCase = default (mỗi inject 1 instance, rẻ). ViewModel = `@HiltViewModel`.
5. **Compose không state ngoài ViewModel** — dùng `collectAsStateWithLifecycle()`.

## Safety Delete Flow (Android 11+)

```
User selects images in MediaScreen
       │
       ▼
MediaViewModel.prepareReview()
  ├─ ids ≤ 200 → ReviewRoute(imageIds = ids)
  └─ ids >  200 → INSERT pending_review rows
                  ReviewRoute(imageIds = empty)
       │
       ▼
navigate → SafetyReviewScreen
       │
       ▼
SafetyReviewViewModel.init
  ├─ args.imageIds.isNotEmpty() → filter from getAllImages()
  └─ empty                       → read from pending_review
       │
       ▼
User confirms delete
       │
   ┌───┴───────────────────────┐
   │ API < 30               API ≥ 30
   │ contentResolver       MediaStore.createDeleteRequest
   │   .delete(uri)          → IntentSender
   │                         → StartIntentSenderForResult launcher
   │                         → System dialog (user confirms again)
   └─────┬─────────────────────┘
         ▼
   onSystemDeleteDone() → Done state
         │
         ▼
   popBackStack(MediaRoute, inclusive=false), selection cleared,
   pending_review.deleteWhereSession(...)
```

## Permission Onboarding Flow

```
MainActivity.onCreate()
       │
       ▼
Read DataStore `onboarding_done`
       │
  ┌────┴───────────────────────┐
  false                       true
  │                           │
  ▼                           ▼
OnboardingRoute            HomeRoute
       │
HorizontalPager (3 steps):
  Step 1: Storage/Media   requestPermissions(READ_MEDIA_*)
  Step 2: Usage Stats     Intent ACTION_USAGE_ACCESS_SETTINGS + verify back
  Step 3: Notifications   requestPermission(POST_NOTIFICATIONS) API 33+
       │
       ▼
DataStore: onboarding_done = true
       │
       ▼
navigate → HomeRoute (popUpTo OnboardingRoute inclusive)
```

## Background Scan Flow

```
CleanerApp.onCreate()
       │
       ▼
WorkManager.initialize(HiltWorkerFactory)
       │
       ▼
enqueueUniquePeriodicWork(
  "media_scan",
  ExistingPeriodicWorkPolicy.KEEP,
  PeriodicWorkRequest<MediaScanWorker>(12, HOURS)
    .setConstraints(BATTERY_NOT_LOW)
)
       │
   12h later (WorkManager schedules when constraints met)
       │
       ▼
MediaScanWorker.doWork()
  ├─ mediaRepository.getAllImages().first()
  ├─ mediaRepository.findDuplicates()  (warm cache)
  ├─ ScanResultDao.clearAll()
  └─ ScanResultDao.insertAll(entities)
       │
       ▼
Result.success()
```

## Crash Reporting Flow (opt-in)

```
CleanerApp.onCreate()
       │
       ▼
Read DataStore `crash_reporting_enabled`
       │
  ┌────┴───────────────────────┐
  false (default)            true
  │                           │
  Skip Sentry init            SentryAndroid.init(this) {
                                dsn = BuildConfig.SENTRY_DSN
                                tracesSampleRate = 0.1
                                beforeSend = scrubFilePaths/Phones/Emails
                              }
                              │
                              ▼
                          CrashReporter (interface) → SentryCrashReporter (impl)
                              │
                              ▼
                          Sentry server (EU region)
```

## DI Graph (Hilt SingletonComponent)

- `AppModule` — Dispatchers (Io/Default/Main), WorkManager
- `DatabaseModule` — Room AppDatabase + DAOs
- `DataStoreModule` — DataStore<Preferences>
- `MediaModule` — bind MediaRepository, provide ContentResolver
- `StorageModule`, `BatteryModule`, `AppUsageModule`, `ContactModule`, `RamModule`, `AppListModule`
- `CrashReporterModule` — bind SentryCrashReporter

ViewModel scope: HiltViewModel — auto-bind tại `@Composable` qua `hiltViewModel()`.

## Testing Boundaries

- Domain layer → pure unit test, không cần Robolectric.
- DataSource → Robolectric (cần Context/PackageManager/MediaStore stub).
- Worker → instrumentation (cần WorkManager + Hilt thật).
- ViewModel → unit + Turbine cho Flow.

Xem thêm [`Testing.md`](Testing.md).

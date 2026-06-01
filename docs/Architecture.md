# Architecture

## Clean Architecture Layers

```
┌─────────────────────────────────────────────────┐
│             PRESENTATION LAYER                   │
│  HomeScreen  MediaScreen  StorageScreen  Battery  │
│  AppUsageScreen  ContactScreen  JunkScreen        │
│  SettingsScreen  OnboardingScreen  SafetyReview   │
│  (Compose + ViewModel + Navigation type-safe)     │
└──────────────────┬──────────────────────────────┘
                   │  observes / calls
┌──────────────────▼──────────────────────────────┐
│               DOMAIN LAYER                       │
│  MediaRepository  StorageRepository              │
│  BatteryRepository  AppUsageRepository           │
│  ContactRepository  (interfaces)                 │
│  GetAllImagesUseCase  FindDuplicateImages         │
│  GetStorageInfoUseCase  GetAppUsageUseCase        │
│  FindLargeFilesUseCase  ScanJunkUseCase          │
│  CleanJunkUseCase                                │
│  Models: MediaImage, StorageInfo, BatteryInfo,   │
│          AppUsageInfo, Contact, JunkItem          │
└──────────────────┬──────────────────────────────┘
                   │  implements
┌──────────────────▼──────────────────────────────┐
│                DATA LAYER                        │
│  MediaRepositoryImpl  StorageRepositoryImpl      │
│  BatteryRepositoryImpl  AppUsageRepositoryImpl   │
│  ContactRepositoryImpl                           │
│  MediaDataSource (MediaStore)                    │
│  StorageDataSource (StatFs + MediaStore)         │
│  BatteryDataSource (BroadcastReceiver)           │
│  AppUsageDataSource (UsageStatsManager)          │
│  ContactDataSource (ContactsContract)            │
│  JunkScanner (StorageStatsManager + File API)    │
│  DuplicateDetector (MD5 hash)                    │
│  MediaScanWorker (WorkManager)                   │
│  AppDatabase (Room v2): ScanResult, PendingReview│
│  DataStore Preferences: Settings + Onboarding    │
└─────────────────────────────────────────────────┘
```

## Safety Delete Flow (Android 11+)

```
User selects images
       │
       ▼
MediaScreen.onDelete
       │
       ▼
MediaViewModel.prepareReview()
  ├─ ids <= 200 → ReviewRoute(imageIds = ids)
  └─ ids >  200 → save to pending_review DB
                  ReviewRoute(imageIds = emptyList)
       │
       ▼
Navigate to SafetyReviewScreen
       │
       ▼
SafetyReviewViewModel.init
  ├─ imageIds not empty → filter getAllImages()
  └─ imageIds empty     → read from pending_review DB
       │
       ▼
User taps "Confirm Delete"
       │
  ┌────┴────┐
  │ API<30  │  API>=30 (Android 11+)
  │ direct  │  createDeleteRequest → IntentSender
  │ delete  │  StartIntentSenderForResult launcher
  └────┬────┘         │
       └──────────────┘
       ▼
onSystemDeleteDone() / Done state
       │
       ▼
popBackStack → MediaScreen (selection cleared)
```

## Permission Onboarding Flow

```
App launch (MainActivity)
       │
       ▼
CoreCleanApp reads DataStore `onboarding_done`
       │
  ┌────┴───────────────────┐
  │ false / not set         │  true
  ▼                         ▼
OnboardingRoute           HomeRoute (normal flow)
       │
  HorizontalPager (3 steps):
  Step 1: Storage/Media → requestPermissions()
  Step 2: Usage Stats   → startActivity(ACTION_USAGE_ACCESS_SETTINGS)
  Step 3: Notifications → requestPermission()
       │
  User completes or skips
       ▼
  DataStore: onboarding_done = true
       │
       ▼
Navigate to HomeRoute (popUpTo Onboarding inclusive)
```


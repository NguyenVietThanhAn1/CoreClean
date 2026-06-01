# Architecture

## Clean Architecture Layers

```
┌─────────────────────────────────────────────────┐
│             PRESENTATION LAYER                   │
│  HomeScreen  MediaScreen  StorageScreen  Battery  │
│  SafetyReviewScreen  (Compose + ViewModel)        │
└──────────────────┬──────────────────────────────┘
                   │  observes / calls
┌──────────────────▼──────────────────────────────┐
│               DOMAIN LAYER                       │
│  MediaRepository  StorageRepository              │
│  BatteryRepository  (interfaces)                 │
│  GetAllImagesUseCase  FindDuplicateImages         │
│  GetStorageInfoUseCase                           │
│  Models: MediaImage, StorageInfo, BatteryInfo     │
└──────────────────┬──────────────────────────────┘
                   │  implements
┌──────────────────▼──────────────────────────────┐
│                DATA LAYER                        │
│  MediaRepositoryImpl  StorageRepositoryImpl      │
│  BatteryRepositoryImpl                           │
│  MediaDataSource (MediaStore)                    │
│  StorageDataSource (StatFs + MediaStore)         │
│  BatteryDataSource (BroadcastReceiver)           │
│  DuplicateDetector (MD5 hash)                    │
│  MediaScanWorker (WorkManager)                   │
│  AppDatabase (Room v2): ScanResult, PendingReview│
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

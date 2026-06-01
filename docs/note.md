# CoreClean - Android Cleaner App

## Tong quan du an
**CoreClean** la mot ung dung ho tro toi uu hoa thiet bi Android, duoc xay dung voi muc tieu
cung cap cac cong cu don dep va quan ly he thong hieu qua, bao mat va hien dai.

- **Package Name:** `com.coreclean.app`
- **Kien truc:** Clean Architecture (Domain, Data, Presentation layers)
- **UI Framework:** Jetpack Compose (Modern UI)

## Trang thai module (Sprint 3 - thang 6/2026)

| Module            | Trang thai          | Ghi chu                        |
|-------------------|---------------------|--------------------------------|
| Media Scanner     | Implemented         | Scan, dedup (MD5), SafetyReview, Worker |
| Storage Analyzer  | Implemented (basic) | StatFs + MediaStore breakdown  |
| Battery Monitor   | Implemented (basic) | BroadcastReceiver reactive Flow |
| App Usage         | Stub                | Sprint 4                       |
| Contacts          | Stub                | Sprint 4                       |

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

---
*Cap nhat: thang 6/2026*

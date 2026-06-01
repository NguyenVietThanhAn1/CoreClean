# Known Issues

## Active

- Battery temperature co the sai tren mot so OEM (Xiaomi/Huawei bao x10 nen phai chia 10)
- App size trong Storage Analyzer yeu cau quyen PACKAGE_USAGE_STATS; neu chua cap thi bao 0
- **APP_CACHE** khong xoa duoc tren Android 8+ — chi guide user mo App Manager trong Settings
- **RAM runningAppProcesses** chi tra ve process cua chinh app tren Android 5+ (platform limit)
- **APK Analyzer uninstall multi-select**: moi app phai confirm rieng (Android system limit)
- **Sentry** tat mac dinh, user phai opt-in trong Settings
- **AppUsage** tren Android 14+ co the bi throttle neu app o background lau
- Storage breakdown chua cover Documents/WhatsApp day du

## Fixed in Sprint 2/3/4/5

- ~~Xoa anh tren Android 11+ chua dung MediaStore.createDeleteRequest~~ - Da fix Sprint 2
- ~~Duplicate detection chi dua size+ten, chua co content hash~~ - Da fix Sprint 2 (MD5 256KB)
- ~~Permission denied khong co flow recovery~~ - Da fix Sprint 2
- ~~Edge-to-edge bat nhung chua xu ly WindowInsets~~ - Da fix Sprint 2 (safeDrawing)
- ~~Room schema khai bao nhung chua dung~~ - Da dung Sprint 2 (ScanResultDao) + Sprint 3 (PendingReview)
- ~~Chua co unit test~~ - Da them Sprint 2/3/4/5
- ~~SelectedImagesHolder - khong survive process death~~ - Da xoa Sprint 3; dung SavedStateHandle + Room
- ~~AppUsage/Contacts la Stub~~ - Da implement Sprint 4
- ~~Chua co permission onboarding~~ - Da implement Sprint 4
- ~~Chua co Settings screen~~ - Da implement Sprint 4
- ~~Junk scan chua dung SAF~~ - Da implement Sprint 5 (OpenDocumentTree)
- ~~Contacts chi phat hien duplicate, chua co merge UI~~ - Da implement Sprint 5 (MergeContactDialog)
- ~~MediaScanWorker chua co instrumentation test~~ - Da them Sprint 5

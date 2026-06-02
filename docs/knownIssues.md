# Known Issues

## Active

- Battery temperature co the sai tren mot so OEM (Xiaomi/Huawei bao x10 nen phai chia 10)
- App size trong Storage Analyzer yeu cau quyen PACKAGE_USAGE_STATS; neu chua cap thi bao 0
- **APP_CACHE** khong xoa duoc tren Android 8+ — chi guide user mo App Manager trong Settings
- **RAM runningAppProcesses** chi tra ve process cua chinh app tren Android 5+ (platform limit)
- **APK Analyzer uninstall multi-select**: moi app phai confirm rieng (Android system limit)
- **Sentry** (gms flavor only) tat mac dinh, user phai opt-in trong Settings; foss flavor dung NoOpCrashReporter, khong co Sentry SDK
- **AppUsage** tren Android 14+ co the bi throttle neu app o background lau
- Storage breakdown chua cover Documents/WhatsApp day du

- **Battery prediction** yêu cầu ≥ 4 điểm sample (15 phút/sample → cần ≥ 1 giờ data); mới cài app sẽ thấy "Đang thu thập dữ liệu..."
- **AutoClean** chỉ động vào TEMP/EMPTY/APK > 30 ngày — KHÔNG đụng ảnh hoặc app data để giữ trust
- **AMOLED mode** may cause slight Canvas redraw overhead (<5%) on some devices due to forced black background recalculation
- **pHash** may produce ~0.5% false positives for screenshots containing dense text/graphs (high-frequency patterns confuse DCT)

## Fixed in Sprint 2/3/4/5/6/7/8

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
- ~~ExampleInstrumentedTest sai package~~ - Da xoa Sprint 6
- ~~Thieu lint-baseline~~ - Da them Sprint 6
- ~~Room dung fallbackToDestructiveMigration() (mat data khi update)~~ - Da fix Sprint 7: proper migrations MIGRATION_1_2 + MIGRATION_2_3 (addMigrations())
- ~~Sentry SDK duoc pull vao ca foss flavor~~ - Da fix Sprint 8: gmsImplementation, foss dung NoOpCrashReporter
- ~~Sentry init truoc khi doc DataStore crash_reporting flag~~ - Da fix Sprint 8: runBlocking(IO) read truoc SentryAndroid.init
- ~~MANAGE_EXTERNAL_STORAGE trong manifest vi pham chinh sach Play Store~~ - Da xoa Sprint 8: scanEmptyFolders dung SAF DocumentFile
- ~~HomeViewModel chi truyen 3/6 param cho GenerateSuggestionsUseCase~~ - Da fix Sprint 8: inject du 3 repo con lai + 6h cache
- ~~Rule 3 filter "DOWNLOAD" khong match JunkCategory nao~~ - Da fix Sprint 8: dung RESIDUAL_APK

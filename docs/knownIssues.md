# Known Issues

## Active

- Storage breakdown chua cover Document / Downloads / WhatsApp (chi co Images/Video/Audio/Apps/Other)
- Battery temperature co the sai tren mot so OEM (Xiaomi/Huawei bao x10 nen phai chia 10)
- MediaScanWorker chua co instrumentation test thuc dia (chi co unit test voi Robolectric)
- App size trong Storage Analyzer yeu cau quyen PACKAGE_USAGE_STATS; neu chua cap thi bao 0

## Fixed in Sprint 2/3

- ~~Xoa anh tren Android 11+ chua dung MediaStore.createDeleteRequest~~ - Da fix Sprint 2
- ~~Duplicate detection chi dua size+ten, chua co content hash~~ - Da fix Sprint 2 (MD5 256KB)
- ~~Permission denied khong co flow recovery~~ - Da fix Sprint 2
- ~~Edge-to-edge bat nhung chua xu ly WindowInsets~~ - Da fix Sprint 2 (safeDrawing)
- ~~Room schema khai bao nhung chua dung~~ - Da dung Sprint 2 (ScanResultDao) + Sprint 3 (PendingReview)
- ~~Chua co unit test~~ - Da them Sprint 2/3
- ~~SelectedImagesHolder - khong survive process death~~ - Da xoa Sprint 3; dung SavedStateHandle + Room

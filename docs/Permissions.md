# Permissions

| Permission | Ly do | Screen su dung | Fallback neu denied |
|---|---|---|---|
| READ_MEDIA_IMAGES (API 33+) | Quet anh MediaStore | Onboarding, MediaScreen | Banner "Can quyen", khong auto popup |
| READ_MEDIA_VIDEO (API 33+) | Quet video MediaStore | Onboarding, MediaScreen | Banner "Can quyen" |
| READ_EXTERNAL_STORAGE (API ≤32) | Tuong duong READ_MEDIA_IMAGES tren Android cũ | Onboarding | Banner |
| MANAGE_EXTERNAL_STORAGE | Truy cap toan bo bo nho ngoai | JunkScanner (empty folder) | Bo qua EMPTY_FOLDERS |
| PACKAGE_USAGE_STATS (protected) | Lay thong ke su dung app | AppUsageScreen | Empty state + nut Mo Settings |
| READ_CONTACTS | Doc danh ba | ContactScreen | Empty state + nut Mo Settings |
| WRITE_CONTACTS | Ghi danh ba (Sprint 5 merge) | Chua su dung | N/A |
| POST_NOTIFICATIONS (API 33+) | Ket qua quet nen | Onboarding step 3 | Khong gui thong bao |
| RECEIVE_BOOT_COMPLETED | Restart WorkManager sau reboot | CleanerApp | Worker khong khoi dong lai |
| FOREGROUND_SERVICE | MediaScanWorker | Worker | N/A — declared |
| FOREGROUND_SERVICE_DATA_SYNC | Tag foreground service type | Worker | N/A |

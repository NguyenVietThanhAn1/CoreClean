# Permissions

**Note:** CoreClean does NOT collect PII (Personally Identifiable Information). Sentry crash reports (GMS flavor only, opt-in) contain only stack traces and device OS info. No contact names, file names, or media content is ever sent.

| Permission | Ly do | Screen su dung | Fallback neu denied |
|---|---|---|---|
| READ_MEDIA_IMAGES (API 33+) | Quet anh MediaStore | Onboarding, MediaScreen | Banner "Can quyen", khong auto popup |
| READ_MEDIA_VIDEO (API 33+) | Quet video MediaStore | Onboarding, MediaScreen | Banner "Can quyen" |
| READ_EXTERNAL_STORAGE (API ≤32) | Tuong duong READ_MEDIA_IMAGES tren Android cu | Onboarding | Banner |
| QUERY_ALL_PACKAGES | APK Analyzer: liet ke tat ca app da cai dat de phan tich kich thuoc, phien ban, thoi gian cai; Junk Cleaner: quet app cache voi StorageStatsManager | AppAnalyzerScreen, JunkScreen | getInstalledApplications() tra ve partial results tren API 30+ (chap nhan duoc — best-effort) |
| PACKAGE_USAGE_STATS (protected) | Lay thong ke su dung app | AppUsageScreen | Empty state + nut Mo Settings |
| READ_CONTACTS | Doc danh ba | ContactScreen | Empty state + nut Mo Settings |
| WRITE_CONTACTS | Ghi danh ba (Sprint 5 merge) | ContactScreen (MergeContactDialog) | N/A |
| POST_NOTIFICATIONS (API 33+) | Ket qua quet nen | Onboarding step 3 | Khong gui thong bao |
| RECEIVE_BOOT_COMPLETED | Restart WorkManager sau reboot | CleanerApp | Worker khong khoi dong lai |
| FOREGROUND_SERVICE | MediaScanWorker | Worker | N/A — declared |
| FOREGROUND_SERVICE_DATA_SYNC | Tag foreground service type | Worker | N/A |

## Permissions da xoa

| Permission | Ly do xoa | Sprint |
|---|---|---|
| MANAGE_EXTERNAL_STORAGE | Vi pham chinh sach Google Play cho ung dung pho thong; EMPTY_FOLDERS scan chuyen sang SAF (DocumentFile tren URI duoc user cap) | Sprint 8 |

## SAF (Storage Access Framework)

JunkScanner EMPTY_FOLDERS chi scan cac URI duoc user cap qua OpenDocumentTree (JunkScreen).
Neu chua chon folder nao, EMPTY_FOLDERS scan se tra ve empty list va JunkScreen se hien thi
goi y chon folder.

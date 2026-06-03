# Play Store Listing — CoreClean

## Short description (≤ 80 chars)
CoreClean: Device optimizer — clean junk, manage storage, monitor battery.

## Full description — Vietnamese

CoreClean là ứng dụng tối ưu hóa thiết bị Android mạnh mẽ, minh bạch và không có quảng cáo lừa đảo.

**Tính năng chính:**

🔍 **Quét & Dọn dẹp thông minh**
- Phát hiện ảnh trùng lặp bằng MD5 content hash và AI perceptual hash (pHash)
- Quét file rác: file tạm, APK cũ, thư mục rỗng
- Dọn dẹp tự động theo lịch (TEMP/APK/EMPTY — không đụng ảnh hay dữ liệu quan trọng)
- Gợi ý dọn dẹp thông minh dựa trên dung lượng thực tế

📊 **Phân tích lưu trữ**
- Biểu đồ doughnut phân tích dung lượng theo loại (ảnh, video, nhạc, ứng dụng...)
- Tìm file lớn (>50 MB) qua MediaStore
- Báo cáo dung lượng chính xác bằng StatFs + MediaStore

🔋 **Theo dõi pin**
- Hiển thị mức pin, nhiệt độ, trạng thái sạc theo thời gian thực
- Dự đoán thời gian còn lại bằng hồi quy tuyến tính (cần ≥ 4 mẫu, ~1 giờ data)
- Lịch sử pin 24 giờ

📱 **Quản lý ứng dụng**
- Thống kê sử dụng ứng dụng (7 ngày / 30 ngày)
- Phân tích kích thước APK, gỡ cài đặt nhiều ứng dụng cùng lúc
- Theo dõi RAM theo thời gian thực

👤 **Quản lý danh bạ**
- Phát hiện và gộp danh bạ trùng lặp
- Hiển thị danh bạ thiếu thông tin

🔒 **Bảo mật & Quyền riêng tư**
- Bảng điều khiển quyền riêng tư: xem và xóa dữ liệu đã lưu
- Báo cáo lỗi Sentry tùy chọn (opt-in, mặc định tắt)
- Không thu thập dữ liệu cá nhân khi chưa đồng ý

**Không có quảng cáo lừa đảo:**
CoreClean không hiển thị số liệu giả, không cảnh báo sai, không tự xóa file mà không hỏi ý kiến người dùng.

## Full description — English

CoreClean is a powerful, transparent Android device optimizer with no fake metrics or scareware.

**Key Features:**

🔍 **Smart Scan & Clean**
- Duplicate image detection using MD5 content hash and AI perceptual hash (pHash)
- Junk file scanner: temp files, residual APKs, empty folders
- Scheduled auto-cleaning (TEMP/APK/EMPTY only — never touches photos or app data)
- Smart cleaning suggestions based on actual storage usage

📊 **Storage Analysis**
- Doughnut chart breakdown by file type (photos, video, music, apps...)
- Large file finder (>50 MB) via MediaStore
- Accurate storage reporting using StatFs + MediaStore

🔋 **Battery Monitor**
- Real-time battery level, temperature, and charge status
- Battery life prediction via linear regression (requires ≥ 4 samples, ~1 hour of data)
- 24-hour battery history

📱 **App Management**
- App usage statistics (7-day / 30-day)
- APK size analysis, batch uninstall support
- Real-time RAM monitoring

👤 **Contacts Management**
- Duplicate contact detection and merging
- Incomplete contact identification

🔒 **Privacy & Security**
- Privacy dashboard: view and delete stored data
- Optional Sentry crash reporting (opt-in, disabled by default)
- No personal data collection without consent

**No scareware:**
CoreClean never shows fake numbers, fake alerts, or deletes files without your explicit confirmation.

---

## Content Rating

**Category:** Tools / Productivity

### IARC / Google Play Rating Questionnaire — answers

| Question | Answer | Notes |
|---|---|---|
| Does the app contain violence? | No | |
| Does the app contain sexual content? | No | |
| Does the app contain profanity? | No | |
| Does the app allow users to interact with others? | No | No social features |
| Does the app share location data? | No | |
| Does the app target children? | No | Target: general (13+) |
| Does the app contain gambling? | No | |
| Does the app contain references to alcohol/tobacco/drugs? | No | |
| Does the app contain horror/fear content? | No | |

**Resulting rating:** Everyone (ESRB E) / 3+ (PEGI) / All ages

---

## Data Safety Form

> Derived from docs/Permissions.md and docs/Telemetry.md. Fill in the Play Console
> Data safety section using the mapping below.

### Does your app collect or share any of the required user data types?

**GMS flavor (with Sentry opt-in):** Yes — crash reports on opt-in.
**FOSS flavor:** No data collected or shared.

Use **GMS flavor** for the Play Store listing; configure the form as follows.

### Data types collected

| Data type | Collected? | Shared? | Required? | Purpose | Encrypted in transit |
|---|---|---|---|---|---|
| Crash logs | Yes (opt-in only) | No | No (user toggles) | App functionality / bug fixing | Yes (Sentry TLS) |
| Diagnostics (device model, OS version, memory) | Yes (opt-in only) | No | No | Bug fixing | Yes |
| Personal files / photos | No | No | — | Processing is local-only, never uploaded | — |
| Contacts | No | No | — | Processing is local-only, never uploaded | — |
| App activity / usage | No | No | — | UsageStatsManager data never leaves device | — |
| Device identifiers | No | No | — | Sentry strips IP; no IDFA/GAID used | — |

### Data shared with third parties

| Third party | Data | Purpose |
|---|---|---|
| Sentry (EU endpoint) | Crash logs + device diagnostics | Crash reporting (opt-in only) |

Sentry EU DSN used → data stored in EU (`o0.ingest.sentry.io/eu/`).

### User controls
- Crash reporting: Settings → "Send crash reports" toggle → OFF by default.
- Opt-out: toggle off → `Sentry.close()` called immediately (no restart needed).
- Data deletion: Sentry dashboard, or email privacy@coreclean.app within 30 days.
- Data retention: 90 days in Sentry; no CoreClean backend.

### Data safety answers (Play Console form fields)

1. **Does your app collect or share any of the required user data types?** → **Yes**
2. **Is all of the user data collected by your app encrypted in transit?** → **Yes**
3. **Do you provide a way for users to request that their data is deleted?** → **Yes** (toggle off + email)
4. Crash logs → Collected: **Optional** (user must enable) → Shared with: **No third parties** (Sentry is a data processor, not a controller)

---

## Permissions required (Play Store declaration)

| Permission | Classification | Declared purpose |
|---|---|---|
| READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE | Storage | Media scanning and duplicate detection |
| READ_MEDIA_VIDEO | Storage | Video file analysis |
| READ_CONTACTS | Contacts | Duplicate detection and merge (optional feature) |
| WRITE_CONTACTS | Contacts | Merge duplicate contacts (optional) |
| PACKAGE_USAGE_STATS | App activity | App usage statistics (requires manual grant in Settings) |
| POST_NOTIFICATIONS | Notifications | Background scan results (optional, API 33+) |
| QUERY_ALL_PACKAGES | App info | APK analyzer — list installed apps for size/version analysis |
| FOREGROUND_SERVICE + FOREGROUND_SERVICE_DATA_SYNC | — | Background media scan worker |
| RECEIVE_BOOT_COMPLETED | — | Restart WorkManager after reboot |

**Removed permissions (no longer in manifest):**
- `MANAGE_EXTERNAL_STORAGE` — removed Sprint 8; replaced with SAF (DocumentFile) for empty-folder scan.

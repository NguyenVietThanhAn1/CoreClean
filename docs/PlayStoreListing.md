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

## Content Rating
- Category: Tools / Utilities
- Contains violence: No
- Contains sexual content: No
- Shares location: No
- Target audience: 13+

## Data Safety Form
See docs/Telemetry.md for complete data collection details.

### Data collected:
- Crash reports (opt-in only, via Sentry) — not shared with third parties
- No analytics without consent
- No personal data stored on servers
- All storage/media processing is local-only

## Permissions required:
- READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE: Media scanning and duplicate detection
- READ_CONTACTS: Contact management (optional)
- PACKAGE_USAGE_STATS: App usage stats (optional, requires manual grant)
- POST_NOTIFICATIONS: Background scan notifications (optional)
- FOREGROUND_SERVICE: Background scanning worker (WorkManager)

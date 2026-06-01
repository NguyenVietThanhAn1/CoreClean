# Privacy Policy

**Effective date:** 2026-06-01
**App:** CoreClean (`com.coreclean.app`)
**Contact:** privacy@coreclean.app

> Đây là draft template. Trước khi publish lên Play Store, phải review pháp lý + host trên website công khai (Play Console yêu cầu URL).

## 1. Tóm tắt 30 giây

- CoreClean **chạy hoàn toàn local** trên thiết bị của bạn.
- App **không gửi** dữ liệu cá nhân (ảnh, danh bạ, lịch sử dùng app, file) ra ngoài.
- Chỉ có **crash report** (nếu bạn opt-in) gửi stack trace + thông tin thiết bị OS — không kèm tên file, nội dung file, hay danh tính của bạn.
- App **không có quảng cáo**, **không bán dữ liệu**.

## 2. Dữ liệu app ĐỌC (chỉ local, không upload)

| Loại | Mục đích | Lưu ở đâu |
|---|---|---|
| Danh sách ảnh / video (MediaStore) | Hiển thị thư viện, phát hiện trùng lặp | RAM + Room DB local (`scan_results`) |
| Hash MD5 256KB đầu mỗi ảnh | Phát hiện trùng lặp nội dung | RAM, không persist |
| Thống kê sử dụng app (UsageStats) | Hiển thị thời gian dùng | RAM, không persist |
| Danh bạ (ContactsContract) | Phát hiện trùng / thiếu thông tin | RAM, không persist |
| Thông tin pin (BatteryManager) | Hiển thị health/temp/voltage | RAM, không persist |
| Cài đặt app (theme, language, ...) | Lưu preferences | DataStore local |

## 3. Dữ liệu app GHI (chỉ local, có thể tác động hệ thống)

| Hành động | Yêu cầu xác nhận | Lưu vết |
|---|---|---|
| Xoá ảnh/video | System dialog (Android 11+) | Lưu count vào lịch sử local |
| Gộp danh bạ (Sprint 5+) | Confirm dialog trong app | Có |
| Xoá junk file (TMP/APK) | Confirm dialog | Có |
| Uninstall app khác | System dialog | Không track |

## 4. Dữ liệu app GỬI ĐI (chỉ khi opt-in)

**Chỉ có crash reporting**, mặc định **TẮT**. Khi bật trong Settings:
- Gửi: stack trace lỗi, model thiết bị, phiên bản Android, version app, locale.
- KHÔNG gửi: tên file, nội dung file, danh tính user, số điện thoại, email.
- Nhà cung cấp: Sentry (https://sentry.io/privacy/).
- User có thể tắt bất cứ lúc nào trong Settings → Crash Reporting.

## 5. Quyền (Permissions)

Xem chi tiết từng permission ở [`Permissions.md`](Permissions.md).
- Tất cả permission đều có **fallback** — nếu deny vẫn dùng được app.
- Không xin permission "nhạy cảm" (Location, Camera, Mic, SMS, Phone) — không cần thiết cho cleaner.

## 6. Trẻ em

App **không nhắm tới trẻ em dưới 13 tuổi** (theo COPPA). Không thu thập dữ liệu của trẻ em.

## 7. Quyền của bạn

- **Quyền xem:** Mọi dữ liệu app lưu hiện ở Privacy Dashboard trong app.
- **Quyền xoá:** Privacy Dashboard → "Xoá toàn bộ lịch sử quét" + "Reset onboarding".
- **Quyền export:** Privacy Dashboard → "Export dữ liệu" (JSON về Downloads).
- **Quyền opt-out crash reporting:** Settings → Crash Reporting → OFF.

## 8. Thay đổi

Khi privacy policy thay đổi, version mới sẽ bump effective date. Thay đổi material sẽ kèm in-app notice.

## 9. Liên hệ

Email: privacy@coreclean.app
Source: https://github.com/NguyenVietThanhAn1/CoreClean

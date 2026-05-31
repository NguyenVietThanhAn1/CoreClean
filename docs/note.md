# CoreClean - Android Cleaner App

## Tổng quan dự án
**CoreClean** là một ứng dụng hỗ trợ tối ưu hóa thiết bị Android, được xây dựng với mục tiêu cung cấp các công cụ dọn dẹp và quản lý hệ thống hiệu quả, bảo mật và hiện đại.

- **Package Name:** `com.coreclean.app`
- **Kiến trúc:** Clean Architecture (Domain, Data, Presentation layers)
- **UI Framework:** Jetpack Compose (Modern UI)

> **Trạng thái Sprint 2 (tháng 6/2026):**
> Chỉ **Media Scanner** đã được implement đầy đủ.
> Các module **Storage Analyzer**, **Battery Monitor**, **App Usage** và **Contacts** hiện là stub — chưa có logic thực.

## Công nghệ sử dụng (Tech Stack)
- **Kotlin 2.1.10**: Ngôn ngữ lập trình chính.
- **Jetpack Compose**: Xây dựng giao diện người dùng khai báo.
- **Hilt (Dependency Injection)**: Quản lý các thành phần và phụ thuộc.
- **Room Database**: Lưu trữ dữ liệu cục bộ (SQLite).
- **Coroutines & Flow**: Xử lý các tác vụ bất đồng bộ và luồng dữ liệu.
- **Navigation Compose (Type-safe)**: Điều hướng giữa các màn hình bằng Kotlin Serialization.
- **Coil 3**: Tải và hiển thị hình ảnh tối ưu.
- **WorkManager**: Thực hiện các tác vụ quét hoặc dọn dẹp định kỳ dưới nền.

## Các tính năng chính
1. **Media Scanner (Dọn dẹp ảnh/video):** ✅ Implemented
   - Quét toàn bộ thư viện ảnh.
   - Phát hiện ảnh trùng lặp dựa trên content hash (MD5 256KB đầu).
   - Hỗ trợ xóa ảnh qua Safety Review screen (Android 11+ dùng createDeleteRequest).
2. **App Usage (Quản lý ứng dụng):** 🔲 Stub — chưa implement.
3. **Battery Monitor (Giám sát pin):** 🔲 Stub — chưa implement.
4. **Storage Manager (Quản lý bộ nhớ):** 🔲 Stub — chưa implement.

## Cấu trúc dự án
- `app/src/main/java/com/coreclean/app/core/`: Chứa các thành phần dùng chung (DI, Utils, Extensions).
- `app/src/main/java/com/coreclean/app/data/`: Thực thi Repository và DataSources (Local DB, Worker).
- `app/src/main/java/com/coreclean/app/domain/`: Chứa Business Logic (UseCases, Interfaces, Models).
- `app/src/main/java/com/coreclean/app/presentation/`: Chứa UI code, ViewModels và Navigation.
- `app/src/main/java/com/coreclean/app/ui/`: Các màn hình chức năng cụ thể (ví dụ: `MediaScreen`).

## Hướng dẫn cài đặt & Build
1. **Yêu cầu:** Android Studio Koala trở lên, JDK 17+.
2. **Gradle Sync:** Đảm bảo sử dụng Gradle 8.10+ và AGP 8.8.2.
3. **Build:** Chạy lệnh `./gradlew assembleDebug` để tạo file APK.
4. **Permissions:** Ứng dụng yêu cầu các quyền:
   - `READ_EXTERNAL_STORAGE` (hoặc `READ_MEDIA_IMAGES` trên Android 13+).
   - `PACKAGE_USAGE_STATS` (Cần người dùng cấp quyền thủ công trong Cài đặt).

---
*Ghi chú: Dự án đã được tối ưu hóa cấu trúc package và giải quyết các lỗi build cơ bản vào tháng 4/2026.*

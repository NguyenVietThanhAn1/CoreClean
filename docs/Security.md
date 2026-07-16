# Security & Threat Model

## Threat Model (lightweight)

CoreClean là app local-only. Threat surface chính:

### T1 — Mất dữ liệu user do bug logic
**Risk:** Xoá nhầm ảnh / danh bạ user muốn giữ.
**Mitigation:**
- **Safety Review screen** bắt buộc trước mọi delete — show preview, count, total size.
- Android 11+ delete đi qua **system dialog** (`createDeleteRequest`) → user xác nhận lần 2 ở cấp OS.
- Duplicate detection có **fast vs hash mode** — mặc định hash (chậm nhưng đúng); chỉ fallback fast khi user chọn.
- Trước khi xoá danh bạ trùng → backup bản hiện tại vào Room (`pending_review`), giữ 7 ngày để rollback.

### T2 — Permission abuse
**Risk:** App có quyền (Contacts, MediaStore) bị lợi dụng đọc/exfiltrate dữ liệu.
**Mitigation:**
- Code **không có network code** (trừ Sentry SDK, opt-in).
- Lint rule custom: chặn `import java.net.*`, `import okhttp3.*`, `import retrofit2.*` (xem `lint-baseline.xml`).
- CI có job `forbidden-imports` chạy grep + fail nếu tìm thấy.
- Sentry SDK chỉ enable khi user opt-in; DSN inject qua BuildConfig, không hardcode.

### T3 — Local DB tampering
**Risk:** Root device → đọc/sửa Room DB.
**Mitigation:**
- Không lưu PII trong Room. `scan_results` chỉ chứa path + size + type + timestamp.
- Không cần SQLCipher (giá trị thấp, overhead cao).
- DataStore preferences cũng không chứa secret.

### T4 — Intent injection
**Risk:** App khác gửi malicious intent vào CoreClean activity.
**Mitigation:**
- Chỉ `MainActivity` `exported=true` (LAUNCHER). Mọi activity khác = không export.
- Không có deep link schema custom → giảm attack surface.
- Worker không nhận data từ external — chỉ enqueue từ trong app.

### T5 — Crash leaking PII
**Risk:** Sentry event chứa file path / content URI / package name / email / số điện thoại.
**Mitigation:**
- `SentryCrashReporter.kt` có `scrubPii` chạy qua `beforeSend`, áp dụng cho mọi event
  (message, exception value, breadcrumb, extras) ở cả 2 nơi gọi `Sentry.init`. Xem chi tiết ở
  `docs/Telemetry.md#scrubbing`.
- Test: `PiiScrubberTest` (34 case, gồm cả false-positive: short digit run, memory address,
  error code, epoch timestamp, IPv4 address, URL hostname, custom URI scheme authority,
  framework/own-app class name không bị đụng).
- Disabled by default.

## Secure Coding Checklist

- [ ] Không hardcode secret (token, DSN, API key) — luôn qua `local.properties` + `BuildConfig`.
- [ ] Không log PII bằng `Log.d/e` — dùng `Timber` với scrubber custom.
- [ ] `WebView` — KHÔNG dùng. Nếu phải dùng: `setJavaScriptEnabled(false)`, no file access.
- [ ] Mọi `Uri.parse(userInput)` phải validate scheme (chỉ `content://` hoặc `file://` local).
- [ ] `ContentResolver.openInputStream()` phải `.use { }` để đóng — tránh leak fd.
- [ ] Hilt scope đúng — `@Singleton` chỉ cho stateless / DB / WorkManager. Repository: `@Singleton`. UseCase: default scope.

## Dependencies — Supply Chain

- Mọi dependency phải có version pin trong `libs.versions.toml`.
- Dependabot bật trên repo (`.github/dependabot.yml`).
- CI có job `gradle-dependency-check` chạy OWASP scanner định kỳ (Sprint 6+).

## Reporting a Vulnerability

Email: security@coreclean.app
- Embargo 90 ngày trước khi public.
- Không bounty (open source / cá nhân).

# Telemetry

## Tóm tắt

CoreClean **mặc định tắt** mọi telemetry. User phải opt-in trong Settings → "Crash reporting".

## What's collected (chỉ khi opt-in)

### Crash reports (Sentry)

| Trường | Ví dụ | Lý do |
|---|---|---|
| Stack trace | `at MediaScreen.kt:142` | Debug crash |
| Exception type + message | `NullPointerException: ...` | Debug crash |
| Device model | `Pixel 7 Pro` | Biết quirk OEM nào trigger |
| Android version | `14 (API 34)` | Compatibility matrix |
| App version | `1.5.0 (10500)` | Tìm regression |
| Locale | `vi-VN` | Reproduce với locale config |
| Free memory | `2.1 GB` | Out-of-memory crash root cause |
| Storage free | `8.4 GB` | Storage-related crash |
| Breadcrumbs (≤ 50) | "Navigated to MediaRoute" | Reproduction steps |

### What is NEVER collected

- ❌ Tên file, đường dẫn file, nội dung file.
- ❌ Tên người trong danh bạ, số điện thoại, email.
- ❌ Package name của app người dùng đã cài.
- ❌ IP address (Sentry mặc định strip).
- ❌ User ID / Device ID / Advertising ID.
- ❌ Screenshot / video.
- ❌ Network requests (app không có network code).

## Scrubbing

`SentryCrashReporter` áp dụng scrubber trước khi gửi:

```kotlin
options.beforeSend = BeforeSendCallback { event, _ ->
    event.message?.formatted = event.message?.formatted
        ?.replace(Regex("/storage/[^\\s]+"), "/storage/<redacted>")
        ?.replace(Regex("\\+?\\d{10,}"), "<phone-redacted>")
        ?.replace(Regex("[\\w.-]+@[\\w.-]+"), "<email-redacted>")
    event
}
```

Test: `SentryCrashReporterTest.assertScrubsFilePathsPhoneEmail()`.

## User Controls

| Setting | Default | Effect |
|---|---|---|
| Crash reporting | OFF | `Sentry.setEnabled(false)` |
| Performance traces | OFF | `tracesSampleRate = 0.0` |
| Breadcrumbs | ON khi crash reporting ON | Auto by Sentry SDK |

## Data Retention

- Sentry account: 90 ngày, sau đó tự xoá.
- Không có backend riêng của CoreClean → không có log server-side.

## Opt-out flow

Settings → Crash reporting → toggle OFF:

```kotlin
Sentry.close()                    // Stop current session
appPreferences.setCrashReportingEnabled(false)
```

Lần khởi động sau, SDK không init nếu flag = false.

## GDPR / Compliance

- User EU/UK: cùng flow opt-in, không có consent banner riêng vì:
  - Không thu thập PII.
  - Mặc định OFF.
  - Mọi data có thể được xoá khỏi Sentry dashboard theo yêu cầu (email privacy@coreclean.app).
- COPPA: app không nhắm trẻ em.
- Trans-border data flow: Sentry chính trên EU (`o0.ingest.sentry.io/eu/`) — config trong DSN.

## Future Telemetry (NOT now)

Nếu thêm analytics (vd: feature usage, retention), phải:
1. Opt-in flow riêng, không gom chung với crash reporting.
2. Sự kiện gửi đi phải list rõ trong file này.
3. Cập nhật PrivacyPolicy.md trước khi enable.
4. Review pháp lý nếu thêm IDFA / Advertising ID.

## Reporting Misuse

Nếu phát hiện app gửi dữ liệu không khai báo: email security@coreclean.app.

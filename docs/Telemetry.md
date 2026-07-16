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
- ❌ Số điện thoại, email (redact bằng regex, xem mục Scrubbing).
- ❌ Package name của app người dùng đã cài.
- ❌ IP address (Sentry mặc định strip).
- ❌ User ID / Device ID / Advertising ID.
- ❌ Screenshot / video.
- ❌ Network requests (app không có network code).

> **Tên người trong danh bạ:** hiện tại **không có call site nào** trong app đưa
> `Contact.displayName` vào `crashReporter.captureException`/`addBreadcrumb` — do đó tên
> liên hệ chưa từng lọt vào crash report nào. Đây **không phải** một guarantee được
> `scrubPii` enforce bằng code: `redactPii` không có cơ chế nhận diện tên người (không
> regex, không lookup theo `Contact.displayName`). Nếu một call site trong tương lai
> interpolate tên liên hệ vào message/breadcrumb/extra, nó sẽ compile bình thường và tên
> thật sẽ lọt vào Sentry — không có test nào bắt được regression này (theo dõi ở
> `TODOS.md`).

## Scrubbing

`configureSentryOptions` (in `SentryCrashReporter.kt`) wires a single `beforeSend` hook —
`scrubPii` — shared by both Sentry.init call sites (app-startup init and the runtime
Settings toggle), so scrubbing can't be wired up at one but forgotten at the other.
`scrubPii` redacts, in this order, in `event.message`, every `exception.value`, every
breadcrumb's message/data, and `event.extras`:

1. `content://` URIs → `[REDACTED_URI]`
2. File paths under `/data`, `/storage`, `/sdcard` → `[REDACTED_PATH]`, matching to the end of
   the line rather than stopping at the first space — Android filenames routinely contain spaces
   (WhatsApp media, screenshots, human-typed names), so any trailing text on the same line after
   the path (including debug context like `(No such file or directory)`) is redacted along with
   it, favoring over-redaction over leaking the tail of a space-containing filename.
3. Email addresses → `[REDACTED_EMAIL]`
4. Phone numbers → `[REDACTED_PHONE]` — **deliberately narrow**: only text with a
   phone-specific shape (a leading `+` international prefix, a parenthesized NANP area
   code `(415) 555-2671`, or a dash/dot-grouped triple `415-555-2671` / `415.555.2671`)
   is matched, unconditionally — the dash/dot form has no NANP-validity check, so a
   malformed or non-US number like `015-234-5678` is still redacted. A **space- or
   tab-separated** triple (e.g. `415 555 2671`) is **never** redacted, even with a
   "Call"/"Phone" label right next to it: an earlier version tried to redact that shape
   only when a nearby keyword suggested it was a phone number, but that heuristic proved
   unreliable in review (see `TODOS.md`), so it was removed — this shape is structurally
   identical to unrelated whitespace-separated number triples this app's own image-dedup
   pipeline can legitimately produce (frame dimensions, coordinates), and there is no
   reliable regex-only way to tell them apart. A bare unformatted run of 10+ digits is
   **not** redacted as a phone number either, since that's far more likely to be a memory
   address, epoch timestamp, database ID, or error code — redacting those would destroy
   debuggability for no privacy benefit.
5. Installed-app package names (3+ dot-separated segments) → `[REDACTED_PACKAGE]`,
   except CoreClean's own namespace and common framework namespaces (`androidx.*`,
   `android.*`, `kotlin(x).*`, `java(x).*`), which are debug-relevant and never identify
   a user-installed app. A dotted identifier immediately after `http://`, `https://`, or
   a bare protocol-relative `//` (e.g. `//api.example.com/resource`) is also exempt — a
   bare web hostname isn't PII, and mangling it destroys the "which host errored"
   debugging signal; any actual PII elsewhere in the same URL (an email in the path, say)
   is still redacted normally by the earlier passes. This carve-out is deliberately
   scoped to `http(s)://` and bare `//` only, not any `scheme://` — an Android
   custom-URI-scheme authority (e.g. `myapp://com.example.SomeActivity` in a deep link)
   can legitimately be a real installed-package name and must still be redacted, and a
   scheme name that merely *ends* in "http"/"https" (e.g. `shttp://...`) is rejected by a
   scheme-boundary check rather than mistaken for the real scheme.

Stack traces (class name, file name, line number) are never touched — they're needed for
debugging and don't contain user data.

Test: `PiiScrubberTest` (34 cases, including false-positive cases for short digit runs,
memory addresses, error codes, epoch timestamps, IPv4 addresses, URL hostnames, custom
URI scheme authorities, and framework/own-app class names).

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

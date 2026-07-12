# TODOS

## Refactor (repository layer boundary)

### Migrate SettingsViewModel to WorkScheduler (WorkManager)

**What:** Replace SettingsViewModel's current background-work scheduling mechanism with a standard `WorkScheduler` built on WorkManager, matching how `MediaScanWorker`/`BatteryHistoryRecorder` are scheduled.

**Why:** Chosen to defer separately during the 2026-07-12 `/review` session — not part of the repository-layer boundary work, standardizing background scheduling is a distinct concern.

**Context:** No investigation done yet into SettingsViewModel's current scheduling mechanism as part of this review — scope and approach still need discovery when this is picked up.

**Effort:** M
**Priority:** P3
**Depends on:** None

## Infra debt (androidTest)

### androidTest mockk-android environment hỏng — dọn chung hạ tầng androidTest

**What:** `MediaScanWorkerInstrumentationTest` fail ở `setUp()` với `NoClassDefFoundError: io.mockk.impl.JvmMockKGateway` (một số run khác ra `ExceptionInInitializerError` cùng vị trí) — mockk-android không khởi tạo được trên thiết bị/emulator. Test này chưa từng compile/pass trước cả Batch B: androidTest source set từng không compile được vì thiếu `androidx.work:work-testing` và `androidx.room:room-testing` trong `androidTestImplementation` (đã khôi phục compile trong 2 commit riêng ngoài Batch B), và sau khi compile được thì lộ ra lỗi runtime mockk-android ở trên.

Cũng trong androidTest source set: `AppDatabaseMigrationTest` (từ Sprint 7, commit `ba4aed8`) — cần kiểm tra lại xem còn pass sau khi khôi phục `room-testing` hay không (chưa chạy riêng để xác nhận trong phiên này).

**Nhận định:** Mock trong instrumentation test (chạy on-device) là anti-pattern — giá trị của `MediaScanWorkerInstrumentationTest` trùng với `MediaScanWorkerTest` (JVM unit test, đã pass, dùng cùng mock). Nên cân nhắc chuyển các `*InstrumentationTest` dạng mock-based về JVM unit test (Robolectric hoặc pure JVM), hoặc viết lại để dùng Room/WorkManager framework thật (test hành vi tích hợp thật sự) thay vì mock trên thiết bị.

**Why:** Phát hiện khi cố chạy T6 (MediaScanWorkerInstrumentationTest) cho Batch B — quyết định defer, không sink thêm thời gian điều tra mockk-android on-device vì đây là nợ hạ tầng có sẵn, không phải lỗi logic của Batch B.

**Context:** 3 commit khôi phục hạ tầng đã giữ lại (không revert): thêm `work-testing`/`room-testing` vào `androidTestImplementation`, và packaging excludes cho `META-INF/LICENSE.md`/`LICENSE-notice.md` (xung đột JUnit5 jupiter kéo theo từ `mockk-android`). Các commit này là bước khôi phục hợp lệ, cần cho lần dọn androidTest sau.

**Effort:** M
**Priority:** P3
**Depends on:** None

## Bug (i18n)

### ContactViewModel dùng message String cứng thay vì messageRes

**What:** `ContactViewModel` dính cùng bug i18n như các ViewModel vừa migrate trong Batch B — dùng message String cứng thay vì `messageRes` (resource id), nên không đi qua strings.xml / không dịch được.

**Why:** Phát hiện trong lúc rà soát các ViewModel liên quan khi làm Batch B (migrate sang repository) — không nằm trong scope của Batch B nên deferred.

**Context:** Gộp vào batch error-handling/i18n sau.

**Effort:** S
**Priority:** P2
**Depends on:** None

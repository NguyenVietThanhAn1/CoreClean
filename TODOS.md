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

## Cleanup (deferred from /review on fix/contact-viewmodel-i18n)

### Snackbar/transient-message pattern duplicated between Privacy and Contact

**What:** `PrivacyViewModel`/`PrivacyDashboardScreen` and `ContactViewModel`/`ContactScreen` now both hand-roll an identical `@StringRes messageRes: Int?` state field + `dismissXxxMessage()` (copy-with-null) + `LaunchedEffect(...) { delay(2_000); dismiss() }` + `Snackbar(Modifier.padding(16.dp)) { Text(stringResource(...)) }` block, with no shared composable/base helper.

**Why:** Flagged during `/review` on the ContactViewModel i18n fix (2026-07-14) — real duplication, but extracting a shared `TimedMessageSnackbar`/message-state helper is a cross-ViewModel refactor beyond the scope of a single-ViewModel i18n bugfix.

**Context:** Consider a shared composable (e.g. `TimedMessageSnackbar(messageRes, formatArgs, onDismiss)`) and/or a small reusable state holder once a third ViewModel needs the same transient-message pattern.

**Effort:** S
**Priority:** P4
**Depends on:** None

### ContactUiState.messageRes + mergedCount are independent fields (invalid state representable)

**What:** `mergedCount: Int` only means anything when `messageRes == R.string.contact_merge_success`; nothing enforces the two stay in sync, and `mergedCount` defaults to `0` (misleading) whenever `messageRes` is null.

**Why:** Flagged during `/review` on the ContactViewModel i18n fix (2026-07-14) — real design smell but fixing it means introducing a small sealed/data holder, which is more churn than the minimal i18n fix scope justifies right now.

**Context:** If another field gets added to this "merge result" concept, collapse `messageRes`/`mergedCount` into one nullable holder (e.g. `data class MergeResult(@StringRes val messageRes: Int, val count: Int)`).

**Effort:** S
**Priority:** P4
**Depends on:** None

## Completed

### ContactViewModel dùng message String cứng thay vì messageRes

**What:** `ContactViewModel.confirmMerge` dùng message String cứng (không dấu, không qua strings.xml) thay vì `messageRes: Int?`. Đã sửa theo đúng pattern Batch B (`PrivacyViewModel`): `mergeMessage: String?` → `@StringRes messageRes: Int?` + `mergedCount: Int`, thêm `contact_merge_success`/`contact_merge_error` vào cả 3 locale (values/values-en/values-fr), `ContactScreen.kt` giờ render Snackbar thật qua `stringResource(messageRes, mergedCount)` (trước đó chỉ có timer tự-dismiss, không hiển thị text). Kèm theo đã fix bug liên quan: `load()` từng ghi đè toàn bộ `ContactUiState` bằng constructor mới (không `.copy()`) nên `messageRes` set xong bị `load()` xoá ngay lập tức trước khi UI kịp hiển thị — đổi 2 chỗ gán state trong `load()` sang `.copy()` để giữ `messageRes`/`mergedCount`/`mergingGroupIndex` qua các lần load. Test mới: `ContactViewModelTest` (3 case: success/error/dismiss).

Rà soát không phát hiện ViewModel nào khác trong `presentation/` còn dính pattern `message: String?` hardcode — không có mục follow-up mới.

**Completed:** (nhánh `fix/contact-viewmodel-i18n`, chưa merge)

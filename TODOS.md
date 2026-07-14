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

### Snackbar/transient-message pattern duplicated between Privacy and Contact (+ bypasses SnackbarHostState)

**What:** `PrivacyViewModel`/`PrivacyDashboardScreen` and `ContactViewModel`/`ContactScreen` now both hand-roll an identical `@StringRes messageRes: Int?` state field + `dismissXxxMessage()` (copy-with-null) + `LaunchedEffect(...) { delay(2_000); dismiss() }` + `Snackbar(Modifier.padding(16.dp)) { Text(stringResource(...)) }` block, with no shared composable/base helper. Additionally, both screens emit the `Snackbar` as a bare composable directly in content (Contact: inside the `Scaffold` content lambda; Privacy: even outside `Scaffold` entirely) instead of wiring through `SnackbarHostState`/`Scaffold(snackbarHost = ...)` — no bottom-anchoring, no accessibility live-region announcement, no dismiss-stacking/queueing.

**Why:** Flagged during `/review` on the ContactViewModel i18n fix (2026-07-14) — real duplication, and the adversarial review pass on the same branch confirmed the `SnackbarHostState` bypass is pre-existing (verified identical in `PrivacyDashboardScreen.kt`, not introduced by the Contact fix). Extracting a shared `TimedMessageSnackbar`/message-state helper + migrating to `SnackbarHostState` is a cross-ViewModel refactor beyond the scope of a single-ViewModel i18n bugfix.

**Context:** Consider a shared composable (e.g. `TimedMessageSnackbar(messageRes, formatArgs, onDismiss)`) built on `remember { SnackbarHostState() }` + `Scaffold(snackbarHost = { SnackbarHost(hostState) })`, once a third ViewModel needs the same transient-message pattern.

**Effort:** S
**Priority:** P4
**Depends on:** None

### LaunchedEffect key can still collide on two identical-outcome merges within 2s

**What:** `ContactScreen.kt`'s `LaunchedEffect(messageRes, uiState.mergedCount)` was fixed during `/review` to restart the auto-dismiss timer when the merge outcome or count changes, but if two merges in a row both succeed with the *same* contact count within the 2s window, the key is identical both times and Compose does not restart the effect — the second Snackbar can be dismissed almost immediately.

**Why:** Flagged by adversarial review during `/ship` (2026-07-14) — narrower remaining edge case of the bug already substantially fixed in this PR; a full fix needs a monotonic nonce (e.g. `messageId: Long` bumped on every message set) rather than value-equality keying.

**Context:** Add `messageId: Long` to `ContactUiState`, increment it in `confirmMerge`, key `LaunchedEffect` on it instead of `(messageRes, mergedCount)`.

**Effort:** S
**Priority:** P4
**Depends on:** None

### contact_merge_success has no plural form ("Merged 1 contacts")

**What:** `"Merged %1$d contacts"` (and vi/fr equivalents) reads wrong for `mergedCount == 1`. In practice this path isn't reachable from the UI today (merge dialogs only open from a `ContactDuplicateGroup`, which by definition has 2+ contacts), but it's a latent i18n correctness gap in a PR specifically about i18n.

**Why:** Flagged by adversarial review during `/ship` (2026-07-14). Converting to Android `<plurals>` + `pluralStringResource` across 3 locales is a bigger scope increase than this bugfix's minimal-change goal, especially since the count==1 case isn't currently reachable.

**Context:** If a merge-count-of-1 path is ever added, convert `contact_merge_success` to a `<plurals>` resource with proper quantity handling per locale.

**Effort:** S
**Priority:** P4
**Depends on:** None

### No format-arg parity check across locale strings.xml files

**What:** `contact_merge_error` has no `%1$d` placeholder in any of the 3 shipped locales, so `stringResource(messageRes, uiState.mergedCount)` silently ignores the extra arg today. Nothing guards against a future translator (this project uses Crowdin community translations per recent commits) adding a mismatched format specifier to one locale's copy of a string, which would throw an uncaught `IllegalFormatConversionException`/`MissingFormatArgumentException` from `stringResource` at runtime for that locale only.

**Why:** Flagged by adversarial review during `/ship` (2026-07-14) — real latent risk but requires new tooling (a lint check or test that parses all `values*/strings.xml` and validates format-specifier parity per resource name), which is infrastructure work, not part of an i18n bugfix.

**Context:** Consider a unit test that parses all `strings.xml` variants and asserts every resource using `%N$` format specifiers has matching specifier types/counts across all locales.

**Effort:** M
**Priority:** P3
**Depends on:** None

### confirmMerge has no re-entrancy guard against double-tap

**What:** `MergeContactDialog`'s confirm button has no disable-after-click/debounce. A fast double-tap before the dialog closes can invoke `ContactViewModel.confirmMerge` twice concurrently with the same contact list, causing duplicate `ContentResolver.applyBatch` writes and a possible spurious "merge failed" message on the second call even though the first succeeded.

**Why:** Flagged by adversarial review during `/ship` (2026-07-14) — pre-existing gap in `MergeContactDialog.kt`, not touched by this diff; previously invisible since no success/failure message was ever shown, now user-visible since the i18n fix makes the message real.

**Context:** Add an `isMerging: Boolean` to `ContactUiState`, set it at the start of `confirmMerge`, and disable/no-op the confirm button (or skip re-entrant calls in the ViewModel) while `true`.

**Effort:** S
**Priority:** P3
**Depends on:** None

### Stale mergingGroupIndex can point at the wrong duplicate group after a reload race

**What:** `startMerge(groupIndex)` captures an index into `uiState.duplicates`. `confirmMerge` calls `load()` afterward, which refetches `duplicates` and can reorder/shrink the list. If a second merge dialog is opened before the first `load()` completes, `mergingGroupIndex` can end up pointing at a different group once the list refreshes underneath it — silently merging the wrong contacts while still reporting "Merged N contacts" success.

**Why:** Flagged by adversarial review during `/ship` (2026-07-14) — pre-existing race, unrelated to this diff's line-level changes, previously invisible since no success message existed; now that success is reported, a wrong-group merge could look like a correct one.

**Context:** Consider keying merge dialogs by a stable group identity (e.g. a hash of the contact IDs in the group) instead of a list index, or disabling the duplicates list while a merge dialog is open.

**Effort:** M
**Priority:** P3
**Depends on:** None

### MergeContactsUseCase reports Result.success on its early-exit no-op paths

**What:** `MergeContactsUseCase.invoke` returns `Result.success(Unit)` from `if (contacts.size < 2) return@runCatching` and `if (rawIds.size < 2) return@runCatching` without performing any aggregation. `ContactViewModel.confirmMerge` maps `result.isSuccess` straight to "Merged N contacts" — so if raw-contact lookup unexpectedly returns fewer than 2 rows (contact deleted concurrently, provider hiccup), the user is told a merge happened when nothing was aggregated.

**Why:** Flagged by adversarial review during `/ship` (2026-07-14) — bug lives in `MergeContactsUseCase.kt`, a file this diff never touches; previously invisible since no success message was ever rendered, now surfaced by the i18n fix that makes the message real.

**Context:** `MergeContactsUseCase` should distinguish "nothing to do" from "merged" (e.g. a sealed result type), and `ContactViewModel` should map the no-op case to a different message than `contact_merge_success`.

**Effort:** S
**Priority:** P2
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

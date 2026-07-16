# TODOS

## Cleanup (deferred from /ship on fix/silent-exception-logging)

### CrashReporter.captureException calls are unguarded across all 5 call sites (crash-in-catch risk)

**What:** In `PerceptualHasher.computeHash`, `JunkViewModel.scan()`, `AppUsageViewModel.load()` (this branch), and the pre-existing `ContactViewModel.confirmMerge`/`SettingsViewModel`, `crashReporter.captureException(e)` runs unwrapped inside a catch block, with no defensive `try/catch` around the reporting call itself. If the `CrashReporter` implementation (`SentryCrashReporter` on gms) ever throws, a previously fully-recovered failure (return null / set Error state) becomes an unhandled crash — for `JunkViewModel`/`AppUsageViewModel` this happens inside `viewModelScope.launch` with no `CoroutineExceptionHandler`, so it's fatal.

**Why:** Flagged by adversarial review during `/ship` (2026-07-15) on `fix/silent-exception-logging` — rated highest severity since it technically contradicts that branch's "no control-flow changes" mandate, but real-world risk is low (Sentry SDK is designed to never throw from `captureException`) and the unguarded pattern is already shipped identically in `ContactViewModel`/`SettingsViewModel`. Wrapping only the 3 new call sites would create a 4th divergent idiom rather than fixing the actual gap.

**Context:** If addressed, fix consistently across all 5 call sites at once (e.g. a shared `CrashReporter.captureExceptionSafely(e)` extension that catches `Throwable` internally), not just the newest ones.

**Effort:** S
**Priority:** P3
**Depends on:** None

### Unscrubbed exception messages (file paths, content URIs, package names) sent to Sentry

**What:** `SentryCrashReporter` has no `beforeSend`/event-processor hook. Exception messages forwarded via `captureException` can embed local file paths, SAF `content://` tree URIs, or installed-package names (from `JunkViewModel.scan()`, `PerceptualHasher.computeHash`, `AppUsageViewModel.load()`, plus the pre-existing `ContactViewModel`/`SettingsViewModel` call sites) — sent unscrubbed to a third-party SaaS for any user who has opted into crash reporting.

**Why:** Flagged by adversarial review during `/ship` (2026-07-15) — real privacy gap, but it's inherent to the existing `CrashReporter`/Sentry setup (predates this branch) and fixing it means touching `SentryCrashReporter.kt`/Sentry init, files this logging-only diff never touches.

**Context:** Add a `beforeSend` hook in `SentryCrashReporter` (or the Sentry Android options config) that strips file paths, `content://` URIs, and package-name-shaped tokens from exception messages before upload, or drop `e.message` from the captured event entirely and keep only type + stack frames.

**Effort:** M
**Priority:** P2
**Depends on:** None

### crashReporter.captureException runs on the Main dispatcher in ViewModel catch blocks

**What:** `JunkViewModel.scan()` and `AppUsageViewModel.load()` run under `viewModelScope.launch` (default `Dispatchers.Main.immediate`), so the new `crashReporter.captureException(e)` call executes on the UI thread — same for the pre-existing `ContactViewModel.confirmMerge`. Sentry's Android transport historically does synchronous local envelope-cache disk writes before handing off to its async HTTP executor, so this is a jank/possible-ANR risk on the error path. `SettingsViewModel.setCrashReporting` already dispatches its crashReporter call on `Dispatchers.IO` with a comment explaining exactly this cost — the same care wasn't applied at the other 3 sites.

**Why:** Flagged by adversarial review during `/ship` (2026-07-15) — real inconsistency with the one call site that already got this right, but fixing it broadly is bigger than a single logging-only diff.

**Context:** If addressed, wrap `crashReporter.captureException(e)` in `withContext(Dispatchers.IO) { ... }` consistently across all ViewModel call sites (Contact, Junk, AppUsage), matching `SettingsViewModel`'s existing precedent.

**Effort:** S
**Priority:** P3
**Depends on:** None

### PerceptualHasher failures in a large duplicate-scan loop could flood Sentry with unthrottled events

**What:** `DuplicateDetector.detectByPerceptualHash` calls `PerceptualHasher.computeHash` once per image in a plain loop over the whole scanned media set (potentially thousands of items), with no dedup/sampling. A systemic failure (revoked SAF grant, corrupted provider rows, a whole corrupt folder) now fires `captureException` once per failing item — Sentry's client-side rate limiting only engages after the server starts returning 429s, so the first burst is unthrottled.

**Why:** Flagged by adversarial review during `/ship` (2026-07-15) — real risk under a large/partially-corrupt media library, but `DuplicateDetector.kt` is a file this diff never touches (pre-existing loop structure); needs load-testing to confirm real-world impact before deciding on a fix (e.g. sampling, or de-duplicating identical exceptions within one scan run).

**Context:** Consider adding a per-scan-run cap or dedup key (e.g. exception class + first stack frame) before calling `captureException` repeatedly in a tight loop.

**Effort:** M
**Priority:** P3
**Depends on:** None

### Three different catch/report idioms coexist for CrashReporter usage

**What:** `JunkViewModel.scan()` and `AppUsageViewModel.load()` both use `try { ... } catch (e: Exception) { crashReporter.captureException(e); uiState = XState.Error(...) }`; `ContactViewModel.confirmMerge` uses a `Result<Unit>`-based idiom instead: `result.exceptionOrNull()?.let { crashReporter.captureException(it) }`. Three ViewModels, two different shapes for the same "report then surface an error" concern.

**Why:** Flagged by the Maintainability specialist during `/ship`'s pre-landing review (2026-07-15) on `fix/silent-exception-logging`. Extracting a shared helper (e.g. a `CrashReporter` extension or small base-ViewModel function) is a refactor beyond that branch's explicit scope ("add logging only, do not change control flow").

**Context:** If a 4th ViewModel needs the same pattern, consider a shared helper — e.g. `suspend fun <T> CrashReporter.reportOnFailure(block: suspend () -> T): Result<T>` — so future call sites don't hand-roll a third variant.

**Effort:** S
**Priority:** P4
**Depends on:** None

### CrashReporter-verification test boilerplate duplicated across ViewModel tests

**What:** `JunkViewModelTest.kt` and `AppUsageViewModelTest.kt` each hand-roll near-identical `mockk<CrashReporter>(relaxed = true)` setup plus a "failure reports exception + sets Error state" test and a "success does not report" test. `ContactViewModelTest.kt` and `PerceptualHasherTest.kt` follow the same shape too.

**Why:** Flagged by the Maintainability specialist during `/ship`'s pre-landing review (2026-07-15) — low-confidence, mechanical observation; not worth a shared test helper for 4 call sites yet.

**Context:** If a 5th+ ViewModel test needs the same "verify captureException called/not-called" assertions, consider a small shared JUnit helper/extension.

**Effort:** S
**Priority:** P4
**Depends on:** None

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

## Cleanup (deferred from /review on fix/sentry-pii-scrubbing)

### scrubPii's beforeSend hook only covers message/exceptions/breadcrumbs/extras — not tags/contexts/user

**What:** `SentryCrashReporter.kt`'s `scrubPii(event: SentryEvent)` redacts `event.message`, `event.exceptions[].value`, `event.breadcrumbs[].message`/`.data`, and `event.extras`, but does not touch `event.tags`, `event.contexts`, `event.user`, or `event.transaction`. Today nothing in the codebase calls `Sentry.setTag`/`Sentry.setUser`/`Sentry.configureScope { it.setContexts(...) }` — the `CrashReporter` interface only exposes `captureException`/`addBreadcrumb` — so no PII currently flows through these fields; the SDK's auto-populated contexts (device model, OS version, app version) are intentionally-collected data, not PII, per `docs/Telemetry.md`.

**Why:** Flagged by adversarial review (Angle G, altitude check) during the PII-scrubbing work (2026-07-15) on `fix/sentry-pii-scrubbing`. Real defense-in-depth gap for a *future* call site, not a current leak — adding scrub logic for fields nothing populates yet would be speculative code with no test able to prove it does anything today.

**Context:** If a future call site ever adds `Sentry.setTag`/`setUser`/`configureScope` with user-supplied text, extend `scrubPii` to redact those fields too (same `redactPii`/`redactStringValues` helpers already exist and generalize directly).

**Effort:** S
**Priority:** P3
**Depends on:** None

### configureSentryOptions wires beforeSend but never beforeSendTransaction — performance transactions bypass scrubbing entirely

**What:** `configureSentryOptions` (`SentryCrashReporter.kt`) sets `options.beforeSend = ... { event, _ -> scrubPii(event) }` but never sets `options.beforeSendTransaction`. `SentryOptions` has two independent callback slots — `io.sentry.protocol.SentryTransaction` extends the same `SentryBaseEvent` base class that owns `breadcrumbs`/`extra`/`tags`, the exact fields `scrubPii` redacts on error events — but transactions are sent through a completely separate pathway that `scrubPii` never touches. `tracesSampleRate = 0.1` (same function, a few lines up) means performance transactions are actively sampled and sent today, not a dormant setting: any breadcrumb/extra/tag data present in Scope when a transaction fires ships to Sentry completely unscrubbed.

**Why:** Flagged by adversarial review (`/ship` Step 11) on `fix/sentry-pii-scrubbing` (2026-07-17) — confirmed by decompiling the actual `io.sentry:sentry:7.18.0` jar (`BeforeSendCallback`/`SentryOptions` have separate `beforeSend`/`beforeSendTransaction` slots). Currently low *realized* risk: per the sibling TODO above, nothing in the codebase populates breadcrumbs/extras/tags with PII yet — `CrashReporter` only exposes `captureException`/`addBreadcrumb`, and `addBreadcrumb` messages go through `scrubPii` fine on error events, just not if the SDK ever bundles that breadcrumb into an auto-captured transaction instead. Deferred rather than fixed immediately because closing it properly (factoring `SentryBaseEvent`-level scrubbing into a shared helper, wiring a second callback, adding tests for the transaction path) is a real design change, not a mechanical fix, and the code comment's claim that scrubbing "can never be wired up at one call site but forgotten at another" is currently overstated for this second pathway.

**Context:** Factor the breadcrumb/extra/tag scrubbing logic already in `scrubPii` into a shared helper (e.g. `scrubBaseEvent(event: SentryBaseEvent)`), call it from both `BeforeSendCallback` and a new `BeforeSendTransactionCallback` registered on `options.beforeSendTransaction`. Add a test asserting a `SentryTransaction` with PII in its breadcrumbs/extras comes out redacted, mirroring the existing `scrubPii` tests.

**Effort:** M
**Priority:** P2
**Depends on:** None

### redactStringValues only scrubs direct String map values — a nested Map/List value would be silently skipped

**What:** `redactStringValues(map: MutableMap<String, Any>)` (`SentryCrashReporter.kt`) only redacts entries where `entry.value as? String` succeeds. A nested `Map`/`List`/custom-object value in `breadcrumb.data` or `event.extras` (e.g. `setExtra("details", mapOf("email" to "user@example.com"))`) would pass through `scrubPii` completely untouched — no cast failure, no exception, just silently skipped.

**Why:** Flagged by adversarial review (`/ship` Step 11, INVESTIGATE) on `fix/sentry-pii-scrubbing` (2026-07-17) as speculative: no current call site produces a structured (non-String) `extras`/breadcrumb-data value, for the same reason the sibling `tags/contexts/user` TODO above is low-risk today — `CrashReporter`'s surface is narrow. Not acted on now since there's nothing in the diff or the current call graph that demonstrates this is reachable.

**Context:** If a future call site ever puts a `Map`/`List`/data-class value into `breadcrumb.data` or `event.extras`, either recursively scrub structured values in `redactStringValues`, or (simpler) require call sites to pre-flatten to strings before calling `addBreadcrumb`/`setExtra` and add a lint/test guard rejecting non-String extras.

**Effort:** M
**Priority:** P4
**Depends on:** None

### SentryInitializer.kt (gms) has zero test coverage across all 3 branches

**What:** `initializeSentry()` (`app/src/gms/java/com/coreclean/app/SentryInitializer.kt`) has three branches with no test: the `BuildConfig.SENTRY_DSN.isEmpty()` early return, the crash-reporting-preference-disabled `return@launch`, and the enabled path that now delegates to `configureSentryOptions` via `SentryAndroid.init`. This logic mostly predates this branch's changes (only the lambda body changed, from inline options-setting to calling the shared `configureSentryOptions`), so a regression here — e.g. always initializing regardless of the enabled flag, or dropping the empty-DSN guard — would go undetected: `configureSentryOptions` itself is now well-covered directly in `PiiScrubberTest.kt`, but that doesn't prove `initializeSentry` wires the enabled-check correctly.

**Why:** Flagged independently by both the `/ship` Step 7 coverage audit and the Step 9.1 testing specialist (2026-07-17) on `fix/sentry-pii-scrubbing`. Deferred rather than closed in this round because testing it properly requires a heavier harness than the rest of this diff (mocking a real `DataStore<Preferences>` + `CleanerApp` + racing `MainScope().launch(Dispatchers.IO)` against a test dispatcher — `Dispatchers.IO`, not `Dispatchers.Main`, so the project's existing `MainDispatcherRule` doesn't directly control it), and the branches themselves are simple, self-evidently-correct early returns whose risk is low relative to that harness cost.

**Context:** Add `app/src/testGms/java/com/coreclean/app/SentryInitializerTest.kt` using Robolectric + MockK + a real `PreferenceDataStoreFactory`-backed `DataStore` (matching the pattern in `SettingsViewModelTest.kt`): assert `SentryAndroid.init` is/isn't invoked based on the `CRASH_REPORTING` preference, and that it's never invoked when `BuildConfig.SENTRY_DSN` is empty.

**Effort:** S
**Priority:** P4
**Depends on:** None

### DOTTED_IDENTIFIER_REGEX only redacts 3+-segment domains — a bare 2-segment domain leaks

**What:** The package-name pass in `redactPii` only fires at `segmentCount >= 3`. A genuine 2-segment second-level domain appearing in free text (e.g. `"Unable to reach consulting-partner.com"`) is not an email (no `@`) and not 3+ segments, so it passes through completely unredacted. Naively lowering the threshold to `>= 2` would instead start swallowing harmless 2-segment tokens like file extensions (`report.pdf`) or version strings (`v2.0`), which is a worse regression.

**Why:** Flagged by adversarial review (`/code-review`, altitude angle) on `fix/sentry-pii-scrubbing` (2026-07-15). Distinguishing a domain-shaped 2-segment token from a file-extension/version-shaped one needs a TLD allowlist or similar structural signal, which is a bigger change than the narrow regex-boundary fixes landed in this pass (space-separated phone numbers, URL-hostname carve-out).

**Context:** Consider gating the 2-segment case on a small known-TLD suffix list (`.com`, `.net`, `.org`, `.io`, etc.) so `"partner-company.com"` is caught while `"report.pdf"`/`"v2.0"` are not.

**Effort:** M
**Priority:** P3
**Depends on:** None

### SAFE_NAMESPACE_PREFIXES is a hardcoded allowlist with no test enforcing it stays current

**What:** `SAFE_NAMESPACE_PREFIXES` in `SentryCrashReporter.kt` is a fixed 7-entry list (`androidx`, `android`, `kotlinx`, `kotlin`, `javax`, `java`, `com.coreclean`). Any third-party library namespace not on the list (e.g. `dagger.hilt.*`, `io.sentry.*`, `com.google.android.gms.*`) that appears as a 3+-segment dotted identifier in a crash message gets redacted as `[REDACTED_PACKAGE]` even though it's a framework/library identifier, not user PII. This is fail-safe (over-redaction, not a leak) but an unbounded maintenance burden — nothing prompts updating the list when a new dependency is added.

**Why:** Flagged by adversarial review (`/code-review`, altitude angle) on `fix/sentry-pii-scrubbing` (2026-07-15). Grouped here with the related 2-segment-domain gap above since both stem from the same underlying limitation: `DOTTED_IDENTIFIER_REGEX` classifies by segment count + a maintained allowlist rather than by a structural "looks like a library/package vs. looks like a personal domain" signal.

**Context:** A deeper fix (e.g. checking against a `Build`-time known-installed-package list, or classifying by TLD-likeness for the last segment) would avoid needing to hand-maintain this list at all. Not urgent since the current failure mode is over-redaction, not a leak.

**Effort:** M
**Priority:** P3
**Depends on:** None

### No test/lint blocks Contact.displayName from ever reaching captureException/addBreadcrumb

**What:** `docs/Telemetry.md` now states accurately (corrected in this pass) that contact names are not currently sent to Sentry only because no call site interpolates `Contact.displayName` into a crash message/breadcrumb/extra — not because `scrubPii` has any name-detection mechanism. Nothing structurally prevents a future call site from doing so (e.g. `crashReporter.addBreadcrumb("Merging ${contact.displayName}")` would compile cleanly and ship a real name to Sentry unredacted).

**Why:** Flagged by adversarial review (`/code-review`) on `fix/sentry-pii-scrubbing` (2026-07-15). The doc wording was corrected to stop overstating the guarantee as code-enforced, but adding actual enforcement (a test or lint rule) is separate follow-up work — writing a general name-detection regex is not tractable, but a targeted call-site guard is.

**Context:** Add a check that fails the build if any `crashReporter.captureException`/`addBreadcrumb` call site's arguments reference `Contact.displayName`/`.name` — e.g. a simple grep-based unit test over the source tree, or a Detekt custom rule — so a future regression is caught at build time instead of shipping.

**Effort:** S (grep-based test) or M (Detekt rule)
**Priority:** P2
**Depends on:** None

### Whitespace-separated (space/tab) NANP phone numbers are not redacted

**What:** `PHONE_REGEX`'s bare (unparenthesized) alternative only matches a `-` or `.`-separated 3-3-4 digit triple, never a space- or tab-separated one. A real phone number written as `"415 555 2671"` (no dash/dot/plus/parens) is therefore not redacted by `redactPii`, regardless of any surrounding context — e.g. `redactPii("Call: 415 555 2671")` stays as-is.

**Why:** Deliberately dropped, not an oversight. A prior iteration tried to close this gap with keyword-proximity gating (redact a space-separated triple only when "call"/"phone"/"tel" appeared nearby), but a full code-review pass found that mechanism itself buggy in multiple ways (cross-match keyword bleed onto unrelated triples, tab-separated triples silently bypassing the gate, `\b`-bounded keywords missing compound words like "telephone") — see the retreat documented in the Completed section below. Rather than keep patching a narrow heuristic, the bare alternative was reverted to dash/dot-only: a whitespace-separated 3-3-4 digit triple is structurally indistinguishable from unrelated whitespace-separated number triples this app's crash/breadcrumb text can legitimately contain — most notably coordinate/dimension triples from the image-dedup pipeline (`PerceptualHasher`/junk-scan frame/region data) — and there is no reliable regex-only way to tell them apart. Over-redacting those would destroy debugging context for no privacy benefit, so the safer failure mode (leave the ambiguous shape alone) was chosen over the failure mode of a buggy keyword heuristic.

**Context:** If real phone numbers are observed leaking through in this exact unformatted style, the fix is not another proximity heuristic — it's either accepting the trade-off permanently, or a structurally different signal (e.g. a Luhn-style NANP validity/format check plus a proper tokenized keyword scan, not a fixed-character-window substring search) if this ever needs revisiting. Note this trade-off is currently low-risk in practice: as of 2026-07-16, no `crashReporter.captureException`/`addBreadcrumb` call site (`ContactViewModel`, `JunkViewModel`, `AppUsageViewModel`, `SettingsViewModel`, `PerceptualHasher`) interpolates raw free-typed user text into a Sentry-bound string — messages are system-/exception-generated. If a future feature adds any free-text field (e.g. a bug-report note) that flows into a crash report, re-evaluate priority above P3.

**Effort:** M
**Priority:** P3
**Depends on:** None

### PHONE_REGEX's dash/dot alternative has no NANP validity check, so it over-redacts coincidental dash/dot-separated triples

**What:** Removing the NANP N-digit ([2-9]) check from the dash/dot alternative (to fix the `"015-234-5678"` regression, see Completed section) was applied unconditionally. A dash- or dot-separated 3-3-4 digit triple that is clearly *not* NANP-valid but happens to look coordinate/counter-shaped is now redacted the same as a real phone number, e.g. `redactPii("codes 100-200-3000")` → `"codes [REDACTED_PHONE]"` and `redactPii("codes 100.200.3000")` → `"codes [REDACTED_PHONE]"` — even though the identical digits space-separated (`"codes 100 200 3000"`) are deliberately left untouched by the very next `PiiScrubberTest` case, for the same underlying reason (structurally indistinguishable from unrelated data).

**Why:** This is an accepted, explicit trade-off (not a bug) — requested directly when fixing the dash/dot N-check regression, on the reasoning that dash/dot punctuation is already unambiguous phone-number formatting for real-world (if malformed or non-US) numbers, and an N-check would just as easily discard a genuine number as a coincidental match. It does mean the space/tab residual above and this dash/dot behavior are asymmetric: whitespace triples favor under-redaction (safe, loses debug context), dash/dot triples favor over-redaction (safe from a privacy standpoint, same loses debug context) — both are privacy-safe failure modes, just in opposite directions depending on separator.

**Context:** If dash/dot-separated coordinate/counter data (not phone numbers) is observed being over-redacted in production Sentry data, reconsider re-adding an N-check to the dash/dot alternative specifically — but note that reintroduces the `"015-234-5678"`-style regression this trade-off was chosen to avoid, so any fix here needs to solve both simultaneously (e.g. a validity check that's more permissive than the NANP N-rule but still rejects `100`/`000`-leading groups).

**Effort:** S
**Priority:** P4
**Depends on:** None

### PHONE_REGEX's dash/dot alternative can partially match inside a longer dotted/dashed numeric sequence, corrupting it instead of cleanly redacting or preserving it

**What:** The dash/dot alternative has no boundary requirement beyond "3 digits, separator, 3 digits, separator, 4 digits" (and, per the entry above, no validity check), so it can match a *sub-run* inside a longer multi-segment numeric sequence rather than the whole thing. E.g. `redactPii("connected to 192.168.001.2345 failed")` → `"connected to 192.[REDACTED_PHONE] failed"` (the engine fails to match starting at `192` since the next group isn't exactly 4 digits, then succeeds starting at `168.001.2345`) — leaving a mangled `192.[REDACTED_PHONE]` hybrid that's neither a clean redaction nor a preserved debug-useful string. Same shape with dashes: `redactPii("build 192-168-001-2345 done")` → `"build 192-[REDACTED_PHONE] done"`.

**Why:** Flagged by adversarial review (`/code-review` follow-up pass) on `fix/sentry-pii-scrubbing` (2026-07-16) — confirmed real, but requires an uncommon shape (a 4+-segment dotted/dashed numeric string with a specific 4-digit trailing group, e.g. an extended version string or non-standard build ID) that's rarer than the already-covered 2-segment IPv4 case (`"192.168.0.1"`, which doesn't trigger this since it has no 4-digit group). Not part of the original bug report scope.

**Context:** If build IDs, extended version strings, or similar 4+-segment dotted/dashed numeric data are observed getting partially mangled in production Sentry data, consider anchoring the dash/dot alternative so it only matches when not immediately adjacent to another digit-and-separator group (a broader "not part of a longer numeric sequence" guard, similar in spirit to the existing `(?<!\d)`/`(?!\d)` lookaround but extended past the immediate separator).

**Effort:** S
**Priority:** P4
**Depends on:** None

## Completed

### ContactViewModel dùng message String cứng thay vì messageRes

**What:** `ContactViewModel.confirmMerge` dùng message String cứng (không dấu, không qua strings.xml) thay vì `messageRes: Int?`. Đã sửa theo đúng pattern Batch B (`PrivacyViewModel`): `mergeMessage: String?` → `@StringRes messageRes: Int?` + `mergedCount: Int`, thêm `contact_merge_success`/`contact_merge_error` vào cả 3 locale (values/values-en/values-fr), `ContactScreen.kt` giờ render Snackbar thật qua `stringResource(messageRes, mergedCount)` (trước đó chỉ có timer tự-dismiss, không hiển thị text). Kèm theo đã fix bug liên quan: `load()` từng ghi đè toàn bộ `ContactUiState` bằng constructor mới (không `.copy()`) nên `messageRes` set xong bị `load()` xoá ngay lập tức trước khi UI kịp hiển thị — đổi 2 chỗ gán state trong `load()` sang `.copy()` để giữ `messageRes`/`mergedCount`/`mergingGroupIndex` qua các lần load. Test mới: `ContactViewModelTest` (3 case: success/error/dismiss).

Rà soát không phát hiện ViewModel nào khác trong `presentation/` còn dính pattern `message: String?` hardcode — không có mục follow-up mới.

**Completed:** (nhánh `fix/contact-viewmodel-i18n`, chưa merge)

### PHONE_REGEX's space-separated NANP alternative has a residual false-positive: two unrelated numbers both starting 2-9

**What:** The space-separator fix (`/code-review` follow-up, 2026-07-15) narrowed `PHONE_REGEX`'s bare-NANP alternative with an N-digit check (area/exchange code must start 2-9) to reject `"100 200 3000"`-style triples, but did not reject a coincidental 3-3-4 space-separated triple where *both* leading groups happen to start with 2-9, e.g. `redactPii("Received frame 480 640 3840")` → `"Received frame [REDACTED_PHONE]"`.

**First attempt (reverted):** `PHONE_REGEX`'s bare-NANP alternative was made a named group (`bareNanp`), and a new `redactPhoneNumbers`/`hasNearbyPhoneKeyword` mechanism required a `"call"/"phone"/"tel"` keyword within a fixed character window before redacting a space-separated match. A dedicated `/code-review` pass on that change found it introduced more real bugs than it fixed: (1) the keyword window wasn't scoped to a specific match, so a keyword next to one phone number wrongly gated-in an unrelated triple elsewhere in the same string (`"Call 415 555 2671, frame 480 640 3840"` redacted *both*); (2) the "is this the space-separated form" check used `contains(' ')` (literal space only) while the regex's own separator class matched any whitespace, so a tab-separated triple silently bypassed the gate entirely and was always redacted, reproducing the exact false-positive class the mechanism existed to prevent; (3) `\b`-bounded keywords couldn't match inside compound words like "telephone"/"cellphone", so real numbers with an obvious label went unredacted; (4) merging the three `PHONE_REGEX` alternatives into one shared N-check silently applied the 2-9 validity check to the dash/dot separators too, so non-NANP-valid dash/dot numbers (e.g. `"015-234-5678"`) stopped matching *at all* — a real coverage regression that a since-corrected TODOS.md entry had also inaccurately called "unaffected."

**Final fix:** Reverted to a robust, narrower subset instead of patching the keyword heuristic further. `PHONE_REGEX`'s bare alternative now requires a `-`/`.` separator only (never space/tab) and has **no** NANP N-digit check — any dash/dot 3-3-4 digit triple is redacted unconditionally, NANP-valid or not (fixes the `"015-234-5678"` regression). A whitespace-separated triple (space or tab), phone-shaped or not, keyword nearby or not, is never redacted — this is now a deliberate, documented residual (see "Whitespace-separated (space/tab) NANP phone numbers are not redacted", P3, above) rather than a heuristic that turned out to be unreliable. All keyword-gating code (`PHONE_KEYWORD_REGEX`, `PHONE_KEYWORD_WINDOW`, `hasNearbyPhoneKeyword`, `redactPhoneNumbers`) was removed. `PiiScrubberTest` covers the full FP matrix: dash/dot redacted unconditionally (including a non-N-valid case), space/tab-separated triples never redacted (plain digits, frame-dimension shape, tab-separated, and even with a "Call:" label present).

**Completed:** (nhánh `fix/sentry-pii-scrubbing`, chưa merge)

### isUrlHostname doesn't recognize protocol-relative URLs ("//host/path"), so their hostname gets over-redacted

**What:** `isUrlHostname` (in `SentryCrashReporter.kt`) only recognized hostnames preceded by literal `http://` or `https://`. A protocol-relative URL (no scheme, format `//host/path`, e.g. `"Load //api.example.com/resource failed"`) has only `//` before the hostname, so `isUrlHostname` returned `false` and the hostname got wrongly redacted to `[REDACTED_PACKAGE]`.

**Fix:** `isUrlHostname` now also exempts a bare `//` immediately before the identifier, guarded so it can't reintroduce the custom-scheme leak this carve-out is deliberately narrow to avoid: the character right before the `//` must be neither `:` (which would re-exempt `myapp://com.attacker.malware.MainActivity`-style deep-link authorities) nor a word character (which would treat a mid-identifier `foo//bar.baz.qux` as a URL). New tests in `PiiScrubberTest`: protocol-relative URL kept intact (mid-string and start-of-string), mid-identifier double-slash still redacted, and a non-http(s) scheme (`ftp://`) still redacted (confirming the carve-out stays scoped to `http(s)://` and bare `//` only, not any `scheme://`).

**Follow-up fix (same branch, same review pass):** The `/code-review` pass on the above also found that `precededByHttp`/`precededByHttps` did a raw fixed-offset substring match with no check on what precedes "http"/"https" itself, so a custom scheme whose name merely *ends* in those letters (e.g. `"shttp://com.attacker.malware.MainActivity"`) was wrongly treated as a real `http(s)` URL and exempted — reintroducing the exact custom-scheme leak this carve-out exists to prevent. Added `isSchemeBoundary`: the scheme match only counts when it starts at the beginning of the text or right after a non-letter character. New test in `PiiScrubberTest`: `"shttp://..."` still redacts the package-shaped identifier after it.

**Completed:** (nhánh `fix/sentry-pii-scrubbing`, chưa merge)

### FILE_PATH_REGEX leaked the tail of any file path containing a space; DOTTED_IDENTIFIER_REGEX dropped a digit-leading domain label

**What:** Two real gaps found by the `/ship` Step 11 adversarial review, both confirmed by directly tracing the shipped regexes against realistic input:
1. `FILE_PATH_REGEX`'s trailing group (`(?:/\S*)?`) stopped at the first whitespace, so any Android filename containing a space — the norm, not the edge case (WhatsApp media, screenshots, human-typed names) — only got partially redacted: `"/storage/.../Download/Jane Doe Resume.pdf"` → `"[REDACTED_PATH] Doe Resume.pdf"`, leaking the filename tail right next to the redaction marker. No test covered a space-containing path.
2. `DOTTED_IDENTIFIER_REGEX` required every segment to start with a letter, so a domain whose leading label starts with a digit either lost its prefix (`"1.bp.blogspot.com"` → only `"bp.blogspot.com"` matched, leaving `"1."` as an unredacted chopped artifact) or, worse, fell entirely below the 3-segment threshold and leaked in full (`"3rdpartyapi.example.com"` → only the 2-segment `"example.com"` matched, so nothing was redacted at all).

**Fix:**
1. `FILE_PATH_REGEX`'s trailing group now matches to end-of-line (`(?:/.*)?` instead of `(?:/\S*)?`), favoring over-redaction over a leak — the documented trade-off is that trailing prose on the same line after a path (e.g. `"(No such file or directory)"`) is now swallowed into the redaction too, rather than preserved as debug context. Updated the 3 existing test expectations this changed and added a regression test for space-containing filenames.
2. `DOTTED_IDENTIFIER_REGEX`'s *first* segment now allows a digit-start (`[a-zA-Z0-9]` instead of `[a-zA-Z]`), while every segment after the first dot still requires a letter-start — narrower than the reviewer's original "widen every segment" suggestion, which would have broken the existing IPv4-exclusion test (`"192.168.0.1"` has 4 digit-led segments and would have matched as a 4-segment package-shaped identifier). Verified empirically (Python regex trace) that the narrow fix closes both digit-leading-domain cases while leaving IPv4 exclusion and all existing package-name tests intact. New tests in `PiiScrubberTest` for both digit-leading-domain shapes.

Both fixes closed the `/ship` Step 7 coverage gate (78% → 90%+ before this round; 34 total test cases after). Two related but out-of-scope gaps found in the same adversarial pass were deferred rather than fixed here — see "configureSentryOptions wires beforeSend but never beforeSendTransaction" (P2) and "redactStringValues only scrubs direct String map values" (P4) above.

**Completed:** (nhánh `fix/sentry-pii-scrubbing`, chưa merge)

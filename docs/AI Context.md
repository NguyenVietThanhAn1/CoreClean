## AI Context

> Một file ngắn để LLM (Claude Code / Copilot) nắm được bối cảnh trước khi sinh code.

### Project Goals
- Cleaner app Android **không bịp** — không hiển thị số RAM/Storage giả, không hù người dùng.
- Performance + lightweight architecture.
- Tuân thủ best practice Android hiện đại (Compose, Hilt, Coroutines, type-safe Navigation).

### Architecture Goals
- Clean Architecture (Domain / Data / Presentation).
- MVVM với `StateFlow`.
- Module hoá theo feature (media, storage, battery, ram, usage, contact, junk, applist, privacy).
- Test-friendly: mọi Repository và DataSource đều inject được fake/Robolectric.

### Coding Standards
- Kotlin-first, Compose-only UI (không XML).
- Hilt cho DI, không singleton thủ công.
- Coroutines + Flow, không RxJava.
- Tránh API deprecated (vd: dùng `MediaStore.createDeleteRequest()` thay vì `delete()`).
- Mọi string đi qua `stringResource()` — không hardcode.

### Module Status (Sprint 5)

Xem nguồn duy nhất: [`note.md`](note.md). Các file khác KHÔNG được copy-paste bảng này — chỉ link tới note.md.

### Critical Files cho AI khi sinh code
- `docs/Architecture.md` — layering rules.
- `docs/Permissions.md` — bảng quyền + fallback.
- `docs/AntiPatterns.md` — những thứ tuyệt đối không được làm (fake numbers, scareware, dark pattern).
- `docs/Security.md` — threat model, data handling.
- `docs/Contributing.md` — commit + branch convention.

### Hỏi gì trước khi sinh code
1. Module mới này có cần permission không? Đã có trong `Permissions.md` chưa?
2. Có chạm vào dữ liệu user (Contacts, MediaStore) không? Có vi phạm `Security.md` không?
3. UI có hiển thị con số nào? Số đó có thể giả/phóng đại không? Xem `AntiPatterns.md`.
4. Có thêm string cứng không? Phải dùng `stringResource` + cập nhật `values-en/strings.xml`.

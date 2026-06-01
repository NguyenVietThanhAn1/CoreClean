# UX Guidelines

## Voice & Tone

- **Trung tính, không cảm thán.** "Bạn có 1.2 GB ảnh trùng" ✅. "🚨 PHÁT HIỆN 1.2 GB RÁC!!" ❌.
- **Số phải có ngữ cảnh.** "5,247 ảnh" → "5,247 ảnh (trong đó 12 nhóm trùng)".
- **Không hứa hẹn.** Không dùng "Tăng tốc!", "Pin lâu hơn!", "Sạch ngay!" — đây là cleaner, không phải magic.
- **Vietnamese as primary** — câu ngắn, không Anh-Việt lẫn ("App của bạn đang load slow" ❌).

## Color Semantics

| Token | Khi nào dùng |
|---|---|
| `primary` | CTA chính (button "Quét", "Xác nhận") |
| `error` | Delete action, low battery, full storage |
| `errorContainer` | Banner cảnh báo (duplicate count, ...) |
| `tertiary` | Thông tin phụ (size, date) |
| `surfaceVariant` | Card disabled, "Coming soon" |

**Không** dùng red cho mọi cảnh báo. Red chỉ cho **destructive** (xoá) hoặc **critical** (pin ≤ 5%, storage ≤ 1GB).

## Spacing Scale

8dp grid:
- Padding card: 16dp.
- Spacing giữa items: 8dp / 12dp.
- Section vertical gap: 24dp.
- Screen edge padding: 16dp (mobile), 24dp (tablet).

## Typography

| Style | Use |
|---|---|
| `headlineMedium` | Screen title trong TopAppBar |
| `titleLarge` | Section header |
| `titleMedium` | Card title |
| `bodyMedium` | Body text (default) |
| `bodySmall` | Subtitle / metadata |
| `labelSmall` | Tag, chip, badge |

KHÔNG override font weight thủ công — dùng style preset.

## Iconography

- Material Icons Extended only.
- Size: 24dp (default), 36dp (feature card), 48dp (empty state hero).
- Tint: theo content color của container.

## Layout Rules

- **TopAppBar** mọi screen → tiêu đề + back arrow (nếu không phải Home).
- **Scaffold** dùng `contentWindowInsets = WindowInsets.safeDrawing` — Sprint 2 đã chuẩn.
- **Empty state** luôn có: icon + 1 dòng giải thích + 1 nút action (nếu áp dụng).
- **Loading state** > 500ms → spinner; < 500ms → skip spinner (tránh flash).
- **Error state** → 1 dòng human-readable + nút "Thử lại" (không show stack trace).

## Confirmation Dialogs

Cho destructive action (delete, uninstall):

```
┌─────────────────────────┐
│ Xoá 12 ảnh trùng?       │
│                         │
│ Bạn sẽ giải phóng       │
│ 245 MB. Hành động       │
│ không thể hoàn tác.     │
│                         │
│  [Huỷ]      [Xoá]      │
└─────────────────────────┘
```

- Title: câu hỏi cụ thể, có con số.
- Body: hệ quả + warning nếu cần.
- 2 button equal width, "Xoá" màu `error`.
- KHÔNG dùng "OK" / "Cancel" — dùng verb cụ thể.

## Motion

- Compose default crossfade / slide.
- Không dùng spring quá bounce (gây cảm giác "kid app").
- Duration: standard 300ms, fast 150ms, slow 500ms.

## Density

- Touch target min 48×48dp (a11y).
- List item height min 56dp.
- Click ripple bật mặc định cho mọi clickable surface.

## Forbidden UI Patterns

Xem [`AntiPatterns.md`](AntiPatterns.md).

## Screenshots cho Play Store

- 9:16 aspect ratio.
- 5-8 ảnh per locale.
- Slide 1: HomeScreen với dashboard.
- Slide 2-4: 3 module nổi bật (Media dedupe, Storage donut, Battery).
- Slide 5: Safety Review (trust signal — confirm trước khi xoá).
- Slide 6: Privacy Dashboard.

Lưu tại `docs/images/screenshots/<locale>/`.

## A11y Targets (Phase 4)

- Tất cả `Icon` có `contentDescription` (null chỉ khi decorative + adjacent text).
- Contrast ratio ≥ 4.5:1 (body), 3:1 (large text).
- Hỗ trợ font scale 200%.
- TalkBack thử nghiệm full flow trước release lớn.

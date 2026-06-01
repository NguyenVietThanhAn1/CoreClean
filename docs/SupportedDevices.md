# Supported Devices & OEM Quirks

## SDK Range

| | Version | API | Tỉ lệ thị trường (2026, ước tính) |
|---|---|---|---|
| `minSdk` | Android 8.0 Oreo | 26 | ~99% |
| `targetSdk` | Android 16 | 36 | latest |
| `compileSdk` | Android 16 | 36 | latest |

Lý do `minSdk=26`: cần `StorageStatsManager` (App size), `Notification Channels`, `JobScheduler` mature.

## Tested Devices

| Brand | Model | Android | Status | Notes |
|---|---|---|---|---|
| Pixel | 6, 7, 8 | 14, 15 | ✅ Full | Reference device |
| Samsung | S22, S23, S24 | 14, 15 | ✅ Full | Knox không can thiệp |
| Xiaomi | Redmi Note 13, Mi 14 | 14 MIUI 15 | ⚠️ Quirks | xem dưới |
| Huawei | Mate 50, P60 | EMUI 13 (no GMS) | ⚠️ Quirks | xem dưới |
| OnePlus | 11, 12 | 14 OxygenOS | ✅ Full | DeepClean có thể trùng feature |
| Oppo / Realme | Reno 11 | ColorOS 14 | ⚠️ Quirks | aggressive background kill |
| Vivo | X100 | OriginOS 4 | ⚠️ Quirks | tương tự Oppo |

## Known OEM Quirks

### Xiaomi / MIUI
- **Background kill aggressive:** Worker bị kill nếu app không trong "Autostart whitelist". Hướng dẫn user trong Onboarding.
- **Battery temperature ×10:** Một số model báo `EXTRA_TEMPERATURE = 320` thay vì `32.0°C`. Heuristic: nếu `temp > 100` → chia 10.
- **MIUI Optimization toggle:** Nếu user bật → Worker bị thay đổi schedule policy. Document trong knownIssues.

### Huawei / EMUI / HarmonyOS
- **No Google Play Services** (post-2019 model): Sentry hoạt động bình thường (REST), nhưng nếu thêm Firebase sau này phải fallback HMS.
- **App Lock**: Một số EMUI có "Phone Manager" tự dọn cache → có thể trigger trùng với CoreClean. Notify user trong tooltip.
- **PowerGenie:** Whitelist required cho Worker.

### Samsung / OneUI
- **Device Care** đã có sẵn cleaner — không xung đột nhưng có thể gây nhầm lẫn. Tooltip Onboarding giải thích khác biệt.
- **Knox container:** User trong Knox không thấy app khác → AppUsage / AppAnalyzer trả về subset.

### Oppo / Realme / Vivo
- **Battery Optimization "Sleep Standby":** Worker bị kill sau 1h idle nếu không whitelist. Hướng dẫn user.
- **Auto-start denied by default:** Phải xin manual trong Settings → Battery → App startup.

### OnePlus
- **OxygenOS Cleaner**: có sẵn — feature parity nên không xung đột; user có thể tắt OxygenOS cleaner nếu chọn CoreClean.

### General — Android 14+ (API 34+)
- **Foreground service types:** WorkManager + `setForeground` cần khai `dataSync` type. Đã có trong AndroidManifest.
- **Selected photos access:** Khi user chỉ grant "Selected photos", `READ_MEDIA_VISUAL_USER_SELECTED` cho thấy subset. UI phải có nút "Xem thêm ảnh" để re-prompt.

## Form Factor

| | Status |
|---|---|
| Phone portrait | ✅ Primary target |
| Phone landscape | ⚠️ Layout không tối ưu (Sprint 6) |
| Tablet portrait | ✅ NavigationRail + 3-column grid (Sprint 7) |
| Tablet landscape | ✅ NavigationRail + 3-column grid (Sprint 7) |
| Foldable (unfolded) | ✅ Expanded width → NavigationRail two-pane (Sprint 7) |
| ChromeOS | 🔲 Untested |
| Wear OS | ❌ Out of scope |
| Android TV | ❌ Out of scope |

## Performance Targets

| Device tier | Cold start | Scan 5K photos |
|---|---|---|
| High (SD 8 Gen 3) | < 500 ms | < 3s |
| Mid (SD 6 Gen 1) | < 800 ms | < 8s |
| Low (Helio G99) | < 1500 ms | < 20s |

Measured via macrobenchmark — xem [`Performance.md`](Performance.md).

# Release Process

## Versioning

`MAJOR.MINOR.PATCH` (SemVer).

- **MAJOR**: breaking change cho user (xoá feature, đổi schema không migration).
- **MINOR**: thêm module mới (Sprint deliverable).
- **PATCH**: bug fix, doc, refactor.

Track trong `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 6           // bump mỗi lần build phát hành (monotonic)
    versionName = "1.5.0"     // SemVer
}
```

`versionCode` luôn tăng — không reset. Convention: `MAJOR*10000 + MINOR*100 + PATCH`. Ví dụ `1.5.0` → `10500`.

## Sprint → Release Mapping

| Sprint | versionName | versionCode | Highlights |
|---|---|---|---|
| 1 | 1.0.0 | 10000 | Initial Media Scanner |
| 2 | 1.1.0 | 10100 | Safety delete, content hash, Worker |
| 3 | 1.2.0 | 10200 | Storage + Battery, dashboard |
| 4 | 1.3.0 | 10300 | Onboarding, AppUsage, Contacts (lite), Junk, Settings |
| 5 | 1.4.0 | 10400 | RAM, APK Analyzer, Privacy, Sentry, i18n, SAF |
| 6 | 1.5.0 | 10500 | TBD |

## Signing

**Debug:** Android default debug keystore (auto-generate).

**Release:**
- Keystore file: `keystore.jks` (NOT in repo — gitignored).
- Path + passwords trong `local.properties`:
  ```properties
  KEYSTORE_PATH=/home/user/keystore.jks
  KEYSTORE_PASSWORD=...
  KEY_ALIAS=coreclean
  KEY_PASSWORD=...
  ```
- `app/build.gradle.kts` load qua `gradleLocalProperties()`.
- Production keystore backup: 1Password vault "CoreClean Signing" (chỉ 2 maintainer access).

## Release Checklist

Trước mỗi release lên Play Store:

- [ ] Tất cả CI checks xanh trên `main` branch.
- [ ] Manual smoke test trên 1 device mid-tier (Pixel 6a) + 1 OEM quirk (Xiaomi/Samsung).
- [ ] CHANGELOG.md có entry cho version mới.
- [ ] `versionCode` + `versionName` bumped.
- [ ] `docs/PrivacyPolicy.md` effective date cập nhật nếu có thay đổi data flow.
- [ ] Screenshots cho Play listing cập nhật nếu UI đổi (3 ngôn ngữ: vi, en, ...).
- [ ] Release notes (≤ 500 ký tự) cho mỗi locale.
- [ ] APK + AAB build với `./gradlew :app:bundleRelease`.
- [ ] `bundletool` test install trên 1 device trước khi upload.

## Play Store Upload

1. Play Console → Production → Create new release.
2. Upload AAB (`app/build/outputs/bundle/release/app-release.aab`).
3. Release notes per locale (chép từ CHANGELOG).
4. **Staged rollout:** 5% → 20% → 50% → 100% (theo dõi Sentry crash-free rate ≥ 99.5% mỗi giai đoạn).
5. Để 24h giữa các stage, abort nếu crash-free drop > 0.5%.

## Hotfix Flow

Nếu phát hiện crash nghiêm trọng sau release:

1. Branch `hotfix/x.y.z+1` từ tag release.
2. Fix + test.
3. Bump `versionCode` (không cần MINOR, chỉ PATCH).
4. Skip staged rollout — push 100% nếu severity cao.
5. Cherry-pick về `main` + `develop`.

## Tag Convention

```
v1.5.0          # release tag
v1.5.0-rc.1     # release candidate
v1.5.1-hotfix   # hotfix
```

Tag từ `main` branch sau khi merge release PR.

## Internal Test Track

Trước Production:
- **Internal testing** (5 user): smoke test full flow.
- **Closed testing** (20 user, beta): 1 tuần.
- **Open testing** (100+ user, optional): 2 tuần với feature mới lớn.

## Rollback

Play Console không cho rollback APK. Workaround:
- Push hotfix version mới với code rollback (giảm `versionName` nhưng bump `versionCode`).
- Trong CHANGELOG ghi rõ "Rolled back X.Y.Z due to ...".

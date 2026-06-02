## Development Roadmap

### Phase 1 - Core Features
- [x] Media scanner
- [x] Storage analyzer (basic - StatFs + MediaStore)
- [x] Battery information (reactive Flow)
- [x] Duplicate image detection (MD5 content hash)
- [x] Background scanning service (WorkManager 12h periodic)
- [x] Cache cleaner (basic — guide + TEMP/APK/Empty scan)

### Phase 2 - Performance Optimization
- [x] RAM usage monitor (Sprint 5)
- [x] Junk file cleaner (Sprint 4/5 — + SAF folder picker)
- [x] APK analyzer (Sprint 5)
- [x] Large file detection (Sprint 4, >50 MB via MediaStore)
- [x] App cache management (guide user, Sprint 4)
- [x] Download folder cleaner (Sprint 5 — SAF OpenDocumentTree)

### Phase 3 - Smart Features
- [x] AI-based duplicate image detection (Sprint 7 — pHash, hammingDistance ≤ 8, toggleable)
- [x] Smart cleaning suggestions (Sprint 6 — rule-based, 5 rules)
- [x] Battery usage prediction (Sprint 6 — linear regression, 24 h window)
- [x] Scheduled auto-cleaning (Sprint 6 — AutoCleanWorker, safe categories only)
- [x] Notification recommendations (Sprint 6 — rate-limited, opt-in)

### Phase 4 - UX & Security
- [x] Material 3 redesign polish (Sprint 6 — WindowSizeClass, tablet layout)
- [x] Permission onboarding flow (Sprint 4)
- [x] Accessibility support (Sprint 6 — TalkBack, Canvas semantics, cd_* strings)
- [x] Privacy dashboard (Sprint 5 + Sprint 6 — battery history, privacy policy link)
- [x] Dark mode optimization (Sprint 7 — AMOLED black mode, toggleable in Settings)

### Phase 5 - Distribution
- [x] FOSS build flavor (Sprint 7 — foss/gms product flavors, Sentry disabled in FOSS)
- [x] GitHub Pages (Sprint 7 — Privacy Policy hosted at annguyn.github.io/CoreClean)
- [x] FOSS APK truly Sentry-free (Sprint 8 — gmsImplementation, NoOpCrashReporter, zero io.sentry in foss)
- [x] MANAGE_EXTERNAL_STORAGE removed (Sprint 8 — Play Store blocker resolved; SAF-only empty-folder scan)
- [x] Baseline profile committed (Sprint 8 — startup hints for profileinstaller)
- [x] CI builds both flavors (Sprint 8 — explicit FOSS + GMS steps, flavor-specific test tasks)
- [ ] Play Store launch (content rating, data safety form ready — see PlayStoreListing.md; unblocked by Sprint 8)
- [ ] F-Droid submission (Sprint 8: all blockers resolved — foss APK Sentry-free, no restricted permissions)
- [ ] Crowdin i18n (additional languages beyond vi/en)

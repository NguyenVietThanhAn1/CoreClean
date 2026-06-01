## Planned Features

### Implemented (Sprint 1-4)
- [x] Media scanner with duplicate detection (content hash MD5)
- [x] Safety delete flow with Android 11+ MediaStore.createDeleteRequest
- [x] Background media scan Worker (12h periodic, BATTERY_NOT_LOW)
- [x] Storage Analyzer: StatFs + MediaStore breakdown donut chart
- [x] Battery Monitor: reactive Flow, Canvas level indicator
- [x] HomeScreen dashboard with module cards
- [x] App Usage statistics (UsageStatsManager, 7d/30d range)
- [x] Contacts cleanup — detect duplicate + incomplete (lite)
- [x] Junk Cleaner: TEMP/APK/Empty folder scan + APP_CACHE guide
- [x] Large file detection (>50 MB, MediaStore)
- [x] Permission Onboarding flow (DataStore-backed, 3-step)
- [x] Settings: Theme / Dynamic Color / Background scan schedule
- [x] CI pipeline: GitHub Actions android-ci.yml

### File Optimization (Sprint 5)
- [ ] Download folder cleaner (SAF)
- [ ] APK extractor / bulk uninstall
- [ ] Junk scanner expanded (SAF external storage)

### RAM & System (Sprint 5)
- [ ] Background app monitor
- [ ] Memory usage stats (RAM monitor)

### App Management (Sprint 5)
- [ ] Contacts merge UI (full delete/merge flow)
- [ ] APK Analyzer

### Battery (Future)
- [ ] Battery drain analysis
- [ ] Charging prediction
- [ ] OEM quirk detection

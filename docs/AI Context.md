## AI Context

### Project Goals
- Build a modern Android cleaner app
- Focus on performance and lightweight architecture
- Avoid fake cleaning / scam cleaner patterns
- Follow modern Android development practices

### Architecture Goals
- Clean Architecture (Domain / Data / Presentation)
- MVVM with StateFlow
- Scalable modular structure
- Easy testing and maintenance

### Coding Standards
- Kotlin-first, Compose-only UI
- Dependency Injection with Hilt
- Coroutines + Flow
- Avoid legacy Android APIs

## Module Status Table

| Module                 | Status              | Notes                                          |
|------------------------|---------------------|------------------------------------------------|
| Media Scanner          | Implemented         | Full: scan, dedup, SafetyReview, Worker        |
| Storage Analyzer       | Implemented         | StatFs + MediaStore + Large File (>50MB)       |
| Battery Monitor        | Implemented (basic) | Reactive BroadcastReceiver                     |
| App Usage              | Implemented         | UsageStatsManager, 7d/30d, system app filter   |
| Contacts               | Implemented (lite)  | Detect duplicate + incomplete; no merge        |
| Junk Cleaner           | Implemented (basic) | TEMP/APK/Empty scan; APP_CACHE guide only      |
| Permission Onboarding  | Implemented         | DataStore flag, 3-step HorizontalPager         |
| Settings               | Implemented         | Theme/DynamicColor/BackgroundScan via DataStore|
| APK Analyzer           | Not implemented     | Sprint 5                                       |

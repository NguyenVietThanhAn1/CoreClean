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

| Module            | Status              | Notes                                  |
|-------------------|---------------------|----------------------------------------|
| Media Scanner     | Implemented         | Full: scan, dedup, SafetyReview, Worker |
| Storage Analyzer  | Implemented (basic) | StatFs + MediaStore; no Doc/Downloads  |
| Battery Monitor   | Implemented (basic) | Reactive BroadcastReceiver             |
| App Usage         | Stub                | Pending Sprint 4                       |
| Contacts          | Stub                | Pending Sprint 4                       |

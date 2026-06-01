# Contributing

## Commit Convention

Format: `<type>(<scope>): <description>`

| Type     | When to use                               |
|----------|-------------------------------------------|
| `feat`   | New feature                               |
| `fix`    | Bug fix                                   |
| `chore`  | Build, CI, tooling, cleanup               |
| `docs`   | Documentation only                        |
| `test`   | Tests only (no production code change)    |
| `refactor` | Refactoring without behavior change     |

Examples:
```
feat(media): add content hash duplicate detection
fix(ui): permission denied recovery flow
chore: update .gitignore with full Android template
docs: add Architecture.md with ASCII diagram
test: unit tests for DuplicateDetector and MediaViewModel
```

## Branch Naming

```
feature/sprint3-storage-analyzer
feature/sprint4-large-file-scan
fix/sprint2-permission-denied
```

## Checklist Before Merge

- [ ] `./gradlew :app:assembleDebug` passes (BUILD SUCCESSFUL)
- [ ] `./gradlew :app:testDebugUnitTest` passes (all tests green)
- [ ] `./gradlew :app:lintDebug` — 0 new warnings
- [ ] New feature has at least one unit test
- [ ] Docs updated if public API changed
- [ ] No large files checked in (binary assets, keystore, APK)
- [ ] CI checks pass on GitHub Actions (android-ci.yml)

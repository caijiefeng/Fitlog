# FitLog Technical Decisions (ADRs)

## ADR-001: Single Module Architecture

**Decision**: Use a single `:app` Gradle module for V0 through V2.

**Rationale**:
- Early-stage velocity is more important than build time optimization.
- Multi-module adds Gradle configuration complexity with little benefit for a 5-screen app.
- Can refactor to `:core`, `:feature:*` modules when the codebase grows beyond ~50 source files.

**Alternatives considered**:
- Multi-module from start: rejected due to over-engineering at V0.
- Dynamic feature modules: rejected — no benefit for a local-first app with no download size concerns.

---

## ADR-002: Material 3 Forced Dark Theme

**Decision**: V0 ships with a single, forced dark color scheme. The app does not follow the system light/dark mode setting.

**Rationale**:
- Matches the "black and white" product vision.
- Simplifies theme maintenance — one color scheme, one set of tokens.
- Prevents UI inconsistencies during development when only dark mode is designed.
- Light mode can be added later as an opt-in user setting.

**Implications**:
- The `FitLogTheme` composable always applies `FitLogDarkColorScheme`.
- `enableEdgeToEdge()` is called in MainActivity; theme sets `isAppearanceLightStatusBars = false`.
- The "外观" setting in Profile will not offer a light/dark toggle until light mode is implemented.

**Alternatives considered**:
- Following system light/dark mode: rejected — the monochrome design vision is dark-first; a light scheme would require separate design work.
- Dynamic color (Material You): rejected — conflicts with the monochrome aesthetic and would make the brand accent inconsistent.

---

## ADR-003: Room + DataStore

**Decision**: Use Room for structured data and DataStore for preferences.

**Rationale**:
- Room is the official Google-recommended local database for Android.
- First-class Hilt and Coroutines support.
- DataStore is the recommended replacement for SharedPreferences in new projects.
- Both are well-documented and widely adopted.

**V0 Note**: Room is not instantiated in V0. The `@Database` annotation, entities, and Hilt provider are all deferred to V1. This avoids creating placeholder tables that would require destructive migration when real entities are introduced.

**Alternatives considered**:
- SQLDelight: rejected — the team has more experience with Room.
- MMKV: rejected — DataStore performance is adequate for settings.
- SharedPreferences: rejected — new projects should prefer DataStore.

---

## ADR-004: Hilt for Dependency Injection

**Decision**: Use Dagger Hilt as the DI framework.

**Rationale**:
- Official Google recommendation for Android.
- Compile-time verification (unlike Koin).
- First-class ViewModel, WorkManager, Navigation integration.
- Single `@HiltAndroidApp` annotation for Application.

**Alternatives considered**:
- Koin: rejected — runtime DI, less type-safe, harder to debug.
- Manual DI: rejected — too much boilerplate as the app grows.

---

## ADR-005: KSP for Annotation Processing

**Decision**: Use KSP (Kotlin Symbol Processing) instead of KAPT.

**Rationale**:
- KSP is the official Google/Kotlin direction — KAPT is in maintenance mode.
- Generally faster compilation than KAPT, especially as processor count scales.
- Supported by Room 2.6+ and Hilt 2.53+ with the KSP compiler plugin.

**Benchmark note**: Performance improvement varies by project size, processor count, and incremental vs. clean build. No fixed multiplier is claimed.

---

## ADR-006: String-Based Navigation Routes

**Decision**: Use simple string-based routes in Navigation Compose for V0.

**Rationale**:
- Only 5 routes in V0 — type-safe navigation overhead is unnecessary.
- Migration path: switch to type-safe navigation (Compose Navigation 2.8+ `@Serializable` routes) when secondary routes (exercise detail, template editor, workout execution) are added.

**Migration trigger**: When the route count exceeds ~12 or when passing complex arguments between screens becomes common (V2+), migrate to type-safe routes.

---

## ADR-007: Feature-Package Organization

**Decision**: Organize source by feature, not by layer.

**Rationale**:
- Easier to find all files related to one screen.
- Scales better as features grow.
- Common/core code is shared across features.
- Matches Android architecture guide recommendations.

**Structure**:
```
feature/today/   → TodayScreen, TodayViewModel
feature/plan/    → PlanScreen, PlanViewModel
...
core/            → Shared design system, navigation, data
domain/          → Shared business logic
```

---

## ADR-008: Min SDK 28 (Android 9)

**Decision**: Target Android 9 (API 28) and above.

**Rationale**:
- Enables `java.time` (no ThreeTenABP needed).
- Allows modern Compose and Room features without compatibility shims.
- CameraX requires API 21+ (well within range).
- API 28 represents the majority of active Android devices; we do not claim a specific percentage without citing a current distribution source.

---

## ADR-009: No Baseline Profile in V0

**Decision**: Defer Baseline Profile and Macrobenchmark to V4+.

**Rationale**:
- Premature optimization before real UI complexity exists.
- Adds CI complexity and build time.
- Will add when performance data shows startup or compose jank issues.

---

## ADR-010: Room Migration Strategy

**Decision**: Use tested, incremental Room migrations for every version change. Never use `fallbackToDestructiveMigration()` in production.

**Rationale**:
- User data is local-only — destructive migration means permanent data loss.
- Each new entity set requires a `Migration( from, to )` with corresponding tests.
- Export schema JSON for each version to enable migration testing.

**Implications**:
- `exportSchema = true` in production builds.
- Migration tests use `MigrationTestHelper`.
- CI validates all migration paths from N-1 to N.

---

## ADR-011: In-Progress Workout Recovery

**Decision**: A `WorkoutSession` with `status = IN_PROGRESS` must survive process death and be restorable when the user returns to the app.

**Strategy**:
- `WorkoutSession` with `status = IN_PROGRESS` is written to Room on each set completion (or at minimum every 30 seconds).
- On app restart or process recreation, the Today tab checks for any session with `status = IN_PROGRESS` and offers to resume.
- The workout execution screen is a full-screen destination outside the bottom-nav scaffold — the nav bar is hidden to prevent accidental navigation away during an active session.
- If the user force-quits and the session is older than 24 hours, the session is auto-cancelled (status = CANCELLED).

**Alternatives considered**:
- `SavedStateHandle` only: rejected because it does not survive process death on all devices.
- In-memory only: rejected — data loss on crash.

---

## ADR-012: Backup and Export Strategy

**Decision**: `android:allowBackup` is set to `false` by default. Data backup, export, and restore are user-initiated actions implemented in V7.

**Rationale**:
- FitLog is a local-first, privacy-first application. Automatic backup to cloud services (Android Auto Backup) conflicts with the privacy guarantee.
- User data should only leave the device through explicit user action.
- Manual export gives users full control over what is exported and where it goes.

**V7 Implementation**:
- CSV export: workouts, measurements, nutrition logs.
- JSON export: full structured data dump.
- Local backup file: encrypted archive saved to user-selected location.
- Local restore: user selects a backup file to restore from.
- Health Connect integration: optional read/write of weight, body fat, and workout data.

**Alternatives considered**:
- Android Auto Backup (allowBackup=true): rejected — auto-uploads to Google Drive, conflicting with the local-first privacy model.
- No backup at all: rejected — users need data portability.

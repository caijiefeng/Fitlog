# FitLog Roadmap

## V0 — Project Skeleton ✅

**Status**: Implementation complete

**Deliverables**:
- Gradle build with Version Catalog
- Hilt DI (DataStore only; Room deferred to V1)
- Compose + Material 3 forced dark theme
- 5-tab bottom navigation with placeholder screens
- Reusable design system components (TopAppBar, Card, EmptyState, PageContainer, SectionHeader)
- Feature-package architecture
- Domain model data classes (complete future model)
- Architecture documentation

**Tests**: ViewModel unit test, Compose UI test, Navigation test, DataStore repository test

---

## V1 — Exercise Library & Training Templates ✅

**Status**: Complete (V1.1 validated)

**Deliverables**:
- Room entities: ExerciseCategory, Exercise, WorkoutTemplate, WorkoutTemplateExercise, WorkoutSchedule
- Room TypeConverters for LocalDate, Instant
- 45 built-in exercises across 11 muscle groups (idempotent seed)
- Custom exercise CRUD (create, edit, soft-delete)
- Exercise search by name and filter by muscle group
- WorkoutTemplate CRUD with exercise configuration (sets, rep range, weight, RPE, RIR, rest)
- Assign templates to days of week (WorkoutSchedule)
- Plan tab: weekly schedule display, links to templates and exercise library
- Today tab: shows today's scheduled template and exercise count

**Tests**: 4 DAO instrumentation tests (not run — no device), 3 Repository unit tests, 3 ViewModel unit tests, 33 total passing unit tests

**Known gaps**: connectedDebugAndroidTest not executed (no device/emulator). 2 ExerciseEditViewModel save-path tests deferred to instrumentation (viewModelScope coroutine timing).

---

## V2 — Workout Execution & Set Logging ✅

**Status**: Complete (V2.2 validated)

**Deliverables**:
- DB Migration 1→2: WorkoutSession, ExerciseSession, SetRecord with snapshots
- Atomic workout creation with single IN_PROGRESS constraint
- Full-screen execution UI with interactive set logging
- Set types: WARMUP/WORKING/DROP/FAILURE with validation
- Persistent rest timer with +15/-15/skip/recovery
- Workout completion state machine (COMPLETED/PARTIALLY_COMPLETED/CANCELLED)
- Workout summary and detail screens (snapshot-based history)
- Exercise picker for quick workouts
- Record tab: history list with navigation to detail
- Bottom nav hidden for non-tab routes

**Tests**: 33+ unit tests passing. connectedDebugAndroidTest not executed (no device).

---

## V3 — Calendar, Scheduling & Reminders ✅

**Status**: Complete

**Deliverables**:
- Calendar view with workout indicators
- Workout scheduling: reschedule, postpone, skip
- Workout notifications and reminders with timezone support
- Reminder entity (timeOfDay, zoneId, daysOfWeek bitmask)
- CheckIn entity (daily mood, energy, weight)
- Workout streak and completion tracking
- Notification permissions on Android 13+

**Tests**: Calendar queries, reminder scheduling, notification trigger logic

---

## V4 — User Profile, Body Measurements & Trends ✅

**Status**: Complete

**Deliverables**:
- Room entities: UserProfile, BodyMeasurement
- Onboarding (birthDate, height, gender)
- Body measurement entry form
- Measurement history list
- Weight trend chart
- Body measurement trend charts (waist)
- Profile tab populated with real data
- Current weight/BF% derived from latest BodyMeasurement
- Goal planning with timeline estimation

**Tests**: Measurement entry validation, trend calculations

---

## V5 — Nutrition, Media & Data Portability ✅

**Status**: Implementation complete, device validation pending

### V5 Base (Complete)
- **Nutrition tracking**: FoodRecord entity, meal logging (breakfast/lunch/dinner/snack), daily calorie/macro totals — Complete
- **TDEE calculator**: Mifflin-St Jeor BMR, activity-level TDEE, goal-adjusted targets — Complete

### V5.2 (Complete)
- **CameraX integration**: photo and video capture with exposure, focus, zoom controls — **UNVERIFIED_DEVICE** (requires physical camera)
- **Media management**: MediaRecord entity, app media storage, gallery, comparison view — **UNVERIFIED_DEVICE** (requires external storage)
- **Data export**: CSV export for workouts, body measurements, nutrition, check-ins (SAF, UTF-8 BOM, RFC 4180) — **UNVERIFIED_DEVICE** (requires SAF flow on device)
- **Backup/Restore**: Full ZIP backup with manifest.json (version, SHA-256 checksum, row counts), db.json, media files. Pre-import backup, replace-strategy import with rollback — **UNVERIFIED_DEVICE** (requires file system I/O on device)

### V5.3 — Device Validation (Current)
- **Purpose**: Manual device test pass covering all features listed in [MANUAL_DEVICE_TEST.md](MANUAL_DEVICE_TEST.md)
- Installation (fresh + upgrade migration)
- 5-tab bottom navigation and deep navigation
- Exercise library, templates, weekly schedule (Plan tab)
- Workout execution, set logging, rest timer
- History, calendar, reschedule/postpone/skip
- Daily check-in CRUD
- Body measurement entry and trend charts
- Nutrition meal logging and macro tracking
- Reminder CRUD and notification delivery
- Camera permissions, photo capture, video recording
- Media gallery and progress photo comparison
- CSV export via SAF
- Backup and restore via SAF
- Settings, profile editing, media cleanup

### Remaining Items
- Food database (common foods + custom, barcode scanning)
- Per-meal calorie/macro budgets (MealNutritionTarget)
- Meal nutrition target entity (DailyNutritionTarget → per-meal budgets)
- Health Connect integration (read/write weight, body fat, workouts)
- Export selected media to system gallery
- PersonalRecord tracking (1RM, max volume, max reps)

**Tests**: 55+ unit tests passing. CSV escape verification (Chinese, commas, quotes, newlines, nulls, ISO dates), manifest JSON round-trip, import validation (version, checksum, corrupt archive, missing files). Device validation tracked in [MANUAL_DEVICE_TEST.md](MANUAL_DEVICE_TEST.md).

---

## V6 — Smart Suggestions

**Goal**: Data-driven training and nutrition insights.

**Scope**:
- Progressive overload suggestions based on history
- Exercise substitution recommendations
- Nutrition adjustments based on weight trend vs. target
- Training volume warnings (prevent overtraining)
- SuggestionLog entity for tracking accepted/declined suggestions

**Tests**: Suggestion logic, trend analysis accuracy

---

## V7 — Health Connect & Integrations

**Goal**: Cross-app data sharing via Health Connect.

**Scope**:
- Health Connect read: weight, body fat, workouts
- Health Connect write: weight, workouts (optional)
- HealthConnectSyncLog entity

**Tests**: Health Connect integration tests, sync verification

---

## V8 — Polish & Release

**Goal**: Production-ready polish and distribution.

**Scope**:
- Home screen widget (today's workout)
- Wear OS companion app
- Exercise demonstration GIFs/videos
- Training program templates (community)
- Advanced periodization support (mesocycles)
- Accessibility audit
- Performance profiling
- Play Store listing preparation

---

## Future (Beyond V8)

- AI-powered training recommendations
- Social features / sharing (optional opt-in)
- Multi-device sync (encrypted)
- Advanced analytics dashboard

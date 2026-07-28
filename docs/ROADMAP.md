# FitLog Roadmap

## V0 — Project Skeleton (Current)

**Goal**: Build the engineering skeleton: build system, design system, navigation, architecture boundaries, tests, and docs.

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

## V2 — Workout Execution & Set Logging

**Goal**: Users can execute workouts, log sets, use a rest timer, and review history.

**Scope**:
- Room entities: WorkoutSession, ExerciseSession, SetRecord, SetType, WorkoutStatus
- Workout execution UI (full-screen, bottom nav hidden)
- Set-by-set logging: reps, weight, RPE, RIR, set type (warmup/working/drop/failure)
- Rest timer between sets
- Workout completion flow → session saved
- In-progress workout survives process death
- History list on Record tab
- Session detail view (per-set data)

**Tests**: Session execution ViewModel, rest timer logic, SetRecordDao

---

## V3 — Calendar, Scheduling & Reminders

**Goal**: Users can see workouts on a calendar, receive reminders, reschedule, skip, and check in.

**Scope**:
- Calendar view with workout indicators
- Workout scheduling: reschedule, postpone, skip
- Workout notifications and reminders with timezone support
- Reminder entity (timeOfDay, zoneId, daysOfWeek bitmask)
- CheckIn entity (daily mood, energy, weight)
- Workout streak and completion tracking
- Notification permissions on Android 13+

**Tests**: Calendar queries, reminder scheduling, notification trigger logic

---

## V4 — User Profile, Body Measurements & Trends

**Goal**: User profile setup, body measurement tracking, and trend statistics.

**Scope**:
- Room entities: UserProfile, BodyMeasurement, PersonalRecord
- Onboarding (birthDate, height, gender)
- Body measurement entry form
- Measurement history list
- Weight trend chart
- Body measurement trend charts (waist, hips, arms, thighs)
- PersonalRecord tracking (1RM, max volume, max reps)
- Profile tab populated with real data
- Current weight/BF% derived from latest BodyMeasurement

**Tests**: Measurement entry validation, trend calculations, PR detection

---

## V5 — Nutrition: TDEE, Targets & Meal Logging

**Goal**: Meal logging with macro tracking, TDEE calculation, and per-meal budgets.

**Scope**:
- Room entities: Food, Meal, MealEntry, DailyNutritionTarget
- TDEE calculator (Mifflin-St Jeor or similar)
- Daily calorie/macro targets
- Target body fat percentage goal
- Per-meal calorie and macro budgets
- Food library (common foods + custom)
- Meal logging UI
- Daily nutrition summary on Today tab
- Nutrition history on Progress tab

**Tests**: TDEE calculation accuracy, macro aggregation, meal budget allocation

---

## V6 — CameraX Photo & Video

**Goal**: Progress photos and video with camera controls.

**Scope**:
- Room entity: MediaRecord
- CameraX integration: photo and video capture
- Exposure compensation control
- Auto-focus and tap-to-focus
- Pinch-to-zoom
- Flash toggle
- Photo/video saved to app-internal directory
- Media gallery (browse, view full-screen)
- Export selected media to system gallery
- Media linked to sessions or measurements

**Tests**: Camera integration tests, media storage paths, export verification

---

## V7 — Health Connect, Export & Backup

**Goal**: Cross-app data sharing via Health Connect, data portability.

**Scope**:
- Health Connect read: weight, body fat, workouts
- Health Connect write: weight, workouts (optional)
- CSV export: workouts, measurements, nutrition
- JSON export: full data dump
- App backup/restore (local file)

**Tests**: Health Connect integration tests, export/import round-trip

---

## V8 — Smart Suggestions

**Goal**: Data-driven training and nutrition insights.

**Scope**:
- Progressive overload suggestions based on history
- Exercise substitution recommendations
- Nutrition adjustments based on weight trend vs. target
- Training volume warnings (prevent overtraining)
- SuggestionLog entity for tracking accepted/declined suggestions

**Tests**: Suggestion logic, trend analysis accuracy

---

## Future (Beyond V8)

- Home screen widget (today's workout)
- Wear OS companion app
- Exercise demonstration GIFs/videos
- Training program templates (community)
- Advanced periodization support (mesocycles)

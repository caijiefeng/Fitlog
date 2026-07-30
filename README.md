# FitLog

A local-first, privacy-focused strength training tracker for Android.

## Status

**V5.2 (in progress)** — Building on the V5 base with CameraX, media management, data export, and backup/restore. The V5 base (exercise library, templates, workout execution, nutrition tracking, etc.) is complete. See [ROADMAP.md](docs/ROADMAP.md) for the full version plan.

## Tech Stack

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose + Material 3 (forced dark theme)
- **Architecture**: MVVM with UDF, feature-package layout
- **DI**: Hilt 2.53.1 + KSP
- **Navigation**: Navigation Compose (string-based routes)
- **Storage**: Room 2.6.1 (database version 7), DataStore Preferences
- **Build**: Gradle 8.11.1 + AGP 8.7.3 + Version Catalog
- **Testing**: JUnit 4, MockK, Turbine, Compose Testing, Hilt Testing

## Requirements

- Android Studio 2024.2+
- JDK 17+
- Android SDK 35
- minSdk 28 / targetSdk 35

## Getting Started

1. Clone the repository.
2. Open in Android Studio: **File → Open → (this directory)**.
3. Wait for Gradle sync to complete.
4. Select **Run → Run 'app'** on an emulator or device.

## Gradle Verification

```bash
# Clean build
./gradlew clean

# Unit tests (50+ tests)
./gradlew test

# Lint
./gradlew lint

# Debug APK
./gradlew assembleDebug

# Connected tests (requires emulator/device)
./gradlew connectedDebugAndroidTest
```

## Current Features

- 5-tab bottom navigation: 今日, 计划, 记录, 进度, 我的
- Forced dark theme (black + white + single accent)
- Reusable design system components
- **Exercise library**: 45 built-in exercises across 11 muscle groups, search, filter, custom exercises
- **Training templates**: create, edit, add exercises with target sets/reps/weight/RPE/RIR/rest
- **Weekly scheduling**: assign templates to days (Mon-Sun), view on Plan tab
- **Today**: shows scheduled workout template, exercise count, quick start, daily check-in
- **Workout execution**: full-screen UI with set logging, set types (warmup/working/drop/failure), rest timer
- **Workout history**: summary, detail, progress charts, streak tracking
- **Calendar**: workout indicators, reschedule, postpone, skip
- **Reminders**: notification scheduling with day-of-week selection
- **Check-ins**: daily mood, energy level, notes
- **User profile**: gender, birthday, height, activity level, goal type
- **Body measurements**: weight, body fat, muscle, waist tracking with trends
- **Nutrition**: meal logging, calorie/macro tracking, TDEE-based targets
- **Progress photos**: camera capture, gallery, comparison view *(V5.2 in progress)*
- **Trend charts**: weight, body fat, waist, calories, protein, training volume
- **Goal planning**: target body fat, weight change, timeline estimation
- **Data export**: CSV export for workouts, body measurements, nutrition, check-ins via SAF *(V5.2 in progress)*
- **Backup/Restore**: full ZIP backup with manifest and SHA-256 verification *(V5.2 in progress)*

## Not Yet Implemented

- Health Connect integration (V7)
- Smart training suggestions (V8)
- Home screen widget
- Wear OS companion app

## Privacy

FitLog is local-first. All data is stored on-device. No analytics, no cloud sync, no network requests. `android:allowBackup` is disabled — user data is only exported through explicit user-initiated actions. See [PRIVACY.md](docs/PRIVACY.md) for details.

## Project Structure

```
app/src/main/java/com/example/fitlog/
├── core/
│   ├── common/          # Result, DateTimeUtils
│   ├── database/        # Room DB (version 7, 14 DAOs, 15 entities)
│   ├── datastore/       # UserPreferences
│   ├── designsystem/    # Theme + reusable components
│   ├── di/              # Hilt modules
│   ├── media/           # Media storage, cleanup manager
│   ├── model/           # Domain data classes
│   ├── navigation/      # NavHost, BottomBar
│   └── time/            # AppClock, timezone utilities
├── data/
│   ├── backup/          # BackupManager, BackupImporter, BackupManifest
│   ├── export/          # CsvExporter
│   └── repository/      # 13 repositories
├── feature/             # 15 feature packages
├── domain/              # Use cases, domain logic (TDEE, goals, streaks)
└── di/                  # App-level Hilt modules
```

## Known Device Test Gaps

- `connectedDebugAndroidTest` not executed — no device/emulator in CI
- `ExerciseEditViewModel` save-path tests deferred to instrumentation (viewModelScope coroutine timing in Robolectric)
- CameraX integration tests require a physical device
- Media file path resolution tests require external storage
- Backup/restore round-trip tests require file system I/O
- Health Connect integration requires Android 14+ with Health Connect app

## Notes for Contributors

- Do **not** commit `local.properties`, `.gradle/`, `.kotlin/`, `app/build/`, or `*.apk`.
- These paths are covered by `.gitignore`.
- See `docs/DECISIONS.md` for architecture decisions.
- See `docs/DATA_MODEL.md` for the database schema.

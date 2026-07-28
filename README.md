# FitLog

A local-first, privacy-focused strength training tracker for Android.

## Status

**V1** — Exercise library, training templates, and weekly scheduling. See [ROADMAP.md](docs/ROADMAP.md) for the full version plan. V2: Workout execution and set logging.

## Tech Stack

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose + Material 3 (forced dark theme)
- **Architecture**: MVVM with UDF, feature-package layout
- **DI**: Hilt 2.53.1 + KSP
- **Navigation**: Navigation Compose (string-based routes)
- **Storage**: Room 2.6.1 (active), DataStore Preferences
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

# Unit tests (4 tests)
./gradlew test

# Lint
./gradlew lint

# Debug APK
./gradlew assembleDebug

# Connected tests (requires emulator/device)
./gradlew connectedDebugAndroidTest
```

## Current Features (V1)

- 5-tab bottom navigation: 今日, 计划, 记录, 进度, 我的
- Forced dark theme (black + white + single accent)
- Reusable design system components
- **Exercise library**: 45 built-in exercises across 11 muscle groups, search, filter, custom exercises
- **Training templates**: create, edit, add exercises with target sets/reps/weight/RPE/RIR/rest
- **Weekly scheduling**: assign templates to days (Mon-Sun), view on Plan tab
- **Today**: shows scheduled workout template and exercise count

## Not Yet Implemented

- Workout execution and set logging (V2)
- Calendar view, reminders, notifications (V3)
- Calendar, scheduling, and reminders (V3)
- User profile and body measurements (V4)
- Nutrition tracking (V5)
- CameraX photo/video (V6)
- Health Connect, export, backup (V7)

## Privacy

FitLog is local-first. All data is stored on-device. No analytics, no cloud sync, no network requests. `android:allowBackup` is disabled — user data is only exported through explicit user-initiated actions (planned V7).

## Project Structure

```
app/src/main/java/com/example/fitlog/
├── core/
│   ├── common/          # Result, DateTimeUtils
│   ├── database/        # Room DB (deferred to V1)
│   ├── datastore/       # UserPreferences
│   ├── designsystem/    # Theme + reusable components
│   ├── di/              # Hilt modules
│   ├── model/           # Domain data classes
│   └── navigation/      # NavHost, BottomBar
├── feature/             # 5 tab screens
│   ├── today/
│   ├── plan/
│   ├── record/
│   ├── progress/
│   └── profile/
├── domain/              # UseCases
└── di/                  # App-level Hilt module
```

## Notes for Contributors

- Do **not** commit `local.properties`, `.gradle/`, `.kotlin/`, `app/build/`, or `*.apk`.
- These paths are covered by `.gitignore`.
- See `docs/DECISIONS.md` for architecture decisions.
- See `docs/DATA_MODEL.md` for the future database schema.

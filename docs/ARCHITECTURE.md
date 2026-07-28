# FitLog Architecture

## Overview

FitLog follows **unidirectional data flow (UDF)** with a **feature-package** organization. The architecture enforces strict layer boundaries:

```
Compose UI → ViewModel → UseCase/Domain Service → Repository → DataSource
```

## Layer Responsibilities

### UI Layer (`feature.*`, `core.designsystem`)
- **Composable functions** render state from ViewModels.
- **No business logic** — composables only handle rendering and user event forwarding.
- Shared UI components in `core.designsystem.component`.

### ViewModel Layer (`feature.*`)
- **Holds UI state** via `StateFlow<XxxUiState>`.
- **Orchestrates UseCases** — never directly accesses DAOs, DataStore, or files.
- **Survives configuration changes** via `@HiltViewModel`.
- One ViewModel per screen.

### Domain Layer (`domain.*`)
- **UseCases** contain single-responsibility business operations.
- Optional for simple CRUD — required when logic spans multiple repositories.
- Pure Kotlin — no Android dependencies.

### Data Layer (`core.database`, `core.datastore`)
- **Repositories** abstract data source access.
- **Room** for structured relational data (workouts, exercises, logs).
- **DataStore** for user preferences/settings.
- Repositories return domain models, not Room entities.

## Package Structure

```
com.example.fitlog
├── FitLogApplication.kt          # @HiltAndroidApp
├── MainActivity.kt               # Single Activity, @AndroidEntryPoint
├── FitLogApp.kt                  # Root composable: theme + scaffold + nav
│
├── core/
│   ├── database/
│   │   └── FitLogDatabase.kt     # Room DB (empty in V0)
│   ├── datastore/
│   │   └── UserPreferencesRepository.kt
│   ├── designsystem/
│   │   ├── theme/                # Color, Type, Theme
│   │   └── component/            # Reusable composables
│   ├── di/                       # Hilt modules (DB, DataStore)
│   ├── model/                    # Domain data classes
│   ├── navigation/               # NavHost, BottomBar, NavItems
│   └── common/                   # Result, DateTimeUtils
│
├── feature/
│   ├── today/                    # Today tab
│   ├── plan/                     # Training plan tab
│   ├── record/                   # Record tab
│   ├── progress/                 # Progress/Stats tab
│   └── profile/                  # Profile/Settings tab
│
└── domain/
    └── example/                  # Example UseCase (template)
```

## Dependency Injection

**Hilt** manages the object graph:

| Scope | Components |
|---|---|
| `@Singleton` | `FitLogDatabase`, `DataStore<Preferences>`, Repositories, UseCases |
| `@HiltViewModel` | ViewModels |
| `@ActivityScoped` | (reserved for future) |

## Navigation

- **Single Activity** with Jetpack Compose Navigation.
- **Bottom Navigation Bar** with 5 tabs: 今日, 计划, 记录, 进度, 我的.
- String-based routes defined in `BottomNavItem` enum.
- `NavHost` with `composable()` routes, one per tab.

## Data Flow

```
User taps "Start Workout"
    → TodayScreen calls viewModel.onStartWorkout()
    → TodayViewModel invokes StartWorkoutUseCase
    → UseCase reads WorkoutPlan from WorkoutPlanRepository
    → Repository queries Room DAO
    → Returns domain model WorkoutSession
    → ViewModel updates StateFlow<TodayUiState>
    → TodayScreen recomposes with workout session data
```

## State Management

- **UI State**: `data class XxxUiState` with all data the screen needs.
- **Events**: One-shot events via `Channel` or `SharedFlow` (snackbar, navigation).
- **Side Effects**: `LaunchedEffect` in composables for one-time triggers.

## Testing Strategy

| Test Type | Location | Framework |
|---|---|---|
| Unit (ViewModel) | `app/src/test/` | JUnit4 + MockK + Turbine |
| Unit (Repository) | `app/src/test/` | JUnit4 + Robolectric |
| UI (Compose) | `app/src/androidTest/` | Compose Testing |
| Navigation | `app/src/androidTest/` | Compose Testing + Nav |

## Conventions

- File naming: `FeatureNameScreen.kt`, `FeatureNameViewModel.kt`
- Composable naming: PascalCase
- ViewModel naming: `FeatureNameViewModel`
- Resource naming: `snake_case`
- Strings: Chinese (primary), English keys

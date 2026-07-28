# FitLog Data Model

## Overview

This document defines the **future complete data model**. Only domain data classes exist in V0 Kotlin source. Room entities will be created incrementally from V1.

## Timestamp Strategy

| Purpose | Java Type | Room Storage | Rationale |
|---|---|---|---|
| Calendar day (training date, measurement date, schedule day) | `LocalDate` | `Long` (epoch day) | Date-only, no ambiguity |
| Precise point in time (record creation, updates, session start/end) | `Instant` | `Long` (epoch millis) | Monotonic, timezone-independent |
| User timezone | `ZoneId` (stored as `String` id) | `String` | Required for reminders and schedule interpretation |
| Reminder time-of-day | `String` (`HH:mm`) | `String` | Local wall-clock time, interpreted in user's zone |

**Do NOT** convert all `LocalDateTime` unconditionally to epoch seconds. Each field must select the appropriate type based on its semantic purpose.

## Entity-Relationship Diagram

```
ExerciseCategory ──< Exercise
                       │
MuscleGroup ───────────┘
                       │
WorkoutTemplate ──< WorkoutTemplateExercise >── Exercise
     │
WorkoutSchedule ── FK → WorkoutTemplate
     │
     ▼
WorkoutSession ──< ExerciseSession >── Exercise
     │                    │
     │                    ▼
     │               SetRecord
     ▼
CheckIn
     │
     ▼
BodyMeasurement
     │
     ▼
MediaRecord

Food ──< MealEntry >── Meal ──> DailyNutritionTarget

Reminder ── FK → WorkoutSchedule

PersonalRecord ── FK → Exercise, WorkoutSession
```

## Domain Models

### MuscleGroup (Enum)

Standard muscle groups for built-in exercise classification.

```
CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, CORE, CARDIO, FULL_BODY
```

### ExerciseCategory

User-defined categories, separate from anatomical muscle groups.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| name | String | e.g. "康复训练", "CrossFit" |
| description | String? | Optional description |
| sortOrder | Int | Display ordering |
| createdAt | Instant | Creation timestamp |

### Exercise

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| name | String | Exercise name |
| primaryMuscleGroup | MuscleGroup | Primary target |
| secondaryMuscleGroup | MuscleGroup? | Secondary target |
| categoryId | Long? | FK → ExerciseCategory (user-defined) |
| notes | String? | Form cues, setup notes |
| isCustom | Boolean | True if user-created |
| isActive | Boolean | Soft-delete flag |
| sortOrder | Int | Display ordering |
| createdAt | Instant | |
| updatedAt | Instant | |

### WorkoutTemplate

A reusable training blueprint (e.g. "Push Day A").

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| name | String | e.g. "Push Day A" |
| notes | String? | Template notes |
| sortOrder | Int | Display ordering |
| isActive | Boolean | Soft-delete |
| createdAt | Instant | |
| updatedAt | Instant | |

### WorkoutTemplateExercise

Links a template to its exercises with target parameters.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| templateId | Long | FK → WorkoutTemplate |
| exerciseId | Long | FK → Exercise |
| targetSets | Int | Default 3 |
| targetRepsMin | Int? | Lower end of rep range |
| targetRepsMax | Int? | Upper end of rep range |
| targetWeightKg | Double? | Target weight |
| targetRpe | Double? | Target RPE (1–10) |
| targetRir | Int? | Target Reps in Reserve (0–5) |
| restSeconds | Int | Rest between sets, default 90 |
| notes | String? | Per-exercise notes |
| sortOrder | Int | Exercise order within template |

### WorkoutSchedule

Assigns a template to a day of the week.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| templateId | Long | FK → WorkoutTemplate |
| dayOfWeek | Int | 1=Mon … 7=Sun |
| isActive | Boolean | |
| createdAt | Instant | |

### WorkoutStatus (Enum)

```
PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
```

### WorkoutSession

A single training session instance.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| scheduleId | Long? | FK → WorkoutSchedule (null = ad-hoc) |
| templateId | Long? | FK → WorkoutTemplate (denormalized for quick lookup) |
| date | LocalDate | Calendar date |
| startTime | Instant? | Actual start |
| endTime | Instant? | Actual end |
| durationSeconds | Int? | Computed duration |
| status | WorkoutStatus | PLANNED / IN_PROGRESS / COMPLETED / CANCELLED |
| notes | String? | Session notes |
| createdAt | Instant | |
| updatedAt | Instant | |

### ExerciseSession

Groups set records for one exercise within a workout session.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| sessionId | Long | FK → WorkoutSession |
| exerciseId | Long | FK → Exercise |
| notes | String? | Per-exercise notes |
| sortOrder | Int | Exercise order within session |

### SetType (Enum)

```
WARMUP, WORKING, DROP, FAILURE
```

### SetRecord

A single set of an exercise.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| exerciseSessionId | Long | FK → ExerciseSession |
| setNumber | Int | 1-indexed |
| setType | SetType | WARMUP / WORKING / DROP / FAILURE |
| reps | Int? | Reps performed |
| weightKg | Double? | Weight lifted |
| rpe | Double? | Rate of Perceived Exertion (1–10) |
| rir | Int? | Reps in Reserve (0–5) |
| restSeconds | Int? | Actual rest taken |
| completed | Boolean | Whether set was completed |
| notes | String? | Per-set notes |

### UserProfile

Personal profile. Current weight and body fat are derived from the latest `BodyMeasurement`, not stored independently.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| name | String | Display name |
| birthDate | LocalDate? | Date of birth (age is derived) |
| heightCm | Double? | Height in cm |
| gender | Gender | MALE / FEMALE / OTHER / UNSPECIFIED |
| createdAt | Instant | |
| updatedAt | Instant | |

### BodyMeasurement

Body measurement log entry. The latest entry provides current weight and BF%.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| date | LocalDate | Measurement date |
| weightKg | Double? | Body weight |
| bodyFatPercent | Double? | Body fat percentage |
| chestCm | Double? | Chest |
| waistCm | Double? | Waist |
| hipsCm | Double? | Hips |
| leftArmCm | Double? | Left arm |
| rightArmCm | Double? | Right arm |
| leftThighCm | Double? | Left thigh |
| rightThighCm | Double? | Right thigh |
| notes | String? | |
| createdAt | Instant | |

### Food

Food item definition for nutrition logging.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| name | String | Food name |
| brand | String? | Brand |
| servingSize | String? | e.g. "100g", "1 cup" |
| caloriesPerServing | Double? | kcal per serving |
| proteinGramsPerServing | Double? | |
| carbsGramsPerServing | Double? | |
| fatGramsPerServing | Double? | |
| isCustom | Boolean | User-created food |
| createdAt | Instant | |

### Meal

A meal grouping for a date.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| date | LocalDate | Meal date |
| mealType | MealType | BREAKFAST / LUNCH / DINNER / SNACK / OTHER |
| notes | String? | |
| createdAt | Instant | |

### MealEntry

Links a food item to a meal with quantity.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| mealId | Long | FK → Meal |
| foodId | Long | FK → Food |
| servings | Double | Number of servings |
| calories | Double? | Computed from food |
| proteinGrams | Double? | |
| carbsGrams | Double? | |
| fatGrams | Double? | |

### DailyNutritionTarget

Daily and per-meal nutrition targets.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| date | LocalDate | Target date |
| tdee | Double? | Total Daily Energy Expenditure |
| targetCalories | Double? | Daily calorie target |
| targetProteinG | Double? | Daily protein target (g) |
| targetCarbsG | Double? | Daily carb target (g) |
| targetFatG | Double? | Daily fat target (g) |
| targetBodyFatPercent | Double? | Target body fat percentage |
| mealBudgetCalories | Double? | Per-meal calorie budget |
| mealBudgetProteinG | Double? | Per-meal protein budget |
| mealBudgetCarbsG | Double? | Per-meal carb budget |
| mealBudgetFatG | Double? | Per-meal fat budget |

### Reminder

Workout reminder with timezone support.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| scheduleId | Long? | FK → WorkoutSchedule |
| label | String | Reminder label |
| timeOfDay | String | "HH:mm" in user's local zone |
| daysOfWeek | Int? | Bitmask (null = all scheduled days) |
| zoneId | String | Timezone ID (e.g. "Asia/Shanghai") |
| isEnabled | Boolean | |
| createdAt | Instant | |

### CheckIn

Daily check-in record (mood, energy, weight).

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| date | LocalDate | Check-in date |
| sessionId | Long? | FK → WorkoutSession (if linked to workout) |
| mood | Int? | 1–5 |
| energyLevel | Int? | 1–5 |
| bodyWeightKg | Double? | Quick morning weight |
| notes | String? | |
| createdAt | Instant | |

### MediaRecord

Photo or video record linked to a session or measurement.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| date | LocalDate | Media date |
| sessionId | Long? | FK → WorkoutSession |
| measurementId | Long? | FK → BodyMeasurement |
| filePath | String | App-internal file path |
| mediaType | MediaType | PHOTO / VIDEO |
| notes | String? | |
| createdAt | Instant | |

### PersonalRecord

Tracked personal bests per exercise.

| Field | Type | Description |
|---|---|---|
| id | Long | Primary key |
| exerciseId | Long | FK → Exercise |
| recordType | PersonalRecordType | ONE_REP_MAX / MAX_VOLUME / MAX_REPS / MAX_WEIGHT |
| value | Double | Record value |
| achievedDate | LocalDate | Date achieved |
| sessionId | Long? | FK → WorkoutSession |
| notes | String? | |
| createdAt | Instant | |

## Room Relational Schema (Future)

All relationships use normalized foreign keys — **no JSON strings** for exercise lists.

- `WorkoutTemplate` 1→N `WorkoutTemplateExercise` N→1 `Exercise`
- `WorkoutSchedule` N→1 `WorkoutTemplate`
- `WorkoutSession` N→1 `WorkoutSchedule`, N→1 `WorkoutTemplate`
- `WorkoutSession` 1→N `ExerciseSession` N→1 `Exercise`
- `ExerciseSession` 1→N `SetRecord`
- `Meal` 1→N `MealEntry` N→1 `Food`

## Room Entity Roadmap

| Version | Entities Added |
|---|---|
| V0 | None (Room not instantiated) |
| V1 | MuscleGroup, ExerciseCategory, Exercise, WorkoutTemplate, WorkoutTemplateExercise, WorkoutSchedule |
| V2 | WorkoutSession, ExerciseSession, SetRecord, WorkoutStatus, SetType |
| V3 | WorkoutSchedule expanded with calendar, Reminder, CheckIn |
| V4 | UserProfile, BodyMeasurement, PersonalRecord |
| V5 | Food, Meal, MealEntry, DailyNutritionTarget |
| V6 | MediaRecord |
| V7 | HealthConnectSyncLog |
| V8 | SuggestionLog |

## Index Design (Future)

- `WorkoutSession(date)` — calendar queries
- `SetRecord(exerciseSessionId)` — exercise history
- `BodyMeasurement(date)` — trend charts
- `Meal(date)` — daily nutrition summaries
- `PersonalRecord(exerciseId, recordType)` — PR lookups

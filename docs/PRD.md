# FitLog Product Requirements Document

## Product Overview

**FitLog** is a local-first, privacy-focused fitness tracking app designed for strength training enthusiasts. It runs entirely on-device with no cloud dependency, helping users plan, log, and analyze their workouts without an internet connection.

## Product Goals

1. Provide a clean, distraction-free interface for logging strength workouts.
2. Enable users to create and follow weekly training plans.
3. Track body measurements and visualize training progress over time.
4. Support nutrition logging with macro tracking.
5. Integrate with Health Connect for cross-app data sharing (future).
6. Respect user privacy — all data stays on device by default.

## Target Users

- **Primary**: Intermediate strength training practitioners (barbell/dumbbell/machine-based training).
- **Secondary**: Beginners who want structured guidance.
- **Tertiary**: Advanced lifters who need detailed RPE/RIR tracking.

## User Personas

### Xiao Wang, 25 — The Structured Lifter
- Follows a PPL (Push/Pull/Legs) split 5 days/week.
- Wants to track progressive overload across weeks.
- Needs rest timers between sets.
- Currently uses a notes app and wants something purpose-built.

### Li Mei, 30 — The Health Tracker
- Trains 3 days/week with a mix of strength and cardio.
- Also tracks body weight and measurements.
- Wants to see correlation between training consistency and body changes.

## Core User Flows

### Flow 1: Plan and Execute a Workout
1. User opens app → lands on Today tab.
2. Today shows scheduled workout (if any) or quick-start option.
3. User starts workout → sees exercise list with target sets/reps/weight.
4. User logs each set: weight, reps, RPE, RIR.
5. Rest timer counts down between sets.
6. User completes workout → session saved to history.

### Flow 2: Track Body Changes
1. User navigates to Record tab.
2. Enters weight, body fat %, circumference measurements.
3. Progress tab shows charts of changes over time.

### Flow 3: Nutrition Tracking
1. User logs meals with food name, portion, macros.
2. App calculates daily totals vs. TDEE-based targets.
3. Nutrient summaries display on Today tab.

## Feature Priority

### P0 (V1-V2) — Core Training MVP
- Exercise library (built-in + custom)
- Weekly workout plan creation
- Training session execution and logging
- Exercise history

### P1 (V3-V4) — Body & Progress
- Body measurement logging
- Progress charts and statistics
- Workout streaks and achievements

### P2 (V5) — Nutrition
- Meal logging
- Macro tracking
- Daily calorie targets (TDEE-based)

### P3 (V6+) — Media & Integration
- CameraX photo/video capture
- Progress photo gallery
- Health Connect integration
- Data export/import

## Out of Scope

- Cloud sync / backend server
- Social features / sharing
- AI coaching
- Subscription / payment
- Login / registration
- Multi-device sync

## Success Metrics

- Training session completion rate
- Weekly active users (device-local)
- Average sessions per week per user
- Body measurement logging frequency

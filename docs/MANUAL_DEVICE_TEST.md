# FitLog Manual Device Test Checklist

**Version**: V5.3 (Commit 4)
**Scope**: Full-application device validation
**Device Requirements**: Android 9+ (minSdk 28) physical device with camera and external storage

All items below are marked `[UNVERIFIED_DEVICE]` until tested and confirmed by a human tester on physical hardware.

---

## V6.0 Immersive Star Experience

### Today
- [UNVERIFIED_DEVICE] Verify the edge-to-edge star Hero, status-bar readability, long user names, and fallback treatment when no approved background image exists.
- [UNVERIFIED_DEVICE] Verify start, resume, schedule, free-workout, nutrition, body-measurement, and camera actions remain tappable.

### Profile
- [UNVERIFIED_DEVICE] Verify avatar updates immediately, grouped settings remain readable in light/dark themes, and theme mode selection works.

### Progress
- [UNVERIFIED_DEVICE] Verify the 90-day heatmap correctly marks completed and partially completed sessions, including no-data and high-volume histories.

### Nutrition
- [UNVERIFIED_DEVICE] Verify calorie ring, no-target state, macro values, meal grouping, long food names, add, edit, and delete actions.

### Calendar and navigation
- [UNVERIFIED_DEVICE] Verify today/selected/multi-plan calendar markers, cross-month navigation, selected-day actions, and all five floating-dock destinations in light and dark themes.

---

## 1. Installation

### 1.1 Fresh Install
- [UNVERIFIED_DEVICE] Install APK on a device with no prior FitLog installation
- [UNVERIFIED_DEVICE] Verify onboarding screen appears on first launch
- [UNVERIFIED_DEVICE] Complete onboarding: enter birth date, height, gender
- [UNVERIFIED_DEVICE] Verify main screen (Today tab) loads after onboarding
- [UNVERIFIED_DEVICE] Verify no crash on first database initialization (Room schema creation, seed data)
- [UNVERIFIED_DEVICE] Verify `android:allowBackup=false` — no automatic backup on install

### 1.2 Upgrade (Data Migration)
- [UNVERIFIED_DEVICE] Install previous version (V5.2), create sample data (exercises, templates, workouts, measurements, nutrition entries)
- [UNVERIFIED_DEVICE] Install new APK over existing installation
- [UNVERIFIED_DEVICE] Verify Room migration completes without error
- [UNVERIFIED_DEVICE] Verify all prior data is present after migration
- [UNVERIFIED_DEVICE] Verify no duplicate seed data after migration (idempotent seeding)

---

## 2. Navigation

### 2.1 Bottom Navigation Bar
- [UNVERIFIED_DEVICE] Verify 5 tabs are visible and labeled: 今日 (Today), 计划 (Plan), 记录 (Record), 进度 (Progress), 我的 (Profile)
- [UNVERIFIED_DEVICE] Tap each tab and verify the correct screen loads
- [UNVERIFIED_DEVICE] Verify bottom nav bar is hidden in non-tab routes (workout execution, template edit, etc.)
- [UNVERIFIED_DEVICE] Verify back navigation from detail screens returns to the correct tab
- [UNVERIFIED_DEVICE] Verify tab state persists after navigating away and back

### 2.2 Deep Navigation
- [UNVERIFIED_DEVICE] Verify system back button behavior across all screen stacks
- [UNVERIFIED_DEVICE] Verify no screen overlap or composable duplication after rapid tab switching

---

## 3. Exercise Library, Templates & Plan

### 3.1 Exercise Library
- [UNVERIFIED_DEVICE] Open exercise library from Plan tab
- [UNVERIFIED_DEVICE] Verify 45 built-in exercises are present
- [UNVERIFIED_DEVICE] Search exercises by name (Chinese and English)
- [UNVERIFIED_DEVICE] Filter exercises by muscle group
- [UNVERIFIED_DEVICE] Create a custom exercise with name, muscle group, description
- [UNVERIFIED_DEVICE] Edit the custom exercise
- [UNVERIFIED_DEVICE] Soft-delete (hide) a custom exercise
- [UNVERIFIED_DEVICE] Verify built-in exercises cannot be deleted

### 3.2 Training Templates
- [UNVERIFIED_DEVICE] Create a new training template
- [UNVERIFIED_DEVICE] Add exercises to template with target sets, rep range, weight, RPE, RIR, rest duration
- [UNVERIFIED_DEVICE] Reorder exercises within a template
- [UNVERIFIED_DEVICE] Remove an exercise from a template
- [UNVERIFIED_DEVICE] Edit an existing template
- [UNVERIFIED_DEVICE] Delete a template
- [UNVERIFIED_DEVICE] Verify template list reflects CRUD operations

### 3.3 Plan / Weekly Schedule
- [UNVERIFIED_DEVICE] Assign a template to a day of the week
- [UNVERIFIED_DEVICE] View the weekly schedule on the Plan tab
- [UNVERIFIED_DEVICE] Tap a scheduled day and verify it links to the template
- [UNVERIFIED_DEVICE] Remove a template from a scheduled day
- [UNVERIFIED_DEVICE] Verify empty-state displays when no templates are scheduled

---

## 4. Workout Execution & Logging

### 4.1 Start Workout
- [UNVERIFIED_DEVICE] Start a workout from Today tab (scheduled template)
- [UNVERIFIED_DEVICE] Start a quick workout (no template) from Today tab
- [UNVERIFIED_DEVICE] Verify full-screen workout UI loads with exercise list
- [UNVERIFIED_DEVICE] Verify only one workout can be IN_PROGRESS at a time

### 4.2 Set Logging
- [UNVERIFIED_DEVICE] Log a set: enter weight, reps, select set type (WARMUP / WORKING / DROP / FAILURE)
- [UNVERIFIED_DEVICE] Log multiple sets for the same exercise
- [UNVERIFIED_DEVICE] Edit a logged set (weight, reps, set type)
- [UNVERIFIED_DEVICE] Delete a logged set
- [UNVERIFIED_DEVICE] Verify set type validation (no duplicate types where restricted)

### 4.3 Exercise Navigation
- [UNVERIFIED_DEVICE] Navigate to next exercise in workout
- [UNVERIFIED_DEVICE] Navigate to previous exercise
- [UNVERIFIED_DEVICE] Complete all sets for an exercise (verify completion indicator)

### 4.4 Workout Completion
- [UNVERIFIED_DEVICE] Complete a workout: verify COMPLETED status
- [UNVERIFIED_DEVICE] Complete a workout with unlogged exercises: verify PARTIALLY_COMPLETED status
- [UNVERIFIED_DEVICE] Cancel a workout: verify CANCELLED status
- [UNVERIFIED_DEVICE] Verify workout summary screen appears after completion

---

## 5. Rest Timer

- [UNVERIFIED_DEVICE] Verify rest timer starts automatically after logging a set
- [UNVERIFIED_DEVICE] Verify timer counts down correctly to 0
- [UNVERIFIED_DEVICE] Tap +15 seconds and verify timer extends
- [UNVERIFIED_DEVICE] Tap -15 seconds and verify timer shortens
- [UNVERIFIED_DEVICE] Tap Skip to dismiss rest timer
- [UNVERIFIED_DEVICE] Tap Recovery (longer rest preset) and verify timer adjusts
- [UNVERIFIED_DEVICE] Verify timer persists across configuration changes (screen rotation)
- [UNVERIFIED_DEVICE] Verify notification/chime when timer reaches 0

---

## 6. History

- [UNVERIFIED_DEVICE] View workout history list on 记录 (Record) tab
- [UNVERIFIED_DEVICE] Verify each history entry shows date, template name, exercise count
- [UNVERIFIED_DEVICE] Tap a history entry to open workout detail screen
- [UNVERIFIED_DEVICE] Verify detail screen shows all logged sets with weight, reps, set type
- [UNVERIFIED_DEVICE] Verify progress charts in history (volume trend, etc.)
- [UNVERIFIED_DEVICE] Verify streak counter displays correctly
- [UNVERIFIED_DEVICE] Verify empty state when no workout history exists

---

## 7. Calendar View

- [UNVERIFIED_DEVICE] Open calendar view from 记录 (Record) tab
- [UNVERIFIED_DEVICE] Verify workout indicators (dots/markers) on days with workouts
- [UNVERIFIED_DEVICE] Navigate between months (previous/next)
- [UNVERIFIED_DEVICE] Tap a day with a workout and verify it shows workout details or links
- [UNVERIFIED_DEVICE] Verify calendar scrolls correctly and months render without lag

---

## 8. Reschedule / Postpone / Skip

- [UNVERIFIED_DEVICE] Reschedule a workout from one day to another
- [UNVERIFIED_DEVICE] Postpone a scheduled workout by one day
- [UNVERIFIED_DEVICE] Skip a scheduled day (mark as skipped, not a workout)
- [UNVERIFIED_DEVICE] Verify calendar/history reflects the change after reschedule/skip
- [UNVERIFIED_DEVICE] Verify streak calculation adjusts for skipped days

---

## 9. Check-in

- [UNVERIFIED_DEVICE] Verify daily check-in prompt appears on Today tab
- [UNVERIFIED_DEVICE] Enter mood rating (scale)
- [UNVERIFIED_DEVICE] Enter energy level
- [UNVERIFIED_DEVICE] Add notes to check-in
- [UNVERIFIED_DEVICE] Save check-in and verify it persists
- [UNVERIFIED_DEVICE] Edit an existing day's check-in
- [UNVERIFIED_DEVICE] Verify previous check-ins are viewable in history

---

## 10. Body Measurement

- [UNVERIFIED_DEVICE] Open body measurement entry form from 我的 (Profile) tab
- [UNVERIFIED_DEVICE] Enter weight (kg/lbs)
- [UNVERIFIED_DEVICE] Enter body fat percentage
- [UNVERIFIED_DEVICE] Enter muscle mass
- [UNVERIFIED_DEVICE] Enter waist measurement
- [UNVERIFIED_DEVICE] Save measurement and verify it appears in measurement history
- [UNVERIFIED_DEVICE] View weight trend chart
- [UNVERIFIED_DEVICE] View body fat trend chart
- [UNVERIFIED_DEVICE] View waist trend chart
- [UNVERIFIED_DEVICE] Delete a measurement entry
- [UNVERIFIED_DEVICE] Verify latest measurement shown on profile

---

## 11. Nutrition

### 11.1 Meal Logging
- [UNVERIFIED_DEVICE] Open nutrition entry from 我的 (Profile) tab
- [UNVERIFIED_DEVICE] Log a meal: select meal type (breakfast/lunch/dinner/snack)
- [UNVERIFIED_DEVICE] Enter food name, calories, protein, carbs, fat
- [UNVERIFIED_DEVICE] Save meal entry and verify it appears in daily log
- [UNVERIFIED_DEVICE] Edit a logged meal entry
- [UNVERIFIED_DEVICE] Delete a logged meal entry

### 11.2 Daily Totals & Targets
- [UNVERIFIED_DEVICE] Verify daily calorie total displays correctly
- [UNVERIFIED_DEVICE] Verify macro totals (protein, carbs, fat) display correctly
- [UNVERIFIED_DEVICE] Verify TDEE-based targets are calculated and displayed
- [UNVERIFIED_DEVICE] Verify calorie/protein trend charts render correctly

---

## 12. Reminders

### 12.1 Create Reminder
- [UNVERIFIED_DEVICE] Open reminder settings from 我的 (Profile) tab
- [UNVERIFIED_DEVICE] Create a new reminder with time of day
- [UNVERIFIED_DEVICE] Select specific days of week for the reminder
- [UNVERIFIED_DEVICE] Save reminder and verify it appears in reminder list

### 12.2 Edit & Delete Reminder
- [UNVERIFIED_DEVICE] Edit an existing reminder (change time, days)
- [UNVERIFIED_DEVICE] Delete a reminder
- [UNVERIFIED_DEVICE] Verify notification fires at the scheduled time on selected days

### 12.3 Notification Permissions
- [UNVERIFIED_DEVICE] Verify notification permission request on Android 13+ (API 33)
- [UNVERIFIED_DEVICE] Verify reminders work after granting permission
- [UNVERIFIED_DEVICE] Verify app handles denied notification permission gracefully

---

## 13. Camera Permissions

- [UNVERIFIED_DEVICE] Verify camera permission request when accessing camera features
- [UNVERIFIED_DEVICE] Verify storage/media permission request on relevant Android versions
- [UNVERIFIED_DEVICE] Verify denied permission shows rationale or graceful fallback
- [UNVERIFIED_DEVICE] Verify permanently denied permission (Don't ask again) is handled

---

## 14. Photo Capture

- [UNVERIFIED_DEVICE] Open camera from progress photo feature
- [UNVERIFIED_DEVICE] Verify CameraX viewfinder renders correctly
- [UNVERIFIED_DEVICE] Tap capture button and verify photo is taken
- [UNVERIFIED_DEVICE] Verify exposure adjustment works
- [UNVERIFIED_DEVICE] Verify focus adjustment (tap-to-focus) works
- [UNVERIFIED_DEVICE] Verify zoom controls work (pinch-to-zoom or slider)
- [UNVERIFIED_DEVICE] Verify captured photo is saved to app media storage
- [UNVERIFIED_DEVICE] Verify captured photo appears in media gallery

---

## 15. Video Recording

- [UNVERIFIED_DEVICE] Switch camera to video mode
- [UNVERIFIED_DEVICE] Start video recording
- [UNVERIFIED_DEVICE] Stop video recording
- [UNVERIFIED_DEVICE] Verify recorded video is saved to app media storage
- [UNVERIFIED_DEVICE] Verify recorded video appears in media gallery
- [UNVERIFIED_DEVICE] Verify focus and zoom work during recording

---

## 16. Media Library & Gallery

- [UNVERIFIED_DEVICE] Open media gallery from progress photo section
- [UNVERIFIED_DEVICE] Verify all captured photos and videos are listed
- [UNVERIFIED_DEVICE] Tap a media item to view it full-screen
- [UNVERIFIED_DEVICE] Delete a media item
- [UNVERIFIED_DEVICE] Verify empty state when no media exists

---

## 17. Progress Photo Comparison

- [UNVERIFIED_DEVICE] Open comparison view with two progress photos
- [UNVERIFIED_DEVICE] Verify side-by-side or overlay comparison renders correctly
- [UNVERIFIED_DEVICE] Verify date labels on each photo
- [UNVERIFIED_DEVICE] Test with photos from different dates (ensure chronological ordering)

---

## 18. CSV Export

- [UNVERIFIED_DEVICE] Open data export from 我的 (Profile) tab
- [UNVERIFIED_DEVICE] Verify exportable categories are listed (workouts, body measurements, nutrition, check-ins)
- [UNVERIFIED_DEVICE] Select one category and export via SAF
- [UNVERIFIED_DEVICE] Verify SAF file picker opens for destination selection
- [UNVERIFIED_DEVICE] Verify CSV file is written with UTF-8 BOM header
- [UNVERIFIED_DEVICE] Inspect CSV: verify RFC 4180 compliance (quoted commas, escaped quotes)
- [UNVERIFIED_DEVICE] Verify Chinese characters render correctly in CSV
- [UNVERIFIED_DEVICE] Export all categories and verify each file is created
- [UNVERIFIED_DEVICE] Verify exported data matches app contents (spot-check row counts and values)

---

## 19. Backup & Restore

### 19.1 Backup
- [UNVERIFIED_DEVICE] Open backup/restore from 我的 (Profile) tab
- [UNVERIFIED_DEVICE] Create a full backup via SAF
- [UNVERIFIED_DEVICE] Verify ZIP file is created at the chosen location
- [UNVERIFIED_DEVICE] Verify ZIP contains: manifest.json, db.json, media/ directory
- [UNVERIFIED_DEVICE] Verify manifest.json contains version, checksum, and row counts
- [UNVERIFIED_DEVICE] Verify SHA-256 checksum in manifest matches the archive

### 19.2 Restore
- [UNVERIFIED_DEVICE] Create test data, then perform restore from a backup
- [UNVERIFIED_DEVICE] Verify pre-import backup is created before restore
- [UNVERIFIED_DEVICE] Verify database is replaced with backup data
- [UNVERIFIED_DEVICE] Verify media files are restored
- [UNVERIFIED_DEVICE] Verify version mismatch is detected and rejected
- [UNVERIFIED_DEVICE] Verify corrupt archive is detected and rejected
- [UNVERIFIED_DEVICE] Verify checksum mismatch is detected and rejected
- [UNVERIFIED_DEVICE] Verify rollback works if import fails mid-operation

---

## 20. Settings & Cleanup

### 20.1 User Profile (我的 Tab)
- [UNVERIFIED_DEVICE] Edit user profile: gender, birthday, height
- [UNVERIFIED_DEVICE] Change activity level and verify TDEE targets update
- [UNVERIFIED_DEVICE] Set goal type and target values
- [UNVERIFIED_DEVICE] Verify goal timeline estimation displays

### 20.2 Media Cleanup
- [UNVERIFIED_DEVICE] Verify orphaned media cleanup (media records without referenced files)
- [UNVERIFIED_DEVICE] Clear app data and verify clean state

### 20.3 General Settings
- [UNVERIFIED_DEVICE] Verify dark theme renders consistently across all screens
- [UNVERIFIED_DEVICE] Verify all text is readable (no contrast issues)
- [UNVERIFIED_DEVICE] Verify no crashes during extended use

---

## Summary

| Section | Items | Pass | Fail | Not Tested |
|---------|-------|------|------|------------|
| 1. Installation | | — | — | — |
| 2. Navigation | | — | — | — |
| 3. Exercise/Template/Plan | | — | — | — |
| 4. Workout Execution | | — | — | — |
| 5. Rest Timer | | — | — | — |
| 6. History | | — | — | — |
| 7. Calendar | | — | — | — |
| 8. Reschedule/Skip | | — | — | — |
| 9. Check-in | | — | — | — |
| 10. Body Measurement | | — | — | — |
| 11. Nutrition | | — | — | — |
| 12. Reminders | | — | — | — |
| 13. Camera Permissions | | — | — | — |
| 14. Photo Capture | | — | — | — |
| 15. Video Recording | | — | — | — |
| 16. Media Library | | — | — | — |
| 17. Progress Photo Comparison | | — | — | — |
| 18. CSV Export | | — | — | — |
| 19. Backup/Restore | | — | — | — |
| 20. Settings/Cleanup | | — | — | — |
| **Total** | | — | — | — |

**Tester**: __________________
**Date**: __________________
**Device Model**: __________________
**Android Version**: __________________

# FitLog Privacy Policy

FitLog is a **local-first** fitness tracking application. Your data stays on your device unless you explicitly choose to export or share it.

## Data Storage

All FitLog data is stored exclusively on-device in the following locations:

| Data Type | Storage Location | Accessible Without App |
|-----------|-----------------|----------------------|
| User profile, body measurements, workout history, nutrition logs, check-ins | Room SQLite database (`app's internal storage`) | No |
| Media files (photos, videos) | `getExternalFilesDir()` — app-specific external storage | Yes, via file manager |
| User preferences | DataStore Preferences (`app's internal storage`) | No |
| App settings | DataStore Preferences (`app's internal storage`) | No |

## Network Access

FitLog makes **no network requests**. The app has no internet permission and no cloud sync capability. All processing happens locally on the device.

## Permissions

### Android Permissions Used

| Permission | Purpose | Required | Data Accessed |
|-----------|---------|----------|--------------|
| `CAMERA` | Capture progress photos and workout form videos | Yes, for camera features | Photos/videos saved to app storage |
| `RECORD_AUDIO` | Record video with audio | Yes, for video recording | Audio data in captured videos |
| `POST_NOTIFICATIONS` (Android 13+) | Schedule workout reminders | Yes, for reminder features | None |
| `READ_MEDIA_IMAGES/VIDEO` | Export media to system gallery | Future use | Only with explicit user action |
| `ACTIVITY_RECOGNITION` | Auto-detect workout activity | Not currently used | N/A |

### Storage Access Framework (SAF)

Data export and backup use Android's Storage Access Framework (SAF):

- **Export**: CSV files are written to user-selected locations via `ACTION_CREATE_DOCUMENT`
- **Backup**: ZIP archives are written to user-selected locations via `ACTION_CREATE_DOCUMENT`
- **Restore**: ZIP archives are read from user-selected files via `ACTION_OPEN_DOCUMENT`

The app never accesses files outside the user's explicit selection through the system file picker.

### Camera Permissions

Camera access is only used for:
1. Capturing progress photos linked to body measurements
2. Recording workout form videos
3. Photos/videos are stored in app-specific external storage
4. Users can delete any captured media at any time

## Media Files

Media files (photos, videos) are stored in the app's external files directory:
- `Pictures/FitLog/` for photos
- `Movies/FitLog/` for videos

These files:
- Are automatically deleted when the app is uninstalled
- Are scoped to the app — other apps cannot access them by default
- Can be exported to the system gallery via explicit user action
- Are included in ZIP backups when the user initiates a backup

## Backup Data

When the user creates a backup, the ZIP archive contains:
- All database records (workouts, measurements, nutrition, check-ins, settings)
- All media files (photos, videos)
- A manifest file with version metadata and SHA-256 checksum

Backups are user-initiated. The app never creates backups automatically without user action.

## Data Export

When the user exports data:
- CSV files contain the selected data type (workouts, measurements, nutrition, or check-ins)
- The user chooses the save location via the system file picker
- Exported files are standard CSV with UTF-8 encoding

## Third-Party Services

FitLog uses no third-party analytics, crash reporting, or advertising SDKs. The app does not include any network libraries.

## Google Play Services

FitLog does not use Google Play Services or Google Play Games. The app functions entirely offline.

## Data Deletion

Users can delete individual records or all data at any time:
- Individual workout sessions, measurements, food records, and media files can be deleted from the app
- Media cleanup manager removes orphan files and database records
- Uninstalling the app removes all stored data (with the exception of media files in external storage, which must be deleted separately)

## Changes to This Policy

Updates to this privacy policy will be reflected in the app's documentation. The app does not have network connectivity to push policy updates.

---

Last updated: July 2026

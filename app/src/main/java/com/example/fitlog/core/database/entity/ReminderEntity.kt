package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.ZoneId

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["schedule_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["is_enabled"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "schedule_id") val scheduleId: Long? = null,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "time_of_day_minutes") val timeOfDayMinutes: Int,
    @ColumnInfo(name = "days_of_week_mask") val daysOfWeekMask: Int,
    @ColumnInfo(name = "zone_id") val zoneId: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        /**
         * Validates that [timeOfDayMinutes] is within the valid range 0..1439.
         */
        fun validateTimeOfDayMinutes(timeOfDayMinutes: Int): Boolean =
            timeOfDayMinutes in 0..1439

        /**
         * Validates that [daysOfWeekMask] is a non-zero 7-bit bitmask (1..127).
         */
        fun validateDaysOfWeekMask(daysOfWeekMask: Int): Boolean =
            daysOfWeekMask in 1..127

        /**
         * Validates that [zoneId] is a valid timezone ID recognized by [ZoneId.of].
         */
        fun validateZoneId(zoneId: String): Boolean =
            try {
                ZoneId.of(zoneId)
                true
            } catch (_: Exception) {
                false
            }

        /**
         * Validates that [label] is not blank when trimmed.
         */
        fun validateLabel(label: String): Boolean =
            label.trim().isNotBlank()

        /**
         * Runs all validations and returns a map of field name to error message
         * for each invalid field. Returns an empty map when all fields are valid.
         */
        fun validate(
            timeOfDayMinutes: Int,
            daysOfWeekMask: Int,
            zoneId: String,
            label: String,
        ): Map<String, String> {
            val errors = mutableMapOf<String, String>()
            if (!validateTimeOfDayMinutes(timeOfDayMinutes)) {
                errors["timeOfDayMinutes"] = "Must be between 0 and 1439"
            }
            if (!validateDaysOfWeekMask(daysOfWeekMask)) {
                errors["daysOfWeekMask"] = "Must be a 7-bit mask between 1 and 127"
            }
            if (!validateZoneId(zoneId)) {
                errors["zoneId"] = "Invalid timezone ID: $zoneId"
            }
            if (!validateLabel(label)) {
                errors["label"] = "Label must not be blank"
            }
            return errors
        }
    }
}

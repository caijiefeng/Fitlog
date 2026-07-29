package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_records",
    indices = [
        Index(value = ["captured_at"]),
        Index(value = ["workout_session_id"]),
        Index(value = ["body_measurement_id"]),
        Index(value = ["check_in_id"]),
        Index(value = ["media_type"]),
        Index(value = ["category"]),
    ],
)
data class MediaRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "media_type")
    val mediaType: String, // PHOTO / VIDEO
    @ColumnInfo(name = "relative_path")
    val relativePath: String, // relative to storage root, never absolute
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "captured_at")
    val capturedAt: Long, // epochMillis
    @ColumnInfo(name = "date")
    val date: Long, // epochDay
    @ColumnInfo(name = "width")
    val width: Int? = null,
    @ColumnInfo(name = "height")
    val height: Int? = null,
    @ColumnInfo(name = "duration_millis")
    val durationMillis: Long? = null, // non-null for VIDEO
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "workout_session_id")
    val workoutSessionId: Long? = null,
    @ColumnInfo(name = "body_measurement_id")
    val bodyMeasurementId: Long? = null,
    @ColumnInfo(name = "check_in_id")
    val checkInId: Long? = null,
    @ColumnInfo(name = "exercise_session_id")
    val exerciseSessionId: Long? = null,
    @ColumnInfo(name = "food_record_id")
    val foodRecordId: Long? = null,
    @ColumnInfo(name = "category")
    val category: String, // BODY_PROGRESS / WORKOUT_FORM / MEAL / GENERAL
    @ColumnInfo(name = "pose_tag")
    val poseTag: String? = null, // FRONT / SIDE_LEFT / SIDE_RIGHT / BACK / OTHER
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)

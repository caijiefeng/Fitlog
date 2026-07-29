package com.example.fitlog.core.database.converter

import androidx.room.TypeConverter
import com.example.fitlog.domain.media.MediaCategory
import com.example.fitlog.domain.media.MediaType
import com.example.fitlog.domain.media.ProgressPose
import java.time.Instant
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    // ── Media enums ──────────────────────────────────────────────────────────

    @TypeConverter
    fun fromMediaType(value: MediaType?): String? = value?.name

    @TypeConverter
    fun toMediaType(value: String?): MediaType? = value?.let { MediaType.valueOf(it) }

    @TypeConverter
    fun fromMediaCategory(value: MediaCategory?): String? = value?.name

    @TypeConverter
    fun toMediaCategory(value: String?): MediaCategory? = value?.let { MediaCategory.valueOf(it) }

    @TypeConverter
    fun fromProgressPose(value: ProgressPose?): String? = value?.name

    @TypeConverter
    fun toProgressPose(value: String?): ProgressPose? = value?.let { ProgressPose.valueOf(it) }
}

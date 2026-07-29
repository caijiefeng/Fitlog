package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_ins",
    indices = [
        Index(value = ["date"], unique = true),
        Index(value = ["session_id"]),
    ],
)
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date") val date: Long,  // epochDay, unique
    @ColumnInfo(name = "session_id") val sessionId: Long? = null,
    @ColumnInfo(name = "mood") val mood: Int? = null,  // 1-5
    @ColumnInfo(name = "energy_level") val energyLevel: Int? = null, // 1-5
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

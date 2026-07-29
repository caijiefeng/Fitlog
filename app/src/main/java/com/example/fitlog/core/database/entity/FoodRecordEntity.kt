package com.example.fitlog.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "food_records",
    indices = [
        Index(value = ["date"]),
    ],
)
data class FoodRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "date") val date: Long,  // epochDay
    @ColumnInfo(name = "meal_type") val mealType: String,  // BREAKFAST / LUNCH / DINNER / SNACK
    @ColumnInfo(name = "food_name") val foodName: String,
    @ColumnInfo(name = "calories") val calories: Double? = null,
    @ColumnInfo(name = "protein_grams") val proteinGrams: Double? = null,
    @ColumnInfo(name = "carbs_grams") val carbsGrams: Double? = null,
    @ColumnInfo(name = "fat_grams") val fatGrams: Double? = null,
    @ColumnInfo(name = "amount") val amount: String? = null,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

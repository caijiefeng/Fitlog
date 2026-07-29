package com.example.fitlog.domain.body

import java.time.LocalDate

data class BodyMeasurement(
    val id: Long = 0,
    val date: LocalDate,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val muscleKg: Double? = null,
    val waistCm: Double? = null,
    val note: String? = null,
)

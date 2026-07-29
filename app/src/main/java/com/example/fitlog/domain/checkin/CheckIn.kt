package com.example.fitlog.domain.checkin

import java.time.LocalDate

data class CheckIn(
    val id: Long,
    val date: LocalDate,
    val sessionId: Long?,
    val mood: Int?,
    val energyLevel: Int?,
    val notes: String?,
)

package com.example.fitlog.core.time

import java.time.LocalDate

interface CurrentDateProvider {
    fun today(): LocalDate
}

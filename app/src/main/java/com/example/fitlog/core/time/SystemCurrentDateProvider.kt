package com.example.fitlog.core.time

import java.time.LocalDate
import javax.inject.Singleton

@Singleton
class SystemCurrentDateProvider : CurrentDateProvider {
    override fun today(): LocalDate = LocalDate.now()
}

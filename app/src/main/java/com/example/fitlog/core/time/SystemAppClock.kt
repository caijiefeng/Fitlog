package com.example.fitlog.core.time

import java.time.Instant
import javax.inject.Singleton

@Singleton
class SystemAppClock : AppClock {
    override fun nowInstant(): Instant = Instant.ofEpochMilli(System.currentTimeMillis())
}

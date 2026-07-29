package com.example.fitlog.core.time

import java.time.Instant

interface AppClock {
    fun nowInstant(): Instant
}

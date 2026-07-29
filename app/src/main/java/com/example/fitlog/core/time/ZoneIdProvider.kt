package com.example.fitlog.core.time

import java.time.ZoneId

interface ZoneIdProvider {
    fun currentZoneId(): ZoneId
}

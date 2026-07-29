package com.example.fitlog.core.time

import java.time.ZoneId
import javax.inject.Singleton

@Singleton
class SystemZoneIdProvider : ZoneIdProvider {
    override fun currentZoneId(): ZoneId = ZoneId.systemDefault()
}

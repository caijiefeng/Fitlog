package com.example.fitlog.core.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object DateTimeUtils {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatDateTime(dateTime: LocalDateTime): String =
        "${dateTime.format(dateFormatter)} ${dateTime.format(timeFormatter)}"

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}h ${mins}min" else "${mins}min"
    }
}

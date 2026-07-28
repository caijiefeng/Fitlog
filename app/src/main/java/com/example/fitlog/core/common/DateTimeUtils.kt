package com.example.fitlog.core.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object DateTimeUtils {

    /**
     * Creates a localized date formatter. Not cached statically because
     * Locale.getDefault() can change at runtime on API 28+ when the user
     * changes language from system settings.
     */
    private fun dateFormatter() = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(java.util.Locale.getDefault())

    private fun timeFormatter() = DateTimeFormatter
        .ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(java.util.Locale.getDefault())

    fun formatDate(date: LocalDate): String = date.format(dateFormatter())

    fun formatDateTime(dateTime: LocalDateTime): String =
        "${dateTime.format(dateFormatter())} ${dateTime.format(timeFormatter())}"

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}h ${mins}min" else "${mins}min"
    }
}

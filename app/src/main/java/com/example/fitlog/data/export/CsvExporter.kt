package com.example.fitlog.data.export

import android.content.Context
import android.net.Uri
import com.example.fitlog.data.repository.BodyMeasurementRepository
import com.example.fitlog.data.repository.CheckInRepository
import com.example.fitlog.data.repository.FoodRecordRepository
import com.example.fitlog.data.repository.WorkoutSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports FitLog data as CSV files using the Storage Access Framework
 * [android.content.Intent.ACTION_CREATE_DOCUMENT] pattern.
 *
 * All output files include a UTF-8 BOM for correct Excel encoding,
 * RFC 4180 escaping (double-quote wrapping, embedded quote doubling),
 * ISO-8601 date formatting (yyyy-MM-dd), and empty fields for null values
 * (never "0" or "null").
 */
@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val checkInRepository: CheckInRepository,
    private val foodRecordRepository: FoodRecordRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
) {

    sealed interface ExportResult {
        data object Success : ExportResult
        data class Error(val message: String) : ExportResult
    }

    companion object {
        private const val UTF8_BOM = "﻿"
        private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val HEADER_BODY = listOf("Date", "WeightKg", "BodyFatPercent", "MuscleKg", "WaistCm", "Note")
        private val HEADER_CHECKIN = listOf("Date", "SessionId", "Mood", "EnergyLevel", "Notes")
        private val HEADER_NUTRITION = listOf("Date", "MealType", "FoodName", "Calories", "ProteinGrams", "CarbsGrams", "FatGrams", "Amount", "Note")
        private val HEADER_WORKOUT = listOf("Date", "TemplateName", "Status", "DurationSeconds", "ExerciseCount", "CompletedSets", "TotalVolumeKg", "Notes")
    }

    suspend fun exportBodyMeasurements(uri: Uri): ExportResult = runCatching {
        val measurements = bodyMeasurementRepository.observeAll().first()
        writeCsv(uri, HEADER_BODY, measurements.size) { w ->
            measurements.forEach { m ->
                w.row(fmtDate(m.date), num(m.weightKg), num(m.bodyFatPercent), num(m.muscleKg), num(m.waistCm), m.note)
            }
        }
        ExportResult.Success
    }.getOrElse { ExportResult.Error(it.message ?: "Export failed") }

    suspend fun exportCheckIns(uri: Uri): ExportResult = runCatching {
        val rangeStart = LocalDate.of(2000, 1, 1)
        val rangeEnd = LocalDate.of(2099, 12, 31)
        val records = checkInRepository.getByDateRange(rangeStart, rangeEnd)
        writeCsv(uri, HEADER_CHECKIN, records.size) { w ->
            records.forEach { c ->
                w.row(fmtDate(c.date), c.sessionId?.toString(), c.mood?.toString(), c.energyLevel?.toString(), c.notes)
            }
        }
        ExportResult.Success
    }.getOrElse { ExportResult.Error(it.message ?: "Export failed") }

    suspend fun exportNutrition(uri: Uri): ExportResult = runCatching {
        val rangeStart = LocalDate.of(2000, 1, 1)
        val rangeEnd = LocalDate.of(2099, 12, 31)
        val records = foodRecordRepository.getByDateRange(rangeStart, rangeEnd)
        writeCsv(uri, HEADER_NUTRITION, records.size) { w ->
            records.forEach { f ->
                w.row(fmtDate(f.date), f.mealType, f.foodName, num(f.calories), num(f.proteinGrams), num(f.carbsGrams), num(f.fatGrams), f.amount, f.note)
            }
        }
        ExportResult.Success
    }.getOrElse { ExportResult.Error(it.message ?: "Export failed") }

    suspend fun exportWorkouts(uri: Uri): ExportResult = runCatching {
        val startEpoch = LocalDate.of(2000, 1, 1).toEpochDay()
        val endEpoch = LocalDate.of(2099, 12, 31).toEpochDay()
        val sessions = workoutSessionRepository.getSessionsInRange(startEpoch, endEpoch)
        writeCsv(uri, HEADER_WORKOUT, sessions.size) { w ->
            sessions.forEach { s ->
                val detail = workoutSessionRepository.getDetail(s.id)
                val exerciseCount = detail?.exercises?.size ?: 0
                val completedSets = detail?.exercises?.sumOf { (_, sets) -> sets.count { it.completed } } ?: 0
                val totalVolume = detail?.exercises?.sumOf { (_, sets) ->
                    sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
                } ?: 0.0
                val durationSeconds = if (s.endTime != null) Duration.between(s.startTime, s.endTime).seconds.toInt() else null
                w.row(fmtDate(s.date), s.templateNameSnapshot, s.status.name, durationSeconds?.toString(),
                    exerciseCount.toString(), completedSets.toString(), "%.1f".format(totalVolume), s.notes)
            }
        }
        ExportResult.Success
    }.getOrElse { ExportResult.Error(it.message ?: "Export failed") }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun writeCsv(uri: Uri, headers: List<String>, rowCount: Int, body: (CsvWriter.() -> Unit)) {
        val out = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Cannot open $uri")
        out.use { os ->
            BufferedWriter(OutputStreamWriter(os, Charsets.UTF_8)).use { bw ->
                bw.write(UTF8_BOM)
                bw.write(headers.joinToCsvLine())
                bw.newLine()
                CsvWriter(bw).body()
                bw.flush()
            }
        }
    }

    private fun fmtDate(d: LocalDate): String = d.format(ISO_DATE)
    private fun num(v: Double?): String? = v?.toString()

    private class CsvWriter(private val bw: BufferedWriter) {
        fun row(vararg values: String?) {
            bw.write(values.joinToCsvLine())
            bw.newLine()
        }
    }

    /** RFC 4180: null -> empty field; embedded commas/quotes/newlines -> wrap in double-quotes, double embedded quotes. */
    private fun Array<out String?>.joinToCsvLine(): String = joinToString(",") { v ->
        when {
            v == null -> ""
            v.any { it == ',' || it == '"' || it == '\n' || it == '\r' } -> "\"${v.replace("\"", "\"\"")}\""
            else -> v
        }
    }
}

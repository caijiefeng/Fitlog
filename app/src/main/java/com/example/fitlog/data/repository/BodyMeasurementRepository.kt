package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.BodyMeasurementDao
import com.example.fitlog.core.database.entity.BodyMeasurementEntity
import com.example.fitlog.domain.body.BodyMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyMeasurementRepository @Inject constructor(
    private val bodyMeasurementDao: BodyMeasurementDao,
) {

    suspend fun saveMeasurement(measurement: BodyMeasurement): BodyMeasurement {
        val entity = BodyMeasurementEntity(
            date = measurement.date.toEpochDay(),
            weightKg = measurement.weightKg,
            bodyFatPercent = measurement.bodyFatPercent,
            muscleKg = measurement.muscleKg,
            waistCm = measurement.waistCm,
            note = measurement.note,
        )
        val id = bodyMeasurementDao.upsert(entity)
        return entity.copy(id = id).toDomain()
    }

    suspend fun getByDateRange(start: LocalDate, end: LocalDate): List<BodyMeasurement> {
        return bodyMeasurementDao.getByDateRange(start.toEpochDay(), end.toEpochDay())
            .map { it.toDomain() }
    }

    suspend fun delete(date: LocalDate) {
        bodyMeasurementDao.deleteByDate(date.toEpochDay())
    }

    fun observeAll(): Flow<List<BodyMeasurement>> {
        return bodyMeasurementDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    private fun BodyMeasurementEntity.toDomain(): BodyMeasurement = BodyMeasurement(
        id = id,
        date = LocalDate.ofEpochDay(date),
        weightKg = weightKg,
        bodyFatPercent = bodyFatPercent,
        muscleKg = muscleKg,
        waistCm = waistCm,
        note = note,
    )
}

package com.example.fitlog.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.database.entity.WorkoutScheduleEntity
import com.example.fitlog.core.database.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WorkoutScheduleDaoTest {

    private lateinit var db: FitLogDatabase
    private lateinit var scheduleDao: WorkoutScheduleDao
    private lateinit var templateDao: WorkoutTemplateDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).build()
        scheduleDao = db.workoutScheduleDao()
        templateDao = db.workoutTemplateDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun createTemplate(name: String): Long =
        templateDao.insert(WorkoutTemplateEntity(name = name))

    @Test
    fun setAndGetByDay() = runBlocking {
        val tId = createTemplate("Push Day")
        scheduleDao.insert(WorkoutScheduleEntity(templateId = tId, dayOfWeek = 1))

        val result = scheduleDao.getByDayOfWeek(1)
        assertNotNull(result)
        assertEquals(tId, result!!.templateId)
    }

    @Test
    fun getFullWeekSchedule() = runBlocking {
        val t1 = createTemplate("Push")
        val t2 = createTemplate("Pull")
        scheduleDao.insert(WorkoutScheduleEntity(templateId = t1, dayOfWeek = 1))
        scheduleDao.insert(WorkoutScheduleEntity(templateId = t2, dayOfWeek = 3))

        val week = scheduleDao.getFullWeekSchedule().first()
        assertEquals(2, week.size)
    }

    @Test
    fun replaceDay_replacesTemplate() = runBlocking {
        val t1 = createTemplate("Push A")
        val t2 = createTemplate("Push B")
        scheduleDao.insert(WorkoutScheduleEntity(templateId = t1, dayOfWeek = 1))
        scheduleDao.deleteByDayOfWeek(1)
        scheduleDao.insert(WorkoutScheduleEntity(templateId = t2, dayOfWeek = 1))

        val result = scheduleDao.getByDayOfWeek(1)
        assertEquals(t2, result!!.templateId)
    }

    @Test
    fun clearDay_removesSchedule() = runBlocking {
        val tId = createTemplate("Test")
        scheduleDao.insert(WorkoutScheduleEntity(templateId = tId, dayOfWeek = 5))
        scheduleDao.clearDay(5)

        val result = scheduleDao.getByDayOfWeek(5)
        assertNull(result)
    }

    @Test
    fun sameTemplate_multipleDays() = runBlocking {
        val tId = createTemplate("Full Body")
        scheduleDao.insert(WorkoutScheduleEntity(templateId = tId, dayOfWeek = 2))
        scheduleDao.insert(WorkoutScheduleEntity(templateId = tId, dayOfWeek = 4))
        scheduleDao.insert(WorkoutScheduleEntity(templateId = tId, dayOfWeek = 6))

        val all = scheduleDao.getAllActiveList()
        assertEquals(3, all.size)
    }

    @Test
    fun dayOfWeek_boundariesAreValid() = runBlocking {
        val tId = createTemplate("Test")
        // Monday (1) and Sunday (7)
        scheduleDao.insert(WorkoutScheduleEntity(templateId = tId, dayOfWeek = 1))
        scheduleDao.insert(WorkoutScheduleEntity(templateId = tId, dayOfWeek = 7))

        assertEquals(2, scheduleDao.getAllActiveList().size)
    }

    @Test
    fun getByDayOfWeek_returnsNull_whenNotSet() = runBlocking {
        val result = scheduleDao.getByDayOfWeek(4)
        assertNull(result)
    }

    @Test
    fun upsert_replacesExisting() = runBlocking {
        val tId = createTemplate("Test")
        scheduleDao.upsert(WorkoutScheduleEntity(templateId = tId, dayOfWeek = 1))
        val result = scheduleDao.getByDayOfWeek(1)
        assertNotNull(result)
        assertEquals(tId, result!!.templateId)
    }
}

package com.example.fitlog.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExerciseDaoTest {

    private lateinit var db: FitLogDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).build()
        dao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetById() = runBlocking {
        val id = dao.insert(ExerciseEntity(name = "测试动作", primaryMuscleGroup = "CHEST"))
        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals("测试动作", result!!.name)
    }

    @Test
    fun getAllActive_onlyReturnsActiveExercises() = runBlocking {
        dao.insert(ExerciseEntity(name = "动作A", primaryMuscleGroup = "CHEST"))
        dao.insert(ExerciseEntity(name = "动作B", primaryMuscleGroup = "BACK"))
        dao.insert(ExerciseEntity(name = "动作C", primaryMuscleGroup = "CHEST", isActive = false))

        val all = dao.getAllActive().first()
        assertEquals(2, all.size)
    }

    @Test
    fun searchByName_findsMatching() = runBlocking {
        dao.insert(ExerciseEntity(name = "杠铃卧推", primaryMuscleGroup = "CHEST"))
        dao.insert(ExerciseEntity(name = "哑铃卧推", primaryMuscleGroup = "CHEST"))
        dao.insert(ExerciseEntity(name = "深蹲", primaryMuscleGroup = "QUADRICEPS"))

        val results = dao.searchByName("卧推").first()
        assertEquals(2, results.size)
    }

    @Test
    fun searchByName_caseInsensitive() = runBlocking {
        dao.insert(ExerciseEntity(name = "Bench Press", primaryMuscleGroup = "CHEST"))
        val results = dao.searchByName("bench").first()
        assertEquals(1, results.size)
    }

    @Test
    fun getByMuscleGroup_filtersCorrectly() = runBlocking {
        dao.insert(ExerciseEntity(name = "动作A", primaryMuscleGroup = "CHEST"))
        dao.insert(ExerciseEntity(name = "动作B", primaryMuscleGroup = "BACK"))
        dao.insert(ExerciseEntity(name = "动作C", primaryMuscleGroup = "CHEST"))

        val chest = dao.getByMuscleGroup("CHEST").first()
        assertEquals(2, chest.size)
    }

    @Test
    fun softDelete_removesFromActiveList() = runBlocking {
        val id = dao.insert(ExerciseEntity(name = "要删除的动作", primaryMuscleGroup = "CHEST"))
        assertEquals(1, dao.getAllActive().first().size)

        dao.softDelete(id)
        assertEquals(0, dao.getAllActive().first().size)
    }

    @Test
    fun softDelete_preservesRecord() = runBlocking {
        val id = dao.insert(ExerciseEntity(name = "软删除动作", primaryMuscleGroup = "BACK"))
        dao.softDelete(id)
        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals(false, result!!.isActive)
    }

    @Test
    fun countByName_detectsDuplicates() = runBlocking {
        dao.insert(ExerciseEntity(name = "杠铃卧推", primaryMuscleGroup = "CHEST"))
        assertEquals(1, dao.countByName("杠铃卧推"))
        assertEquals(0, dao.countByName("不存在的动作"))
    }

    @Test
    fun isCustom_flag_isPreserved() = runBlocking {
        val id = dao.insert(ExerciseEntity(name = "自定义", primaryMuscleGroup = "CHEST", isCustom = true))
        val result = dao.getById(id)
        assertEquals(true, result!!.isCustom)
    }
}

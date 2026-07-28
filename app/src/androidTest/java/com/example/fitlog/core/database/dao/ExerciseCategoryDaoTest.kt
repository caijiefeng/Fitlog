package com.example.fitlog.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.fitlog.core.database.FitLogDatabase
import com.example.fitlog.core.database.entity.ExerciseCategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExerciseCategoryDaoTest {

    private lateinit var db: FitLogDatabase
    private lateinit var dao: ExerciseCategoryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).build()
        dao = db.exerciseCategoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetAll() = runBlocking {
        dao.insert(ExerciseCategoryEntity(name = "康复训练"))
        dao.insert(ExerciseCategoryEntity(name = "CrossFit", sortOrder = 1))

        val all = dao.getAll().first()
        assertEquals(2, all.size)
    }

    @Test
    fun categoriesSortedByOrder() = runBlocking {
        dao.insert(ExerciseCategoryEntity(name = "B", sortOrder = 2))
        dao.insert(ExerciseCategoryEntity(name = "A", sortOrder = 0))
        dao.insert(ExerciseCategoryEntity(name = "C", sortOrder = 1))

        val all = dao.getAll().first()
        assertEquals("A", all[0].name)
        assertEquals("C", all[1].name)
        assertEquals("B", all[2].name)
    }

    @Test
    fun getById_returnsCorrectCategory() = runBlocking {
        val id = dao.insert(ExerciseCategoryEntity(name = "测试分类"))
        val result = dao.getById(id)
        assertEquals("测试分类", result!!.name)
    }

    @Test
    fun duplicateName_isIgnored() = runBlocking {
        val id1 = dao.insert(ExerciseCategoryEntity(name = "相同名称"))
        val id2 = dao.insert(ExerciseCategoryEntity(name = "相同名称"))
        assertEquals(-1, id2) // Insert ignored
        val count = dao.count()
        assertEquals(1, count)
    }

    @Test
    fun count_returnsCorrectTotal() = runBlocking {
        assertEquals(0, dao.count())
        dao.insert(ExerciseCategoryEntity(name = "A"))
        dao.insert(ExerciseCategoryEntity(name = "B"))
        assertEquals(2, dao.count())
    }
}

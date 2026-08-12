package com.example.fitlog.data.repository

import com.example.fitlog.core.database.dao.UserProfileDao
import com.example.fitlog.core.database.entity.UserProfileEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UserProfileRepositoryTest {

    private val dao = mockk<UserProfileDao>(relaxed = true)
    private val repository = UserProfileRepository(dao)

    @Test
    fun `updateDisplayName trims and persists an existing profile`() = runTest {
        val existing = profile(displayName = "旧昵称")
        coEvery { dao.get() } returns existing
        coEvery { dao.upsert(any()) } returns existing.id

        val result = repository.updateDisplayName("  西七  ")

        assertEquals("西七", result.displayName)
        coVerify {
            dao.upsert(match { it.id == existing.id && it.displayName == "西七" })
        }
    }

    @Test
    fun `updateDisplayName clears whitespace only name`() = runTest {
        val existing = profile(displayName = "旧昵称")
        coEvery { dao.get() } returns existing
        coEvery { dao.upsert(any()) } returns existing.id

        val result = repository.updateDisplayName("   ")

        assertEquals(null, result.displayName)
        coVerify { dao.upsert(match { it.displayName == null }) }
    }

    private fun profile(displayName: String?) = UserProfileEntity(
        id = 7L,
        gender = "OTHER",
        birthday = 0L,
        activityLevel = "SEDENTARY",
        goalType = "MAINTAIN",
        displayName = displayName,
    )
}

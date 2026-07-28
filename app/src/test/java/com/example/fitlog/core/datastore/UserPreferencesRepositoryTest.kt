package com.example.fitlog.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserPreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createTestDataStore(): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("test_preferences.preferences_pb") }
        )
    }

    @Test
    fun `initial preferences have correct defaults`() = runBlocking {
        val dataStore = createTestDataStore()
        val repository = UserPreferencesRepository(dataStore)

        val prefs = repository.preferences.first()

        assertFalse(prefs.isOnboardingComplete)
    }

    @Test
    fun `setOnboardingComplete updates preference`() = runBlocking {
        val dataStore = createTestDataStore()
        val repository = UserPreferencesRepository(dataStore)

        repository.setOnboardingComplete(true)

        val prefs = repository.preferences.first()
        assertTrue(prefs.isOnboardingComplete)
    }
}

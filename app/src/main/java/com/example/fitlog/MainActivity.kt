package com.example.fitlog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import com.example.fitlog.core.datastore.UserPreferencesRepository
import com.example.fitlog.data.repository.UserProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    /**
     * Incremented whenever the activity receives a reminder deep link
     * (fitlog://reminder/...), which drives navigation to the Today tab.
     */
    private val openTodayRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (isReminderDeepLink(intent)) {
            openTodayRequests.intValue++
        }
        setContent {
            FitLogApp(
                preferencesRepository = preferencesRepository,
                userProfileRepository = userProfileRepository,
                openTodayCounter = openTodayRequests.intValue,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isReminderDeepLink(intent)) {
            openTodayRequests.intValue++
        }
    }

    /** A reminder notification deep link, e.g. fitlog://reminder/123/open. */
    private fun isReminderDeepLink(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        return data.scheme == "fitlog" && data.host == "reminder"
    }
}

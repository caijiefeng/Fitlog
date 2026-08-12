package com.example.fitlog.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.datastore.UserPreferencesRepository
import com.example.fitlog.core.designsystem.theme.ThemeMode
import com.example.fitlog.data.repository.BodyMeasurementRepository
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.body.GoalType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "",
    val isProfileComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val avatarType: AvatarType = AvatarType.DEFAULT,
    val avatarKey: String? = null,
    val customAvatarPath: String? = null,
    // ── Hero 数据 ──────────────────────────────────────────────
    val goalLabel: String? = null,
    val heightCm: Double? = null,
    val latestWeightKg: Double? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val userProfileRepository: UserProfileRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    themeMode = prefs.themeMode,
                )
            }
        }
        viewModelScope.launch {
            userProfileRepository.observe().collect { profile ->
                _uiState.value = _uiState.value.copy(
                    avatarType = profile?.avatarType ?: AvatarType.DEFAULT,
                    avatarKey = profile?.avatarKey,
                    customAvatarPath = profile?.customAvatarPath,
                    userName = profile?.displayName.orEmpty(),
                    heightCm = profile?.heightCm,
                    goalLabel = profile?.goalType?.let { goalTypeLabel(it) },
                )
            }
        }
        viewModelScope.launch {
            try {
                val latest = bodyMeasurementRepository.getLatestOnOrBefore(java.time.LocalDate.now())
                _uiState.value = _uiState.value.copy(latestWeightKg = latest?.weightKg)
            } catch (_: Exception) { }
        }
    }

    private fun goalTypeLabel(type: GoalType): String = when (type) {
        GoalType.MUSCLE_GAIN -> "增肌"
        GoalType.LEAN_GAIN -> "瘦体重增长"
        GoalType.FAT_LOSS -> "减脂"
        GoalType.MAINTAIN -> "保持"
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun updateDisplayName(displayName: String) {
        viewModelScope.launch {
            userProfileRepository.updateDisplayName(displayName)
        }
    }
}

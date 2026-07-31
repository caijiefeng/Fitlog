package com.example.fitlog.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.datastore.UserPreferencesRepository
import com.example.fitlog.core.designsystem.theme.ThemeMode
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.avatar.AvatarType
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
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val userProfileRepository: UserProfileRepository,
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
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }
}

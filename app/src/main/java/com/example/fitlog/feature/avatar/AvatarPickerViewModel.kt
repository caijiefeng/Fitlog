package com.example.fitlog.feature.avatar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.data.repository.UserProfileRepository
import com.example.fitlog.domain.avatar.AvatarType
import com.example.fitlog.domain.avatar.BuiltInAvatar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AvatarPickerUiState(
    val avatarType: AvatarType = AvatarType.DEFAULT,
    val avatarKey: String? = null,
    val customAvatarPath: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AvatarPickerViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvatarPickerUiState())
    val uiState: StateFlow<AvatarPickerUiState> = _uiState.asStateFlow()

    init {
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

    /** Selects one of the built-in sports avatars and persists it immediately. */
    fun selectBuiltIn(avatar: BuiltInAvatar) {
        applyAndSave(AvatarType.BUILT_IN, avatar.key, null)
    }

    /** Persists a photo previously copied into internal storage ([AvatarPickerScreen] does the copy). */
    fun selectCustom(customAvatarPath: String) {
        applyAndSave(AvatarType.CUSTOM, null, customAvatarPath)
    }

    /** Reports a UI-level failure (e.g. the picked photo could not be read). */
    fun onError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    private fun applyAndSave(avatarType: AvatarType, avatarKey: String?, customAvatarPath: String?) {
        val current = _uiState.value
        if (current.isSaving) return
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, error = null, saved = false)
            try {
                userProfileRepository.updateAvatar(avatarType, avatarKey, customAvatarPath)
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败",
                )
            }
        }
    }
}

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
    /** Pending avatar type — updates the preview immediately on selection. */
    val avatarType: AvatarType = AvatarType.DEFAULT,
    /** Pending built-in avatar key ("" resolves to null at save time). */
    val avatarKey: String? = null,
    /** Pending custom photo path (relative to filesDir). */
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

    /** Updates the preview to a built-in sports star; persisted on [save]. */
    fun selectBuiltIn(avatar: BuiltInAvatar) {
        _uiState.value = _uiState.value.copy(
            avatarType = AvatarType.BUILT_IN,
            avatarKey = avatar.key,
            customAvatarPath = null,
            saved = false,
            error = null,
        )
    }

    /**
     * Updates the preview to a photo previously copied into internal storage
     * ([AvatarPickerScreen] does the copy); persisted on [save].
     */
    fun selectCustom(customAvatarPath: String) {
        _uiState.value = _uiState.value.copy(
            avatarType = AvatarType.CUSTOM,
            avatarKey = null,
            customAvatarPath = customAvatarPath,
            saved = false,
            error = null,
        )
    }

    /** Persists the pending selection, then marks the screen as saved. */
    fun save() {
        val current = _uiState.value
        if (current.isSaving) return
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, error = null, saved = false)
            try {
                userProfileRepository.updateAvatar(
                    avatarType = current.avatarType,
                    avatarKey = current.avatarKey,
                    customAvatarPath = current.customAvatarPath,
                )
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "保存失败",
                )
            }
        }
    }

    /** Reports a UI-level failure (e.g. the picked photo could not be read). */
    fun onError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }
}

package com.example.fitlog.feature.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlog.core.model.WorkoutTemplate
import com.example.fitlog.data.repository.WorkoutTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplateListUiState(
    val templates: List<WorkoutTemplate> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface TemplateListEvent {
    data class NavigateToEdit(val templateId: Long) : TemplateListEvent
    data object NavigateToCreate : TemplateListEvent
}

@HiltViewModel
class TemplateListViewModel @Inject constructor(
    private val templateRepository: WorkoutTemplateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateListUiState())
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TemplateListEvent>()
    val events: SharedFlow<TemplateListEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            templateRepository.getAllActive().collect { templates ->
                _uiState.value = TemplateListUiState(templates = templates, isLoading = false)
            }
        }
    }

    fun onCreateNew() {
        viewModelScope.launch { _events.emit(TemplateListEvent.NavigateToCreate) }
    }

    fun onTemplateClicked(id: Long) {
        viewModelScope.launch { _events.emit(TemplateListEvent.NavigateToEdit(id)) }
    }
}

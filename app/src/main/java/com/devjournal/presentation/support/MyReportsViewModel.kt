package com.devjournal.presentation.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.BugReport
import com.devjournal.domain.usecase.GetUserBugReportsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyReportsUiState(
    val reports: List<BugReport> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MyReportsViewModel @Inject constructor(
    private val getUserBugReportsUseCase: GetUserBugReportsUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyReportsUiState())
    val uiState: StateFlow<MyReportsUiState> = _uiState.asStateFlow()

    init {
        loadUserReports()
    }

    fun loadUserReports() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Please sign in to view your bug reports.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getUserBugReportsUseCase(currentUserId)
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.localizedMessage ?: "Failed to load reports"
                        )
                    }
                }
                .collect { reportsList ->
                    _uiState.update {
                        it.copy(
                            reports = reportsList,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }
}

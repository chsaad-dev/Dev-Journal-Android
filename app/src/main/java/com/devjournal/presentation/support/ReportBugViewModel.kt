package com.devjournal.presentation.support

import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.BuildConfig
import com.devjournal.data.model.BugReport
import com.devjournal.domain.usecase.SubmitBugReportUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportBugUiState(
    val title: String = "",
    val description: String = "",
    val screenshotUri: Uri? = null,
    val deviceInfo: String = "",
    val appVersion: String = "",
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() = title.isNotBlank() && description.isNotBlank() && !isSubmitting
}

@HiltViewModel
class ReportBugViewModel @Inject constructor(
    private val submitBugReportUseCase: SubmitBugReportUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReportBugUiState(
            deviceInfo = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}",
            appVersion = "v${BuildConfig.VERSION_NAME}"
        )
    )
    val uiState: StateFlow<ReportBugUiState> = _uiState.asStateFlow()

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, errorMessage = null) }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(description = newDescription, errorMessage = null) }
    }

    fun onScreenshotSelected(uri: Uri?) {
        _uiState.update { it.copy(screenshotUri = uri, errorMessage = null) }
    }

    fun onRemoveScreenshot() {
        _uiState.update { it.copy(screenshotUri = null) }
    }

    fun submitReport() {
        val currentState = _uiState.value
        if (!currentState.canSubmit) return

        val user = auth.currentUser
        val userId = user?.uid ?: ""
        val userEmail = user?.email ?: ""

        val bugReport = BugReport(
            userId = userId,
            userEmail = userEmail,
            title = currentState.title.trim(),
            description = currentState.description.trim(),
            deviceInfo = "${currentState.deviceInfo}, App ${currentState.appVersion}",
            appVersion = BuildConfig.VERSION_NAME,
            status = "open"
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = submitBugReportUseCase(bugReport, currentState.screenshotUri)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isSuccess = true,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.localizedMessage ?: "Failed to submit bug report. Please try again."
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.update {
            ReportBugUiState(
                deviceInfo = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}",
                appVersion = "v${BuildConfig.VERSION_NAME}"
            )
        }
    }
}

package com.devjournal.presentation.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.AppNotification
import com.devjournal.domain.usecase.MarkAllNotificationsReadUseCase
import com.devjournal.domain.usecase.MarkNotificationReadUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.ObserveNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val errorMessage: String? = null
) {
    val unreadCount: Int get() = notifications.count { !it.read }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val observeNotificationsUseCase: ObserveNotificationsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        observeAuth()
    }

    private fun observeAuth() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                _uiState.update { it.copy(currentUserId = user?.uid) }
                if (user != null) {
                    observeNotifications(user.uid)
                } else {
                    _uiState.update { it.copy(notifications = emptyList(), isLoading = false) }
                }
            }
        }
    }

    private fun observeNotifications(uid: String) {
        viewModelScope.launch {
            observeNotificationsUseCase(uid)
                .catch { e ->
                    Log.e("DevJournal", "Failed to observe notifications", e)
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { notifications ->
                    _uiState.update { it.copy(notifications = notifications, isLoading = false) }
                }
        }
    }

    fun onNotificationClick(notification: AppNotification) {
        if (!notification.read) {
            val uid = _uiState.value.currentUserId ?: return
            viewModelScope.launch {
                runCatching { markNotificationReadUseCase(uid, notification.id) }
            }
        }
    }

    fun onMarkAllRead() {
        val uid = _uiState.value.currentUserId ?: return
        viewModelScope.launch {
            runCatching { markAllNotificationsReadUseCase(uid) }
                .onFailure { e ->
                    Log.e("DevJournal", "Mark all read failed", e)
                    _uiState.update { it.copy(errorMessage = "Failed to mark all as read") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

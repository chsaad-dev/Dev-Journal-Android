package com.devjournal.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.AppNotification
import com.devjournal.domain.repository.NotificationRepository
import com.devjournal.domain.usecase.MarkAllNotificationsReadUseCase
import com.devjournal.domain.usecase.MarkNotificationReadUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val notificationRepository: NotificationRepository,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase
) : ViewModel() {

    // Tracks the current user uid for mark-read operations
    private val _currentUserId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NotificationsUiState> = observeAuthStateUseCase()
        .flatMapLatest { user ->
            _currentUserId.value = user?.uid
            if (user != null) {
                notificationRepository.observeNotifications(user.uid)
                    .catch { emit(emptyList()) }
                    .map { list ->
                        NotificationsUiState(
                            notifications = list,
                            isLoading = false,
                            currentUserId = user.uid
                        )
                    }
            } else {
                flowOf(NotificationsUiState(isLoading = false))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationsUiState(isLoading = true)
        )

    fun onNotificationClick(notification: AppNotification) {
        if (!notification.read) {
            val uid = _currentUserId.value ?: return
            viewModelScope.launch {
                runCatching { markNotificationReadUseCase(uid, notification.id) }
            }
        }
    }

    fun onMarkAllRead() {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            runCatching { markAllNotificationsReadUseCase(uid) }
        }
    }

    fun clearError() {
        // No-op in the new StateFlow-based approach; kept for API compatibility
    }
}

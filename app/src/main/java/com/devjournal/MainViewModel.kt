package com.devjournal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.domain.model.ThemeMode
import com.devjournal.domain.repository.NotificationRepository
import com.devjournal.domain.usecase.GetThemeModeUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getThemeModeUseCase: GetThemeModeUseCase,
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    notificationRepository: NotificationRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = getThemeModeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    /** Live count of unread notifications for the bell badge in the top bar. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadNotificationCount: StateFlow<Int> = observeAuthStateUseCase()
        .flatMapLatest { user ->
            if (user != null) {
                notificationRepository.observeNotifications(user.uid)
                    .map { list -> list.count { !it.read } }
            } else {
                flowOf(0)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
}

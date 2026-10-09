package com.devjournal.domain.usecase

import com.devjournal.data.model.AppNotification
import com.devjournal.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotificationsUseCase @Inject constructor(
    private val repo: NotificationRepository
) {
    operator fun invoke(uid: String): Flow<List<AppNotification>> =
        repo.observeNotifications(uid)
}

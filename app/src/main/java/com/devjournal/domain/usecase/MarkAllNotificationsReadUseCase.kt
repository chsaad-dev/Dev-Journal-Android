package com.devjournal.domain.usecase

import com.devjournal.domain.repository.NotificationRepository
import javax.inject.Inject

class MarkAllNotificationsReadUseCase @Inject constructor(
    private val repo: NotificationRepository
) {
    suspend operator fun invoke(uid: String) = repo.markAllAsRead(uid)
}

package com.devjournal.domain.repository

import com.devjournal.data.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    /** Real-time stream of the current user's notifications, newest first. */
    fun observeNotifications(uid: String): Flow<List<AppNotification>>

    /** Mark a single notification as read. */
    suspend fun markAsRead(uid: String, notificationId: String)

    /** Mark all unread notifications as read. */
    suspend fun markAllAsRead(uid: String)

    /** Count of unread notifications for badge display. */
    fun observeUnreadCount(uid: String): Flow<Int>
}

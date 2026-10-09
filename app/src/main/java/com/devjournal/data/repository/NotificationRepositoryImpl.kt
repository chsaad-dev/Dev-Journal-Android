package com.devjournal.data.repository

import com.devjournal.data.model.AppNotification
import com.devjournal.domain.repository.NotificationRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private fun notificationsCollection(uid: String) =
        firestore.collection("users").document(uid).collection("notifications")

    override fun observeNotifications(uid: String): Flow<List<AppNotification>> = callbackFlow {
        val listener = notificationsCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notifications = snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        AppNotification(
                            id = doc.id,
                            type = doc.getString("type") ?: "",
                            title = doc.getString("title") ?: "",
                            body = doc.getString("body") ?: "",
                            read = doc.getBoolean("read") ?: false,
                            postId = doc.getString("postId"),
                            senderUid = doc.getString("senderUid"),
                            targetUid = doc.getString("targetUid"),
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    }.getOrNull()
                }
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }

    override fun observeUnreadCount(uid: String): Flow<Int> =
        observeNotifications(uid).map { list -> list.count { !it.read } }

    override suspend fun markAsRead(uid: String, notificationId: String) {
        notificationsCollection(uid)
            .document(notificationId)
            .update("read", true)
            .await()
    }

    override suspend fun markAllAsRead(uid: String) {
        val unread = notificationsCollection(uid)
            .whereEqualTo("read", false)
            .get()
            .await()

        if (unread.isEmpty) return

        val batch = firestore.batch()
        unread.documents.forEach { doc ->
            batch.update(doc.reference, "read", true)
        }
        batch.commit().await()
    }
}

package com.devjournal.data.model

import com.google.firebase.Timestamp

data class AppNotification(
    val id: String = "",
    val type: String = "",          // new_like, new_comment, comment_reply, follow, new_share, post_published
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val postId: String? = null,
    val senderUid: String? = null,
    val targetUid: String? = null,
    val createdAt: Timestamp? = null
)

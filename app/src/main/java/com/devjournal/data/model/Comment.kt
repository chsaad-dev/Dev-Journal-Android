package com.devjournal.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId var id: String = "",
    var userId: String = "",
    var text: String = "",
    var createdAt: Timestamp? = null,
    var parentCommentId: String? = null,
    var replyToUsername: String? = null
)

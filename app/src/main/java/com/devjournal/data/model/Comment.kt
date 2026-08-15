package com.devjournal.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

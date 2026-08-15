package com.devjournal.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Post(
    @DocumentId val id: String = "",
    val title: String = "",
    val slug: String = "",
    val content: String = "",
    val excerpt: String = "",
    val coverImageUrl: String = "",
    val coverImagePublicId: String = "",
    val authorId: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val published: Boolean = false,
    val readTimeMinutes: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0
)

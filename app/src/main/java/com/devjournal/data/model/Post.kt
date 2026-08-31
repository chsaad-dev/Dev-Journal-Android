package com.devjournal.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Post(
    @DocumentId var id: String = "",
    var title: String = "",
    var slug: String = "",
    var content: String = "",
    var excerpt: String = "",
    var coverImageUrl: String = "",
    var coverImagePublicId: String = "",
    var authorId: String = "",
    var tags: List<String> = emptyList(),
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
    var published: Boolean = false,
    var readTimeMinutes: Int = 0,
    var likeCount: Int = 0,
    var commentCount: Int = 0,
    var viewCount: Int = 0
)

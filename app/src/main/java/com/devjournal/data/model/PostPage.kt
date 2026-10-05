package com.devjournal.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class PostPage(
    val posts: List<Post> = emptyList(),
    val lastVisibleDocument: DocumentSnapshot? = null,
    val hasMore: Boolean = false
)

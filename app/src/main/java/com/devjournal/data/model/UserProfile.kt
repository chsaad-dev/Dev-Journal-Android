package com.devjournal.data.model

import com.google.firebase.firestore.DocumentId

data class UserProfile(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val role: String = "reader",
    val fcmTokens: List<String> = emptyList()
)

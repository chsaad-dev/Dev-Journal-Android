package com.devjournal.data.model

import com.google.firebase.firestore.DocumentId

data class UserProfile(
    @DocumentId var uid: String = "",
    var name: String = "",
    var email: String = "",
    var photoUrl: String = "",
    var bio: String = "",
    var role: String = "reader",
    var fcmTokens: List<String> = emptyList(),
    var followerCount: Int = 0,
    var followingCount: Int = 0,
    var suspended: Boolean = false
)

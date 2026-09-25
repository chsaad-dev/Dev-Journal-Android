package com.devjournal.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class PostViewer(
    @DocumentId var uid: String = "",
    var userName: String = "",
    var userPhotoUrl: String = "",
    var viewedAt: Timestamp? = null
)

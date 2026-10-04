package com.devjournal.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class BugReport(
    @DocumentId var id: String = "",
    var userId: String = "",
    var userEmail: String = "",
    var title: String = "",
    var description: String = "",
    var screenshotUrl: String = "",
    var deviceInfo: String = "",
    var appVersion: String = "",
    var status: String = "open",
    @ServerTimestamp var createdAt: Timestamp? = null,
    var resolvedAt: Timestamp? = null,
    var adminNotes: String = ""
)

package com.devjournal.data.repository

import com.devjournal.data.model.UserProfile
import com.devjournal.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override suspend fun getUserProfile(uid: String): UserProfile? {
        val snapshot = firestore.collection("users").document(uid).get().await()
        return snapshot.toObject(UserProfile::class.java)
    }

    override suspend fun createOrUpdateUserProfile(profile: UserProfile) {
        firestore.collection("users")
            .document(profile.uid)
            .set(profile, SetOptions.merge())
            .await()
    }

    override suspend fun updateFcmToken(uid: String, token: String) {
        firestore.collection("users")
            .document(uid)
            .update("fcmTokens", FieldValue.arrayUnion(token))
            .await()
    }
}

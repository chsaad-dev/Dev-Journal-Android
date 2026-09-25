package com.devjournal.data.repository

import com.devjournal.data.model.UserProfile
import com.devjournal.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
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

    override suspend fun followUser(currentUid: String, targetUid: String): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val currentUserRef = firestore.collection("users").document(currentUid)
            val targetUserRef = firestore.collection("users").document(targetUid)
            val followingRef = currentUserRef.collection("following").document(targetUid)
            val followerRef = targetUserRef.collection("followers").document(currentUid)

            transaction.set(followingRef, mapOf("followedAt" to FieldValue.serverTimestamp()))
            transaction.set(followerRef, mapOf("followedAt" to FieldValue.serverTimestamp()))
            transaction.update(targetUserRef, "followerCount", FieldValue.increment(1))
            transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
            null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unfollowUser(currentUid: String, targetUid: String): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            val currentUserRef = firestore.collection("users").document(currentUid)
            val targetUserRef = firestore.collection("users").document(targetUid)
            val followingRef = currentUserRef.collection("following").document(targetUid)
            val followerRef = targetUserRef.collection("followers").document(currentUid)

            transaction.delete(followingRef)
            transaction.delete(followerRef)
            transaction.update(targetUserRef, "followerCount", FieldValue.increment(-1))
            transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
            null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun isFollowing(currentUid: String, targetUid: String): Flow<Boolean> = callbackFlow {
        if (currentUid.isBlank() || targetUid.isBlank()) {
            trySend(false)
            return@callbackFlow
        }
        val docRef = firestore.collection("users").document(currentUid)
            .collection("following").document(targetUid)
        
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(false)
                return@addSnapshotListener
            }
            trySend(snapshot != null && snapshot.exists())
        }
        
        awaitClose { subscription.remove() }
    }

    override suspend fun getFollowers(uid: String): List<UserProfile> {
        return fetchRelatedUsers(uid, "followers")
    }

    override suspend fun getFollowing(uid: String): List<UserProfile> {
        return fetchRelatedUsers(uid, "following")
    }

    private suspend fun fetchRelatedUsers(uid: String, subcollection: String): List<UserProfile> {
        val snapshot = firestore.collection("users").document(uid)
            .collection(subcollection)
            .get()
            .await()

        val relatedUids = snapshot.documents.mapNotNull { it.id }
        if (relatedUids.isEmpty()) return emptyList()

        // Firestore whereIn supports max 30 items, batch if needed
        return relatedUids.chunked(30).flatMap { batch ->
            val usersSnapshot = firestore.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                .get()
                .await()
            usersSnapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
        }
    }
}

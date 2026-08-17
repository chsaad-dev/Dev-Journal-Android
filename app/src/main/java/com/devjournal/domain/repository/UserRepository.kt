package com.devjournal.domain.repository

import com.devjournal.data.model.UserProfile

interface UserRepository {
    suspend fun getUserProfile(uid: String): UserProfile?
    suspend fun createOrUpdateUserProfile(profile: UserProfile)
    suspend fun updateFcmToken(uid: String, token: String)
    suspend fun followUser(currentUid: String, targetUid: String): Result<Unit>
    suspend fun unfollowUser(currentUid: String, targetUid: String): Result<Unit>
    fun isFollowing(currentUid: String, targetUid: String): kotlinx.coroutines.flow.Flow<Boolean>
}

package com.devjournal.domain.repository

import com.devjournal.data.model.UserProfile

interface UserRepository {
    suspend fun getUserProfile(uid: String): UserProfile?
    suspend fun createOrUpdateUserProfile(profile: UserProfile)
    suspend fun updateFcmToken(uid: String, token: String)
}

package com.devjournal.domain.usecase

import com.devjournal.domain.repository.AuthRepository
import com.devjournal.domain.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke() {
        // Remove this device's FCM token from the signing-out user's
        // Firestore document BEFORE clearing the auth session, so
        // push notifications stop targeting this device for that account.
        try {
            val uid = authRepository.currentUser?.uid
            if (uid != null) {
                val token = FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    userRepository.removeFcmToken(uid, token)
                }
            }
        } catch (_: Exception) {
            // Best-effort: don't block sign-out if token removal fails
        }
        authRepository.signOut()
    }
}

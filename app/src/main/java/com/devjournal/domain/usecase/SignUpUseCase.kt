package com.devjournal.domain.usecase

import com.devjournal.data.model.UserProfile
import com.devjournal.domain.repository.AuthRepository
import com.devjournal.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<FirebaseUser> {
        val result = authRepository.signUpWithEmail(email, password)
        if (result.isSuccess) {
            result.getOrNull()?.let { user ->
                val defaultProfile = UserProfile(
                    uid = user.uid,
                    email = user.email ?: email,
                    name = user.displayName ?: email.substringBefore("@"),
                    role = "reader"
                )
                userRepository.createOrUpdateUserProfile(defaultProfile)
            }
        }
        return result
    }
}

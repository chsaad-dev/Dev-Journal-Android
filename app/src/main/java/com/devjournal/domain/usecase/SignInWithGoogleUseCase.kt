package com.devjournal.domain.usecase

import com.devjournal.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<FirebaseUser> {
        return authRepository.signInWithGoogle(idToken)
    }
}

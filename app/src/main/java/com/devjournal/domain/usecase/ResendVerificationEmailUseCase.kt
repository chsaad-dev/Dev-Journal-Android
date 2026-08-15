package com.devjournal.domain.usecase

import com.devjournal.domain.repository.AuthRepository
import javax.inject.Inject

class ResendVerificationEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return authRepository.resendVerificationEmail(email, password)
    }
}

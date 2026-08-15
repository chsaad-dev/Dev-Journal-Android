package com.devjournal.domain.usecase

import com.devjournal.domain.repository.UserRepository
import javax.inject.Inject

class RegisterFcmTokenUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(uid: String, token: String) {
        userRepository.updateFcmToken(uid, token)
    }
}

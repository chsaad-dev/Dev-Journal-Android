package com.devjournal.domain.usecase

import com.devjournal.domain.repository.UserRepository
import javax.inject.Inject

class CheckUsernameAvailabilityUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(username: String, currentUid: String): Boolean {
        return userRepository.isUsernameAvailable(username, currentUid)
    }
}

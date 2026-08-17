package com.devjournal.domain.usecase

import com.devjournal.domain.repository.UserRepository
import javax.inject.Inject

class UnfollowUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(currentUid: String, targetUid: String): Result<Unit> {
        if (currentUid.isBlank() || targetUid.isBlank() || currentUid == targetUid) {
            return Result.failure(IllegalArgumentException("Invalid IDs for unfollow operation"))
        }
        return userRepository.unfollowUser(currentUid, targetUid)
    }
}

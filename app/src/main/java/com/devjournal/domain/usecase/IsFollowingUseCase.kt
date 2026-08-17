package com.devjournal.domain.usecase

import com.devjournal.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsFollowingUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(currentUid: String, targetUid: String): Flow<Boolean> {
        return userRepository.isFollowing(currentUid, targetUid)
    }
}

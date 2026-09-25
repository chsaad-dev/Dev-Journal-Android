package com.devjournal.domain.usecase

import com.devjournal.data.model.UserProfile
import com.devjournal.domain.repository.UserRepository
import javax.inject.Inject

class GetFollowersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(uid: String): List<UserProfile> {
        return userRepository.getFollowers(uid)
    }
}

package com.devjournal.domain.usecase

import com.devjournal.data.model.UserProfile
import com.devjournal.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(uid: String): Flow<UserProfile?> {
        return userRepository.observeUserProfile(uid)
    }
}

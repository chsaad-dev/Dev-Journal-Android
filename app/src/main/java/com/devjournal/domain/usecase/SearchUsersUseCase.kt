package com.devjournal.domain.usecase

import com.devjournal.data.model.UserProfile
import com.devjournal.domain.repository.UserRepository
import javax.inject.Inject

class SearchUsersUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(query: String): List<UserProfile> {
        return userRepository.searchUsers(query)
    }
}

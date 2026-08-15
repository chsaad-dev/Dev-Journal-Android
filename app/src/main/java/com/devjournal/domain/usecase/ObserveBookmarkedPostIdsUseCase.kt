package com.devjournal.domain.usecase

import com.devjournal.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBookmarkedPostIdsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    operator fun invoke(uid: String): Flow<Set<String>> {
        return postRepository.observeBookmarkedPostIds(uid)
    }
}

package com.devjournal.domain.usecase

import com.devjournal.domain.repository.PostRepository
import javax.inject.Inject

class BookmarkPostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String, uid: String, alreadyBookmarked: Boolean) {
        if (alreadyBookmarked) {
            postRepository.unbookmarkPost(postId, uid)
        } else {
            postRepository.bookmarkPost(postId, uid)
        }
    }
}

package com.devjournal.domain.usecase

import com.devjournal.data.model.PostViewer
import com.devjournal.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPostViewersUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(postId: String): Flow<List<PostViewer>> {
        return repository.getPostViewers(postId)
    }
}

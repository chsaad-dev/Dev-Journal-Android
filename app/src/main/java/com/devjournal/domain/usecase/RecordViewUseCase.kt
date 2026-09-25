package com.devjournal.domain.usecase

import com.devjournal.domain.repository.PostRepository
import javax.inject.Inject

class RecordViewUseCase @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(
        postId: String, 
        uid: String, 
        userName: String? = null, 
        userPhotoUrl: String? = null
    ) {
        repository.recordView(postId, uid, userName, userPhotoUrl)
    }
}

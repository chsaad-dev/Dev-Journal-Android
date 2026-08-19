package com.devjournal.presentation.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.data.remote.CloudinaryUploader
import com.devjournal.data.remote.NotifyWorkerApi
import com.devjournal.domain.usecase.CreatePostUseCase
import com.devjournal.domain.usecase.GetDraftUseCase
import com.devjournal.domain.usecase.GetDraftByIdUseCase
import com.devjournal.domain.usecase.SaveDraftUseCase
import com.devjournal.domain.usecase.DeleteDraftUseCase
import com.devjournal.data.model.local.DraftEntity
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.UpdatePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostEditorUiState(
    val title: String = "",
    val content: String = "",
    val excerpt: String = "",
    val tagsInput: String = "",
    val tags: List<String> = emptyList(),
    val coverImageUrl: String = "",
    val coverImagePublicId: String = "",
    val pendingCoverUri: Uri? = null,
    val isUploadingCover: Boolean = false,
    val published: Boolean = true,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val localDraftId: String? = null,
    val currentUserId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PostEditorViewModel @Inject constructor(
    private val createPostUseCase: CreatePostUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val getDraftUseCase: GetDraftUseCase,
    private val getLocalDraftUseCase: GetDraftByIdUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val deleteDraftUseCase: DeleteDraftUseCase,
    private val cloudinaryUploader: CloudinaryUploader,
    private val notifyWorkerApi: NotifyWorkerApi,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val postId: String? = savedStateHandle.get<String>("postId")?.takeIf { it.isNotBlank() && it != "{postId}" }
    val draftId: String? = savedStateHandle.get<String>("draftId")?.takeIf { it.isNotBlank() && it != "{draftId}" }
    val effectivePostId: String? = postId ?: draftId

    private val _uiState = MutableStateFlow(PostEditorUiState(isEditMode = effectivePostId != null, localDraftId = draftId ?: (postId ?: java.util.UUID.randomUUID().toString())))
    val uiState: StateFlow<PostEditorUiState> = _uiState.asStateFlow()
    
    private var autoSaveJob: Job? = null

    init {
        observeCurrentUser()
        if (!postId.isNullOrBlank()) {
            loadExistingPost(postId)
        } else if (!draftId.isNullOrBlank()) {
            loadExistingPost(draftId)
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                _uiState.update { it.copy(currentUserId = user?.uid) }
            }
        }
    }

    private fun loadExistingPost(id: String) {
        viewModelScope.launch {
            val post = getDraftUseCase(id)
            if (post != null) {
                _uiState.update {
                    it.copy(
                        title = post.title,
                        content = post.content,
                        excerpt = post.excerpt,
                        tags = post.tags,
                        coverImageUrl = post.coverImageUrl,
                        coverImagePublicId = post.coverImagePublicId,
                        published = post.published,
                        isEditMode = true
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Could not load post draft") }
            }
        }
    }

    private fun loadLocalDraft(id: String) {
        viewModelScope.launch {
            val draft = getLocalDraftUseCase(id)
            if (draft != null) {
                _uiState.update {
                    it.copy(
                        title = draft.title,
                        content = draft.content,
                        excerpt = draft.excerpt,
                        tags = draft.tags.split(",").filter { t -> t.isNotBlank() },
                        coverImageUrl = draft.coverImageUri,
                        isEditMode = false // Assume new post if loaded from local drafts, unless we want to track remote ID
                    )
                }
            }
        }
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000) // Debounce 2 seconds
            val state = _uiState.value
            if (state.title.isNotBlank() || state.content.isNotBlank()) {
                val draft = DraftEntity(
                    id = state.localDraftId ?: java.util.UUID.randomUUID().toString(),
                    title = state.title,
                    content = state.content,
                    excerpt = state.excerpt,
                    tags = state.tags.joinToString(","),
                    coverImageUri = state.coverImageUrl,
                    lastUpdated = System.currentTimeMillis()
                )
                saveDraftUseCase(draft)
            }
        }
    }

    fun onTitleChange(text: String) {
        _uiState.update { it.copy(title = text, errorMessage = null) }
        triggerAutoSave()
    }

    fun onContentChange(text: String) {
        _uiState.update { it.copy(content = text, errorMessage = null) }
        triggerAutoSave()
    }

    fun onExcerptChange(text: String) {
        _uiState.update { it.copy(excerpt = text, errorMessage = null) }
    }

    fun onTagsInputChange(text: String) {
        if (text.contains(",")) {
            val parts = text.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
            val currentTags = _uiState.value.tags.toMutableList()
            parts.forEach { part ->
                if (!currentTags.contains(part)) {
                    currentTags.add(part)
                }
            }
            _uiState.update { it.copy(tags = currentTags, tagsInput = "") }
        } else {
            _uiState.update { it.copy(tagsInput = text) }
        }
    }

    fun onAddCurrentTag() {
        val raw = _uiState.value.tagsInput.trim().removePrefix("#")
        if (raw.isNotBlank()) {
            val currentTags = _uiState.value.tags.toMutableList()
            if (!currentTags.contains(raw)) {
                currentTags.add(raw)
            }
            _uiState.update { it.copy(tags = currentTags, tagsInput = "") }
        }
    }

    fun onRemoveTag(tag: String) {
        val currentTags = _uiState.value.tags.toMutableList()
        currentTags.remove(tag)
        _uiState.update { it.copy(tags = currentTags) }
        triggerAutoSave()
    }

    fun onCoverImageSelected(uri: Uri) {
        // Store the local URI only; upload happens on Save/Post
        _uiState.update {
            it.copy(
                pendingCoverUri = uri,
                coverImageUrl = uri.toString(),
                errorMessage = null
            )
        }
        triggerAutoSave()
    }

    fun onPublishedToggle(value: Boolean) {
        _uiState.update { it.copy(published = value) }
    }

    fun onSaveClick(onSuccess: () -> Unit) {
        val state = _uiState.value
        val title = state.title.trim()
        val content = state.content.trim()

        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
            return
        }
        if (content.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Content cannot be empty") }
            return
        }

        // Commit any pending tag
        val finalTags = state.tags.toMutableList()
        val pendingTag = state.tagsInput.trim().removePrefix("#")
        if (pendingTag.isNotBlank() && !finalTags.contains(pendingTag)) {
            finalTags.add(pendingTag)
        }

        val excerpt = if (state.excerpt.isNotBlank()) state.excerpt.trim() else content.take(150)
        val authorId = state.currentUserId ?: ""

        val post = Post(
            id = effectivePostId ?: "",
            title = title,
            content = content,
            excerpt = excerpt,
            coverImageUrl = state.coverImageUrl,
            coverImagePublicId = state.coverImagePublicId,
            authorId = authorId,
            tags = finalTags,
            published = state.published
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                // Upload pending cover image to Cloudinary now (deferred from selection)
                var finalCoverUrl = post.coverImageUrl
                var finalCoverPublicId = post.coverImagePublicId
                val pendingUri = state.pendingCoverUri
                if (pendingUri != null) {
                    _uiState.update { it.copy(isUploadingCover = true) }
                    val uploadResult = cloudinaryUploader.uploadImage(pendingUri)
                    uploadResult.onSuccess { secureUrl ->
                        finalCoverUrl = secureUrl
                        _uiState.update { it.copy(isUploadingCover = false, pendingCoverUri = null, coverImageUrl = secureUrl) }
                    }.onFailure { error ->
                        _uiState.update { it.copy(isSaving = false, isUploadingCover = false, errorMessage = "Cover upload failed: ${error.localizedMessage}") }
                        return@launch
                    }
                }

                val finalPost = post.copy(coverImageUrl = finalCoverUrl, coverImagePublicId = finalCoverPublicId)

                if (state.isEditMode && !effectivePostId.isNullOrBlank()) {
                    val result = updatePostUseCase(effectivePostId, finalPost)
                    result.onSuccess {
                        _uiState.update { it.copy(isSaving = false) }
                        onSuccess()
                    }.onFailure { e ->
                        _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to update: ${e.message}") }
                    }
                } else {
                    val result = createPostUseCase(finalPost)
                    result.onSuccess { _ ->
                        if (state.published) {
                            try {
                                notifyWorkerApi.sendNotification(
                                    type = "new_post",
                                    targetUid = "",
                                    title = "New post published: $title",
                                    body = excerpt
                                )
                            } catch (_: Exception) {}
                        }
                        
                        // Delete local draft on success
                        state.localDraftId?.let { deleteDraftUseCase(it) }
                        
                        _uiState.update { it.copy(isSaving = false) }
                        onSuccess()
                    }.onFailure { e ->
                        _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to create: ${e.message}") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Unexpected error: ${e.message}") }
            }
        }
    }
}

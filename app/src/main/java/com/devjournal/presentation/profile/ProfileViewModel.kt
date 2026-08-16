package com.devjournal.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.data.model.UserProfile
import com.devjournal.data.remote.CloudinaryUploader
import com.devjournal.domain.usecase.GetLikedPostsUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.SignOutUseCase
import com.devjournal.domain.usecase.UpdateUserProfileUseCase
import com.devjournal.domain.usecase.GetBookmarkedPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val likedPosts: List<Post> = emptyList(),
    val bookmarkedPosts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editBio: String = "",
    val isUploadingPhoto: Boolean = false,
    val isSignedOut: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val getLikedPostsUseCase: GetLikedPostsUseCase,
    private val getBookmarkedPostsUseCase: GetBookmarkedPostsUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val cloudinaryUploader: CloudinaryUploader,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var profileJob: kotlinx.coroutines.Job? = null

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                if (user == null) {
                    profileJob?.cancel()
                    _uiState.update {
                        it.copy(
                            isSignedOut = true,
                            isLoading = false,
                            likedPosts = emptyList(),
                            bookmarkedPosts = emptyList(),
                            profile = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSignedOut = false) }
                    loadProfileAndLikedPosts(user.uid, user.email, user.displayName, user.photoUrl?.toString())
                }
            }
        }
    }

    private fun loadProfileAndLikedPosts(uid: String, email: String?, displayName: String?, photoUrl: String?) {
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            val existingProfile = getUserProfileUseCase(uid) ?: UserProfile(
                uid = uid,
                email = email ?: "",
                name = displayName ?: email?.substringBefore("@") ?: "Developer",
                photoUrl = photoUrl ?: "",
                role = "reader"
            )

            _uiState.update {
                it.copy(
                    profile = existingProfile,
                    editName = existingProfile.name,
                    editBio = existingProfile.bio,
                    isLoading = false
                )
            }

            launch {
                getLikedPostsUseCase(uid)
                    .catch { /* ignore on signout */ }
                    .collect { liked ->
                        _uiState.update { it.copy(likedPosts = liked) }
                    }
            }

            launch {
                getBookmarkedPostsUseCase(uid)
                    .catch { /* ignore on signout */ }
                    .collect { bookmarked ->
                        _uiState.update { it.copy(bookmarkedPosts = bookmarked) }
                    }
            }
        }
    }

    fun onEditClick() {
        val currentProfile = _uiState.value.profile
        _uiState.update {
            it.copy(
                isEditing = !it.isEditing,
                editName = currentProfile?.name ?: "",
                editBio = currentProfile?.bio ?: "",
                errorMessage = null
            )
        }
    }

    fun onCancelEdit() {
        val currentProfile = _uiState.value.profile
        _uiState.update {
            it.copy(
                isEditing = false,
                editName = currentProfile?.name ?: "",
                editBio = currentProfile?.bio ?: "",
                errorMessage = null
            )
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun onBioChange(bio: String) {
        _uiState.update { it.copy(editBio = bio) }
    }

    fun onSaveProfileClick() {
        val currentProfile = _uiState.value.profile ?: return
        val newName = _uiState.value.editName.trim()
        val newBio = _uiState.value.editBio.trim()

        if (newName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val updated = currentProfile.copy(name = newName, bio = newBio)
            val result = updateUserProfileUseCase(updated)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        profile = updated,
                        isEditing = false,
                        isSaving = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to update profile: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onPhotoSelected(uri: Uri) {
        val currentProfile = _uiState.value.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, errorMessage = null) }
            val result = cloudinaryUploader.uploadImage(uri)
            result.onSuccess { photoUrl ->
                val updated = currentProfile.copy(photoUrl = photoUrl)
                updateUserProfileUseCase(updated)
                _uiState.update {
                    it.copy(
                        profile = updated,
                        isUploadingPhoto = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUploadingPhoto = false,
                        errorMessage = "Photo upload failed: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onSignOutClick() {
        profileJob?.cancel()
        signOutUseCase()
        _uiState.update { it.copy(isSignedOut = true) }
    }
}

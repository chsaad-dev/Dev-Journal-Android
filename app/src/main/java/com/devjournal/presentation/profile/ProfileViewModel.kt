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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { user ->
                if (user == null) {
                    _uiState.update { it.copy(isSignedOut = true, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isSignedOut = false) }
                    loadProfileAndLikedPosts(user.uid, user.email, user.displayName, user.photoUrl?.toString())
                }
            }
        }
    }

    private fun loadProfileAndLikedPosts(uid: String, email: String?, displayName: String?, photoUrl: String?) {
        viewModelScope.launch {
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
                getLikedPostsUseCase(uid).collect { liked ->
                    _uiState.update { it.copy(likedPosts = liked) }
                }
            }

            launch {
                getBookmarkedPostsUseCase(uid).collect { bookmarked ->
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

    fun onNameChange(text: String) {
        _uiState.update { it.copy(editName = text, errorMessage = null) }
    }

    fun onBioChange(text: String) {
        _uiState.update { it.copy(editBio = text, errorMessage = null) }
    }

    fun onSaveClick() {
        val currentProfile = _uiState.value.profile ?: return
        val newName = _uiState.value.editName.trim()
        val newBio = _uiState.value.editBio.trim()

        if (newName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            val updated = currentProfile.copy(name = newName, bio = newBio)
            try {
                updateUserProfileUseCase(updated)
                _uiState.update {
                    it.copy(
                        profile = updated,
                        isEditing = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update profile: ${e.message}") }
            }
        }
    }

    fun onPhotoSelected(uri: Uri) {
        val currentProfile = _uiState.value.profile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, errorMessage = null) }
            val uploadResult = cloudinaryUploader.uploadImage(uri)
            uploadResult.onSuccess { secureUrl ->
                val updated = currentProfile.copy(photoUrl = secureUrl)
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
        signOutUseCase()
        _uiState.update { it.copy(isSignedOut = true) }
    }
}

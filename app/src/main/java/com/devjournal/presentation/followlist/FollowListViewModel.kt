package com.devjournal.presentation.followlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.UserProfile
import com.devjournal.data.remote.NotifyWorkerApi
import com.devjournal.domain.usecase.FollowUserUseCase
import com.devjournal.domain.usecase.GetFollowersUseCase
import com.devjournal.domain.usecase.GetFollowingUseCase
import com.devjournal.domain.usecase.GetUserProfileUseCase
import com.devjournal.domain.usecase.IsFollowingUseCase
import com.devjournal.domain.usecase.ObserveAuthStateUseCase
import com.devjournal.domain.usecase.UnfollowUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FollowListType { FOLLOWERS, FOLLOWING }

data class FollowListUiState(
    val users: List<UserProfile> = emptyList(),
    val followStatusMap: Map<String, Boolean> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val title: String = "Followers",
    val targetUid: String = "",
    val listType: FollowListType = FollowListType.FOLLOWERS,
    val currentUserUid: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val isFollowingUseCase: IsFollowingUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val notifyWorkerApi: NotifyWorkerApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    private val navUid: String = savedStateHandle.get<String>("uid") ?: ""
    private val navType: String = savedStateHandle.get<String>("type") ?: "followers"

    init {
        val listType = if (navType == "following") FollowListType.FOLLOWING else FollowListType.FOLLOWERS
        _uiState.update {
            it.copy(
                targetUid = navUid,
                listType = listType,
                title = if (listType == FollowListType.FOLLOWERS) "Followers" else "Following"
            )
        }

        viewModelScope.launch {
            val user = observeAuthStateUseCase().firstOrNull()
            val currentUid = user?.uid ?: ""
            _uiState.update { it.copy(currentUserUid = currentUid) }
            loadUsers(currentUid)
        }
    }

    fun refresh() {
        val currentUid = _uiState.value.currentUserUid
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            loadUsers(currentUid)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun loadUsers(currentUid: String) {
        try {
            val users = when (_uiState.value.listType) {
                FollowListType.FOLLOWERS -> getFollowersUseCase(navUid)
                FollowListType.FOLLOWING -> getFollowingUseCase(navUid)
            }

            _uiState.update { it.copy(users = users, isLoading = false, errorMessage = null) }

            // Check follow status for each user (against current logged-in user)
            if (currentUid.isNotBlank()) {
                kotlinx.coroutines.coroutineScope {
                    users.forEach { profile ->
                        if (profile.uid != currentUid) {
                            launch {
                                isFollowingUseCase(currentUid, profile.uid).collect { isFollowing ->
                                    _uiState.update { state ->
                                        state.copy(
                                            followStatusMap = state.followStatusMap + (profile.uid to isFollowing)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Failed to load: ${e.localizedMessage}"
                )
            }
        }
    }

    fun onFollowClick(targetUid: String) {
        val currentUid = _uiState.value.currentUserUid
        if (currentUid.isBlank() || currentUid == targetUid) return

        val currentlyFollowing = _uiState.value.followStatusMap[targetUid] ?: false

        // Optimistic update
        _uiState.update { state ->
            state.copy(
                followStatusMap = state.followStatusMap + (targetUid to !currentlyFollowing)
            )
        }

        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                unfollowUserUseCase(currentUid, targetUid)
            } else {
                followUserUseCase(currentUid, targetUid)
            }

            result.onSuccess {
                // Fire follow notification only on the follow action (not unfollow)
                if (!currentlyFollowing) {
                    try {
                        val senderName = getUserProfileUseCase(currentUid)?.name?.ifBlank { null }
                            ?: observeAuthStateUseCase().firstOrNull()?.displayName?.ifBlank { null }
                            ?: "Someone"
                        notifyWorkerApi.sendNotification(
                            type = "follow",
                            targetUid = targetUid,
                            title = "$senderName started following you",
                            body = "Tap to view their profile",
                            data = mapOf("senderUid" to currentUid)
                        )
                    } catch (_: Exception) { /* best-effort */ }
                }
            }

            result.onFailure {
                // Revert on failure
                _uiState.update { state ->
                    state.copy(
                        followStatusMap = state.followStatusMap + (targetUid to currentlyFollowing),
                        errorMessage = "Failed to update follow status"
                    )
                }
            }
        }
    }
}

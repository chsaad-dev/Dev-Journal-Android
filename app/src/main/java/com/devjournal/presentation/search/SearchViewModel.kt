package com.devjournal.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.data.model.UserProfile
import com.devjournal.domain.usecase.GetPostsUseCase
import com.devjournal.domain.usecase.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchTab {
    ARTICLES,
    ACCOUNTS
}

data class SearchUiState(
    val searchQuery: String = "",
    val selectedTab: SearchTab = SearchTab.ARTICLES,
    val allPosts: List<Post> = emptyList(),
    val searchResults: List<Post> = emptyList(),
    val accountResults: List<UserProfile> = emptyList(),
    val isLoading: Boolean = true,
    val isSearchingAccounts: Boolean = false,
    val popularTags: List<String> = listOf("Android", "Compose", "Kotlin", "Architecture", "Firebase")
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val searchUsersUseCase: SearchUsersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var accountSearchJob: Job? = null

    init {
        loadPosts()
        searchAccounts("")
    }

    private fun loadPosts() {
        viewModelScope.launch {
            getPostsUseCase().collect { postsList ->
                _uiState.update { state ->
                    val filtered = filterPosts(postsList, state.searchQuery)
                    state.copy(
                        allPosts = postsList,
                        searchResults = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onTabSelected(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == SearchTab.ACCOUNTS && _uiState.value.accountResults.isEmpty()) {
            searchAccounts(_uiState.value.searchQuery)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                searchResults = filterPosts(state.allPosts, query)
            )
        }
        searchAccounts(query)
    }

    fun onTagClicked(tag: String) {
        _uiState.update { it.copy(selectedTab = SearchTab.ARTICLES) }
        onSearchQueryChanged(tag)
    }

    private fun searchAccounts(query: String) {
        accountSearchJob?.cancel()
        accountSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingAccounts = true) }
            try {
                val results = searchUsersUseCase(query)
                _uiState.update { it.copy(accountResults = results, isSearchingAccounts = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingAccounts = false) }
            }
        }
    }

    private fun filterPosts(posts: List<Post>, query: String): List<Post> {
        if (query.isBlank()) {
            return posts
        }
        val lowercaseQuery = query.lowercase()
        return posts.filter { post ->
            post.title.lowercase().contains(lowercaseQuery) ||
            post.content.lowercase().contains(lowercaseQuery) ||
            post.tags.any { it.lowercase().contains(lowercaseQuery) || it.lowercase().replace("#", "").contains(lowercaseQuery) }
        }
    }
}

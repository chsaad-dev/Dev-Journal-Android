package com.devjournal.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devjournal.data.model.Post
import com.devjournal.domain.usecase.GetPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val searchQuery: String = "",
    val allPosts: List<Post> = emptyList(),
    val searchResults: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val popularTags: List<String> = listOf("Android", "Compose", "Kotlin", "Architecture", "Firebase")
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
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

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                searchResults = filterPosts(state.allPosts, query)
            )
        }
    }

    fun onTagClicked(tag: String) {
        onSearchQueryChanged(tag)
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

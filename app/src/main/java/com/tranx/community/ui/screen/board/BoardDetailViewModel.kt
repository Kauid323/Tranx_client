package com.tranx.community.ui.screen.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.Board
import com.tranx.community.data.model.CreateFolderRequest
import com.tranx.community.data.model.Folder
import com.tranx.community.data.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BoardDetailUiState {
    object Loading : BoardDetailUiState()
    data class Success(val board: Board, val posts: List<Post>) : BoardDetailUiState()
    data class Error(val message: String) : BoardDetailUiState()
}

class BoardDetailViewModel : ViewModel() {
    private val prefs = TranxApp.instance.preferencesManager

    private val _uiState = MutableStateFlow<BoardDetailUiState>(BoardDetailUiState.Loading)
    val uiState: StateFlow<BoardDetailUiState> = _uiState.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    fun loadBoard(boardId: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = BoardDetailUiState.Loading
                val token = prefs.getToken() ?: throw Exception("未登录")
                val api = RetrofitClient.getApiService()

                val boardResponse = api.getBoard(token, boardId)
                if (boardResponse.code != 200 || boardResponse.data == null) {
                    throw Exception(boardResponse.message)
                }

                val postsResponse = api.getPostList(
                    token = token,
                    boardId = boardId,
                    sort = "latest"
                )
                val posts = if (postsResponse.code == 200 && postsResponse.data != null) {
                    postsResponse.data.list ?: emptyList()
                } else {
                    emptyList()
                }

                _uiState.value = BoardDetailUiState.Success(boardResponse.data, posts)
            } catch (e: Exception) {
                _uiState.value = BoardDetailUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            try {
                val token = prefs.getToken() ?: return@launch
                val api = RetrofitClient.getApiService()
                val response = api.likePost(token, postId)
                if (response.code == 200 && response.data != null) {
                    updatePost(postId) { post ->
                        post.copy(likes = response.data.likes, isLiked = response.data.isLiked)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun coinPost(postId: Int, amount: Int, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefs.getToken() ?: return@launch
                val api = RetrofitClient.getApiService()
                val response = api.coinPost(token, postId, mapOf("amount" to amount))
                if (response.code == 200) {
                    val coins = response.data?.coins ?: amount
                    updatePost(postId) { post -> post.copy(coins = coins) }
                    onResult(true, null)
                } else {
                    onResult(false, response.message)
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            try {
                val token = prefs.getToken() ?: return@launch
                val api = RetrofitClient.getApiService()
                val response = api.getMyFolders(token)
                if (response.code == 200 && response.data != null) {
                    _folders.value = response.data
                }
            } catch (_: Exception) {
            }
        }
    }

    fun createFolder(name: String, description: String?, isPublic: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefs.getToken() ?: return@launch
                val api = RetrofitClient.getApiService()
                val response = api.createFolder(token, CreateFolderRequest(name, description, isPublic))
                if (response.code == 200) {
                    loadFolders()
                    onComplete()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun addPostToFolder(postId: Int, folderId: Int, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefs.getToken() ?: return@launch
                val api = RetrofitClient.getApiService()
                val response = api.addPostToFolder(token, folderId, mapOf("post_id" to postId))
                if (response.code == 200) {
                    updatePost(postId) { post -> post.copy(favorites = post.favorites + 1, isFavorited = true) }
                    onResult(true, null)
                } else {
                    onResult(false, response.message)
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    private fun updatePost(postId: Int, transform: (Post) -> Post) {
        val currentState = _uiState.value
        if (currentState is BoardDetailUiState.Success) {
            val updated = currentState.posts.map { post ->
                if (post.id == postId) transform(post) else post
            }
            _uiState.value = currentState.copy(posts = updated)
        }
    }
}


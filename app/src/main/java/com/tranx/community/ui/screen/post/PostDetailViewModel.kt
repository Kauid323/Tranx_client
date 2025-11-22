package com.tranx.community.ui.screen.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.Comment
import com.tranx.community.data.model.CreateCommentRequest
import com.tranx.community.data.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PostDetailUiState {
    object Loading : PostDetailUiState()
    data class Success(val post: Post, val comments: List<Comment> = emptyList()) : PostDetailUiState()
    data class Error(val message: String) : PostDetailUiState()
}

class PostDetailViewModel : ViewModel() {
    private val prefsManager = TranxApp.instance.preferencesManager

    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited.asStateFlow()

    fun loadPost(postId: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = PostDetailUiState.Loading

                val token = prefsManager.getToken() ?: throw Exception("未登录")
                val apiService = RetrofitClient.getApiService()

                // 加载帖子详情
                val postResponse = apiService.getPost(token, postId)
                if (postResponse.code != 200 || postResponse.data == null) {
                    throw Exception(postResponse.message)
                }

                // 加载评论列表
                val comments = try {
                    val commentsResponse = apiService.getCommentList(
                        token = token,
                        postId = postId
                    )
                    if (commentsResponse.code == 200 && commentsResponse.data != null) {
                        commentsResponse.data.list
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }

                _uiState.value = PostDetailUiState.Success(postResponse.data, comments)
            } catch (e: Exception) {
                _uiState.value = PostDetailUiState.Error("加载失败: ${e.message}")
            }
        }
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                
                if (_isLiked.value) {
                    apiService.unlikePost(token, postId)
                    _isLiked.value = false
                } else {
                    apiService.likePost(token, postId)
                    _isLiked.value = true
                }
                
                loadPost(postId)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun favoritePost(postId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.favoritePost(token, postId)
                
                if (response.code == 200) {
                    _isFavorited.value = true
                    onSuccess()
                    loadPost(postId)
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("收藏失败: ${e.message}")
            }
        }
    }

    fun coinPost(postId: Int, amount: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.coinPost(token, postId, mapOf("amount" to amount))
                
                if (response.code == 200) {
                    onSuccess()
                    loadPost(postId)
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("投币失败: ${e.message}")
            }
        }
    }

    fun coinComment(commentId: Int, postId: Int, amount: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.coinComment(token, commentId, mapOf("amount" to amount))
                
                if (response.code == 200) {
                    onSuccess()
                    loadPost(postId)
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("投币失败: ${e.message}")
            }
        }
    }

    fun addComment(postId: Int, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.createComment(
                    token,
                    CreateCommentRequest(postId, content)
                )

                if (response.code == 200) {
                    onSuccess()
                    loadPost(postId)
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun likeComment(commentId: Int, postId: Int) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                apiService.likeComment(token, commentId)
                loadPost(postId)
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
}


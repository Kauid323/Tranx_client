package com.tranx.community.ui.screen.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.*
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

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _commentReplies = MutableStateFlow<Map<Int, List<Comment>>>(emptyMap())
    val commentReplies: StateFlow<Map<Int, List<Comment>>> = _commentReplies.asStateFlow()

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
                _isLiked.value = postResponse.data.isLiked == true
                _isFavorited.value = postResponse.data.isFavorited == true

                // 加载评论列表
                val comments = try {
                    val commentsResponse = apiService.getCommentList(
                        token = token,
                        postId = postId
                    )
                    if (commentsResponse.code == 200 && commentsResponse.data != null) {
                        commentsResponse.data.list ?: emptyList()  // 处理list为null的情况
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
                val response = apiService.likePost(token, postId)
                if (response.code == 200 && response.data != null) {
                    _isLiked.value = response.data.isLiked
                    updatePostState { current ->
                        current.copy(likes = response.data.likes)
                    }
                }
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
                    updatePostState { post ->
                        post.copy(favorites = post.favorites + 1, isFavorited = true)
                    }
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
                    val coins = response.data?.coins ?: amount
                    updatePostState { post ->
                        post.copy(coins = coins)
                    }
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
                    val coins = response.data?.coins ?: amount
                    updateComments { list ->
                        list.map { comment ->
                            if (comment.id == commentId) {
                                comment.copy(coins = coins)
                            } else comment
                        }
                    }
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("投币失败: ${e.message}")
            }
        }
    }

    fun addComment(postId: Int, content: String, parentId: Int? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.createComment(
                    token,
                    CreateCommentRequest(postId, parentId, content)
                )

                if (response.code == 200) {
                    onSuccess()
                    if (parentId != null) {
                        loadCommentReplies(parentId)
                    } else {
                        loadPost(postId)
                    }
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun deletePost(postId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.deletePost(token, postId)
                
                if (response.code == 200) {
                    onSuccess()
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("删除失败: ${e.message}")
            }
        }
    }

    fun deleteComment(commentId: Int, postId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.deleteComment(token, commentId)
                
                if (response.code == 200) {
                    onSuccess()
                    loadPost(postId)
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("删除失败: ${e.message}")
            }
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.getMyFolders(token)
                
                if (response.code == 200 && response.data != null) {
                    _folders.value = response.data
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun createFolder(name: String, description: String?, isPublic: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.createFolder(
                    token,
                    CreateFolderRequest(name, description, isPublic)
                )
                
                if (response.code == 200) {
                    onSuccess()
                    loadFolders()
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("创建失败: ${e.message}")
            }
        }
    }

    fun addPostToFolder(postId: Int, folderId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.addPostToFolder(
                    token,
                    folderId,
                    mapOf("post_id" to postId)
                )
                
                if (response.code == 200) {
                    onSuccess()
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("收藏失败: ${e.message}")
            }
        }
    }

    fun loadCommentReplies(commentId: Int) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.getCommentReplies(token, commentId)
                
                if (response.code == 200 && response.data != null) {
                    val currentReplies = _commentReplies.value.toMutableMap()
                    currentReplies[commentId] = response.data.list ?: emptyList()
                    _commentReplies.value = currentReplies
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
                val response = apiService.likeComment(token, commentId)
                if (response.code == 200 && response.data != null) {
                    updateComments { list ->
                        list.map { comment ->
                            if (comment.id == commentId) {
                                comment.copy(
                                    likes = response.data.likes,
                                    isLiked = response.data.isLiked
                                )
                            } else comment
                        }
                    }
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    private fun updatePostState(transform: (Post) -> Post) {
        val currentState = _uiState.value
        if (currentState is PostDetailUiState.Success) {
            _uiState.value = currentState.copy(post = transform(currentState.post))
        }
    }

    private fun updateComments(transform: (List<Comment>) -> List<Comment>) {
        val currentState = _uiState.value
        if (currentState is PostDetailUiState.Success) {
            _uiState.value = currentState.copy(comments = transform(currentState.comments))
        }
    }
}


package com.tranx.community.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.CreateFolderRequest
import com.tranx.community.data.model.Folder
import com.tranx.community.data.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val posts: List<Post>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel : ViewModel() {
    private val prefsManager = TranxApp.instance.preferencesManager

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _sortType = MutableStateFlow("latest")
    val sortType: StateFlow<String> = _sortType.asStateFlow()

    // 跟踪已点赞的帖子
    private val _likedPosts = MutableStateFlow<Set<Int>>(emptySet())
    val likedPosts: StateFlow<Set<Int>> = _likedPosts.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = HomeUiState.Loading

                val token = prefsManager.getToken() ?: throw Exception("未登录")
                
                // 确保RetrofitClient已初始化
                if (!RetrofitClient.isInitialized()) {
                    RetrofitClient.initialize(prefsManager.getServerUrl())
                }
                
                println("加载数据，服务器地址: ${RetrofitClient.getCurrentBaseUrl()}")
                println("Token: $token")
                
                val apiService = RetrofitClient.getApiService()

                // 加载板块列表
                // 加载帖子列表
                val postsResponse = try {
                    apiService.getPostList(
                        token = token,
                        sort = _sortType.value
                    )
                } catch (e: Exception) {
                    println("帖子加载失败: ${e.message}")
                    e.printStackTrace()
                    throw Exception("加载帖子失败: ${e.message}")
                }

                val posts = if (postsResponse.code == 200 && postsResponse.data != null) {
                    postsResponse.data.list ?: emptyList()
                } else {
                    println("帖子响应错误: code=${postsResponse.code}, message=${postsResponse.message}")
                    throw Exception(postsResponse.message ?: "加载帖子失败")
                }

                _uiState.value = HomeUiState.Success(posts)
            } catch (e: Exception) {
                println("HomeViewModel loadData 异常: ${e.message}")
                e.printStackTrace()
                _uiState.value = HomeUiState.Error("加载失败: ${e.message}")
            }
        }
    }

    fun changeSortType(sort: String) {
        _sortType.value = sort
        loadData()
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()

                val response = apiService.likePost(token, postId)
                if (response.code == 200 && response.data != null) {
                    if (response.data.isLiked) {
                        _likedPosts.value = _likedPosts.value + postId
                    } else {
                        _likedPosts.value = _likedPosts.value - postId
                    }
                    updatePost(postId) { post ->
                        post.copy(likes = response.data.likes, isLiked = response.data.isLiked)
                    }
                }
            } catch (e: Exception) {
                println("点赞操作失败: ${e.message}")
            }
        }
    }

    fun isPostLiked(postId: Int): Boolean {
        return _likedPosts.value.contains(postId)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken()
                if (token != null) {
                    val apiService = RetrofitClient.getApiService()
                    apiService.logout(token)
                }
            } catch (e: Exception) {
                // 忽略错误
            } finally {
                prefsManager.clearAll()
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
            } catch (_: Exception) {
            }
        }
    }

    fun addPostToFolder(postId: Int, folderId: Int, onResult: (Boolean, String?) -> Unit) {
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

    fun createFolder(name: String, description: String?, isPublic: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.createFolder(token, CreateFolderRequest(name, description, isPublic))
                if (response.code == 200) {
                    loadFolders()
                    onComplete()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun coinPost(postId: Int, amount: Int, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.coinPost(token, postId, mapOf("amount" to amount))
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

    private fun updatePost(postId: Int, transform: (Post) -> Post) {
        val currentState = _uiState.value
        if (currentState is HomeUiState.Success) {
            val updated = currentState.posts.map { post ->
                if (post.id == postId) transform(post) else post
            }
            _uiState.value = HomeUiState.Success(updated)
        }
    }
}


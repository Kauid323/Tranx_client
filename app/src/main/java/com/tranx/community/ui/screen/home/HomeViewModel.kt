package com.tranx.community.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.Board
import com.tranx.community.data.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val posts: List<Post>, val boards: List<Board>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel : ViewModel() {
    private val prefsManager = TranxApp.instance.preferencesManager

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _currentBoardId = MutableStateFlow<Int?>(null)
    val currentBoardId: StateFlow<Int?> = _currentBoardId.asStateFlow()

    private val _sortType = MutableStateFlow("latest")
    val sortType: StateFlow<String> = _sortType.asStateFlow()

    // 跟踪已点赞的帖子
    private val _likedPosts = MutableStateFlow<Set<Int>>(emptySet())
    val likedPosts: StateFlow<Set<Int>> = _likedPosts.asStateFlow()

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
                val boardsResponse = try {
                    apiService.getBoardList(token)
                } catch (e: Exception) {
                    println("板块加载失败: ${e.message}")
                    null
                }
                
                val boards = if (boardsResponse?.code == 200 && boardsResponse.data != null) {
                    boardsResponse.data
                } else {
                    emptyList()
                }

                // 加载帖子列表
                val postsResponse = try {
                    apiService.getPostList(
                        token = token,
                        boardId = _currentBoardId.value,
                        sort = _sortType.value
                    )
                } catch (e: Exception) {
                    println("帖子加载失败: ${e.message}")
                    e.printStackTrace()
                    throw Exception("加载帖子失败: ${e.message}")
                }

                val posts = if (postsResponse.code == 200 && postsResponse.data != null) {
                    postsResponse.data.list
                } else {
                    println("帖子响应错误: code=${postsResponse.code}, message=${postsResponse.message}")
                    throw Exception(postsResponse.message ?: "加载帖子失败")
                }

                _uiState.value = HomeUiState.Success(posts, boards)
            } catch (e: Exception) {
                println("HomeViewModel loadData 异常: ${e.message}")
                e.printStackTrace()
                _uiState.value = HomeUiState.Error("加载失败: ${e.message}")
            }
        }
    }

    fun selectBoard(boardId: Int?) {
        _currentBoardId.value = boardId
        loadData()
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
                
                val isCurrentlyLiked = _likedPosts.value.contains(postId)
                
                if (isCurrentlyLiked) {
                    // 取消点赞
                    val response = apiService.unlikePost(token, postId)
                    if (response.code == 200) {
                        _likedPosts.value = _likedPosts.value - postId
                        loadData()
                    }
                } else {
                    // 点赞
                    val response = apiService.likePost(token, postId)
                    if (response.code == 200) {
                        _likedPosts.value = _likedPosts.value + postId
                        loadData()
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
}


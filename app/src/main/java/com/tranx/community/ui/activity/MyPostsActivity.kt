package com.tranx.community.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.Post
import com.tranx.community.ui.component.PostItem
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyPostsActivity : ComponentActivity() {
    private val viewModel: MyPostsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefsManager = TranxApp.instance.preferencesManager
            TranxCommunityTheme(
                darkTheme = false, // 简化处理，实际应从配置读取
                dynamicColor = prefsManager.getUseDynamicColor(),
                primaryColor = prefsManager.getPrimaryColor()
            ) {
                MyPostsScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onPostClick = { postId ->
                        val intent = Intent(this, PostDetailActivity::class.java)
                        intent.putExtra("POST_ID", postId)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

sealed class MyPostsUiState {
    object Loading : MyPostsUiState()
    data class Success(val posts: List<Post>) : MyPostsUiState()
    data class Error(val message: String) : MyPostsUiState()
}

class MyPostsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MyPostsUiState>(MyPostsUiState.Loading)
    val uiState: StateFlow<MyPostsUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            try {
                _uiState.value = MyPostsUiState.Loading
                val token = TranxApp.instance.preferencesManager.getToken() ?: throw Exception("未登录")
                val response = RetrofitClient.getApiService().getMyPosts(token)
                if (response.code == 200) {
                    _uiState.value = MyPostsUiState.Success(response.data?.list ?: emptyList())
                } else {
                    _uiState.value = MyPostsUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = MyPostsUiState.Error(e.message ?: "加载失败")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen(
    viewModel: MyPostsViewModel,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的帖子") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is MyPostsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MyPostsUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadPosts() }) { Text("重试") }
                }
                is MyPostsUiState.Success -> {
                    if (state.posts.isEmpty()) {
                        Text("暂无帖子", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.posts) { post ->
                                PostItem(
                                    post = post,
                                    onClick = { onPostClick(post.id) },
                                    onLike = {}, // 列表页暂不处理点赞
                                    onFavorite = {},
                                    onCoin = {},
                                    isLiked = post.isLiked == true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.lifecycle.ViewModelProvider
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

class FolderDetailActivity : ComponentActivity() {
    private var folderId: Int = -1
    private var folderName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        folderId = intent.getIntExtra("FOLDER_ID", -1)
        folderName = intent.getStringExtra("FOLDER_NAME") ?: "收藏夹"
        
        if (folderId == -1) {
            finish()
            return
        }

        val viewModel: FolderDetailViewModel by viewModels {
            FolderDetailViewModelFactory(folderId)
        }

        setContent {
            val prefsManager = TranxApp.instance.preferencesManager
            TranxCommunityTheme(
                darkTheme = false,
                dynamicColor = prefsManager.getUseDynamicColor(),
                primaryColor = prefsManager.getPrimaryColor()
            ) {
                FolderDetailScreen(
                    title = folderName,
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

sealed class FolderDetailUiState {
    object Loading : FolderDetailUiState()
    data class Success(val posts: List<Post>) : FolderDetailUiState()
    data class Error(val message: String) : FolderDetailUiState()
}

class FolderDetailViewModel(private val folderId: Int) : ViewModel() {
    private val _uiState = MutableStateFlow<FolderDetailUiState>(FolderDetailUiState.Loading)
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            try {
                _uiState.value = FolderDetailUiState.Loading
                val token = TranxApp.instance.preferencesManager.getToken() ?: throw Exception("未登录")
                val response = RetrofitClient.getApiService().getFolderPosts(token, folderId)
                if (response.code == 200) {
                    _uiState.value = FolderDetailUiState.Success(response.data?.posts?.list ?: emptyList())
                } else {
                    _uiState.value = FolderDetailUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = FolderDetailUiState.Error(e.message ?: "加载失败")
            }
        }
    }
}

class FolderDetailViewModelFactory(private val folderId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FolderDetailViewModel(folderId) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    title: String,
    viewModel: FolderDetailViewModel,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                is FolderDetailUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is FolderDetailUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadPosts() }) { Text("重试") }
                }
                is FolderDetailUiState.Success -> {
                    if (state.posts.isEmpty()) {
                        Text("收藏夹为空", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.posts) { post ->
                                PostItem(
                                    post = post,
                                    onClick = { onPostClick(post.id) },
                                    onLike = {},
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

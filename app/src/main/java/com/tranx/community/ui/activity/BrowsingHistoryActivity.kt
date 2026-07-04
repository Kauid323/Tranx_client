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
import com.tranx.community.data.model.HistoryItem
import com.tranx.community.ui.component.PostItem
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowsingHistoryActivity : ComponentActivity() {
    private val viewModel: BrowsingHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefsManager = TranxApp.instance.preferencesManager
            TranxCommunityTheme(
                darkTheme = false,
                dynamicColor = prefsManager.getUseDynamicColor(),
                primaryColor = prefsManager.getPrimaryColor()
            ) {
                BrowsingHistoryScreen(
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

sealed class BrowsingHistoryUiState {
    object Loading : BrowsingHistoryUiState()
    data class Success(val history: List<HistoryItem>) : BrowsingHistoryUiState()
    data class Error(val message: String) : BrowsingHistoryUiState()
}

class BrowsingHistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<BrowsingHistoryUiState>(BrowsingHistoryUiState.Loading)
    val uiState: StateFlow<BrowsingHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                _uiState.value = BrowsingHistoryUiState.Loading
                val token = TranxApp.instance.preferencesManager.getToken() ?: throw Exception("未登录")
                val response = RetrofitClient.getApiService().getBrowsingHistory(token)
                if (response.code == 200) {
                    _uiState.value = BrowsingHistoryUiState.Success(response.data?.list ?: emptyList())
                } else {
                    _uiState.value = BrowsingHistoryUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = BrowsingHistoryUiState.Error(e.message ?: "加载失败")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowsingHistoryScreen(
    viewModel: BrowsingHistoryViewModel,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("浏览历史") },
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
                is BrowsingHistoryUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is BrowsingHistoryUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadHistory() }) { Text("重试") }
                }
                is BrowsingHistoryUiState.Success -> {
                    if (state.history.isEmpty()) {
                        Text("暂无历史记录", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.history) { item ->
                                Column {
                                    Text(
                                        text = "阅读于: ${item.viewedAt}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    PostItem(
                                        post = item.post,
                                        onClick = { onPostClick(item.post.id) },
                                        onLike = {},
                                        onFavorite = {},
                                        onCoin = {},
                                        isLiked = item.post.isLiked == true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

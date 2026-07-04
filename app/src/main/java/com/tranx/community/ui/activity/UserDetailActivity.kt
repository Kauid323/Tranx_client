package com.tranx.community.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.data.model.Folder
import com.tranx.community.data.model.Post
import com.tranx.community.data.model.User
import com.tranx.community.data.model.UserDetailResponse
import com.tranx.community.ui.component.PostItem
import com.tranx.community.ui.component.formatTime
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userId = intent.getIntExtra(EXTRA_USER_ID, -1)
        if (userId <= 0) {
            finish()
            return
        }

        val viewModel = ViewModelProvider(
            this,
            UserDetailViewModelFactory(userId)
        )[UserDetailViewModel::class.java]

        setContent {
            val prefsManager = TranxApp.instance.preferencesManager
            val themeMode = remember { prefsManager.getThemeMode() }
            val primaryColor = remember { prefsManager.getPrimaryColor() }
            val useDynamicColor = remember { prefsManager.getUseDynamicColor() }

            val darkTheme = when (themeMode) {
                PreferencesManager.ThemeMode.LIGHT -> false
                PreferencesManager.ThemeMode.DARK -> true
                PreferencesManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            TranxCommunityTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor,
                primaryColor = if (useDynamicColor) null else primaryColor
            ) {
                UserDetailScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_USER_ID = "USER_ID"
    }
}

sealed class UserDetailUiState {
    object Loading : UserDetailUiState()
    data class Success(val detail: UserDetailResponse) : UserDetailUiState()
    data class Error(val message: String) : UserDetailUiState()
}

class UserDetailViewModel(private val userId: Int) : ViewModel() {
    private val prefsManager = TranxApp.instance.preferencesManager

    private val _uiState = MutableStateFlow<UserDetailUiState>(UserDetailUiState.Loading)
    val uiState: StateFlow<UserDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            try {
                _uiState.value = UserDetailUiState.Loading
                val token = prefsManager.getToken() ?: throw Exception("未登录")
                val api = RetrofitClient.getApiService()

                val resp = api.getUserDetail(token, userId)
                if (resp.code != 200 || resp.data == null) {
                    throw Exception(resp.message)
                }

                _uiState.value = UserDetailUiState.Success(resp.data)
            } catch (e: Exception) {
                _uiState.value = UserDetailUiState.Error("加载失败: ${e.message}")
            }
        }
    }
}

class UserDetailViewModelFactory(private val userId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserDetailViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    viewModel: UserDetailViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is UserDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UserDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.load() }) {
                            Text("重试")
                        }
                    }
                }
            }

            is UserDetailUiState.Success -> {
                val detail = state.detail
                val user = detail.user

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val avatarSize = 72.dp
                            if (!user.avatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = user.avatar,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(avatarSize)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(avatarSize)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.username,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                val levelText = user.userLevel?.let { "Lv$it" } ?: "Lv-"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = levelText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "硬币 ${user.coins}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatBlock(title = "关注", value = (detail.followingCount ?: 0).toString())
                                StatBlock(title = "粉丝", value = (detail.followerCount ?: 0).toString())
                                StatBlock(title = "帖子", value = (detail.postCount ?: 0).toString())
                                StatBlock(title = "收藏", value = (detail.favoriteCount ?: 0).toString())
                            }
                        }
                    }

                    // 收藏夹
                    if (!detail.folders.isNullOrEmpty()) {
                        item {
                            SectionHeader(title = "收藏夹 (${detail.folders.size})")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(detail.folders) { folder ->
                                    FolderCard(folder) {
                                        val intent = Intent(context, FolderDetailActivity::class.java)
                                        intent.putExtra("FOLDER_ID", folder.id)
                                        intent.putExtra("FOLDER_NAME", folder.name)
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        }
                    }

                    // 最近帖子
                    if (!detail.posts.isNullOrEmpty()) {
                        item {
                            SectionHeader(title = "最近发布")
                        }
                        items(detail.posts) { post ->
                            PostItem(
                                post = post,
                                onClick = {
                                    val intent = Intent(context, PostDetailActivity::class.java)
                                    intent.putExtra("POST_ID", post.id)
                                    context.startActivity(intent)
                                },
                                onLike = {},
                                onFavorite = {},
                                onCoin = {}
                            )
                        }
                    }

                    // 最近收藏
                    if (!detail.favorites.isNullOrEmpty()) {
                        item {
                            SectionHeader(title = "最近收藏")
                        }
                        items(detail.favorites) { post ->
                            PostItem(
                                post = post,
                                onClick = {
                                    val intent = Intent(context, PostDetailActivity::class.java)
                                    intent.putExtra("POST_ID", post.id)
                                    context.startActivity(intent)
                                },
                                onLike = {},
                                onFavorite = {},
                                onCoin = {}
                            )
                        }
                    }

                    item {
                        if (!user.createdAt.isNullOrBlank()) {
                            Text(
                                text = "注册于 ${formatTime(user.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderCard(folder: Folder, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "${folder.itemCount} 项目",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StatBlock(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

package com.tranx.community.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tranx.community.ui.component.CoinAmountDialog
import com.tranx.community.ui.component.CreateFolderDialog
import com.tranx.community.ui.component.FavoriteBottomSheet
import com.tranx.community.ui.component.PostItem
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tranx.community.TranxApp
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.data.model.Board
import com.tranx.community.data.model.Post
import com.tranx.community.ui.screen.board.BoardDetailUiState
import com.tranx.community.ui.theme.TranxCommunityTheme

class BoardDetailActivity : ComponentActivity() {
    private val viewModel: BoardDetailViewModel by viewModels()
    private var boardId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        boardId = intent.getIntExtra("BOARD_ID", -1)
        if (boardId == -1) {
            finish()
            return
        }

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
                BoardDetailScreen(
                    boardId = boardId,
                    viewModel = viewModel,
                    onBack = { finish() },
                    onPostClick = { postId ->
                        val intent = Intent(this, PostDetailActivity::class.java)
                        intent.putExtra("POST_ID", postId)
                        startActivity(intent)
                    },
                    onCreatePost = {
                        val intent = Intent(this, CreatePostActivity::class.java)
                        intent.putExtra("BOARD_ID", boardId)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (boardId != -1) {
            viewModel.loadBoard(boardId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(
    boardId: Int,
    viewModel: BoardDetailViewModel,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit,
    onCreatePost: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current

    var selectedPostForFavorite by remember { mutableStateOf<Post?>(null) }
    var coinPostTarget by remember { mutableStateOf<Post?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分区详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onCreatePost) {
                        Icon(Icons.Default.Edit, contentDescription = "发帖")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is BoardDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is BoardDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadBoard(boardId) }) {
                            Text("重试")
                        }
                    }
                }
            }

            is BoardDetailUiState.Success -> {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing = false),
                    onRefresh = { viewModel.loadBoard(boardId) },
                    modifier = Modifier.padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BoardInfoCard(board = state.board)

                        if (state.posts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("该分区暂无帖子", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.posts) { post ->
                                    PostItem(
                                        post = post,
                                        onClick = { onPostClick(post.id) },
                                        onLike = { viewModel.likePost(post.id) },
                                        onFavorite = {
                                            selectedPostForFavorite = post
                                            viewModel.loadFolders()
                                        },
                                        onCoin = { coinPostTarget = post },
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

    selectedPostForFavorite?.let { targetPost ->
        FavoriteBottomSheet(
            folders = folders,
            onDismiss = { selectedPostForFavorite = null },
            onCreateFolder = { name, description, isPublic ->
                viewModel.createFolder(name, description, isPublic) {
                    Toast.makeText(context, "收藏夹已创建", Toast.LENGTH_SHORT).show()
                }
            },
            onSelectFolder = { folderId ->
                viewModel.addPostToFolder(targetPost.id, folderId) { success, message ->
                    Toast.makeText(
                        context,
                        if (success) "收藏成功" else (message ?: "收藏失败"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                selectedPostForFavorite = null
            }
        )
    }

    coinPostTarget?.let { targetPost ->
        CoinAmountDialog(
            onDismiss = { coinPostTarget = null },
            onConfirm = { amount ->
                viewModel.coinPost(targetPost.id, amount) { success, message ->
                    Toast.makeText(
                        context,
                        if (success) "投币成功" else (message ?: "投币失败"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                coinPostTarget = null
            }
        )
    }
}

@Composable
private fun BoardInfoCard(board: Board) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!board.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = board.avatarUrl,
                    contentDescription = board.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = board.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = board.description ?: "暂无介绍",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                board.creatorName?.let { creator ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "创建者：$creator",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                board.createdAt?.let { created ->
                    Text(
                        text = "创建时间：$created",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


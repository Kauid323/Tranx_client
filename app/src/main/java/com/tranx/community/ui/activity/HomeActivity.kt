package com.tranx.community.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.tranx.community.ui.screen.board.BoardListScreen
import com.tranx.community.ui.screen.board.BoardListViewModel
import com.tranx.community.ui.screen.app.AppListScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tranx.community.TranxApp
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.data.model.Board
import com.tranx.community.data.model.Post
import com.tranx.community.ui.screen.home.HomeUiState
import com.tranx.community.ui.screen.home.HomeViewModel
import com.tranx.community.ui.screen.profile.ProfileViewModel
import com.tranx.community.ui.theme.TranxCommunityTheme

class HomeActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val profileViewModel: com.tranx.community.ui.screen.profile.ProfileViewModel by viewModels()
    private val boardListViewModel: BoardListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
                MainScreen(
                    homeViewModel = homeViewModel,
                    profileViewModel = profileViewModel,
                    boardListViewModel = boardListViewModel,
                    onPostClick = { postId ->
                        val intent = Intent(this, PostDetailActivity::class.java)
                        intent.putExtra("POST_ID", postId)
                        startActivity(intent)
                    },
                    onCreatePostClick = {
                        val intent = Intent(this, CreatePostActivity::class.java)
                        // 传递当前选中的板块ID
                        val currentBoardId = homeViewModel.currentBoardId.value
                        if (currentBoardId != null) {
                            intent.putExtra("BOARD_ID", currentBoardId)
                        }
                        startActivity(intent)
                    },
                    onLogout = {
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    profileViewModel: com.tranx.community.ui.screen.profile.ProfileViewModel,
    boardListViewModel: BoardListViewModel,
    onPostClick: (Int) -> Unit,
    onCreatePostClick: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { 
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "首页"
                        )
                    },
                    label = { Text("首页") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "分区"
                        )
                    },
                    label = { Text("分区") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.Apps else Icons.Outlined.Apps,
                            contentDescription = "应用"
                        )
                    },
                    label = { Text("应用") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "我的"
                        )
                    },
                    label = { Text("我的") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> HomeScreen(
                viewModel = homeViewModel,
                onPostClick = onPostClick,
                onCreatePostClick = onCreatePostClick,
                paddingValues = paddingValues
            )
            1 -> BoardListScreen(
                viewModel = boardListViewModel,
                paddingValues = paddingValues,
                onBoardClick = { boardId ->
                    selectedTab = 0  // 切换到首页
                    homeViewModel.selectBoard(boardId)  // 选择对应的板块
                }
            )
            2 -> AppListScreen(
                paddingValues = paddingValues,
                onAppClick = { packageName ->
                    val intent = Intent(context, AppDetailActivity::class.java)
                    intent.putExtra("PACKAGE_NAME", packageName)
                    context.startActivity(intent)
                }
            )
            3 -> ProfileScreen(
                viewModel = profileViewModel,
                onLogout = onLogout,
                paddingValues = paddingValues,
                onUploadAppClick = {
                    val intent = Intent(context, UploadAppActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPostClick: (Int) -> Unit,
    onCreatePostClick: () -> Unit,
    paddingValues: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentBoardId by viewModel.currentBoardId.collectAsState()
    val sortType by viewModel.sortType.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text("Tranx 社区") },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "排序")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("最新发布") },
                            onClick = {
                                viewModel.changeSortType("latest")
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortType == "latest") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("最近回复") },
                            onClick = {
                                viewModel.changeSortType("reply")
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortType == "reply") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("热门") },
                            onClick = {
                                viewModel.changeSortType("hot")
                                showSortMenu = false
                            },
                            leadingIcon = {
                                if (sortType == "hot") {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePostClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Edit, contentDescription = "发帖")
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadData() }) {
                            Text("重试")
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                val isRefreshing by remember { mutableStateOf(false) }
                
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = { viewModel.loadData() },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (state.boards.isNotEmpty()) {
                            BoardFilterRow(
                                boards = state.boards,
                                selectedBoardId = currentBoardId,
                                onBoardSelected = { viewModel.selectBoard(it) }
                            )
                        }

                        if (state.posts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无帖子", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.posts) { post ->
                                    PostItem(
                                        post = post,
                                        onClick = { onPostClick(post.id) },
                                        onLike = { viewModel.likePost(post.id) },
                                        onFavorite = { /* TODO */ },
                                        onCoin = { /* TODO */ },
                                        isLiked = viewModel.isPostLiked(post.id)
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

@Composable
fun BoardFilterRow(
    boards: List<Board>,
    selectedBoardId: Int?,
    onBoardSelected: (Int?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedBoardId == null,
                onClick = { onBoardSelected(null) },
                label = { Text("全部") }
            )
        }
        items(boards) { board ->
            FilterChip(
                selected = selectedBoardId == board.id,
                onClick = { onBoardSelected(board.id) },
                label = { Text(board.name) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostItem(
    post: Post,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onCoin: () -> Unit,
    isLiked: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 右上角浏览量
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = post.viewCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题行：发布者 · 时间
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 60.dp) // 给右上角浏览量留空间
                ) {
                    Text(
                        text = post.publisher,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(post.publishTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 标题
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 内容预览
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // 图片预览
                if (!post.imageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部互动按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 点赞按钮
                    TextButton(
                        onClick = { onLike() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "点赞",
                            modifier = Modifier.size(18.dp),
                            tint = if (isLiked) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(post.likes.toString())
                    }

                    // 收藏按钮
                    TextButton(
                        onClick = { onFavorite() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.StarBorder,
                            contentDescription = "收藏",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(post.favorites.toString())
                    }

                    // 投币按钮
                    TextButton(
                        onClick = { onCoin() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = "投币",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (post.coins > 0) post.coins.toString() else "投币")
                    }

                    // 评论数（仅显示）
                    TextButton(
                        onClick = { onClick() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Comment,
                            contentDescription = "评论",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(post.commentCount.toString())
                    }
                }
            }
        }
    }
}

fun formatTime(time: String): String {
    return try {
        time.substring(0, 10)
    } catch (e: Exception) {
        time
    }
}
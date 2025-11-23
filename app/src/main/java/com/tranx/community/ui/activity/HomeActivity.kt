package com.tranx.community.ui.activity

import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.tranx.community.ui.component.CoinAmountDialog
import com.tranx.community.ui.component.CreateFolderDialog
import com.tranx.community.ui.component.FavoriteBottomSheet
import com.tranx.community.ui.component.PostItem
import com.tranx.community.ui.component.formatTime
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tranx.community.TranxApp
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.data.model.Post
import com.tranx.community.ui.screen.home.HomeUiState
import com.tranx.community.ui.screen.home.HomeViewModel
import com.tranx.community.ui.screen.profile.ProfileViewModel
import com.tranx.community.ui.theme.TranxCommunityTheme

class HomeActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
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
    profileViewModel: ProfileViewModel,
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
                    val intent = Intent(context, BoardDetailActivity::class.java)
                    intent.putExtra("BOARD_ID", boardId)
                    context.startActivity(intent)
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
    val sortType by viewModel.sortType.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedPostForFavorite by remember { mutableStateOf<Post?>(null) }
    var coinPostTarget by remember { mutableStateOf<Post?>(null) }

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
                        listOf(
                            "latest" to "最新发布",
                            "reply" to "最近回复",
                            "hot" to "热门"
                        ).forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.changeSortType(value)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortType == value) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
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
                val swipeState = rememberSwipeRefreshState(uiState is HomeUiState.Loading)
                SwipeRefresh(
                    state = swipeState,
                    onRefresh = { viewModel.loadData() },
                    modifier = Modifier.padding(innerPadding)
                ) {
                    if (state.posts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
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
                                    onFavorite = {
                                        selectedPostForFavorite = post
                                        viewModel.loadFolders()
                                    },
                                    onCoin = { coinPostTarget = post },
                                    isLiked = viewModel.isPostLiked(post.id)
                                )
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
}
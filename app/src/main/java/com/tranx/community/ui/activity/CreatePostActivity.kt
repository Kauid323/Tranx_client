package com.tranx.community.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.data.model.CreatePostRequest
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlinx.coroutines.launch

class CreatePostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val selectedBoardId = intent.getIntExtra("BOARD_ID", 1) // 默认为综合讨论板块
        
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
                CreatePostScreen(
                    selectedBoardId = selectedBoardId,
                    onBackClick = { finish() },
                    onPostCreated = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    selectedBoardId: Int,
    onBackClick: () -> Unit,
    onPostCreated: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var boardId by remember { mutableStateOf(selectedBoardId) }
    var isLoading by remember { mutableStateOf(false) }
    var boards by remember { mutableStateOf<List<com.tranx.community.data.model.Board>>(emptyList()) }
    var showBoardSelector by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 加载板块列表
    LaunchedEffect(Unit) {
        try {
            val prefsManager = TranxApp.instance.preferencesManager
            val token = prefsManager.getToken()
            if (token != null) {
                val apiService = RetrofitClient.getApiService()
                val response = apiService.getBoardList(token)
                if (response.code == 200 && response.data != null) {
                    boards = response.data
                }
            }
        } catch (e: Exception) {
            // 处理错误
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发布帖子") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val prefsManager = TranxApp.instance.preferencesManager
                                        val token = prefsManager.getToken() ?: return@launch
                                        val apiService = RetrofitClient.getApiService()
                                        
                                        val response = apiService.createPost(
                                            token,
                                            CreatePostRequest(
                                                boardId = boardId,
                                                title = title,
                                                content = content
                                            )
                                        )

                                        if (response.code == 200) {
                                            onPostCreated()
                                        }
                                    } catch (e: Exception) {
                                        // 处理错误
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && title.isNotBlank() && content.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text("发布")
                        }
                    }
                }
            )
        }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 板块选择器
            OutlinedTextField(
                value = boards.find { it.id == boardId }?.name ?: "选择分区",
                onValueChange = { },
                label = { Text("发布分区") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showBoardSelector = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择分区")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                placeholder = { Text("请输入帖子标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("内容") },
                placeholder = { Text("请输入帖子内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = 20
            )
        }
    }

    // 板块选择对话框
    if (showBoardSelector && boards.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBoardSelector = false },
            title = { Text("选择发布分区") },
            text = {
                LazyColumn {
                    items(boards) { board ->
                        TextButton(
                            onClick = {
                                boardId = board.id
                                showBoardSelector = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = board.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (!board.description.isNullOrEmpty()) {
                                    Text(
                                        text = board.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBoardSelector = false }) {
                    Text("取消")
                }
            }
        )
    }
}


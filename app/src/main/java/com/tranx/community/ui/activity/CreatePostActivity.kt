package com.tranx.community.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onBackClick: () -> Unit,
    onPostCreated: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                                                boardId = 1, // 默认板块
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
}


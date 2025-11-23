package com.tranx.community.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tranx.community.TranxApp
import com.tranx.community.data.api.PicuiUploader
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
        val editingPostId = intent.getIntExtra("POST_ID", -1).takeIf { it > 0 }
        
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
                    editingPostId = editingPostId,
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
    editingPostId: Int?,
    onBackClick: () -> Unit,
    onPostCreated: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var boardId by remember { mutableStateOf(selectedBoardId) }
    var isSubmitting by remember { mutableStateOf(false) }
    var boards by remember { mutableStateOf<List<com.tranx.community.data.model.Board>>(emptyList()) }
    var showBoardSelector by remember { mutableStateOf(false) }
    var postType by remember { mutableStateOf("text") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var isEditMode by remember { mutableStateOf(editingPostId != null) }
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

    // 加载编辑帖子数据
    LaunchedEffect(editingPostId) {
        val postId = editingPostId ?: return@LaunchedEffect
        try {
            isSubmitting = true
            val prefsManager = TranxApp.instance.preferencesManager
            val token = prefsManager.getToken() ?: return@LaunchedEffect
            val apiService = RetrofitClient.getApiService()
            val response = apiService.getPost(token, postId)
            if (response.code == 200 && response.data != null) {
                val post = response.data
                title = post.title
                content = post.content
                boardId = post.boardId
                postType = post.type ?: "text"
                imageUrl = post.imageUrl
                isEditMode = true
            }
        } catch (_: Exception) {
        } finally {
            isSubmitting = false
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isUploadingImage = true
            uploadError = null
            val result = PicuiUploader.uploadImage(context, uri)
            result
                .onSuccess { uploadedUrl ->
                    imageUrl = uploadedUrl
                    Toast.makeText(context, "图片上传成功", Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    uploadError = it.message
                    Toast.makeText(context, "上传失败: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            isUploadingImage = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑帖子" else "发布帖子") },
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
                                    isSubmitting = true
                                    try {
                                        val prefsManager = TranxApp.instance.preferencesManager
                                        val token = prefsManager.getToken() ?: return@launch
                                        val apiService = RetrofitClient.getApiService()

                                        val requestBody = CreatePostRequest(
                                            boardId = boardId,
                                            title = title,
                                            content = content,
                                            type = postType,
                                            imageUrl = imageUrl
                                        )

                                        val response = if (editingPostId != null) {
                                            apiService.updatePost(token, editingPostId, requestBody)
                                        } else {
                                            apiService.createPost(token, requestBody)
                                        }

                                        if (response.code == 200) {
                                            onPostCreated()
                                        } else {
                                            Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        // 处理错误
                                        Toast.makeText(context, e.message ?: "发布失败", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting && title.isNotBlank() && content.isNotBlank()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text(if (isEditMode) "保存" else "发布")
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

            Spacer(modifier = Modifier.height(16.dp))

            Text("内容格式", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                listOf("text" to "文本", "markdown" to "Markdown").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = postType == value,
                        onClick = { postType = value },
                        shape = SegmentedButtonDefaults.itemShape(index, 2)
                    ) {
                        Text(label)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("封面图片 (可选)", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isUploadingImage
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("上传图片")
                }
                if (imageUrl != null) {
                    TextButton(onClick = { imageUrl = null }) {
                        Text("移除")
                    }
                }
            }

            if (isUploadingImage) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            uploadError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            imageUrl?.let { url ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(end = 12.dp)
                        )
                        Text(
                            text = url,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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


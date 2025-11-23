package com.tranx.community.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tranx.community.TranxApp
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.data.model.Comment
import com.tranx.community.data.model.Post
import com.tranx.community.data.model.Folder
import com.tranx.community.ui.screen.post.PostDetailUiState
import com.tranx.community.ui.screen.post.PostDetailViewModel
import com.tranx.community.ui.component.CoinAmountDialog
import com.tranx.community.ui.component.CommentRepliesBottomSheet
import com.tranx.community.ui.component.CreateFolderDialog
import com.tranx.community.ui.component.FavoriteBottomSheet
import com.tranx.community.ui.component.formatTime
import com.tranx.community.ui.theme.TranxCommunityTheme

class PostDetailActivity : ComponentActivity() {
    private val viewModel: PostDetailViewModel by viewModels()
    private var postId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        postId = intent.getIntExtra("POST_ID", -1)
        if (postId == -1) {
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
                PostDetailScreen(
                    postId = postId,
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (postId != -1) {
            viewModel.loadPost(postId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    viewModel: PostDetailViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val commentReplies by viewModel.commentReplies.collectAsState()
    
    var showCommentDialog by remember { mutableStateOf(false) }
    var showFavoriteSheet by remember { mutableStateOf(false) }
    var showRepliesSheet by remember { mutableStateOf<Comment?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPostCoinDialog by remember { mutableStateOf(false) }
    var commentCoinTarget by remember { mutableStateOf<Comment?>(null) }
    var commentText by remember { mutableStateOf("") }
    var replyToComment by remember { mutableStateOf<Comment?>(null) }

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
        viewModel.loadFolders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帖子详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState is PostDetailUiState.Success) {
                        val post = (uiState as PostDetailUiState.Success).post
                        val currentUserId = TranxApp.instance.preferencesManager.getUser()?.id
                        if (currentUserId != null && currentUserId == post.userId) {
                            IconButton(onClick = {
                                val intent = Intent(context, CreatePostActivity::class.java)
                                intent.putExtra("POST_ID", post.id)
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑帖子")
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除帖子")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState is PostDetailUiState.Success) {
                // 使用 let 来处理智能转换
                (uiState as PostDetailUiState.Success).let { successState ->
                    val currentPost = successState.post
                    Surface(
                        tonalElevation = 3.dp,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 点赞按钮 - 显示点赞数
                            TextButton(onClick = { viewModel.likePost(postId) }) {
                                Icon(
                                    Icons.Default.ThumbUp,
                                    contentDescription = "点赞",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentPost.likes.toString(),
                                    color = if (isLiked) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 收藏按钮 - 显示收藏数
                            TextButton(onClick = { showFavoriteSheet = true }) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "收藏",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentPost.favorites.toString(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                        // 投币按钮 - 显示投币数
                        TextButton(onClick = { showPostCoinDialog = true }) {
                                Icon(
                                    Icons.Default.MonetizationOn,
                                    contentDescription = "投币",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentPost.coins > 0) currentPost.coins.toString() else "投币",
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }

                            FilledTonalButton(
                                onClick = { 
                                    replyToComment = null
                                    showCommentDialog = true 
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Comment, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("发表评论")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is PostDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is PostDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPost(postId) }) {
                            Text("重试")
                        }
                    }
                }
            }

            is PostDetailUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    item {
                        PostContent(post = state.post)
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = 8.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    item {
                        Text(
                            text = "评论 (${state.comments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (state.comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "暂无评论，快来发表第一条评论吧！",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(state.comments) { comment ->
                            CommentItem(
                                comment = comment,
                                replies = commentReplies[comment.id] ?: emptyList(),
                                onLike = { viewModel.likeComment(comment.id, postId) },
                                onReply = { 
                                    replyToComment = comment
                                    showCommentDialog = true 
                                },
                                onShowReplies = { 
                                    viewModel.loadCommentReplies(comment.id)
                                    showRepliesSheet = comment 
                                },
                                onDelete = {
                                    viewModel.deleteComment(comment.id, postId,
                                        onSuccess = {
                                            Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onCoin = { commentCoinTarget = comment },
                                viewModel = viewModel,
                                postId = postId
                            )
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

    // 评论对话框
    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCommentDialog = false
                replyToComment = null
            },
            title = { 
                Text(if (replyToComment != null) "回复评论" else "发表评论") 
            },
            text = {
                Column {
                    if (replyToComment != null) {
                        Text(
                            text = "回复 @${replyToComment!!.username ?: "匿名用户"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("请输入评论内容...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(postId, commentText, replyToComment?.id) {
                                commentText = ""
                                showCommentDialog = false
                                replyToComment = null
                            }
                        }
                    },
                    enabled = commentText.isNotBlank()
                ) {
                    Text("发表")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCommentDialog = false
                    replyToComment = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 收藏底部表单
    if (showFavoriteSheet) {
        FavoriteBottomSheet(
            folders = folders,
            onDismiss = { showFavoriteSheet = false },
            onCreateFolder = { name, description, isPublic ->
                viewModel.createFolder(name, description, isPublic,
                    onSuccess = {
                        Toast.makeText(context, "创建收藏夹成功", Toast.LENGTH_SHORT).show()
                    },
                    onError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onSelectFolder = { folderId ->
                viewModel.addPostToFolder(postId, folderId,
                    onSuccess = {
                        showFavoriteSheet = false
                        Toast.makeText(context, "收藏成功", Toast.LENGTH_SHORT).show()
                    },
                    onError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    if (showPostCoinDialog) {
        CoinAmountDialog(
            onDismiss = { showPostCoinDialog = false },
            onConfirm = { amount ->
                viewModel.coinPost(postId, amount,
                    onSuccess = {
                        Toast.makeText(context, "投币成功", Toast.LENGTH_SHORT).show()
                    },
                    onError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
                showPostCoinDialog = false
            }
        )
    }

    commentCoinTarget?.let { target ->
        CoinAmountDialog(
            onDismiss = { commentCoinTarget = null },
            onConfirm = { amount ->
                viewModel.coinComment(target.id, postId, amount,
                    onSuccess = {
                        Toast.makeText(context, "投币成功", Toast.LENGTH_SHORT).show()
                    },
                    onError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
                commentCoinTarget = null
            }
        )
    }

    // 子评论底部表单
    showRepliesSheet?.let { comment ->
        CommentRepliesBottomSheet(
            comment = comment,
            replies = commentReplies[comment.id] ?: emptyList(),
            onDismiss = { showRepliesSheet = null },
            onReply = { parentComment ->
                replyToComment = parentComment
                showCommentDialog = true
                showRepliesSheet = null
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除帖子") },
            text = { Text("确定要删除这个帖子吗？删除后无法恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePost(postId,
                            onSuccess = {
                                showDeleteDialog = false
                                onBack()
                            },
                            onError = { message ->
                                showDeleteDialog = false
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun PostContent(post: Post) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = post.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = post.publisher,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatTime(post.publishTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = post.content, style = MaterialTheme.typography.bodyLarge)

        if (!post.imageUrl.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentItem(
    comment: Comment,
    replies: List<Comment>,
    onLike: () -> Unit,
    onReply: () -> Unit,
    onShowReplies: () -> Unit,
    onDelete: () -> Unit,
    onCoin: () -> Unit,
    viewModel: PostDetailViewModel,
    postId: Int
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 评论头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.username ?: "匿名用户",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (comment.isAuthor) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "楼主",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${comment.floor ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 删除按钮（仅作者可见）
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 评论内容
            Text(
                text = comment.content ?: "",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 显示前3个子评论
            if (replies.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    replies.take(3).forEach { reply ->
                        Row {
                            Text(
                                text = "${reply.username ?: "匿名用户"}：",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = reply.content ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (reply != replies.take(3).last()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                if ((comment.replyCount ?: 0) > 0) {
                    TextButton(
                        onClick = onShowReplies,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("查看更多回复 (${comment.replyCount})")
                    }
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = onLike) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = "点赞",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${comment.likes ?: 0}")
                }

                TextButton(onClick = onCoin) {
                    Icon(
                        Icons.Default.MonetizationOn,
                        contentDescription = "投币",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if ((comment.coins ?: 0) > 0) (comment.coins ?: 0).toString() else "投币",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                TextButton(onClick = onReply) {
                    Icon(
                        Icons.Default.Reply,
                        contentDescription = "回复",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("回复")
                }
            }

            Text(
                text = formatTime(comment.publishTime ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
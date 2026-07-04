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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.tranx.community.ui.screen.post.PostDetailUiState
import com.tranx.community.ui.screen.post.PostDetailViewModel
import com.tranx.community.ui.component.CoinAmountDialog
import com.tranx.community.ui.component.CommentRepliesBottomSheet
import com.tranx.community.ui.component.ContentRenderer
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

                        if (!state.post.imageUrl.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AsyncImage(
                                model = state.post.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
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
                                onCoin = { commentCoinTarget = comment }
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(start = 16.dp + 32.dp + 12.dp, end = 16.dp)
                            )
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
                            text = "回复 @${replyToComment!!.publisher ?: "匿名用户"}",
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
        
        // 使用ContentRenderer根据帖子类型渲染内容
        ContentRenderer(
            content = post.content,
            contentType = post.type,
            style = MaterialTheme.typography.bodyLarge
        )
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
    onCoin: () -> Unit
) {
    val avatarSize = 32.dp
    val liked = comment.isLiked == true
 
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!comment.avatar.isNullOrBlank()) {
                AsyncImage(
                    model = comment.avatar,
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
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
 
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = comment.publisher ?: "匿名用户",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (comment.isAuthor) {
                        Text(
                            text = "楼主",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "#${comment.floor ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
 
                if (!comment.content.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ContentRenderer(
                        content = comment.content,
                        contentType = "markdown",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
 
                if (replies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(8.dp)
                    ) {
                        replies.take(3).forEachIndexed { index, reply ->
                            Row {
                                Text(
                                    text = "${reply.publisher ?: "匿名用户"}：",
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
                            if (index != replies.take(3).lastIndex) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
 
                    if ((comment.replyCount ?: 0) > 0) {
                        TextButton(
                            onClick = onShowReplies,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "查看更多回复 (${comment.replyCount})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
 
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onLike,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "点赞",
                            modifier = Modifier.size(14.dp),
                            tint = if (liked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.likes ?: 0}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
 
                    TextButton(
                        onClick = onCoin,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = "投币",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if ((comment.coins ?: 0) > 0) (comment.coins ?: 0).toString() else "投币",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
 
                    TextButton(
                        onClick = onReply,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            Icons.Default.Reply,
                            contentDescription = "回复",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "回复",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
 
                    Spacer(modifier = Modifier.weight(1f))
 
                    if (comment.isMyComment == true) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
}
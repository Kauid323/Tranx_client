package com.tranx.community.ui.activity

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tranx.community.data.model.User
import com.tranx.community.ui.screen.profile.ProfileUiState
import com.tranx.community.ui.screen.profile.ProfileViewModel
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlin.math.floor
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    paddingValues: PaddingValues,
    onUploadAppClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text("个人中心") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "退出登录")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfile() }) {
                            Text("重试")
                        }
                    }
                }
            }

            is ProfileUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 用户信息卡片 - 扁平化设计
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 头像在左边
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // 用户信息在右边
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 用户名
                                Text(
                                    text = state.user.username,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                // 等级进度条和等级显示
                                if (state.user.exp != null && state.user.userLevel != null) {
                                    val level = state.user.userLevel
                                    val exp = state.user.exp
                                    val progress = calculateLevelProgress(exp, level)
                                    val nextLevelExp = level * level * 100
                                    val currentLevelExp = (level - 1) * (level - 1) * 100
                                    val expNeeded = nextLevelExp - currentLevelExp
                                    val expProgress = exp - currentLevelExp

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = progress,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(8.dp),
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Lv$level",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "经验: $expProgress / $expNeeded",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // 硬币显示
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "${state.user.coins} 硬币",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // 签到卡片
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text("每日签到") },
                            supportingContent = {
                                Text(
                                    if (state.checkinStatus.checkedIn) 
                                        "今天已签到，获得50硬币和25经验" 
                                    else 
                                        "签到可获得50硬币和25经验"
                                )
                            },
                            leadingContent = {
                                Icon(
                                    if (state.checkinStatus.checkedIn) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (state.checkinStatus.checkedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                if (!state.checkinStatus.checkedIn) {
                                    Button(
                                        onClick = {
                                            viewModel.checkin { message ->
                                                // 签到成功，刷新用户信息
                                                viewModel.loadProfile()
                                            }
                                        }
                                    ) {
                                        Text("签到")
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }

                    // 功能列表
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text("我的帖子") },
                                leadingContent = { Icon(Icons.Default.Article, contentDescription = null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                                modifier = Modifier.clickable { /* TODO */ }
                            )
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("我的收藏") },
                                leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                                modifier = Modifier.clickable { /* TODO */ }
                            )
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("浏览历史") },
                                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                                modifier = Modifier.clickable { /* TODO */ }
                            )
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("上传应用") },
                                leadingContent = { Icon(Icons.Default.Upload, contentDescription = null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                                modifier = Modifier.clickable { onUploadAppClick() }
                            )
                            HorizontalDivider()
                            ListItem(
                                headlineContent = { Text("设置") },
                                leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 退出登录确认对话框
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 计算当前等级的进度
 * @param exp 当前经验值
 * @param level 当前等级
 * @return 进度百分比 (0.0 - 1.0)
 */
private fun calculateLevelProgress(exp: Int, level: Int): Float {
    val currentLevelExp = (level - 1) * (level - 1) * 100
    val nextLevelExp = level * level * 100
    val expRange = nextLevelExp - currentLevelExp
    if (expRange <= 0) return 0f
    val expInCurrentLevel = exp - currentLevelExp
    return (expInCurrentLevel.toFloat() / expRange.toFloat()).coerceIn(0f, 1f)
}
package com.tranx.community.ui.activity

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tranx.community.data.model.User
import com.tranx.community.ui.screen.profile.ProfileUiState
import com.tranx.community.ui.screen.profile.ProfileViewModel
import com.tranx.community.ui.theme.TranxCommunityTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    paddingValues: PaddingValues
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
                        .padding(16.dp)
                ) {
                    // 用户头像和信息
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = state.user.username,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                UserStatItem(
                                    icon = Icons.Default.MonetizationOn,
                                    label = "硬币",
                                    value = state.user.coins.toString()
                                )
                                if (state.user.exp != null && state.user.userLevel != null) {
                                    UserStatItem(
                                        icon = Icons.Default.Star,
                                        label = "等级",
                                        value = "Lv${state.user.userLevel}"
                                    )
                                    UserStatItem(
                                        icon = Icons.Default.TrendingUp,
                                        label = "经验",
                                        value = state.user.exp.toString()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 签到卡片
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text("每日签到") },
                            supportingContent = {
                                Text(
                                    if (state.checkinStatus.checkedIn) 
                                        "今天已签到" 
                                    else 
                                        "签到可获得50硬币和25经验"
                                )
                            },
                            leadingContent = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                            },
                            trailingContent = {
                                if (!state.checkinStatus.checkedIn) {
                                    Button(
                                        onClick = {
                                            viewModel.checkin { message ->
                                                // 签到成功
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // 功能列表
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            ListItem(
                                headlineContent = { Text("我的帖子") },
                                leadingContent = { Icon(Icons.Default.Article, contentDescription = null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                            )
                            Divider()
                            ListItem(
                                headlineContent = { Text("我的收藏") },
                                leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                            )
                            Divider()
                            ListItem(
                                headlineContent = { Text("浏览历史") },
                                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                            )
                            Divider()
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

@Composable
private fun UserStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}


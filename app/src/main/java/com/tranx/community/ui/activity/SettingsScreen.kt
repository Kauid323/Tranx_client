package com.tranx.community.ui.activity

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.local.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onThemeModeChanged: (PreferencesManager.ThemeMode) -> Unit,
    onPrimaryColorChanged: (Long?) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onServerUrlChanged: () -> Unit
) {
    val prefsManager = TranxApp.instance.preferencesManager
    var showServerDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showPicuiTokenDialog by remember { mutableStateOf(false) }
    
    var currentThemeMode by remember { mutableStateOf(prefsManager.getThemeMode()) }
    var currentServerUrl by remember { mutableStateOf(prefsManager.getServerUrl()) }
    var useDynamicColor by remember { mutableStateOf(prefsManager.getUseDynamicColor()) }
    var currentPicuiToken by remember { mutableStateOf(prefsManager.getPicuiToken().orEmpty()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 外观设置
            ListItem(
                headlineContent = { Text("外观", style = MaterialTheme.typography.titleSmall) },
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    // 主题模式
                    ListItem(
                        headlineContent = { Text("主题模式") },
                        supportingContent = { 
                            Text(when (currentThemeMode) {
                                PreferencesManager.ThemeMode.LIGHT -> "浅色"
                                PreferencesManager.ThemeMode.DARK -> "深色"
                                PreferencesManager.ThemeMode.SYSTEM -> "跟随系统"
                            })
                        },
                        leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) }
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentThemeMode == PreferencesManager.ThemeMode.LIGHT,
                            onClick = {
                                currentThemeMode = PreferencesManager.ThemeMode.LIGHT
                                onThemeModeChanged(PreferencesManager.ThemeMode.LIGHT)
                            },
                            label = { Text("浅色") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentThemeMode == PreferencesManager.ThemeMode.DARK,
                            onClick = {
                                currentThemeMode = PreferencesManager.ThemeMode.DARK
                                onThemeModeChanged(PreferencesManager.ThemeMode.DARK)
                            },
                            label = { Text("深色") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentThemeMode == PreferencesManager.ThemeMode.SYSTEM,
                            onClick = {
                                currentThemeMode = PreferencesManager.ThemeMode.SYSTEM
                                onThemeModeChanged(PreferencesManager.ThemeMode.SYSTEM)
                            },
                            label = { Text("跟随系统") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Divider()
                    
                    // 莫奈取色开关（Android 12+）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ListItem(
                            headlineContent = { Text("莫奈取色") },
                            supportingContent = { Text("跟随系统壁纸颜色（Android 12+）") },
                            leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                            trailingContent = {
                                Switch(
                                    checked = useDynamicColor,
                                    onCheckedChange = { enabled ->
                                        useDynamicColor = enabled
                                        onDynamicColorChanged(enabled)
                                    }
                                )
                            }
                        )
                        
                        Divider()
                    }
                    
                    // 主题色（莫奈取色关闭时才显示）
                    if (!useDynamicColor) {
                        ListItem(
                            headlineContent = { Text("主题色") },
                            supportingContent = { Text("自定义应用主题色") },
                            leadingContent = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                            modifier = Modifier.clickable { showColorPicker = true }
                        )
                        
                        Divider()
                    }
                }
            }
            
            // 网络设置
            ListItem(
                headlineContent = { Text("网络", style = MaterialTheme.typography.titleSmall) },
                modifier = Modifier.padding(top = 16.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("服务器地址") },
                        supportingContent = { Text(currentServerUrl) },
                        leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { showServerDialog = true }
                    )
                    Divider()
                    ListItem(
                        headlineContent = { Text("Picui 图床 Token") },
                        supportingContent = { 
                            Text(
                                if (currentPicuiToken.isBlank()) "未设置"
                                else "已设置 (点击修改)"
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Image, contentDescription = null) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier.clickable { showPicuiTokenDialog = true }
                    )
                }
            }
            
            // 关于
            ListItem(
                headlineContent = { Text("关于", style = MaterialTheme.typography.titleSmall) },
                modifier = Modifier.padding(top = 16.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("版本信息") },
                        supportingContent = { Text("v1.0.0") },
                        leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                }
            }
        }
    }
    
    // 服务器配置对话框
    if (showServerDialog) {
        ServerConfigDialog(
            currentUrl = currentServerUrl,
            onDismiss = { showServerDialog = false },
            onConfirm = { newUrl ->
                prefsManager.saveServerUrl(newUrl)
                currentServerUrl = newUrl
                RetrofitClient.initialize(newUrl)
                showServerDialog = false
                onServerUrlChanged()
            }
        )
    }

    if (showPicuiTokenDialog) {
        PicuiTokenDialog(
            currentToken = currentPicuiToken,
            onDismiss = { showPicuiTokenDialog = false },
            onConfirm = { token ->
                currentPicuiToken = token
                prefsManager.savePicuiToken(token)
                showPicuiTokenDialog = false
            }
        )
    }
    
    // 颜色选择器
    if (showColorPicker) {
        ColorPickerDialog(
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                onPrimaryColorChanged(color?.toLong())
                showColorPicker = false
            }
        )
    }
}

@Composable
private fun PicuiTokenDialog(
    currentToken: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var token by remember { mutableStateOf(currentToken) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Picui 图床 Token") },
        text = {
            Column {
                Text(
                    text = "从 Picui 个人中心复制 Bearer Token，支持游客上传但推荐配置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Bearer Token") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(token.trim()) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Int?) -> Unit
) {
    val predefinedColors = listOf(
        Color(0xFF6750A4) to "紫色",
        Color(0xFFD32F2F) to "红色",
        Color(0xFF1976D2) to "蓝色",
        Color(0xFF388E3C) to "绿色",
        Color(0xFFF57C00) to "橙色",
        Color(0xFFE91E63) to "粉色",
        Color(0xFF00796B) to "青色",
        Color(0xFF5D4037) to "棕色"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题色") },
        text = {
            Column {
                predefinedColors.chunked(4).forEach { rowColors ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowColors.forEach { (color, _) ->
                Surface(
                    onClick = { 
                        val colorValue = color.toArgb().toLong() and 0xFFFFFFFFL
                        onColorSelected(colorValue.toInt())
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = color,
                    border = ButtonDefaults.outlinedButtonBorder
                ) {}
                        }
                        // 填充空白
                        repeat(4 - rowColors.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 恢复默认
                TextButton(
                    onClick = { onColorSelected(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("恢复默认")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerConfigDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var serverUrl by remember { mutableStateOf(currentUrl) }
    var useHttps by remember { 
        mutableStateOf(currentUrl.startsWith("https://"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务器设置") },
        text = {
            Column {
                Text(
                    text = "请输入服务器地址",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("协议: ", style = MaterialTheme.typography.bodyMedium)
                    
                    FilterChip(
                        selected = !useHttps,
                        onClick = { useHttps = false },
                        label = { Text("HTTP") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    FilterChip(
                        selected = useHttps,
                        onClick = { useHttps = true },
                        label = { Text("HTTPS") }
                    )
                }

                OutlinedTextField(
                    value = serverUrl.replace("http://", "").replace("https://", ""),
                    onValueChange = { serverUrl = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("例如: localhost:4999") },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "预览: ${if (useHttps) "https" else "http"}://${serverUrl.replace("http://", "").replace("https://", "")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "常用地址:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { serverUrl = "localhost:4999"; useHttps = false },
                        label = { Text("本地") }
                    )
                    AssistChip(
                        onClick = { serverUrl = "10.0.2.2:4999"; useHttps = false },
                        label = { Text("模拟器") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanUrl = serverUrl.replace("http://", "").replace("https://", "")
                    val finalUrl = "${if (useHttps) "https" else "http"}://$cleanUrl"
                    onConfirm(finalUrl)
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


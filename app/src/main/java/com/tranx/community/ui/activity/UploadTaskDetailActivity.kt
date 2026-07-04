package com.tranx.community.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tranx.community.TranxApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.api.ReviewRequest
import com.tranx.community.data.model.UploadTask
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlinx.coroutines.launch

class UploadTaskDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val isReviewMode = intent.getBooleanExtra("IS_REVIEW_MODE", false)
        
        if (taskId == -1) {
            Toast.makeText(this, "无效的任务ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            val prefsManager = TranxApp.instance.preferencesManager
            val themeMode = remember { prefsManager.getThemeMode() }
            val primaryColor = remember { prefsManager.getPrimaryColor() }
            val useDynamicColor = remember { prefsManager.getUseDynamicColor() }
            val darkTheme = when (themeMode) {
                com.tranx.community.data.local.PreferencesManager.ThemeMode.LIGHT -> false
                com.tranx.community.data.local.PreferencesManager.ThemeMode.DARK -> true
                com.tranx.community.data.local.PreferencesManager.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            TranxCommunityTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor,
                primaryColor = if (useDynamicColor) null else primaryColor
            ) {
                UploadTaskDetailScreen(
                    taskId = taskId,
                    isReviewMode = isReviewMode,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadTaskDetailScreen(taskId: Int, isReviewMode: Boolean, onBack: () -> Unit) {
    var task by remember { mutableStateOf<UploadTask?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val loadTask = {
        scope.launch {
            isLoading = true
            try {
                val token = TranxApp.instance.preferencesManager.getToken() ?: ""
                val response = RetrofitClient.getApiService().getUploadTaskDetail(token, taskId)
                if (response.code == 200) {
                    task = response.data
                } else {
                    Toast.makeText(TranxApp.instance, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(TranxApp.instance, "获取详情失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(taskId) {
        loadTask()
    }

    val reviewApp = { accept: Int, reason: String? ->
        scope.launch {
            try {
                val token = TranxApp.instance.preferencesManager.getToken() ?: ""
                val response = RetrofitClient.getApiService().reviewApp(
                    token,
                    ReviewRequest(taskId, accept, reason)
                )
                if (response.code == 200) {
                    Toast.makeText(TranxApp.instance, if (accept == 1) "审核已通过" else "已拒绝", Toast.LENGTH_SHORT).show()
                    onBack() // 审核完返回列表
                } else {
                    Toast.makeText(TranxApp.instance, response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(TranxApp.instance, "审核失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isReviewMode) "审核详情 (16.9)" else "任务详情 (16.7)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            if (isReviewMode && task != null && task!!.status == "pending") {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRejectDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("拒绝")
                        }
                        Button(
                            onClick = { reviewApp(1, null) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("通过")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (task == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("任务信息不存在")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (task!!.iconUrl != null) {
                            AsyncImage(
                                model = task!!.iconUrl,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Column {
                            Text(task!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(task!!.packageName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                InfoItem("任务 ID", task!!.taskId.toString())
                InfoItem("版本", "${task!!.version} (${task!!.versionCode ?: "未知"})")
                InfoItem("包大小", task!!.size?.let { formatSize(it) } ?: "未知")
                InfoItem("状态", task!!.statusLabel ?: task!!.status)
                InfoItem("提交时间", task!!.uploadTime ?: "未知")
                InfoItem("渠道", task!!.channel ?: "未知")
                
                if (task!!.downloadUrl != null) {
                    InfoItem("下载链接", task!!.downloadUrl!!)
                }
                
                if (task!!.description != null) {
                    InfoItem("应用介绍", task!!.description!!)
                }

                if (task!!.status == "rejected" && task!!.rejectReason != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("拒绝原因", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(task!!.rejectReason!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                
                // 为了底部按钮不遮挡内容
                if (isReviewMode && task!!.status == "pending") {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("拒绝审核") },
            text = {
                Column {
                    Text("请输入拒绝原因:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    reviewApp(0, rejectReason)
                    showRejectDialog = false
                    rejectReason = ""
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun formatSize(size: Long): String {
    if (size <= 0) return "未知大小"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) return "超大文件"
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}

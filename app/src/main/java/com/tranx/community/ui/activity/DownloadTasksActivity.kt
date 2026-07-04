package com.tranx.community.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tranx.community.TranxApp
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.ui.theme.TranxCommunityTheme
import com.tranx.community.util.DownloadStatus
import com.tranx.community.util.DownloadTask
import kotlinx.coroutines.delay

class DownloadTasksActivity : ComponentActivity() {

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
                PreferencesManager.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            TranxCommunityTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor,
                primaryColor = if (useDynamicColor) null else primaryColor
            ) {
                DownloadTasksScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadTasksScreen(onBack: () -> Unit) {
    val downloadManager = TranxApp.instance.downloadManager
    // Simple way to refresh UI
    var refreshTick by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            refreshTick++
        }
    }

    val tasks = remember(refreshTick) { downloadManager.tasks.toList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载任务") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无下载任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    DownloadTaskItem(
                        task = task,
                        onDelete = { downloadManager.deleteTask(task) },
                        onToggle = {
                            if (task.status == DownloadStatus.DOWNLOADING) {
                                downloadManager.pauseDownload(task)
                            } else {
                                downloadManager.startDownload(task)
                            }
                        },
                        onOpenDir = { downloadManager.openFileDirectory(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadTaskItem(
    task: DownloadTask,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onOpenDir: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = task.status == DownloadStatus.COMPLETED) { onOpenDir() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (task.status) {
                        DownloadStatus.COMPLETED -> Icons.Default.FileDownloadDone
                        DownloadStatus.DOWNLOADING -> Icons.Default.Downloading
                        DownloadStatus.PAUSED -> Icons.Default.Pause
                        DownloadStatus.ERROR -> Icons.Default.Error
                        else -> Icons.Default.FileDownload
                    },
                    contentDescription = null,
                    tint = if (task.status == DownloadStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        when (task.status) {
                            DownloadStatus.COMPLETED -> "已完成"
                            DownloadStatus.DOWNLOADING -> "下载中..."
                            DownloadStatus.PAUSED -> "已暂停"
                            DownloadStatus.ERROR -> "下载出错"
                            else -> "待下载"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (task.status != DownloadStatus.COMPLETED) {
                    IconButton(onClick = onToggle) {
                        Icon(if (task.status == DownloadStatus.DOWNLOADING) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            val progress = if (task.totalLength > 0) task.downloadedLength.toFloat() / task.totalLength else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (task.status == DownloadStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${formatSize(task.downloadedLength)} / ${formatSize(task.totalLength)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (task.status == DownloadStatus.COMPLETED) {
                    Text("点击查看文件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun formatSize(size: Long): String {
    if (size <= 0) return "未知大小"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) return "超大文件"
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

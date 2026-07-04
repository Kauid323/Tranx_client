package com.tranx.community.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.UploadTask
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlinx.coroutines.launch

class PendingAppReviewsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                PendingAppReviewsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingAppReviewsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf<List<UploadTask>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val loadTasks = {
        scope.launch {
            isLoading = true
            try {
                val token = TranxApp.instance.preferencesManager.getToken() ?: ""
                val response = RetrofitClient.getApiService().getPendingApps(token)
                if (response.code == 200) {
                    tasks = response.data?.list ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTasks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待审核应用") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无待审核应用")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
                    PendingTaskItem(
                        task = task,
                        onClick = {
                            val intent = Intent(context, UploadTaskDetailActivity::class.java).apply {
                                putExtra("TASK_ID", task.taskId)
                                putExtra("IS_REVIEW_MODE", true)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PendingTaskItem(
    task: UploadTask,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(task.packageName, style = MaterialTheme.typography.bodySmall)
                    Text("版本: ${task.version}", style = MaterialTheme.typography.bodySmall)
                    Text("上传者: ${task.statusLabel ?: "未知"}", style = MaterialTheme.typography.bodySmall)
                    task.uploadTime?.let {
                        Text("时间: $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

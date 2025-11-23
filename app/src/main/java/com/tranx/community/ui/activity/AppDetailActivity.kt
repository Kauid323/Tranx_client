package com.tranx.community.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.tranx.community.TranxApp
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.data.model.AppDetail
import com.tranx.community.ui.screen.app.AppDetailUiState
import com.tranx.community.ui.screen.app.AppDetailViewModel
import com.tranx.community.ui.screen.app.formatFileSize
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlinx.coroutines.launch

class AppDetailActivity : ComponentActivity() {
    
    private val viewModel: AppDetailViewModel by viewModels {
        AppDetailViewModelFactory(intent.getStringExtra("PACKAGE_NAME") ?: "")
    }
    
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
                AppDetailScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCoinDialog by remember { mutableStateOf(false) }
    var showScreenshot by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AppDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is AppDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadAppDetail() }) {
                            Text("重试")
                        }
                    }
                }
            }
            
            is AppDetailUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 应用基本信息
                    item {
                        AppInfoCard(app = state.app)
                    }
                    
                    // 操作按钮
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 下载按钮
                            Button(
                                onClick = {
                                    state.app.downloadUrl?.let { url ->
                                        viewModel.recordDownload()
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } ?: Toast.makeText(context, "下载链接不可用", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("下载")
                            }
                            
                            // 投币按钮
                            OutlinedButton(
                                onClick = { showCoinDialog = true }
                            ) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("投币 (${state.app.totalCoins})")
                            }
                        }
                    }
                    
                    // 应用截图
                    if (!state.app.screenshots.isNullOrEmpty()) {
                        item {
                            Text(
                                "应用截图",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.app.screenshots) { screenshot ->
                                    Card(
                                        modifier = Modifier
                                            .height(200.dp)
                                            .clickable { showScreenshot = screenshot }
                                    ) {
                                        AsyncImage(
                                            model = screenshot,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxHeight(),
                                            contentScale = ContentScale.FillHeight
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 应用描述
                    if (!state.app.description.isNullOrBlank()) {
                        item {
                            Text(
                                "应用介绍",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.app.description,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    // 更新内容
                    if (!state.app.updateContent.isNullOrBlank()) {
                        item {
                            Text(
                                "更新内容",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.app.updateContent,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    // 应用信息
                    item {
                        Text(
                            "应用信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        AppDetailsCard(app = state.app)
                    }
                }
            }
        }
    }
    
    // 投币对话框
    if (showCoinDialog && uiState is AppDetailUiState.Success) {
        CoinDialog(
            onDismiss = { showCoinDialog = false },
            onConfirm = { coins ->
                scope.launch {
                    viewModel.coinApp(
                        coins = coins,
                        onSuccess = {
                            Toast.makeText(context, "投币成功！", Toast.LENGTH_SHORT).show()
                            showCoinDialog = false
                        },
                        onError = { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )
    }
    
    // 截图查看对话框
    showScreenshot?.let { screenshot ->
        Dialog(
            onDismissRequest = { showScreenshot = null }
        ) {
            Card(
                modifier = Modifier.fillMaxSize(0.9f)
            ) {
                AsyncImage(
                    model = screenshot,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showScreenshot = null },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun AppInfoCard(app: AppDetail) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 应用图标
            Card(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                AsyncImage(
                    model = app.iconUrl,
                    contentDescription = app.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // 应用信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // 开发者
                app.developerName?.let { developer ->
                    Text(
                        text = developer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 评分和下载量
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 评分
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format("%.1f", app.rating),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "(${app.ratingCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 下载量
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDownloadCount(app.downloadCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 标签
                if (!app.tags.isNullOrEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(app.tags) { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppDetailsCard(app: AppDetail) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 版本
            AppDetailItem("版本", "${app.version} (${app.versionCode})")
            
            // 大小
            AppDetailItem("大小", formatFileSize(app.size))
            
            // 更新时间
            app.updateTime?.let {
                AppDetailItem("更新时间", it)
            }
            
            // 上传者
            app.uploaderName?.let {
                AppDetailItem("上传者", it)
            }
            
            // 分类
            if (app.mainCategory != null && app.subCategory != null) {
                AppDetailItem("分类", "${app.mainCategory} / ${app.subCategory}")
            }
            
            // 渠道
            app.channel?.let {
                AppDetailItem("渠道", getChannelName(it))
            }
            
            // 广告级别
            app.adLevel?.let {
                AppDetailItem("广告", getAdLevelName(it))
            }
            
            // 付费类型
            app.paymentType?.let {
                AppDetailItem("付费", getPaymentTypeName(it))
            }
            
            // 运营方式
            app.operationType?.let {
                AppDetailItem("运营", getOperationTypeName(it))
            }
        }
    }
}

@Composable
fun AppDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CoinDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var coinAmount by remember { mutableStateOf(1) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("投币支持") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("选择投币数量：")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 1..5) {
                        FilterChip(
                            selected = coinAmount == i,
                            onClick = { coinAmount = i },
                            label = { Text(i.toString()) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 6..10) {
                        FilterChip(
                            selected = coinAmount == i,
                            onClick = { coinAmount = i },
                            label = { Text(i.toString()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(coinAmount) }) {
                Text("确认投币")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

fun formatDownloadCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 10000 -> String.format("%.1fK", count / 1000.0)
        count < 100000 -> String.format("%dK", count / 1000)
        else -> String.format("%.1fW", count / 10000.0)
    }
}

fun getChannelName(channel: String): String {
    return when (channel) {
        "official" -> "官方版"
        "international" -> "国际版"
        "test" -> "测试版"
        "custom" -> "定制版"
        else -> channel
    }
}

fun getAdLevelName(adLevel: String): String {
    return when (adLevel) {
        "none" -> "无广告"
        "few" -> "少量广告"
        "many" -> "超多广告"
        "adware" -> "广告软件"
        else -> adLevel
    }
}

fun getPaymentTypeName(paymentType: String): String {
    return when (paymentType) {
        "free" -> "免费"
        "iap" -> "内购"
        "few_iap" -> "少量内购"
        "paid" -> "不给钱不让用"
        else -> paymentType
    }
}

fun getOperationTypeName(operationType: String): String {
    return when (operationType) {
        "team" -> "团队开发"
        "indie" -> "独立开发"
        "opensource" -> "开源软件"
        else -> operationType
    }
}

class AppDetailViewModelFactory(
    private val packageName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppDetailViewModel(
                SavedStateHandle(mapOf("packageName" to packageName))
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

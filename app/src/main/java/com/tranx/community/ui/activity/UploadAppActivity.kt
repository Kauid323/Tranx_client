package com.tranx.community.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.google.accompanist.permissions.*
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.data.model.UploadAppRequest
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UploadAppActivity : ComponentActivity() {
    
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
                UploadAppScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun UploadAppScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showInstalledApps by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    var customUrl by remember { mutableStateOf("") }
    var showUploadDialog by remember { mutableStateOf(false) }
    
    // 权限状态 - 仅在Android 10及以下需要存储权限
    val needStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
    val storagePermissionState = if (needStoragePermission) {
        rememberPermissionState(permission = Manifest.permission.READ_EXTERNAL_STORAGE)
    } else null
    
    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // 处理选择的APK文件
            scope.launch {
                handleSelectedApk(context, uri) { appInfo ->
                    selectedApp = InstalledApp(
                        packageName = appInfo.packageName,
                        name = appInfo.name,
                        version = appInfo.version,
                        versionCode = appInfo.versionCode,
                        size = appInfo.size,
                        iconUrl = null,
                        apkPath = uri.toString()
                    )
                    showUploadDialog = true
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("上传应用") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "选择上传方式",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // 从已安装应用提取
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInstalledApps = true }
                ) {
                    ListItem(
                        headlineContent = { Text("从已安装应用提取") },
                        supportingContent = { Text("选择设备上已安装的应用") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    )
                }
            }
            
            // 从本地存储选择
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!needStoragePermission || 
                                storagePermissionState?.status?.isGranted == true) {
                                filePickerLauncher.launch("application/vnd.android.package-archive")
                            } else {
                                storagePermissionState?.launchPermissionRequest()
                            }
                        }
                ) {
                    ListItem(
                        headlineContent = { Text("从本地存储选择") },
                        supportingContent = { Text("选择存储中的APK文件") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    )
                }
            }
            
            // 自定义下载URL
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBottomSheet = true }
                ) {
                    ListItem(
                        headlineContent = { Text("自定义下载URL") },
                        supportingContent = { Text("提供应用的下载链接") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    )
                }
            }
            
            // 说明
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "上传须知",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "• 上传的应用需要经过审核才能在应用市场显示\n" +
                            "• 请确保应用安全无毒，不包含恶意代码\n" +
                            "• 请勿上传侵权或违法应用\n" +
                            "• 上传后可在个人中心查看审核状态",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
    
    // 已安装应用列表
    if (showInstalledApps) {
        InstalledAppsBottomSheet(
            onDismiss = { showInstalledApps = false },
            onAppSelected = { app ->
                selectedApp = app
                showInstalledApps = false
                showUploadDialog = true
            }
        )
    }
    
    // 自定义URL底部弹窗
    if (showBottomSheet) {
        CustomUrlBottomSheet(
            url = customUrl,
            onUrlChange = { customUrl = it },
            onDismiss = { showBottomSheet = false },
            onConfirm = {
                if (customUrl.isNotBlank()) {
                    // 创建虚拟应用信息
                    selectedApp = InstalledApp(
                        packageName = "custom.url.app",
                        name = "自定义应用",
                        version = "1.0.0",
                        versionCode = 1,
                        size = 0,
                        iconUrl = null,
                        apkPath = customUrl
                    )
                    showBottomSheet = false
                    showUploadDialog = true
                } else {
                    Toast.makeText(context, "请输入下载链接", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    // 上传信息填写界面
    if (showUploadDialog && selectedApp != null) {
        UploadInfoScreen(
            app = selectedApp!!,
            onBack = { 
                showUploadDialog = false
                selectedApp = null
            },
            onUpload = { uploadRequest ->
                scope.launch {
                    uploadApp(context, uploadRequest) { success ->
                        if (success) {
                            Toast.makeText(context, "上传成功，等待审核", Toast.LENGTH_LONG).show()
                            onBack()
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledAppsBottomSheet(
    onDismiss: () -> Unit,
    onAppSelected: (InstalledApp) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            apps = getInstalledApps(context)
            isLoading = false
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                "选择应用",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("搜索应用") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val filteredApps = if (searchQuery.isBlank()) apps else {
                    apps.filter { 
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                    }
                }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps) { app ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                        ) {
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        app.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        Text(
                                            app.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${app.version} | ${formatFileSize(app.size)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                leadingContent = {
                                    Card(
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        AsyncImage(
                                            model = app.iconDrawable,
                                            contentDescription = app.name,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomUrlBottomSheet(
    url: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "自定义下载URL",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "请输入应用的直接下载链接（APK文件URL）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("下载链接") },
                placeholder = { Text("https://example.com/app.apk") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确认")
                }
            }
        }
    }
}

// 数据类
data class InstalledApp(
    val packageName: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val size: Long,
    val iconUrl: String?,
    val apkPath: String,
    val iconDrawable: Any? = null
)

// 获取已安装应用列表
fun getInstalledApps(context: android.content.Context): List<InstalledApp> {
    val pm = context.packageManager
    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    
    return packages
        .filter { 
            // 过滤系统应用
            (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
        .mapNotNull { appInfo ->
            try {
                val packageInfo = pm.getPackageInfo(appInfo.packageName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo.packageName)
                val apkFile = File(appInfo.sourceDir)
                
                InstalledApp(
                    packageName = appInfo.packageName,
                    name = appName,
                    version = packageInfo.versionName ?: "1.0.0",
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    },
                    size = apkFile.length(),
                    iconUrl = null,
                    apkPath = appInfo.sourceDir,
                    iconDrawable = icon
                )
            } catch (e: Exception) {
                null
            }
        }
        .sortedBy { it.name }
}

// 处理选择的APK文件
suspend fun handleSelectedApk(
    context: android.content.Context,
    uri: Uri,
    onSuccess: (InstalledApp) -> Unit
) {
    // TODO: 解析APK文件信息
    withContext(Dispatchers.Main) {
        Toast.makeText(context, "APK文件解析功能开发中", Toast.LENGTH_SHORT).show()
    }
}

// 上传应用
suspend fun uploadApp(
    context: android.content.Context,
    request: UploadAppRequest,
    onResult: (Boolean) -> Unit
) {
    try {
        val token = TranxApp.instance.preferencesManager.getToken()
        if (token.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
            return
        }
        
        val response = RetrofitClient.getApiService().uploadApp(token, request)
        withContext(Dispatchers.Main) {
            if (response.code == 200) {
                onResult(true)
            } else {
                Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
            onResult(false)
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadInfoScreen(
    app: InstalledApp,
    onBack: () -> Unit,
    onUpload: (UploadAppRequest) -> Unit
) {
    var name by remember { mutableStateOf(app.name) }
    var description by remember { mutableStateOf("") }
    var updateContent by remember { mutableStateOf("初始版本") }
    var developerName by remember { mutableStateOf("") }
    var shareDesc by remember { mutableStateOf("") }
    
    // 分类相关
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMainCategory by remember { mutableStateOf("") }
    var subCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSubCategory by remember { mutableStateOf("") }
    
    // 属性选择
    var channel by remember { mutableStateOf("official") }
    var adLevel by remember { mutableStateOf("none") }
    var paymentType by remember { mutableStateOf("free") }
    var operationType by remember { mutableStateOf("indie") }
    
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 加载分类
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = RetrofitClient.getApiService().getAppCategories()
                if (response.code == 200) {
                    categories = response.data ?: emptyList()
                }
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
    
    // 加载子分类
    LaunchedEffect(selectedMainCategory) {
        if (selectedMainCategory.isNotEmpty()) {
            scope.launch {
                try {
                    val response = RetrofitClient.getApiService().getSubCategories(selectedMainCategory)
                    if (response.code == 200) {
                        subCategories = response.data?.subCategories ?: emptyList()
                    }
                } catch (e: Exception) {
                    // 忽略错误
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("填写应用信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (name.isNotBlank() && selectedMainCategory.isNotEmpty() && 
                                selectedSubCategory.isNotEmpty() && developerName.isNotBlank()) {
                                val request = UploadAppRequest(
                                    packageName = app.packageName,
                                    name = name,
                                    iconUrl = app.iconUrl,
                                    version = app.version,
                                    versionCode = app.versionCode,
                                    size = app.size,
                                    channel = channel,
                                    mainCategory = selectedMainCategory,
                                    subCategory = selectedSubCategory,
                                    screenshots = null,
                                    description = description.ifBlank { null },
                                    shareDesc = shareDesc.ifBlank { null },
                                    updateContent = updateContent.ifBlank { null },
                                    developerName = developerName.ifBlank { null },
                                    adLevel = adLevel,
                                    paymentType = paymentType,
                                    operationType = operationType,
                                    downloadUrl = app.apkPath
                                )
                                onUpload(request)
                            }
                        },
                        enabled = !isLoading && name.isNotBlank() && 
                                selectedMainCategory.isNotEmpty() && 
                                selectedSubCategory.isNotEmpty() &&
                                developerName.isNotBlank()
                    ) {
                        Text("上传")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本信息
            item {
                Text(
                    "基本信息",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("应用名称 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            item {
                OutlinedTextField(
                    value = developerName,
                    onValueChange = { developerName = it },
                    label = { Text("开发者名称 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            // 分类选择
            item {
                Text(
                    "分类选择",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            item {
                var mainCategoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = mainCategoryExpanded,
                    onExpandedChange = { mainCategoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMainCategory,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("主分类 *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mainCategoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = mainCategoryExpanded,
                        onDismissRequest = { mainCategoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = { 
                                    selectedMainCategory = category
                                    selectedSubCategory = ""
                                    mainCategoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            if (subCategories.isNotEmpty()) {
                item {
                    var subCategoryExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = subCategoryExpanded,
                        onExpandedChange = { subCategoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSubCategory,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("子分类 *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = subCategoryExpanded,
                            onDismissRequest = { subCategoryExpanded = false }
                        ) {
                            subCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = { 
                                        selectedSubCategory = category
                                        subCategoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // 应用属性
            item {
                Text(
                    "应用属性",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 渠道
                    Column(modifier = Modifier.weight(1f)) {
                        Text("渠道", style = MaterialTheme.typography.bodyMedium)
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = channel == "official",
                                onClick = { channel = "official" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("官方", style = MaterialTheme.typography.bodySmall)
                            }
                            SegmentedButton(
                                selected = channel == "custom",
                                onClick = { channel = "custom" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("定制", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    // 广告
                    Column(modifier = Modifier.weight(1f)) {
                        Text("广告", style = MaterialTheme.typography.bodyMedium)
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = adLevel == "none",
                                onClick = { adLevel = "none" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("无", style = MaterialTheme.typography.bodySmall)
                            }
                            SegmentedButton(
                                selected = adLevel == "few",
                                onClick = { adLevel = "few" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("少量", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 付费类型
                    Column(modifier = Modifier.weight(1f)) {
                        Text("付费", style = MaterialTheme.typography.bodyMedium)
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = paymentType == "free",
                                onClick = { paymentType = "free" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("免费", style = MaterialTheme.typography.bodySmall)
                            }
                            SegmentedButton(
                                selected = paymentType == "iap",
                                onClick = { paymentType = "iap" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("内购", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    // 运营方式
                    Column(modifier = Modifier.weight(1f)) {
                        Text("运营", style = MaterialTheme.typography.bodyMedium)
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = operationType == "indie",
                                onClick = { operationType = "indie" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("独立", style = MaterialTheme.typography.bodySmall)
                            }
                            SegmentedButton(
                                selected = operationType == "team",
                                onClick = { operationType = "team" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("团队", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            
            // 详细信息
            item {
                Text(
                    "详细信息",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("应用介绍") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
            
            item {
                OutlinedTextField(
                    value = updateContent,
                    onValueChange = { updateContent = it },
                    label = { Text("更新内容") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
            
            item {
                OutlinedTextField(
                    value = shareDesc,
                    onValueChange = { shareDesc = it },
                    label = { Text("分享描述") },
                    placeholder = { Text("分享给朋友时显示的描述") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            // 应用信息预览
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "应用信息",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text("包名: ${app.packageName}", style = MaterialTheme.typography.bodySmall)
                        Text("版本: ${app.version} (${app.versionCode})", style = MaterialTheme.typography.bodySmall)
                        Text("大小: ${formatFileSize(app.size)}", style = MaterialTheme.typography.bodySmall)
                        Text("下载URL: ${if (app.apkPath.startsWith("http")) app.apkPath else "本地文件"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

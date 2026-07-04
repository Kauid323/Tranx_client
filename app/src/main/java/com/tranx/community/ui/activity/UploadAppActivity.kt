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
import com.tranx.community.data.model.ValueLabelOption
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
    var showInstalledApps by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
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
            scope.launch {
                handleSelectedApk(context, uri) { appInfo ->
                    selectedApp = InstalledApp(
                        packageName = appInfo.packageName,
                        name = appInfo.name,
                        version = appInfo.version,
                        versionCode = appInfo.versionCode,
                        size = appInfo.size,
                        iconUrl = "",
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
                    "应用来源",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { showInstalledApps = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("选择已安装应用")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                if (!needStoragePermission || 
                                    storagePermissionState?.status?.isGranted == true) {
                                    filePickerLauncher.launch("application/vnd.android.package-archive")
                                } else {
                                    storagePermissionState?.launchPermissionRequest()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("从本地文件选择 (APK)")
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text("或者直接输入下载链接", style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            label = { Text("下载 URL") },
                            placeholder = { Text("https://example.com/app.apk") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (customUrl.isNotBlank()) {
                                        selectedApp = InstalledApp(
                                            packageName = "",
                                            name = "新应用",
                                            version = "1.0.0",
                                            versionCode = 1,
                                            size = 0,
                                            iconUrl = "",
                                            apkPath = customUrl
                                        )
                                        showUploadDialog = true
                                    }
                                }) {
                                    Icon(Icons.Default.Send, contentDescription = "确定")
                                }
                            }
                        )
                    }
                }
            }
            
            item {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "上传须知",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "• 上传的应用需要经过审核才能在应用市场显示\n" +
                            "• 请确保应用安全无毒，不包含恶意代码\n" +
                            "• 请务必填写正确的图标 URL 和至少一张截图 URL",
                            style = MaterialTheme.typography.bodyMedium
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
    val context = LocalContext.current

    var packageName by remember { mutableStateOf(app.packageName) }
    var name by remember { mutableStateOf(app.name) }
    var version by remember { mutableStateOf(app.version) }
    var versionCode by remember { mutableStateOf(app.versionCode) }
    val size = app.size

    var iconUrl by remember { mutableStateOf(app.iconUrl ?: "") }
    var screenshotsText by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }
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

    var channelOptions by remember { mutableStateOf<List<ValueLabelOption>>(emptyList()) }
    var adLevelOptions by remember { mutableStateOf<List<ValueLabelOption>>(emptyList()) }
    var paymentTypeOptions by remember { mutableStateOf<List<ValueLabelOption>>(emptyList()) }
    var operationTypeOptions by remember { mutableStateOf<List<ValueLabelOption>>(emptyList()) }
    
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

    // 加载上传选项（渠道/广告/付费/运营方式）
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val api = RetrofitClient.getApiService()
                // 这些接口不需要 Token，且返回结构稳定，避免 getUploadOptions 返回 Map 导致的类型推断问题
                val chResp = api.getAppChannels()
                if (chResp.code == 200) {
                    channelOptions = (chResp.data ?: emptyList()).map { m ->
                        ValueLabelOption(value = m["value"] ?: "", label = m["label"] ?: (m["value"] ?: ""))
                    }
                }

                val adResp = api.getAppAdLevels()
                if (adResp.code == 200) {
                    adLevelOptions = (adResp.data ?: emptyList()).map { m ->
                        ValueLabelOption(value = m["value"] ?: "", label = m["label"] ?: (m["value"] ?: ""))
                    }
                }

                val payResp = api.getAppPaymentTypes()
                if (payResp.code == 200) {
                    paymentTypeOptions = (payResp.data ?: emptyList()).map { m ->
                        ValueLabelOption(value = m["value"] ?: "", label = m["label"] ?: (m["value"] ?: ""))
                    }
                }

                val opResp = api.getAppOperationTypes()
                if (opResp.code == 200) {
                    operationTypeOptions = (opResp.data ?: emptyList()).map { m ->
                        ValueLabelOption(value = m["value"] ?: "", label = m["label"] ?: (m["value"] ?: ""))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(selectedMainCategory) {
        if (selectedMainCategory.isNotBlank()) {
            try {
                val api = RetrofitClient.getApiService()
                val resp = api.getSubCategories(selectedMainCategory)
                subCategories = resp.data?.subCategories ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
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
                            if (name.isBlank() || selectedMainCategory.isBlank() || iconUrl.isBlank() || screenshotsText.isBlank() || downloadUrl.isBlank()) {
                                Toast.makeText(context, "请补全必填信息 (*)", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            

                            val screenshots = screenshotsText.split("\n")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }

                            onUpload(
                                UploadAppRequest(
                                    packageName = packageName,
                                    name = name,
                                    iconUrl = iconUrl,
                                    version = version,
                                    versionCode = versionCode,
                                    size = size,
                                    channel = channel,
                                    mainCategory = selectedMainCategory,
                                    subCategory = selectedSubCategory,
                                    screenshots = screenshots,
                                    description = description.ifBlank { null },
                                    shareDesc = shareDesc.ifBlank { null },
                                    updateContent = updateContent.ifBlank { null },
                                    developer_name = developerName.ifBlank { null },
                                    adLevel = adLevel,
                                    paymentType = paymentType,
                                    operationType = operationType,
                                    downloadUrl = downloadUrl
                                )
                            )
                        }
                    ) {
                        Text("提交审核", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("基本信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("应用名称 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("包名 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = version,
                            onValueChange = { version = it },
                            label = { Text("版本名 *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = versionCode.toString(),
                            onValueChange = { versionCode = it.toIntOrNull() ?: 0 },
                            label = { Text("版本号 *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Text("媒体资源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = iconUrl,
                        onValueChange = { iconUrl = it },
                        label = { Text("图标 URL *") },
                        placeholder = { Text("https://example.com/icon.png") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = screenshotsText,
                        onValueChange = { screenshotsText = it },
                        label = { Text("截图 URL (一行一个) *") },
                        placeholder = { Text("https://example.com/screen1.png\nhttps://example.com/screen2.png") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("下载 URL *") },
                        placeholder = { Text("https://example.com/app.apk") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            item {
                Text("分类与属性", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            modifier = Modifier.fillMaxWidth().menuAnchor()
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

                    if (subCategories.isNotEmpty()) {
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
                                modifier = Modifier.fillMaxWidth().menuAnchor()
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
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 渠道选择
                    var channelExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = channelExpanded,
                        onExpandedChange = { channelExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        val selectedLabel = channelOptions.find { it.value == channel }?.label ?: channel
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {} ,
                            readOnly = true,
                            label = { Text("渠道") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = channelExpanded,
                            onDismissRequest = { channelExpanded = false }
                        ) {
                            channelOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        channel = opt.value
                                        channelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 广告
                    var adExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = adExpanded,
                        onExpandedChange = { adExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        val selectedLabel = adLevelOptions.find { it.value == adLevel }?.label ?: adLevel
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {} ,
                            readOnly = true,
                            label = { Text("广告") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = adExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = adExpanded,
                            onDismissRequest = { adExpanded = false }
                        ) {
                            adLevelOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        adLevel = opt.value
                                        adExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = developerName,
                    onValueChange = { developerName = it },
                    label = { Text("开发者名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text("应用介绍", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("详细介绍") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    
                    OutlinedTextField(
                        value = updateContent,
                        onValueChange = { updateContent = it },
                        label = { Text("更新内容") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    
                    OutlinedTextField(
                        value = shareDesc,
                        onValueChange = { shareDesc = it },
                        label = { Text("分享描述") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
    }
}

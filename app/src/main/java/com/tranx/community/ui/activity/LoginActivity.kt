package com.tranx.community.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tranx.community.TranxApp
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.ui.screen.login.LoginUiState
import com.tranx.community.ui.screen.login.LoginViewModel
import com.tranx.community.ui.theme.TranxCommunityTheme

class LoginActivity : ComponentActivity() {
    private val viewModel: LoginViewModel by viewModels()

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
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> {
                if (isRegisterMode) {
                    isRegisterMode = false
                    password = ""
                    confirmPassword = ""
                } else {
                    onLoginSuccess()
                }
            }
            else -> {}
        }
    }

    if (showServerDialog) {
        ServerConfigDialog(
            currentUrl = serverUrl,
            onDismiss = { showServerDialog = false },
            onConfirm = { newUrl ->
                viewModel.updateServerUrl(newUrl)
                showServerDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRegisterMode) "用户注册" else "登录") },
                actions = {
                    IconButton(onClick = { showServerDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "服务器设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "欢迎来到 Tranx 社区",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "服务器: $serverUrl",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) 
                        VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = { 
                            focusManager.clearFocus()
                            if (!isRegisterMode) {
                                viewModel.login(username, password)
                            }
                        }
                    )
                )

                if (isRegisterMode) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("确认密码") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码"
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.register(username, password, confirmPassword)
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isRegisterMode) {
                            viewModel.register(username, password, confirmPassword)
                        } else {
                            viewModel.login(username, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = uiState !is LoginUiState.Loading
                ) {
                    if (uiState is LoginUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isRegisterMode) "注册" else "登录")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        password = ""
                        confirmPassword = ""
                        viewModel.resetUiState()
                    }
                ) {
                    Text(if (isRegisterMode) "已有账号？点击登录" else "没有账号？点击注册")
                }

                when (val state = uiState) {
                    is LoginUiState.Error -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is LoginUiState.Success -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {}
                }
            }
        }
    }
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


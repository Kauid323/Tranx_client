package com.tranx.community.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.Folder
import com.tranx.community.ui.theme.TranxCommunityTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyFavoritesActivity : ComponentActivity() {
    private val viewModel: MyFavoritesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefsManager = TranxApp.instance.preferencesManager
            TranxCommunityTheme(
                darkTheme = false,
                dynamicColor = prefsManager.getUseDynamicColor(),
                primaryColor = prefsManager.getPrimaryColor()
            ) {
                MyFavoritesScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onFolderClick = { folderId, folderName ->
                        val intent = Intent(this, FolderDetailActivity::class.java)
                        intent.putExtra("FOLDER_ID", folderId)
                        intent.putExtra("FOLDER_NAME", folderName)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

sealed class MyFavoritesUiState {
    object Loading : MyFavoritesUiState()
    data class Success(val folders: List<Folder>) : MyFavoritesUiState()
    data class Error(val message: String) : MyFavoritesUiState()
}

class MyFavoritesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MyFavoritesUiState>(MyFavoritesUiState.Loading)
    val uiState: StateFlow<MyFavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            try {
                _uiState.value = MyFavoritesUiState.Loading
                val token = TranxApp.instance.preferencesManager.getToken() ?: throw Exception("未登录")
                val response = RetrofitClient.getApiService().getMyFolders(token)
                if (response.code == 200) {
                    _uiState.value = MyFavoritesUiState.Success(response.data ?: emptyList())
                } else {
                    _uiState.value = MyFavoritesUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = MyFavoritesUiState.Error(e.message ?: "加载失败")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFavoritesScreen(
    viewModel: MyFavoritesViewModel,
    onBack: () -> Unit,
    onFolderClick: (Int, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的收藏") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is MyFavoritesUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MyFavoritesUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadFolders() }) { Text("重试") }
                }
                is MyFavoritesUiState.Success -> {
                    if (state.folders.isEmpty()) {
                        Text("暂无收藏夹", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.folders) { folder ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFolderClick(folder.id, folder.name) }
                                ) {
                                    ListItem(
                                        headlineContent = { Text(folder.name) },
                                        supportingContent = { Text("${folder.itemCount} 个项目") },
                                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                                        trailingContent = { 
                                            if (!folder.isPublic) {
                                                Text("私密", style = MaterialTheme.typography.labelSmall)
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
    }
}

package com.tranx.community.ui.screen.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.AppDetail
import com.tranx.community.data.model.CoinRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AppDetailUiState {
    object Loading : AppDetailUiState()
    data class Success(val app: AppDetail) : AppDetailUiState()
    data class Error(val message: String) : AppDetailUiState()
}

class AppDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val packageName: String = checkNotNull(savedStateHandle["packageName"])
    
    private val _uiState = MutableStateFlow<AppDetailUiState>(AppDetailUiState.Loading)
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadAppDetail()
    }
    
    fun loadAppDetail() {
        viewModelScope.launch {
            _uiState.value = AppDetailUiState.Loading
            try {
                val response = RetrofitClient.getApiService().getAppDetail(packageName)
                if (response.code == 200 && response.data != null) {
                    _uiState.value = AppDetailUiState.Success(response.data)
                } else {
                    _uiState.value = AppDetailUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = AppDetailUiState.Error(e.message ?: "加载失败")
            }
        }
    }
    
    fun coinApp(coins: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = TranxApp.instance.preferencesManager.getToken()
                if (token.isNullOrEmpty()) {
                    onError("请先登录")
                    return@launch
                }
                
                val response = RetrofitClient.getApiService().coinApp(
                    token = token,
                    packageName = packageName,
                    request = CoinRequest(coins)
                )
                
                if (response.code == 200) {
                    onSuccess()
                    // 重新加载以更新投币数
                    loadAppDetail()
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError(e.message ?: "投币失败")
            }
        }
    }
    
    fun recordDownload() {
        viewModelScope.launch {
            try {
                RetrofitClient.getApiService().recordDownload(packageName)
            } catch (e: Exception) {
                // 静默失败，不影响用户体验
            }
        }
    }
}

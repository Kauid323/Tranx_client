package com.tranx.community.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.LoginRequest
import com.tranx.community.data.model.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val message: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel : ViewModel() {
    private val prefsManager = TranxApp.instance.preferencesManager

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _serverUrl = MutableStateFlow(prefsManager.getServerUrl())
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _uiState.value = LoginUiState.Loading

                if (username.isBlank() || password.isBlank()) {
                    _uiState.value = LoginUiState.Error("用户名和密码不能为空")
                    return@launch
                }

                // 确保RetrofitClient已初始化
                if (!RetrofitClient.isInitialized()) {
                    RetrofitClient.initialize(prefsManager.getServerUrl())
                }

                println("尝试登录，服务器地址: ${RetrofitClient.getCurrentBaseUrl()}")
                
                val apiService = RetrofitClient.getApiService()
                val response = apiService.login(LoginRequest(username, password))

                if (response.code == 200 && response.data != null) {
                    prefsManager.saveToken(response.data.token)
                    prefsManager.saveUser(response.data.user)
                    _uiState.value = LoginUiState.Success("登录成功")
                } else {
                    _uiState.value = LoginUiState.Error(response.message)
                }
            } catch (e: Exception) {
                println("登录异常: ${e.message}")
                e.printStackTrace()
                _uiState.value = LoginUiState.Error("登录失败: ${e.message}")
            }
        }
    }

    fun register(username: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            try {
                _uiState.value = LoginUiState.Loading

                if (username.isBlank() || password.isBlank()) {
                    _uiState.value = LoginUiState.Error("用户名和密码不能为空")
                    return@launch
                }

                if (username.length < 3 || username.length > 20) {
                    _uiState.value = LoginUiState.Error("用户名长度应为3-20个字符")
                    return@launch
                }

                if (password.length < 8) {
                    _uiState.value = LoginUiState.Error("密码至少需要8位")
                    return@launch
                }

                if (password != confirmPassword) {
                    _uiState.value = LoginUiState.Error("两次输入的密码不一致")
                    return@launch
                }

                val apiService = RetrofitClient.getApiService()
                val response = apiService.register(RegisterRequest(username, password))

                if (response.code == 200) {
                    _uiState.value = LoginUiState.Success("注册成功，请登录")
                } else {
                    _uiState.value = LoginUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("注册失败: ${e.message}")
            }
        }
    }

    fun updateServerUrl(url: String) {
        prefsManager.saveServerUrl(url)
        val savedUrl = prefsManager.getServerUrl()
        _serverUrl.value = savedUrl
        RetrofitClient.initialize(savedUrl)
    }

    fun resetUiState() {
        _uiState.value = LoginUiState.Idle
    }
}


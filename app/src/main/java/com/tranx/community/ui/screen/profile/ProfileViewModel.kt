package com.tranx.community.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.CheckinStatus
import com.tranx.community.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User, val checkinStatus: CheckinStatus) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {
    private val prefsManager = TranxApp.instance.preferencesManager

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = ProfileUiState.Loading

                val token = prefsManager.getToken() ?: throw Exception("未登录")
                val apiService = RetrofitClient.getApiService()

                // 获取用户信息
                val userResponse = apiService.getCurrentUser(token)
                if (userResponse.code != 200 || userResponse.data == null) {
                    throw Exception(userResponse.message)
                }

                // 从返回的data中提取user信息
                val userData = userResponse.data["user"] as? Map<*, *>
                if (userData != null) {
                    val user = User(
                        id = (userData["id"] as? Double)?.toInt() ?: 0,
                        username = userData["username"] as? String ?: "",
                        email = userData["email"] as? String,
                        level = (userData["level"] as? Double)?.toInt() ?: 0,
                        userLevel = (userData["user_level"] as? Double)?.toInt(),
                        exp = (userData["exp"] as? Double)?.toInt(),
                        coins = (userData["coins"] as? Double)?.toInt() ?: 0,
                        avatar = userData["avatar"] as? String,
                        createdAt = userData["created_at"] as? String,
                        updatedAt = userData["updated_at"] as? String
                    )
                    // 更新本地保存的用户信息
                    prefsManager.saveUser(user)
                } else {
                    throw Exception("用户数据解析失败")
                }
                
                val currentUser = prefsManager.getUser() ?: throw Exception("用户信息不存在")

                // 获取签到状态
                val checkinResponse = apiService.getCheckinStatus(token)
                val checkinStatus = if (checkinResponse.code == 200 && checkinResponse.data != null) {
                    checkinResponse.data
                } else {
                    CheckinStatus(false, true)
                }

                _uiState.value = ProfileUiState.Success(currentUser, checkinStatus)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("加载失败: ${e.message}")
            }
        }
    }

    fun checkin(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: return@launch
                val apiService = RetrofitClient.getApiService()
                val response = apiService.checkin(token)

                if (response.code == 200) {
                    onSuccess("签到成功！获得50硬币和25经验")
                    loadProfile()
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken()
                if (token != null) {
                    val apiService = RetrofitClient.getApiService()
                    apiService.logout(token)
                }
            } catch (e: Exception) {
                // 忽略错误
            } finally {
                prefsManager.clearAll()
            }
        }
    }
}


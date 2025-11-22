package com.tranx.community.ui.screen.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.TranxApp
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.Board
import com.tranx.community.data.model.CreateBoardRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BoardListUiState {
    object Loading : BoardListUiState()
    data class Success(val boards: List<Board>) : BoardListUiState()
    data class Error(val message: String) : BoardListUiState()
}

class BoardListViewModel : ViewModel() {
    private val prefsManager = TranxApp.instance.preferencesManager

    private val _uiState = MutableStateFlow<BoardListUiState>(BoardListUiState.Loading)
    val uiState: StateFlow<BoardListUiState> = _uiState.asStateFlow()

    init {
        loadBoards()
    }

    fun loadBoards() {
        viewModelScope.launch {
            try {
                _uiState.value = BoardListUiState.Loading

                val token = prefsManager.getToken() ?: throw Exception("未登录")
                val apiService = RetrofitClient.getApiService()

                val response = apiService.getBoardList(token)
                if (response.code == 200 && response.data != null) {
                    _uiState.value = BoardListUiState.Success(response.data)
                } else {
                    _uiState.value = BoardListUiState.Error(response.message)
                }
            } catch (e: Exception) {
                _uiState.value = BoardListUiState.Error("加载失败: ${e.message}")
            }
        }
    }

    fun createBoard(name: String, description: String?, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = prefsManager.getToken() ?: throw Exception("未登录")
                val apiService = RetrofitClient.getApiService()

                val response = apiService.createBoard(token, CreateBoardRequest(name, description))
                if (response.code == 200) {
                    onSuccess()
                    loadBoards()
                } else {
                    onError(response.message)
                }
            } catch (e: Exception) {
                onError("创建失败: ${e.message}")
            }
        }
    }
}


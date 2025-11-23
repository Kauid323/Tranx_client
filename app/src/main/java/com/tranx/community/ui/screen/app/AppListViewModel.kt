package com.tranx.community.ui.screen.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.model.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppListUiState(
    val isLoading: Boolean = false,
    val apps: List<App> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val sortType: String = "download",
    val error: String? = null,
    val hasMore: Boolean = true,
    val currentPage: Int = 1
)

class AppListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadApps()
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getApiService().getAppCategories()
                if (response.code == 200) {
                    _uiState.value = _uiState.value.copy(
                        categories = response.data ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }

    fun loadApps(loadMore: Boolean = false) {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val page = if (loadMore) _uiState.value.currentPage + 1 else 1
                val response = RetrofitClient.getApiService().getAppList(
                    category = _uiState.value.selectedCategory,
                    sort = _uiState.value.sortType,
                    page = page,
                    pageSize = 20
                )
                
                if (response.code == 200) {
                    val newApps = response.data?.list ?: emptyList()
                    val currentApps = if (loadMore) _uiState.value.apps else emptyList()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        apps = currentApps + newApps,
                        hasMore = newApps.size >= 20,
                        currentPage = page
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectCategory(category: String?) {
        if (_uiState.value.selectedCategory != category) {
            _uiState.value = _uiState.value.copy(
                selectedCategory = category,
                currentPage = 1
            )
            loadApps()
        }
    }

    fun changeSortType(sortType: String) {
        if (_uiState.value.sortType != sortType) {
            _uiState.value = _uiState.value.copy(
                sortType = sortType,
                currentPage = 1
            )
            loadApps()
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(currentPage = 1)
        loadApps()
    }
}

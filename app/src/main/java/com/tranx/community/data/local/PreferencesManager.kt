package com.tranx.community.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.tranx.community.data.model.User

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "tranx_community_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER = "user"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_PRIMARY_COLOR = "primary_color"
        private const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color"
        private const val DEFAULT_SERVER_URL = "http://localhost:4999"
    }

    // Theme modes
    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }

    // Token管理
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    // 用户信息管理
    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER, userJson).apply()
    }

    fun getUser(): User? {
        val userJson = prefs.getString(KEY_USER, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else {
            null
        }
    }

    fun clearUser() {
        prefs.edit().remove(KEY_USER).apply()
    }

    // 服务器地址管理
    fun saveServerUrl(url: String) {
        val serverUrl = com.tranx.community.utils.NetworkUtils.formatServerUrl(url)
        prefs.edit().putString(KEY_SERVER_URL, serverUrl).apply()
        println("保存服务器地址: $serverUrl")
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun hasServerUrl(): Boolean {
        return prefs.contains(KEY_SERVER_URL)
    }

    // 主题模式管理
    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getThemeMode(): ThemeMode {
        val mode = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(mode ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    // 主题色管理
    fun savePrimaryColor(color: Long) {
        // 确保颜色值在有效范围内
        val safeColor = color and 0xFFFFFFFFL
        prefs.edit().putLong(KEY_PRIMARY_COLOR, safeColor).apply()
        println("保存主题色: $safeColor")
    }

    fun getPrimaryColor(): Long? {
        return if (prefs.contains(KEY_PRIMARY_COLOR)) {
            val color = prefs.getLong(KEY_PRIMARY_COLOR, 0)
            if (color == 0L) null else color
        } else {
            null
        }
    }

    fun clearPrimaryColor() {
        prefs.edit().remove(KEY_PRIMARY_COLOR).apply()
    }

    // 莫奈取色管理
    fun setUseDynamicColor(use: Boolean) {
        prefs.edit().putBoolean(KEY_USE_DYNAMIC_COLOR, use).apply()
    }

    fun getUseDynamicColor(): Boolean {
        return prefs.getBoolean(KEY_USE_DYNAMIC_COLOR, false)
    }

    // 清除所有数据
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // 检查是否已登录
    fun isLoggedIn(): Boolean {
        return getToken() != null && getUser() != null
    }
}


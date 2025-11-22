package com.tranx.community.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    fun validateServerUrl(url: String): Boolean {
        return try {
            val cleanUrl = url.trim()
            cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }
    
    fun formatServerUrl(url: String): String {
        var serverUrl = url.trim()
        // 确保URL格式正确
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            serverUrl = "http://$serverUrl"
        }
        // 移除末尾的斜杠
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.dropLast(1)
        }
        return serverUrl
    }
}

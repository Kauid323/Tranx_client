package com.tranx.community

import android.app.Application
import com.tranx.community.data.api.RetrofitClient
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.util.DownloadManager

class TranxApp : Application() {
    lateinit var preferencesManager: PreferencesManager
        private set
    
    lateinit var downloadManager: DownloadManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
        downloadManager = DownloadManager(this)
        
        // 初始化Retrofit客户端
        val serverUrl = preferencesManager.getServerUrl()
        RetrofitClient.initialize(serverUrl)
    }

    companion object {
        lateinit var instance: TranxApp
            private set
    }
}


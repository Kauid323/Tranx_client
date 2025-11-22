package com.tranx.community

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tranx.community.ui.activity.HomeActivity
import com.tranx.community.ui.activity.LoginActivity
import com.tranx.community.ui.theme.TranxCommunityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefsManager = TranxApp.instance.preferencesManager
        
        // 检查是否已登录
        if (prefsManager.isLoggedIn()) {
            // 已登录，跳转到主页
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            // 未登录，跳转到登录页
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}


package com.tranx.community.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.tranx.community.MainActivity
import com.tranx.community.TranxApp
import com.tranx.community.data.local.PreferencesManager
import com.tranx.community.ui.theme.TranxCommunityTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val prefsManager = TranxApp.instance.preferencesManager
            val savedThemeMode = remember { prefsManager.getThemeMode() }
            val savedPrimaryColor = remember { prefsManager.getPrimaryColor() }
            val savedUseDynamicColor = remember { prefsManager.getUseDynamicColor() }
            
            var themeMode by remember { mutableStateOf(savedThemeMode) }
            var primaryColor by remember { mutableStateOf(savedPrimaryColor) }
            var useDynamicColor by remember { mutableStateOf(savedUseDynamicColor) }
            
            val darkTheme = when (themeMode) {
                PreferencesManager.ThemeMode.LIGHT -> false
                PreferencesManager.ThemeMode.DARK -> true
                PreferencesManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            TranxCommunityTheme(
                darkTheme = darkTheme,
                dynamicColor = useDynamicColor,
                primaryColor = if (useDynamicColor) null else primaryColor
            ) {
                SettingsScreen(
                    onBackClick = { finish() },
                    onThemeModeChanged = { mode ->
                        themeMode = mode
                        prefsManager.saveThemeMode(mode)
                    },
                    onPrimaryColorChanged = { color ->
                        primaryColor = color
                        if (color != null) {
                            prefsManager.savePrimaryColor(color)
                        } else {
                            prefsManager.clearPrimaryColor()
                        }
                    },
                    onDynamicColorChanged = { enabled ->
                        useDynamicColor = enabled
                        prefsManager.setUseDynamicColor(enabled)
                    },
                    onServerUrlChanged = {
                        // 需要重启应用
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}


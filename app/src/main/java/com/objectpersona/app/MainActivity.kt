package com.objectpersona.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.objectpersona.app.ui.navigation.ObjectPersonaNavGraph
import com.objectpersona.app.ui.theme.ObjectPersonaTheme
import dagger.hilt.android.AndroidEntryPoint

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

/**
 * 單一 Activity 架構，所有畫面透過 Jetpack Compose Navigation 管理。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 請求所有檔案存取權限 (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        enableEdgeToEdge()
        setContent {
            ObjectPersonaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ObjectPersonaNavGraph()
                }
            }
        }
    }
}

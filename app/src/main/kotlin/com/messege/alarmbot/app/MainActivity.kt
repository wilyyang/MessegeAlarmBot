package com.messege.alarmbot.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.messege.alarmbot.core.presentation.compose.theme.AlarmBotTheme
import com.messege.alarmbot.util.log.Logger
import dagger.hilt.android.AndroidEntryPoint

const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationPermissionRequestCode = 1

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlarmBotTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.align(Alignment.Center)){
                        Text(text = "Message Alarm Bot", style = MaterialTheme.typography.titleLarge)

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }

        /**
         * Database 외부 저장소 접근 권한
         */
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)

            finish()
            return
        }

        /**
         * 화면 꺼짐 방지
         */
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        /**
         * 알림 권한
         */
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Request POST_NOTIFICATIONS", Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), notificationPermissionRequestCode)

            finish()
            return
        }

        /**
         * Notification Listener 권한
         */
        val isNotificationListener = Settings.Secure.getString(
            contentResolver,
            ENABLED_NOTIFICATION_LISTENERS
        ).contains(packageName)
        if (!isNotificationListener) {
            Toast.makeText(this, "Request ACTION_NOTIFICATION_LISTENER_SETTINGS", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

            finish()
            return
        }

        /**
         * 배터리 최적화 방지 권한
         */
        val isIgnoringBatteryOptimizations = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(packageName)
        if (!isIgnoringBatteryOptimizations) {
            Toast.makeText(this, "Request ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)

            finish()
            return
        }

        /**
         * 슈퍼 유저 권한
         */
        val cmd = arrayOf("su", "-c", "ls")
        try {
            val process = Runtime.getRuntime().exec(cmd)
            val resultCode = process.waitFor()
            Logger.d("[super] 권한 얻기 성공 : $resultCode")
        } catch (e: Exception) {
            Logger.e("[super] 권한 얻기 실패 ${e.message}")
        }
    }
}
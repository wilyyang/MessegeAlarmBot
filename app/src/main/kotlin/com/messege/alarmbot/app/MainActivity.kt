package com.messege.alarmbot.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.messege.alarmbot.core.presentation.compose.theme.AlarmBotTheme
import com.messege.alarmbot.util.log.Logger

const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlarmBotTheme {
                Box(modifier = Modifier.fillMaxSize()){
                    Text(text = "Message Alarm Bot", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        /**
         * 화면 꺼짐 방지
         */
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val isNotificationListener = Settings.Secure.getString(
            contentResolver,
            ENABLED_NOTIFICATION_LISTENERS
        ).contains(packageName)

        val isIgnoringBatteryOptimizations = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(packageName)

        if (!isNotificationListener) {
            Logger.i("Request ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

            finish()
            return
        }

        if (!isIgnoringBatteryOptimizations) {
            Logger.i("Request ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)

            finish()
            return
        }

        val serviceIntent = Intent(this, AlarmBotNotificationListenerService::class.java)

        Logger.d( "Service call")
        startService(serviceIntent)
    }
}
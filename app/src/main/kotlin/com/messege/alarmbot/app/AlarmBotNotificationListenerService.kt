package com.messege.alarmbot.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Person
import android.app.Service
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import androidx.core.app.NotificationCompat
import com.messege.alarmbot.core.common.replyActionIndex
import com.messege.alarmbot.core.common.targetPackageName
import com.messege.alarmbot.domain.model.ChatRoomKey
import com.messege.alarmbot.processor.CmdProcessor
import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmBotNotificationListenerService : NotificationListenerService() {

    private val cmdProcessor = CmdProcessor(this)
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Logger.d( "Service onCreate")
        val channel = NotificationChannel(
            "alarm_bot",
            "Alarm Bot Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for Alarm Bot service notifications"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, "alarm_bot")
            .setContentTitle("Message Alarm Bot")
            .setContentText("Running in the foreground")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    override fun onNotificationPosted(sbn : StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val key = sbn?.notification?.getKey()
        val action = sbn?.notification?.actions?.get(replyActionIndex)

        val user = sbn?.notification?.getPerson()
        val text = when(val textObject = sbn?.notification?.extras?.get(Notification.EXTRA_TEXT)){
            is SpannableString -> textObject.toString()
            is String -> textObject
            else -> null
        }

        if (sbn?.packageName == targetPackageName && key != null && action != null && user != null && !text.isNullOrBlank()) {
            serviceScope.launch {
                cmdProcessor.deliverNotification(chatRoomKey = key, action = action, user = user, text = text)
            }
        }
    }

    private fun Notification.getKey() : ChatRoomKey? {
        val isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val roomName = extras.getString(Notification.EXTRA_SUB_TEXT, "")
        val userName = extras.getString(Notification.EXTRA_TITLE, "")

        return if (isGroupConversation && roomName.isNotBlank()) {
            ChatRoomKey(isGroupConversation = true, roomName = roomName)
        } else if(!isGroupConversation && userName.isNotBlank()){
            ChatRoomKey(isGroupConversation = false, roomName = userName)
        } else{
            null
        }
    }

    private fun Notification.getPerson() : Person? {
        val messages = extras.getParcelableArray("android.messages")
        if(!messages.isNullOrEmpty()){
            messages[0]?.let { parcelable ->
                return (parcelable as? Bundle)?.getParcelable<Person>("sender_person")
            }
        }
        return null
    }
}
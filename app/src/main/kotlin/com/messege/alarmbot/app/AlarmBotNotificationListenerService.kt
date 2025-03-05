package com.messege.alarmbot.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Person
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.messege.alarmbot.core.common.KAKAO_PACKAGE_NAME
import com.messege.alarmbot.core.common.REPLY_ACTION_INDEX
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.processor.CmdProcessor
import com.messege.alarmbot.processor.CmdProcessorEntryPoint
import com.messege.alarmbot.processor.model.ResetMemberPoint
import com.messege.alarmbot.util.format.toTimeFormat
import com.messege.alarmbot.util.log.Logger
import com.messege.alarmbot.work.member.MemberPointResetWorker
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val NOTIFICATION_ID : String = "com.messege.alarmbot.NotificationGroup"
const val NOTIFICATION_TITLE : String = "AlarmBot"
const val SERVICE_ID : Int = 18541

class AlarmBotNotificationListenerService : NotificationListenerService() {
    private lateinit var cmdProcessor: CmdProcessor
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private val resetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            cmdProcessor.sendCommand(ResetMemberPoint)
        }
    }

    companion object {
        const val ACTION_RESET_POINTS = "com.messege.alarmbot.RESET_POINTS"
    }

    override fun onCreate() {
        super.onCreate()

        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, CmdProcessorEntryPoint::class.java)
        cmdProcessor = entryPoint.getCmdProcessor()

        val notificationChannel = NotificationChannel(NOTIFICATION_ID, NOTIFICATION_TITLE, NotificationManager.IMPORTANCE_HIGH)
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_ID)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentTitle(NOTIFICATION_TITLE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentText("This is a notification from AlarmBot.")

        val notification = notificationBuilder.build()

        notificationManager.createNotificationChannel(notificationChannel)
        startForeground(SERVICE_ID, notification)

        // BroadcastReceiver 등록
        val filter = IntentFilter(ACTION_RESET_POINTS)
        registerReceiver(resetReceiver, filter, RECEIVER_NOT_EXPORTED)

        // Worker 실행
        scheduleMemberPointResetWork()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(resetReceiver)
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    override fun onNotificationPosted(sbn : StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.run {
            val text = when(val textObject = notification?.extras?.get(Notification.EXTRA_TEXT)){
                is SpannableString -> textObject.toString()
                is String -> textObject
                else -> null
            }

            if (packageName == KAKAO_PACKAGE_NAME && !text.isNullOrBlank()){
                val user = notification?.getPerson()
                val key = notification?.getKey(sbnKey = sbn.key, user = user)
                val action = notification?.actions?.get(REPLY_ACTION_INDEX)
                if (user != null && key != null && action != null) {
                    serviceScope.launch {
                        cmdProcessor.deliverNotification(chatRoomKey = key, user = user, action = action, text = text)
                    }
                }
            }
        }
    }

    private fun Notification.getPerson() : Person? {
        val messages = extras.getParcelableArray("android.messages", Parcelable::class.java)
        if(!messages.isNullOrEmpty()){
            messages[0]?.let { parcelable ->
                return (parcelable as? Bundle)?.getParcelable("sender_person", Person::class.java)
            }
        }
        return null
    }

    private fun Notification.getKey(sbnKey: String, user : Person?) : ChatRoomKey? {
        val isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val roomName = extras.getString(Notification.EXTRA_SUB_TEXT, "")
        val sbnIdKey = sbnKey.split("|")[3]

        return if (isGroupConversation && roomName.isNotBlank()) {
            ChatRoomKey(isGroupConversation = true, roomName = roomName, roomKey = sbnIdKey)
        } else if(!isGroupConversation && user != null && user.name != null){
            ChatRoomKey(isGroupConversation = false, roomName = "${user.name}", roomKey = "${user.name}")
        } else{
            null
        }
    }

    /**
     * Worker 실행 함수
     */
    private fun scheduleMemberPointResetWork() {
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 현재 시간이 이미 0분을 지난 경우, 다음 시간으로 설정
            if (before(now)) {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }

        val initialDelay = nextRun.timeInMillis - now.timeInMillis
        Logger.e("[time] ${(initialDelay/1000)/60}m ${nextRun.timeInMillis.toTimeFormat()} ${now.timeInMillis.toTimeFormat()}")

        val workRequest = PeriodicWorkRequestBuilder<MemberPointResetWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)  // 다음 정각까지 기다림
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MemberPointResetWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.messege.alarmbot.core.common.KAKAO_PACKAGE_NAME
import com.messege.alarmbot.core.common.REPLY_ACTION_INDEX
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.processor.CmdProcessor
import com.messege.alarmbot.processor.CmdProcessorEntryPoint
import com.messege.alarmbot.processor.model.LikeWeeklyRanking
import com.messege.alarmbot.processor.model.MacroKakaoTalkRoomNews
import com.messege.alarmbot.processor.model.QuizEnd
import com.messege.alarmbot.processor.model.QuizStart
import com.messege.alarmbot.processor.model.ResetMemberPoint
import com.messege.alarmbot.util.format.toTimeFormat
import com.messege.alarmbot.util.log.Logger
import com.messege.alarmbot.work.member.MemberLikeWeeklyRankingWorker
import com.messege.alarmbot.work.member.MemberPointResetWorker
import com.messege.alarmbot.work.news.DailyNewsWorker
import com.messege.alarmbot.work.quiz.QuizEndWorker
import com.messege.alarmbot.work.quiz.QuizStartWorker
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

    private val rankingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            cmdProcessor.sendCommand(LikeWeeklyRanking)
        }
    }

    private val newsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            cmdProcessor.sendCommand(MacroKakaoTalkRoomNews)
        }
    }

    private val quizStartReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            cmdProcessor.sendCommand(QuizStart)
            scheduleQuizEnd()
        }
    }

    private val quizEndReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            cmdProcessor.sendCommand(QuizEnd)
            scheduleQuizStart()
        }
    }

    companion object {
        const val ACTION_RESET_POINTS = "com.messege.alarmbot.RESET_POINTS"
        const val ACTION_LIKE_WEEKLY_RANKING = "com.messege.alarmbot.LIKE_WEEKLY_RANKING"
        const val ACTION_DAILY_NEWS = "com.messege.alarmbot.DAILY_NEWS"
        const val ACTION_QUIZ_START = "com.messege.alarmbot.QUIZ_START"
        const val ACTION_QUIZ_END = "com.messege.alarmbot.QUIZ_END"
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
        val resetFilter = IntentFilter(ACTION_RESET_POINTS)
        registerReceiver(resetReceiver, resetFilter, RECEIVER_NOT_EXPORTED)
        val rankingFilter = IntentFilter(ACTION_LIKE_WEEKLY_RANKING)
        registerReceiver(rankingReceiver, rankingFilter, RECEIVER_NOT_EXPORTED)
        val newsFilter = IntentFilter(ACTION_DAILY_NEWS)
        registerReceiver(newsReceiver, newsFilter, RECEIVER_NOT_EXPORTED)

        val quizStartFilter = IntentFilter(ACTION_QUIZ_START)
        registerReceiver(quizStartReceiver, quizStartFilter, RECEIVER_NOT_EXPORTED)
        val quizEndFilter = IntentFilter(ACTION_QUIZ_END)
        registerReceiver(quizEndReceiver, quizEndFilter, RECEIVER_NOT_EXPORTED)

        // Worker 실행
        scheduleMemberPointResetWork()
        scheduleMemberLikeWeeklyRankingWork()
        scheduleDailyNewsWork()
        scheduleQuizStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(resetReceiver)
        unregisterReceiver(rankingReceiver)
        unregisterReceiver(newsReceiver)
        unregisterReceiver(quizStartReceiver)
        unregisterReceiver(quizEndReceiver)
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
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialMinuteDelay = (nextRun.timeInMillis - now.timeInMillis) / 1000 / 60 + 1

        Logger.e("[time.reset] $initialMinuteDelay m ${nextRun.timeInMillis.toTimeFormat()} ${now.timeInMillis.toTimeFormat()}")

        val workRequest = PeriodicWorkRequestBuilder<MemberPointResetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialMinuteDelay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MemberPointResetWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun scheduleMemberLikeWeeklyRankingWork() {
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(now)) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val initialMinuteDelay = (nextRun.timeInMillis - now.timeInMillis) / 1000 / 60 + 1

        Logger.e("[time.rank] $initialMinuteDelay m ${nextRun.timeInMillis.toTimeFormat()} ${now.timeInMillis.toTimeFormat()}")

        val workRequest = PeriodicWorkRequestBuilder<MemberLikeWeeklyRankingWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialMinuteDelay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MemberLikeWeeklyRankingWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun scheduleDailyNewsWork() {
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialMinuteDelay = (nextRun.timeInMillis - now.timeInMillis) / 1000 / 60 + 1

        Logger.e("[time.news] $initialMinuteDelay m ${nextRun.timeInMillis.toTimeFormat()} ${now.timeInMillis.toTimeFormat()}")

        val workRequest = PeriodicWorkRequestBuilder<DailyNewsWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(initialMinuteDelay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyNewsWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun scheduleQuizStart() {
        val delayMs = calcDelayToNextSlot(listOf(5, 25, 45))

        Logger.e("[time.quiz.start] delay=${delayMs / 1000 / 60}m")

        val request = OneTimeWorkRequestBuilder<QuizStartWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "QuizStartWorker",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleQuizEnd() {
        val delayMs = calcDelayToNextSlot(listOf(15, 35, 55))

        Logger.e("[time.quiz.end] delay=${delayMs / 1000 / 60}m")

        val request = OneTimeWorkRequestBuilder<QuizEndWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "QuizEndWorker",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun calcDelayToNextSlot(minuteSlots: List<Int>): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            val curMin = get(Calendar.MINUTE)

            val nextMinute = minuteSlots.firstOrNull { curMin < it } ?: run {
                // 현재 시각에서 마지막 슬롯(45 or 55)도 지났으면, 다음 시각으로 넘김
                add(Calendar.HOUR_OF_DAY, 1)
                minuteSlots.first()
            }

            set(Calendar.MINUTE, nextMinute)
        }

        return next.timeInMillis - now.timeInMillis // ms 단위
    }
}
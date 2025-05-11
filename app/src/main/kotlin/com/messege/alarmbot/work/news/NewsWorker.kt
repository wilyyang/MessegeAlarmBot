package com.messege.alarmbot.work.news

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.messege.alarmbot.app.AlarmBotNotificationListenerService
import com.messege.alarmbot.util.log.Logger

class DailyNewsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Logger.e("[worker] DailyNewsWorker work")
        val intent = Intent(AlarmBotNotificationListenerService.ACTION_DAILY_NEWS)
        applicationContext.sendBroadcast(intent)
        return Result.success()
    }
}
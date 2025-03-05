package com.messege.alarmbot.work.member

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.messege.alarmbot.app.AlarmBotNotificationListenerService
import com.messege.alarmbot.util.log.Logger

class MemberPointResetWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Logger.e("[worker] MemberPointResetWorker work")
        val intent = Intent(AlarmBotNotificationListenerService.ACTION_RESET_POINTS)
        applicationContext.sendBroadcast(intent)
        return Result.success()
    }
}
package com.messege.alarmbot.work.quiz

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.messege.alarmbot.app.AlarmBotNotificationListenerService
import com.messege.alarmbot.util.log.Logger

class QuizStartWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Logger.e("[worker] QuizStartWorker work")
        val intent = Intent(AlarmBotNotificationListenerService.ACTION_QUIZ_START)
        applicationContext.sendBroadcast(intent)
        return Result.success()
    }
}

class QuizEndWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Logger.e("[worker] QuizEndWorker work")
        val intent = Intent(AlarmBotNotificationListenerService.ACTION_QUIZ_END)
        applicationContext.sendBroadcast(intent)
        return Result.success()
    }
}
package com.messege.alarmbot.contents

import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TimeWork(val seconds : Int, val onComplete: suspend () -> Unit)
class Timer(private val scope: CoroutineScope) {
    private var totalSeconds: Int = 0
    private val _remainingTimeFlow = MutableStateFlow(0)

    private var timerJob: Job? = null
    private var timeWork: TimeWork = TimeWork(0) {}
    private var isStopped = false
    private var onCompleteCalled = false

    fun start(work: TimeWork) {
        timerJob?.cancel()

        timeWork = work
        totalSeconds = timeWork.seconds
        _remainingTimeFlow.value = totalSeconds
        isStopped = false
        onCompleteCalled = false

        timerJob = scope.launch {
            val startTime = System.currentTimeMillis()
            Logger.i("[timer] : start ($totalSeconds s)")
            while (_remainingTimeFlow.value > 0) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                _remainingTimeFlow.value = (totalSeconds - elapsed).toInt()
                if (_remainingTimeFlow.value <= 0) break
                delay(1000)
            }
            if (!isStopped && !onCompleteCalled) {
                Logger.i("[timer] : end ($totalSeconds s)")
                timeWork.onComplete()
                onCompleteCalled = true
            }
        }
    }

    suspend fun stop() {
        if (timerJob?.isActive == true) {
            isStopped = true
            timerJob?.cancel()
            Logger.i("[timer] : stopped")
            if (!onCompleteCalled) {
                onCompleteCalled = true
            }
        }
    }

    fun getRemainingTime(): Int = _remainingTimeFlow.value
}
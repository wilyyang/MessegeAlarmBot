package com.messege.alarmbot.contents

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Timer {
    private var totalSeconds: Int = 0
    private val _remainingTimeFlow = MutableStateFlow(totalSeconds)

    private var timerJob: Job? = null

    fun start(seconds : Int, onComplete: () -> Unit) {
        timerJob?.cancel()

        totalSeconds = seconds
        _remainingTimeFlow.value = totalSeconds
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = totalSeconds - (elapsedTime / 1000).toInt()
                if (remainingTime <= 0) {
                    _remainingTimeFlow.value = 0
                    break
                }
                _remainingTimeFlow.value = remainingTime
                delay(1000)
            }
            onComplete()
        }
    }

    fun stop() {
        timerJob?.cancel()
    }

    fun getRemainingTime(): Int {
        return _remainingTimeFlow.value
    }
}
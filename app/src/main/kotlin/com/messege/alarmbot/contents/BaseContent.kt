package com.messege.alarmbot.contents

import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Message
import kotlinx.coroutines.channels.Channel

interface BaseContent{
    val commandChannel : Channel<Command>
    val contentsName : String

    suspend fun request(message : Message)
}
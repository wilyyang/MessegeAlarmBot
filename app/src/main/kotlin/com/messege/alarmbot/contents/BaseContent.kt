package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.ChatRoomKey
import kotlinx.coroutines.channels.Channel

interface BaseContent{
    val commandChannel : Channel<Command>
    val contentsName : String

    suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String)
}
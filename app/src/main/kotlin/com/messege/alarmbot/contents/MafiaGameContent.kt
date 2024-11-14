package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.CommonText
import com.messege.alarmbot.core.common.MafiaText
import com.messege.alarmbot.core.common.helpKeyword
import kotlinx.coroutines.channels.Channel

class MafiaGameContent(
    override val commandChannel: Channel<Command>
) : BaseContent {
    override val contentsName: String = "마피아"

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        when(text){
            contentsName + helpKeyword -> {
                commandChannel.send(MainChatTextResponse(text = MafiaText.HELP))
            }
            else -> {
            }
        }
    }
}
package com.messege.alarmbot.contents

import com.messege.alarmbot.core.common.Rank
import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Group1RoomTextResponse
import com.messege.alarmbot.processor.model.Message
import kotlinx.coroutines.channels.Channel

interface BaseContent{
    val commandChannel : Channel<Command>
    val contentsName : String

    suspend fun request(message : Message)

    suspend fun rankCheckAndResponse(rank: String, min : Int = 0) : Boolean{
        return if(Rank.getRankByName(rank).rank < min){
            commandChannel.send(Group1RoomTextResponse("랭크가 낮아서 기능을 사용할 수 없습니다."))
            false
        }else {
            true
        }
    }
}
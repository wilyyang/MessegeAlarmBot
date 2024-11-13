package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.helpKeyword
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.CommonText
import kotlinx.coroutines.channels.Channel

class CommonContent(override val commandChannel : Channel<Command>) : BaseContent{
    override val contentsName : String = "기본"
    private val userMap : MutableMap<String, MutableList<String>> = mutableMapOf()

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        when(text){
            hostKeyword + helpKeyword -> {
                commandChannel.send(MainChatTextResponse(text = CommonText.HELP))
            }
            else -> {
                val currentName = "${user.name}"
                val currentKey = "${user.key}"
                val userNameList = userMap[currentKey]
                if(userNameList.isNullOrEmpty()){
                    userMap[currentKey] = mutableListOf(currentName)
                }else if(userNameList.last() != currentName){
                    userMap[currentKey]!!.add(currentName)
                    commandChannel.send(MainChatTextResponse(text = CommonText.alreadyUser(name = currentName, alreadyUsers = userNameList)))
                }
            }
        }
    }
}
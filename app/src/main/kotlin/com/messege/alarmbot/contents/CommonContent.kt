package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.CommonText
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.core.common.helpKeyword
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.data.database.user.model.UserNameData
import kotlinx.coroutines.channels.Channel

class CommonContent(
    override val commandChannel: Channel<Command>,
    private val insertUser: suspend (UserNameData) -> Unit,
    private val getLatestUserName: suspend (String) -> String?,
    private val getUserNameList: suspend (String) -> List<String>
) : BaseContent {
    override val contentsName: String = "기본"

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        if(chatRoomKey == TARGET_KEY) {
            updateUserName(postTime, user)
            when {
                text == "$hostKeyword$helpKeyword" -> {
                    commandChannel.send(MainChatTextResponse(text = CommonText.HELP))
                }
            }
        }
    }

    private suspend fun updateUserName(postTime : Long, user : Person){
        val currentName = "${user.name}"
        val currentKey = "${user.key}"
        val latestSavedName = getLatestUserName(currentKey)?:""
        if(latestSavedName == ""){
            insertUser(UserNameData(updateTime = postTime, userKey = currentKey, name = currentName))
        }else if(latestSavedName != currentName){
            val userNameList = getUserNameList(currentKey)
            insertUser(UserNameData(updateTime = postTime, userKey = currentKey, name = currentName))
            commandChannel.send(HostChatTextResponse(text = CommonText.alreadyUser(name = currentName, alreadyUsers = userNameList)))
        }
    }
}
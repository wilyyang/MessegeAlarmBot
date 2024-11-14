package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.helpKeyword
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.CommonText
import com.messege.alarmbot.data.database.user.model.UserData
import kotlinx.coroutines.channels.Channel

class CommonContent(
    override val commandChannel: Channel<Command>,
    private val insertUser: suspend (UserData) -> Unit,
    private val getUserNameList: suspend (String) -> List<String>
) : BaseContent {
    override val contentsName: String = "기본"

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        when(text){
            hostKeyword + helpKeyword -> {
                commandChannel.send(MainChatTextResponse(text = CommonText.HELP))
            }
            else -> {
                val currentName = "${user.name}"
                val currentKey = "${user.key}"
                val userNameList = getUserNameList(currentKey)
                if(userNameList.isEmpty()){
                    insertUser(UserData(updateTime = postTime, userKey = currentKey, name = currentName))
                }else if(userNameList.first() != currentName){
                    insertUser(UserData(updateTime = postTime, userKey = currentKey, name = currentName))
                    commandChannel.send(MainChatTextResponse(text = CommonText.alreadyUser(name = currentName, alreadyUsers = userNameList)))
                }
            }
        }
    }
}
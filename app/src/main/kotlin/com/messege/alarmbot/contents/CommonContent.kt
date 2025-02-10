package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.helpKeyword
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.CommonText
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.core.common.topicRecommend
import com.messege.alarmbot.data.database.user.model.UserNameData
import com.messege.alarmbot.data.network.topic.model.TopicData
import com.messege.alarmbot.util.format.toTimeFormat
import kotlinx.coroutines.channels.Channel

class CommonContent(
    override val commandChannel: Channel<Command>,
    private val insertUser: suspend (UserNameData) -> Unit,
    private val getUserNameList: suspend (String) -> List<String>,
    private val insertTopic: suspend (TopicData) -> Unit,
    private val recommendTopic: suspend () -> TopicData?,
) : BaseContent {
    override val contentsName: String = "기본"

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        if(chatRoomKey == TARGET_KEY){
            when {
                text == "$hostKeyword$helpKeyword" -> {
                    commandChannel.send(MainChatTextResponse(text = CommonText.HELP))
                }
                "$hostKeyword$topicRecommend" in text -> {
                    val topicText = text.substringAfter(" ", missingDelimiterValue = "")
                    if(topicText.isNotEmpty()){
                        val updateTime = System.currentTimeMillis()
                        val currentKey = "${user.key}"
                        val currentName = "${user.name}"
                        insertTopic(TopicData(updateTime = updateTime, userKey = currentKey, userName = currentName, topic = topicText))
                        commandChannel.send(MainChatTextResponse(text = "주제가 추가되었습니다."))
                    }else{
                        val topic = recommendTopic()
                        val recommendTopicText = topic?.let {
                            "${it.topic} - [${it.updateTime.toTimeFormat()} ${it.userName}]"
                        }?: "등록된 주제가 없습니다."
                        commandChannel.send(MainChatTextResponse(text = recommendTopicText))
                    }
                }
                else -> {
                    val currentName = "${user.name}"
                    val currentKey = "${user.key}"
                    val userNameList = getUserNameList(currentKey)
                    if(userNameList.isEmpty()){
                        insertUser(UserNameData(updateTime = postTime, userKey = currentKey, name = currentName))
                    }else if(userNameList.first() != currentName){
                        insertUser(UserNameData(updateTime = postTime, userKey = currentKey, name = currentName))
                        commandChannel.send(HostChatTextResponse(text = CommonText.alreadyUser(name = currentName, alreadyUsers = userNameList)))
                    }
                }
            }
        }
    }
}
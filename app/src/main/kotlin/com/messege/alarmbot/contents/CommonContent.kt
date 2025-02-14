package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.helpKeyword
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.CommonText
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.core.common.topicAdd
import com.messege.alarmbot.core.common.topicRecommend
import com.messege.alarmbot.data.database.user.model.UserNameData
import com.messege.alarmbot.data.network.topic.model.TopicData
import com.messege.alarmbot.util.format.toTimeFormatDate
import kotlinx.coroutines.channels.Channel

class CommonContent(
    override val commandChannel: Channel<Command>,
    private val insertUser: suspend (UserNameData) -> Unit,
    private val getLatestUserName: suspend (String) -> String?,
    private val getUserNameList: suspend (String) -> List<String>,
    private val insertTopic: suspend (TopicData) -> Long,
    private val recommendTopic: suspend () -> TopicData?,
    private val selectTopic: suspend (Long) -> TopicData?
) : BaseContent {
    override val contentsName: String = "기본"

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        if(chatRoomKey == TARGET_KEY){
            updateUserName(postTime, user)
            when {
                text == "$hostKeyword$helpKeyword" -> {
                    commandChannel.send(MainChatTextResponse(text = CommonText.HELP))
                }
                text.startsWith("$hostKeyword$topicRecommend") -> {
                    val number = text.substringAfter(" ", missingDelimiterValue = "").toIntOrNull()
                    val topic = if (number == null) {
                        recommendTopic()
                    } else selectTopic(number.toLong())

                    val recommendTopicText = topic?.let {
                        val latestName = getLatestUserName(it.userKey)?:"-"
                        "${it.idx}. ${it.topic} \n- ${it.updateTime.toTimeFormatDate()} $latestName"
                    } ?: "등록된 주제가 없습니다."
                    commandChannel.send(MainChatTextResponse(text = recommendTopicText))
                }
                text.startsWith("$hostKeyword$topicAdd ") -> {
                    val updateTime = System.currentTimeMillis()
                    val currentKey = "${user.key}"
                    val currentName = "${user.name}"
                    val topicText = text.substringAfter(" ", missingDelimiterValue = "")
                    val key = insertTopic(TopicData(updateTime = updateTime, userKey = currentKey, userName = currentName, topic = topicText))
                    commandChannel.send(MainChatTextResponse(text = "주제가 추가되었습니다. (key = $key)"))
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
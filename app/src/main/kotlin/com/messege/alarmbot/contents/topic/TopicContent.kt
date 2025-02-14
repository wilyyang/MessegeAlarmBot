package com.messege.alarmbot.contents.topic

import android.app.Person
import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.Command
import com.messege.alarmbot.contents.MainChatTextResponse
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.core.common.admins
import com.messege.alarmbot.core.common.topicAdd
import com.messege.alarmbot.core.common.topicDelete
import com.messege.alarmbot.core.common.topicRecommend
import com.messege.alarmbot.data.network.topic.model.TopicData
import com.messege.alarmbot.util.format.toTimeFormatDate
import kotlinx.coroutines.channels.Channel

class TopicContent(
    override val commandChannel: Channel<Command>,
    private val getLatestUserName: suspend (String) -> String?,
    private val insertTopic: suspend (TopicData) -> Long,
    private val recommendTopic: suspend () -> TopicData?,
    private val selectTopic: suspend (Long) -> TopicData?,
    private val deleteTopic: suspend (Long) -> Int
) : BaseContent {
    override val contentsName: String = "기본"

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        if(chatRoomKey == TARGET_KEY){
            when {
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

                text.startsWith("$hostKeyword$topicDelete") -> {
                    val currentKey = "${user.key}"
                    val number = text.substringAfter(" ", missingDelimiterValue = "").toIntOrNull()
                    val message = if (number == null) {
                        "삭제할 주제 넘버를 지정해주세요."
                    } else {
                        if(currentKey in admins.map { it.second }){
                            val deleteNumber = deleteTopic(number.toLong())
                            if(deleteNumber > 0){
                                "$number 번 주제가 삭제되었습니다."
                            }else {
                                "삭제할 주제가 없어요."
                            }
                        }else{
                            "관리자만 삭제가 가능합니다."
                        }
                    }
                    commandChannel.send(MainChatTextResponse(text = message))
                }
            }
        }
    }
}
package com.messege.alarmbot.contents.topic

import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.member.model.MemberData
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
import com.messege.alarmbot.data.database.topic.model.TopicData
import com.messege.alarmbot.data.database.topic.model.TopicReplyData
import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Group1RoomTextResponse
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.util.format.toTimeFormatDate
import kotlinx.coroutines.channels.Channel

class TopicContent(
    override val commandChannel: Channel<Command>,
    private val topicDatabaseDao: TopicDatabaseDao,
    private val memberDatabaseDao : MemberDatabaseDao
) : BaseContent {
    override val contentsName: String = "주제"

    override suspend fun request(message : Message) {
        if(message.type == ChatRoomType.GroupRoom1) {
            val user = memberDatabaseDao.getMember(message.userId).getOrNull(0)
            val isSuperAdmin = user?.isSuperAdmin ?: false
            val isAdmin = isSuperAdmin || user?.isAdmin ?: false

            if(message is Message.Talk){
                when {
                    message.text.startsWith("$TOPIC_KEYWORD$TOPIC_ADD") -> {
                        val updateTime = System.currentTimeMillis()
                        val topicText = message.text.substringAfter(" ", missingDelimiterValue = "")

                        val key = topicDatabaseDao.insertTopic(TopicData(
                            updateTime = updateTime,
                            userKey = message.userId,
                            userName = message.userName,
                            topic = topicText
                        ))
                        commandChannel.send(Group1RoomTextResponse(text = "주제가 추가되었습니다. (key = $key)"))
                    }

                    message.text.startsWith("$TOPIC_KEYWORD$TOPIC_RECOMMEND") -> {
                        val number = message.text.substringAfter(" ", missingDelimiterValue = "").toIntOrNull()
                        val topic = if (number == null) {
                            topicDatabaseDao.getRandomTopic()
                        } else {
                            topicDatabaseDao.getSelectTopic(number.toLong())
                        }

                        val recommendTopicText = topic?.let {
                            val latestName = memberDatabaseDao.getMember(it.userKey).getOrNull(0)?.latestName?:"-"
                            val response = "$TOPIC_PREFIX${it.idx}. ${it.topic} \n- ${it.updateTime.toTimeFormatDate()} $latestName"

                            var replyText = ""
                            val replyList = topicDatabaseDao.getSelectReplyList(topic.idx)
                            for(reply in replyList){
                                val replyUserName = memberDatabaseDao.getMember(reply.userKey).getOrNull(0)?.latestName ?: reply.userName
                                replyText += "\nㄴ ${reply.reply} - $replyUserName"
                            }
                            response + replyText
                        } ?: "등록된 주제가 없습니다."
                        commandChannel.send(Group1RoomTextResponse(text = recommendTopicText))
                    }

                    message.text.startsWith("$TOPIC_KEYWORD$TOPIC_DELETE") && isAdmin -> {
                        val number = message.text.substringAfter(" ", missingDelimiterValue = "").toIntOrNull()
                        val responseText = if (number == null) {
                            "삭제할 주제 넘버를 지정해주세요."
                        } else {
                            val deleteNumber = topicDatabaseDao.deleteTopic(number.toLong())
                            topicDatabaseDao.deleteReplyLists(number.toLong())
                            if(deleteNumber > 0){
                                "$number 번 주제가 삭제되었습니다."
                            }else {
                                "삭제할 주제가 없어요."
                            }
                        }
                        commandChannel.send(Group1RoomTextResponse(text = responseText))
                    }

                    message.replyMessage?.startsWith(TOPIC_PREFIX) == true -> {
                        val topicKey = message.replyMessage.substring(TOPIC_PREFIX.length, message.replyMessage.indexOf('.')).toLongOrNull()
                        if(topicKey != null){
                            topicDatabaseDao.insertReplyTopic(TopicReplyData(
                                topicKey = topicKey,
                                updateTime = System.currentTimeMillis(),
                                userKey = message.userId,
                                userName = message.userName,
                                reply = message.text
                            ))
                            commandChannel.send(Group1RoomTextResponse(text = "답글이 추가되었습니다."))
                        }
                    }
                }
            }
        }
    }
}
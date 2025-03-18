package com.messege.alarmbot.contents.topic

import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.core.common.FOLDING_TEXT
import com.messege.alarmbot.core.common.Rank
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
import com.messege.alarmbot.data.database.topic.model.TopicData
import com.messege.alarmbot.data.database.topic.model.TopicReplyData
import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Group1RoomTextResponse
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.util.format.toTimeFormatDate
import com.messege.alarmbot.util.log.Logger
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
            val rank = if(user != null) Rank.getRankByName(user.rank) else Rank.Unemployed

            if(message is Message.Talk){
                when {
                    message.text.startsWith("$TOPIC_KEYWORD$TOPIC_ADD ") -> {
                        if(rank.tier < 0){
                            commandChannel.send(Group1RoomTextResponse(text = "티어가 부족합니다. (현재 ${rank.tier} 티어 : ${rank.korName})"))
                            return
                        }

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
                        if(rank.tier < 0){
                            commandChannel.send(Group1RoomTextResponse(text = "티어가 부족합니다. (현재 ${rank.tier} 티어 : ${rank.korName})"))
                            return
                        }

                        val number = message.text.substringAfter(" ", missingDelimiterValue = "").toIntOrNull()
                        val topic = if (number == null) {
                            topicDatabaseDao.getRandomTopic()
                        } else {
                            topicDatabaseDao.getSelectTopic(number.toLong())
                        }

                        val recommendTopicText = topic?.let {
                            val latestName = memberDatabaseDao.getMember(it.userKey).getOrNull(0)?.latestName?:"-"
                            val response = "$TOPIC_PREFIX${it.idx}. ${it.topic} \n- ${it.updateTime.toTimeFormatDate()} $latestName"

                            val replyList = topicDatabaseDao.getSelectReplyList(topic.idx)
                            var replyText = if(replyList.isNotEmpty()) FOLDING_TEXT else ""
                            for(reply in replyList){
                                val replyUserName = memberDatabaseDao.getMember(reply.userKey).getOrNull(0)?.latestName ?: reply.userName
                                replyText += "\nㄴ ${reply.reply} - $replyUserName"
                            }
                            response + replyText
                        } ?: "등록된 주제가 없습니다."
                        commandChannel.send(Group1RoomTextResponse(text = recommendTopicText))
                    }

                    message.text.startsWith("$TOPIC_KEYWORD$TOPIC_DELETE ") -> {
                        if(rank.tier < 4){
                            commandChannel.send(Group1RoomTextResponse(text = "티어가 부족합니다. (현재 ${rank.tier} 티어 : ${rank.korName})"))
                            return
                        }

                        val replyNumber = message.text.substringAfter("-", "").toIntOrNull()
                        val topicNumber = if (replyNumber == null) {
                            message.text.substringAfter(" ", "").toIntOrNull()
                        } else {
                            message.text.substringAfter(" ", "").substringBefore("-",  "").toIntOrNull()
                        }
                        val responseText = if (topicNumber == null) {
                            "삭제할 주제 넘버를 지정해주세요."
                        } else {
                            if(replyNumber == null){
                                val deleteNumber = topicDatabaseDao.deleteTopic(topicNumber.toLong())
                                topicDatabaseDao.deleteReplyLists(topicNumber.toLong())
                                if(deleteNumber > 0){
                                    "$topicNumber 번 주제가 삭제되었습니다."
                                }else {
                                    "삭제할 주제가 없어요."
                                }
                            }else{
                                val deleteResult = topicDatabaseDao.deleteReplyTopicOrderNumber(topicNumber.toLong(), replyNumber - 1)
                                if(deleteResult > 0){
                                    "$topicNumber 주제의 $replyNumber 번 답글이 삭제되었습니다."
                                }else {
                                    "삭제할 답글이 없어요."
                                }
                            }
                        }
                        commandChannel.send(Group1RoomTextResponse(text = responseText))
                    }

                    message.replyMessage?.startsWith(TOPIC_PREFIX) == true -> {
                        if(rank.tier < 0){
                            commandChannel.send(Group1RoomTextResponse(text = "티어가 부족합니다. (현재 ${rank.tier} 티어 : ${rank.korName})"))
                            return
                        }

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
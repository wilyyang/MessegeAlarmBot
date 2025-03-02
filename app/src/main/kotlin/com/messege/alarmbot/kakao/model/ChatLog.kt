package com.messege.alarmbot.kakao.model

import com.google.gson.Gson
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.processor.model.EVENT_APPOINT_MANAGER_CODE
import com.messege.alarmbot.processor.model.EVENT_DELETE_MESSAGE_CODE
import com.messege.alarmbot.processor.model.EVENT_ENTER_CODE
import com.messege.alarmbot.processor.model.EVENT_KICK_CODE
import com.messege.alarmbot.processor.model.EVENT_OUT_CODE
import com.messege.alarmbot.processor.model.EVENT_RELEASE_MANAGER_CODE

data class MessageEvent(
    val feedType: Int,
    val logId: Long? = null,
    val member: MessageEventMember? = null,
    val members: List<MessageEventMember>? = null
)

data class MessageEventMember(
    val userId: Long,
    val nickName : String
)

data class Attachment(
    val src_logId: Long?,
    val src_userId: Long?,
    val src_message: String?,
    val mentions: List<Mention>?
)

data class Mention(
    val user_id: Long
)

data class ChatLog(
    val id: Long,
    val type: Int?,
    val chatId: Long,
    val userId: Long? = null,
    val message: String? = null,
    val attachment: String? = null,
    val createdAt: Long? = null,
    val deletedAt: Long? = null,
    val clientMessageId: Long? = null,
    val prevId: Long? = null,
    val referer: Int? = null,
    val supplement: String? = null,
    val v: ChatMetadata? = null
) {
    suspend fun toMessage(
        getName : suspend (Long) -> String,
        getLogText : suspend (Long) -> String
    ) : Message {
        return try {
            val userName = getName(userId!!)
            val roomType = when(chatId){
                ChatRoomType.GroupRoom1.roomKey -> ChatRoomType.GroupRoom1
                ChatRoomType.GroupRoom2.roomKey -> ChatRoomType.GroupRoom2
                ChatRoomType.AdminRoom.roomKey -> ChatRoomType.AdminRoom
                else -> ChatRoomType.IndividualRoom(roomKey = chatId)
            }

            val gson = Gson()
            if(type == 0){
                // Event
                val event = gson.fromJson(message, MessageEvent::class.java)
                if(event.feedType == EVENT_DELETE_MESSAGE_CODE){
                    Message.Event.DeleteMessage(
                        time = createdAt!!,
                        type = roomType,
                        userId = userId,
                        userName = userName,
                        deleteMessage = getLogText(event.logId!!)
                    )
                }else{
                    // Manage Event
                    when(event.feedType){
                        EVENT_ENTER_CODE -> {
                            Message.Event.ManageEvent.EnterEvent(
                                time = createdAt!!,
                                type = roomType,
                                userId = userId,
                                userName = userName,
                                targetId = event.members!![0].userId,
                                targetName = event.members[0].nickName
                            )
                        }
                        EVENT_OUT_CODE -> {
                            Message.Event.ManageEvent.OutEvent(
                                time = createdAt!!,
                                type = roomType,
                                userId = userId,
                                userName = userName,
                                targetId = event.member!!.userId,
                                targetName = event.member.nickName
                            )
                        }
                        EVENT_KICK_CODE -> {
                            Message.Event.ManageEvent.KickEvent(
                                time = createdAt!!,
                                type = roomType,
                                userId = userId,
                                userName = userName,
                                targetId = event.member!!.userId,
                                targetName = event.member.nickName
                            )
                        }
                        EVENT_APPOINT_MANAGER_CODE -> {
                            Message.Event.ManageEvent.AppointManagerEvent(
                                time = createdAt!!,
                                type = roomType,
                                userId = userId,
                                userName = userName,
                                targetId = event.member!!.userId,
                                targetName = event.member.nickName
                            )
                        }
                        EVENT_RELEASE_MANAGER_CODE -> {
                            Message.Event.ManageEvent.ReleaseManagerEvent(
                                time = createdAt!!,
                                type = roomType,
                                userId = userId,
                                userName = userName,
                                targetId = event.member!!.userId,
                                targetName = event.member.nickName
                            )
                        }
                        else -> {
                            Message.None(
                                time = createdAt!!,
                                type = roomType,
                                userId = userId,
                                userName = userName
                            )
                        }
                    }
                }
            }else {
                // Message
                val attachmentObject = attachment?.let { gson.fromJson(attachment, Attachment::class.java) }

                Message.Talk(
                    time = createdAt!!,
                    type = roomType,
                    userId = userId,
                    userName = userName,
                    text = message!!,
                    replyMessage = attachmentObject?.src_message,
                    replyUserId = attachmentObject?.src_userId,
                    mentionIds = attachmentObject?.mentions?.map { it.user_id } ?: listOf()
                )
            }

        }catch (e : Exception){
            Message.None()
        }
    }
}

data class ChatMetadata(
    val notDecoded: Boolean,
    val origin: String,
    val c: String,
    val modifyRevision: Int,
    val isSingleDefaultEmoticon: Boolean,
    val defaultEmoticonsCount: Int,
    val isMine: Boolean,
    val enc: Int
)
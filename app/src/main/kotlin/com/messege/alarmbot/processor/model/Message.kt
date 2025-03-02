package com.messege.alarmbot.processor.model

import com.messege.alarmbot.core.common.ChatRoomType

const val EVENT_DELETE_MESSAGE_CODE = 14
const val EVENT_ENTER_CODE = 4
const val EVENT_OUT_CODE = 2
const val EVENT_KICK_CODE = 6
const val EVENT_APPOINT_MANAGER_CODE = 11
const val EVENT_RELEASE_MANAGER_CODE = 12

sealed class Message(
    open val time : Long, open val type : ChatRoomType,
    open val userId : Long, open val userName: String
) {

    data class None(
        override val time : Long = 0L, override val type : ChatRoomType = ChatRoomType.IndividualRoom(0),
        override val userId : Long = 0L, override val userName: String = ""
    ) : Message(time, type, userId, userName)

    data class Talk(
        override val time : Long, override val type : ChatRoomType,
        override val userId : Long, override val userName : String,
        val text : String,  val replyMessage : String? = null, val replyUserId : Long? = null,
        val mentionIds : List<Long> = listOf()
    ) : Message(time, type, userId, userName)

    sealed class Event(
        override val time : Long, override val type : ChatRoomType,
        override val userId : Long, override val userName : String,
        open val eventCode : Int
    ) : Message(time, type, userId, userName) {

        data class DeleteMessage(
            override val time : Long, override val type : ChatRoomType,
            override val userId : Long, override val userName : String,
            val deleteMessage : String
        ) : Event(time, type, userId, userName, eventCode = EVENT_DELETE_MESSAGE_CODE)

        sealed class ManageEvent(
            override val time : Long, override val type : ChatRoomType,
            override val userId : Long, override val userName : String,
            open val targetId : Long, open val targetName : String,
            override val eventCode : Int
        ) : Event(time, type, userId, userName, eventCode) {

            data class EnterEvent(
                override val time : Long, override val type : ChatRoomType,
                override val userId : Long, override val userName : String,
                override val targetId : Long, override val targetName : String
            ): ManageEvent(time, type, userId, userName, targetId, targetName, eventCode = EVENT_ENTER_CODE)

            data class OutEvent(
                override val time : Long,  override val type : ChatRoomType,
                override val userId : Long, override val userName : String,
                override val targetId : Long, override val targetName : String
            ): ManageEvent(time, type, userId, userName, targetId, targetName, eventCode = EVENT_OUT_CODE)

            data class KickEvent(
                override val time : Long, override val type : ChatRoomType,
                override val userId : Long, override val userName : String,
                override val targetId : Long, override val targetName : String
            ): ManageEvent(time, type, userId, userName, targetId, targetName, eventCode = EVENT_KICK_CODE)

            data class AppointManagerEvent(
                override val time : Long, override val type : ChatRoomType,
                override val userId : Long, override val userName : String,
                override val targetId : Long, override val targetName : String
            ) : ManageEvent(time, type, userId, userName, targetId, targetName, eventCode = EVENT_APPOINT_MANAGER_CODE)

            data class ReleaseManagerEvent(
                override val time : Long, override val type : ChatRoomType,
                override val userId : Long, override val userName : String,
                override val targetId : Long, override val targetName : String
            ) : ManageEvent(time, type, userId, userName, targetId, targetName, eventCode = EVENT_RELEASE_MANAGER_CODE)
        }
    }
}
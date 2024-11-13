package com.messege.alarmbot.core.common

data class ChatRoomKey(
    val isGroupConversation : Boolean,
    val roomName : String,
    val roomKey : String
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatRoomKey) return false

        return isGroupConversation == other.isGroupConversation && roomKey == other.roomKey
    }

    override fun hashCode(): Int {
        return 31 * isGroupConversation.hashCode() + roomKey.hashCode()
    }
}
package com.messege.alarmbot.core.common

data class ChatRoomKey(
    val isGroupConversation : Boolean,
    val roomName : String,
    val roomKey : String
)
package com.messege.alarmbot.core.common

private const val GROUP_1_KEY = 18438856397998764L
private const val GROUP_2_KEY = 18443633643038305L
private const val ADMIN_KEY = 18444807651854804L

sealed class ChatRoomType(open val roomKey : Long) {
    data object GroupRoom1 : ChatRoomType(GROUP_1_KEY)
    data object GroupRoom2 : ChatRoomType(GROUP_2_KEY)
    data object AdminRoom : ChatRoomType(ADMIN_KEY)
    data class OtherGroupRoom(override val roomKey : Long = 0L) : ChatRoomType(roomKey)
    data class IndividualRoom(override val roomKey : Long = 0L) : ChatRoomType(roomKey)
}
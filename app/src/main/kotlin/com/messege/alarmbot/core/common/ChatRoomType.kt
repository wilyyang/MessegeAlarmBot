package com.messege.alarmbot.core.common

import com.messege.alarmbot.app.ADMIN_KEY_APPLIED
import com.messege.alarmbot.app.GROUP_1_KEY_APPLIED
import com.messege.alarmbot.app.GROUP_2_KEY_APPLIED

sealed class ChatRoomType(open val roomKey : Long) {
    data object GroupRoom1 : ChatRoomType(0L) {
        override val roomKey: Long
            get() = GROUP_1_KEY_APPLIED
    }

    data object GroupRoom2 : ChatRoomType(0L) {
        override val roomKey: Long
            get() = GROUP_2_KEY_APPLIED
    }

    data object AdminRoom : ChatRoomType(0L) {
        override val roomKey: Long
            get() = ADMIN_KEY_APPLIED
    }
    data class IndividualRoom(override val roomKey : Long = 0L) : ChatRoomType(roomKey)
}
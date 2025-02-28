package com.messege.alarmbot.kakao.model

data class ChatMember(
    val userId: Long,
    val type: Int = 0,
    val nickName: String = "",
    val chatId: Long,
    val privilege : Int,
    val enc : Int
)
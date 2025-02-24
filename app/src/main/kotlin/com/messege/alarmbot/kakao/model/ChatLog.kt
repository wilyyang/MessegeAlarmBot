package com.messege.alarmbot.kakao.model

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
)

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
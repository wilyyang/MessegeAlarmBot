package com.messege.alarmbot.core.common

const val KAKAO_PACKAGE_NAME = "com.kakao.talk"
const val REPLY_ACTION_INDEX = 1

const val MAIN_CHAT_ROOM_NAME = "제노방"
const val DEBUG_CHAT_ROOM_NAME = "제노관리자방"

const val MAIN_CHAT_ROOM_KEY = "18433121139565846"
const val DEBUG_CHAT_ROOM_KEY = "18434244645817433"

val MAIN_KEY = ChatRoomKey(isGroupConversation = true, roomName = MAIN_CHAT_ROOM_NAME, roomKey = MAIN_CHAT_ROOM_KEY)
val SUB_KEY = ChatRoomKey(isGroupConversation = true, roomName = DEBUG_CHAT_ROOM_NAME, roomKey = DEBUG_CHAT_ROOM_KEY)

val TARGET_KEY = MAIN_KEY
package com.messege.alarmbot.core.common

const val KAKAO_PACKAGE_NAME = "com.kakao.talk"
const val REPLY_ACTION_INDEX = 1

const val DEFAULT_ROOM_NAME = "[]"
const val MAIN_CHAT_ROOM_NAME = ""
const val DEBUG_CHAT_ROOM_NAME = ""

const val MAIN_CHAT_ROOM_KEY = "0|com.kakao.talk|2|18433121139565846|10316"
const val DEBUG_CHAT_ROOM_KEY = "0|com.kakao.talk|2|18434244645817433|10316"

val MAIN_KEY = ChatRoomKey(isGroupConversation = true, roomName = DEFAULT_ROOM_NAME, roomKey = MAIN_CHAT_ROOM_KEY)
val DEBUG_KEY = ChatRoomKey(isGroupConversation = true, roomName = DEFAULT_ROOM_NAME, roomKey = DEBUG_CHAT_ROOM_KEY)

val TARGET_KEY = DEBUG_KEY
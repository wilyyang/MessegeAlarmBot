package com.messege.alarmbot.core.common

const val KAKAO_PACKAGE_NAME = "com.kakao.talk"
const val REPLY_ACTION_INDEX = 1

const val MAIN_CHAT_ROOM_NAME = "오토주방"
const val GAME_CHAT_ROOM_NAME = "오토주게임방"
const val HOST_CHAT_ROOM_NAME = "오토주관리자방"

const val MAIN_CHAT_ROOM_KEY = "18438856397998764"
const val GAME_CHAT_ROOM_KEY = "18438856397998764"
const val HOST_CHAT_ROOM_KEY = "18439815724549290"

val MAIN_KEY = ChatRoomKey(isGroupConversation = true, roomName = MAIN_CHAT_ROOM_NAME, roomKey = MAIN_CHAT_ROOM_KEY)
val GAME_SUB_KEY = ChatRoomKey(isGroupConversation = true, roomName = GAME_CHAT_ROOM_NAME, roomKey = GAME_CHAT_ROOM_KEY)
val HOST_KEY = ChatRoomKey(isGroupConversation = true, roomName = HOST_CHAT_ROOM_NAME, roomKey = HOST_CHAT_ROOM_KEY)
var GAME_KEY = GAME_SUB_KEY

val TARGET_KEY = MAIN_KEY
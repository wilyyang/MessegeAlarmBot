package com.messege.alarmbot.core.common

const val DECRYPT_MEMBER_KEY = 421448547L

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

val admins = listOf(
    "오토주" to "5dbddf17a31bc9b293cbf7e7b78fca62ebb4397b78916d299c8f92aa2cd4c76d",
    "애디" to "5881b8ed3e4a0b71b37f2861f7fd82a84c152fd1490d9500e97db99b620f7423",
    "빵구" to "cdbecb0381aaaede10beaa443e9c6171ba96a7409ad968d0e0dd320509b242b2",
    "토리" to "f96a824648974878411d8653cf9f1da590fad183251c57aa3bea3bb84f7d3c5d",
    "인간" to "0a2f06f8dbe09b9c23fb747cf2455f063355f8ff914bc0b866d794f29f9fd6cc",
    "모순" to "db21ed676531b454baa457ef435a8c24a9b5a9289c9a9f23de7fea67ae1fe1c7",
    "신" to "de8cb26bccc35ad5df66938394782c838a9595baedaf8f320e832a50e99ca74c",
    "텨붕" to "6c3190ec886056f9bddb071d77fe05cd9f00d4df60f8c3fbb346307aa125bedb",
    "연" to "f35c56279d010dcb24c471f3a44c0217192a13c1503a2d03c5907b4e5ce1fa59"
)
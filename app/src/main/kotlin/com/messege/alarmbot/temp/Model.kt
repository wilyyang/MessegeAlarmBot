package com.messege.alarmbot.temp

import com.messege.alarmbot.core.common.ChatRoomKey

interface Command

data class UserKey(val name: String, val key: String)

object None : Command
data class MainChatTextResponse(val text: String) : Command
data class GameChatTextResponse(val text: String) : Command
data class HostChatTextResponse(val text: String) : Command
data class UserTextResponse(val userKey: ChatRoomKey, val text: String) : Command

package com.messege.alarmbot.domain.model

import com.messege.alarmbot.core.common.ChatRoomKey

interface Command

object None : Command
data class MainChatTextResponse(val text: String) : Command
data class UserTextResponse(val userKey: ChatRoomKey, val text: String) : Command

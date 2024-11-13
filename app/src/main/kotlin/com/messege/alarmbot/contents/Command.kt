package com.messege.alarmbot.contents

import com.messege.alarmbot.core.common.ChatRoomKey

interface Command

object None : Command
data class MainChatTextResponse(val text: String) : Command
data class UserTextResponse(val userKey: ChatRoomKey, val text: String) : Command

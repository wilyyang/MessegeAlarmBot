package com.messege.alarmbot.processor.model

import com.messege.alarmbot.core.common.ChatRoomKey

interface Command

object None : Command
data class Group1RoomTextResponse(val text: String) : Command
data class Group2RoomTextResponse(val text: String) : Command
data class AdminRoomTextResponse(val text: String) : Command
data class IndividualRoomTextResponse(val userKey: ChatRoomKey, val text: String) : Command

data object ResetMemberPoint : Command
data object LikeWeeklyRanking : Command
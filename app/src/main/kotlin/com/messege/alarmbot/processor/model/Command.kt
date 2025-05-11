package com.messege.alarmbot.processor.model

import com.messege.alarmbot.core.common.ChatRoomKey

const val DELAY_DEFAULT = 500L
abstract class Command(open val delayMilliSeconds: Long)

object None : Command(DELAY_DEFAULT)
data class Group1RoomTextResponse(val text: String, override val delayMilliSeconds : Long = DELAY_DEFAULT) : Command(delayMilliSeconds)
data class Group2RoomTextResponse(val text: String, override val delayMilliSeconds : Long = DELAY_DEFAULT) : Command(delayMilliSeconds)
data class AdminRoomTextResponse(val text: String, override val delayMilliSeconds : Long = DELAY_DEFAULT) : Command(delayMilliSeconds)
data class IndividualRoomTextResponse(val userKey: ChatRoomKey, val text: String, override val delayMilliSeconds : Long = DELAY_DEFAULT) : Command(delayMilliSeconds)

data object ResetMemberPoint : Command(DELAY_DEFAULT)
data object LikeWeeklyRanking : Command(DELAY_DEFAULT)
data object UpdateKakaoMembers : Command(DELAY_DEFAULT)
data object MacroKakaoTalkRoomNews : Command(DELAY_DEFAULT)
package com.messege.alarmbot.core.common

const val TEMP_PROFILE_TYPE = -1
const val NOT_TALK_PROFILE_TYPE = 4
val inNotTalkType = { type: Int -> type == TEMP_PROFILE_TYPE || type == NOT_TALK_PROFILE_TYPE }
const val DECRYPT_MEMBER_KEY = 421448547L

const val KAKAO_PACKAGE_NAME = "com.kakao.talk"
const val REPLY_ACTION_INDEX = 1
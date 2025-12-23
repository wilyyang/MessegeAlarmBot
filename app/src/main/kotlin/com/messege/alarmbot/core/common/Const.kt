package com.messege.alarmbot.core.common

const val SUPER_ADMIN_ME = 8811795357724748089
const val SUPER_ADMIN_ME_2 = 7676381769248936246
const val TEMP_PROFILE_TYPE = -1
const val NOT_TALK_PROFILE_TYPE_1 = 1
const val NOT_TALK_PROFILE_TYPE_2 = 2
const val NOT_TALK_PROFILE_TYPE_4 = 4
val inNotTalkType = { type: Int -> type == TEMP_PROFILE_TYPE || type == NOT_TALK_PROFILE_TYPE_1
    || type == NOT_TALK_PROFILE_TYPE_2 || type == NOT_TALK_PROFILE_TYPE_4 }
const val DECRYPT_MEMBER_KEY = 426760033L

const val KAKAO_PACKAGE_NAME = "com.kakao.talk"
const val REPLY_ACTION_INDEX = 1

val FOLDING_TEXT : String = "\u200B".repeat(500)
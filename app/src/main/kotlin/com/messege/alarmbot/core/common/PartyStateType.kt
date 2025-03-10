package com.messege.alarmbot.core.common

enum class PartyState(korName: String) {
    None(""),

    Active("활성"),
    Dissolution("해산");

    companion object {
        fun getStateByName(paramName : String): PartyState {
            return PartyState.entries.firstOrNull { it.name == paramName } ?: None
        }
    }
}

enum class PartyLogType(korName: String) {
    None(""),

    Founding("창당"),
    Dissolution("해산"),

    Inauguration("취임"),
    Delegation("위임"),

    Application("신청"),
    Cancellation("취소"),

    Approval("승인"),
    Rejection("거절"),

    Withdrawal("탈당"),
    Expulsion("제명");

    companion object {
        fun getTypeByName(paramName : String): PartyLogType {
            return PartyLogType.entries.firstOrNull { it.name == paramName } ?: None
        }
    }
}

enum class PartyMemberState(korName: String) {
    None("무당"),

    PartyLeader("당대표"),
    PartyMember("당원"),

    Applicant("신청자");

    companion object {
        fun getStateByName(paramName : String): PartyMemberState {
            return PartyMemberState.entries.firstOrNull { it.name == paramName } ?: None
        }
    }
}
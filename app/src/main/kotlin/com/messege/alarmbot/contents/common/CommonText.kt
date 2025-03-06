package com.messege.alarmbot.contents.common

import com.messege.alarmbot.core.common.Rank

const val COMMAND_HELP = "명령어 모음\n\n" +
    ".? 관리자 : 관리자용 명령어 확인\n" +
    ".? 직업 : 직업표 확인\n\n" +

    ".조회 @빵구 : 유저 정보를 확인\n\n" +

    "[포인트]\n" +
    ".랭킹 : 유저 좋아요, 싫어요 랭킹 TOP 10\n" +
    ".랭킹 주간 : 주간 좋아요, 싫어요 랭킹 TOP 10\n" +
    ".좋아 @빵구 : 해당 유저에게 좋아요 10 주기\n" +
    ".싫어 @빵구 : 해당 유저에게 싫어요 10 주기\n\n" +

    "[주제]\n" +
    ".주제 ..입력 : 입력 텍스트를 주제로 등록\n" +
    ".추천 : 등록된 주제 중 랜덤으로 추천\n" +
    ".추천 4 : 키값 4에 해당하는 주제 출력\n" +
    ".삭제 4 : 키값 4에 해당하는 주제 삭제 (관리자만)\n" +
    ".삭제 4-1 : 주제의 1번째 답글 삭제 (관리자만)\n" +
    "(주제글에 답글) : 해당 주제글에 답글이 추가됨\n"

const val ADMIN_COMMAND_HELP = "관리자 명령어 모음\n\n" +
    "[관리자]\n" +
    ".제재 @빵구 : 해당 유저에게 제재 1회 부여\n" +
    ".경고 @빵구 : 해당 유저에게 제재 2회 부여\n" +
    ".감면 @빵구 : 해당 유저의 제재 -1회\n" +
    ".(제재/경고/감면) n @빵구 ... : 제재 횟수 및 사유 입력\n" +
    ".제재내역 @빵구 : 제재 내역을 관리자방에서 확인\n\n" +

    "[슈퍼 관리자]\n" +
    ".임명 @모순 : 수동으로 부방 임명\n"+
    ".해제 @모순 : 수동으로 부방 해제\n"

fun rankHelp() = "<직업>\n\n" + "필요 포인트 = 좋아요 - 싫어요\n\n" +
    Rank.entries.sortedBy { it.rank }.joinToString("\n\n") {
        "[${it.tier}] " + (if(it.isSuperAdmin) "[방장]" else if(it.isAdmin) "[부방장]" else "") +
            " ${it.korName} : \n- 데일리 포인트 (${it.resetPoints})\n- 필요 포인트 (${it.requirePoint})"
    }

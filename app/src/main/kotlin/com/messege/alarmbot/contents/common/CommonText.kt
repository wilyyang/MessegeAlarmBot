package com.messege.alarmbot.contents.common

import com.messege.alarmbot.core.common.FOLDING_TEXT
import com.messege.alarmbot.core.common.Rank

val COMMAND_HELP = "\uD83D\uDCA1 명령어 모음\n" +
    "${FOLDING_TEXT}\n" +
    ".? 관리자 : 관리자용 명령어 확인\n" +
    ".? 정당 : 정당용 명령어 확인\n\n" +
    ".? 직업 : 직업표 확인\n\n" +

    ".조회 @빵구 : 유저 정보를 확인\n\n" +

    "[포인트]\n" +
    ".랭킹 : 유저 좋아요, 싫어요 랭킹 TOP 10\n" +
    ".랭킹 주간 : 주간 좋아요, 싫어요 랭킹 TOP 10\n" +
    ".좋아 @빵구 : 해당 유저에게 좋아요 10 주기\n" +
    ".싫어 @빵구 : 해당 유저에게 싫어요 10 주기\n" +
    " * 매일 오전 6시마다 할당 포인트가 갱신됩니다\n\n" +

    "[주제]\n" +
    ".주제 ..입력 : 입력 텍스트를 주제로 등록\n" +
    ".추천 : 등록된 주제 중 랜덤으로 추천\n" +
    ".추천 4 : 키값 4에 해당하는 주제 출력\n" +
    ".삭제 4 : 키값 4에 해당하는 주제 삭제 (관리자만)\n" +
    ".삭제 4-1 : 주제의 1번째 답글 삭제 (관리자만)\n" +
    "(주제글에 답글) : 해당 주제글에 답글이 추가됨\n"

val ADMIN_COMMAND_HELP = "\uD83D\uDCA1 관리자 명령어 모음\n" +
    "${FOLDING_TEXT}\n" +
    "[관리자]\n" +
    ".제재 @빵구 : 해당 유저에게 제재 1회 부여\n" +
    ".경고 @빵구 : 해당 유저에게 제재 2회 부여\n" +
    ".감면 @빵구 : 해당 유저의 제재 -1회\n" +
    ".(제재/경고/감면) n @빵구 ... : 제재 횟수 및 사유 입력\n" +
    ".제재내역 @빵구 : 제재 내역을 관리자방에서 확인\n\n" +

    "[슈퍼 관리자]\n" +
    ".임명 @모순 : 수동으로 부방 임명\n"+
    ".해제 @모순 : 수동으로 부방 해제\n"

fun rankHelp() = "\uD83D\uDCA1 직업\n" +
        "${FOLDING_TEXT}\n" +
        "- 필요 포인트가 오르면 직업도 올라가요!\n" +
        " * 필요 포인트 = 좋아요 - 싫어요\n" +
        "- [티어]에 따라 할 수 있는 기능이 달라져요!\n" +
        " * 4티어 이상 시 주제 삭제 가능\n" +
        "- 직업에 따라 매일 할당받는 포인트도 달라집니다\n\n" +
    Rank.entries.sortedBy { it.rank }.joinToString("\n\n") {
        "[${it.tier}] " + (if(it.isSuperAdmin) "[방장]" else if(it.isAdmin) "[부방장]" else "") +
            " ${it.korName} : \n- 데일리 포인트 (${it.resetPoints})\n- 필요 포인트 (${it.requirePoint})"
    }


val PARTY_COMMAND_HELP = "\uD83D\uDCA1 정당 명령어 모음\n" +
    "${FOLDING_TEXT}\n" +
    "[정당]\n" +
    ".창당 {이름} : 창당 (당이 없어야 가능)\n" +
    ".해산 : 당 해산 (당대표만 가능)\n" +
    ".당조회 {이름} : 당대표, 인원, 당원을 조회\n" +
    ".당랭킹 : 인원수대로 정당 랭킹을 나열\n" +
    ".당명 {당명} : 당의 당명을 변경 (당대표만 가능)\n" +
    ".당소개 {소개} : 당의 소개글을 변경 (당대표만 가능)\n\n" +
    ".당규추가 {당규} : 당의 규칙을 추가 (당대표만 가능)\n" +
    ".당규삭제 n : n번째 당규를 삭제 (당대표만 가능)\n" +
    ".당규 {당명} : 당의 규칙 조회\n\n" +
    "[유저]\n" +
    ".위임 @당원 : 해당 유저에게 당대표 위임 (당대표만 가능)\n" +
    ".신청 {당명} : 당 가입 신청 (당이 없어야 가능)\n" +
    ".취소 : 가입 신청한 당에 신청 취소\n" +
    ".승인 @신청자 : 신청자의 가입을 승인 (당대표만 가능)\n" +
    ".거절 @신청자 : 신청자의 가입을 거절 (당대표만 가능)\n" +
    ".탈당 : 가입한 정당을 탈당\n" +
    ".제명 @당원 : 당원을 제명함 (당대표만 가능)\n" +
    ".신청목록 {당명} : 해당 당명의 신청자 목록을 확인"

package com.messege.alarmbot.core.common

import com.messege.alarmbot.contents.mafia.Player

object MafiaText {
    const val GAME_RULE = "마피아 게임 설명입니다.\n\n" +
            "- .마피아 : 마피아를 시작하거나 게임 진행상황을 확인\n" +
            "- .마피아종료 : 마피아 게임을 종료합니다 (참여자만 가능)\n\n" +
            "* 단계\n" +
            "- 인원 참여\n" +
            "- 게임 진행\n" +
            "-- 1 대화 : 3분\n" +
            "-- 2 투표 : 마피아를 투표\n" +
            "-- 3 암살 : 마피아가 암살\n" +
            "-- 4 수사 : 경찰이 수사\n\n" +
            "* 세력\n" +
            "1. 시민 : 마피아를 제거하여 승리\n" +
            "1.1 경찰 : 수사 시간에 직업 확인\n" +
            "2. 마피아 : 마피아 많으면 승리\n" +
            "3. 바보 : 투표로 죽으면 승리\n\n" +
            "* 인원 배합\n" +
            "4: 마피아1, 시민3\n" +
            "5: 마피아1, 바보1, 시민3\n" +
            "6: 마피아2, 시민3, 경찰1\n" +
            "7: 마피아2, 바보1, 시민3, 경찰1\n" +
            "8: 마피아3, 시민4, 경찰1"

    const val GAME_ALREADY_START = "[이미 게임 진행 중입니다.]"
    const val GAME_ALREADY_USER = "[이미 같은 이름의 플레이어가 있습니다.]"
    const val GAME_WAIT_END = "[인원이 마감되었거나 게임 진행중이 아닙니다.]"


    fun gameRemainingTime(state: String, total: Int, remain: Int) = "[$state 단계 남은 시간 : $remain / $total 초]"

    fun gameEndWaitTimeOut(num : Int) = "[시간 만료. 인원이 부족하여 게임을 시작할 수 없습니다. ($num 명) ]"

    fun gameWaitTimeOutGoToCheck(players : List<Player>) = "[시간 만료. (${players.size} 명) ]\n* 참여인원\n${playerProgressToText(players)}"


    fun mafiaMessage(name: String, text : String) =
        "[$name 마피아님이 전달한 메시지]\n$text"

    fun hostStartGame(hostName: String) =
        "[마피아 게임 시작]\n\n" +
                "* 호스트: $hostName\n"+
                "!! 마피아 게임을 원하면 같은 닉네임으로 봇에게 \"참여\"라고 말해주세요.\n" +
                "!! 인원이 충분하면 $hostName 님은 \".참여종료\"라고 톡해주세요."

    fun userInviteGame(userName: String, players : List<Player>) = "[$userName 가 게임에 참여했습니다]\n${playerProgressToText(players)}"

    private fun playerProgressToText(
        players: List<Player>): String {

        val playersText = players.joinToString("\n") {
            "- ${it.name} : ${
                if (it is Player.Assign) {
                    if (it.isSurvive) "생존" else "사망"
                } else {
                    "직업 미할당"
                }
            }"
        }
        return playersText
    }


    const val GAME_END = "[마피아 종료]"
    const val GAME_NOT_START = "[게임이 없습니다.]"
}
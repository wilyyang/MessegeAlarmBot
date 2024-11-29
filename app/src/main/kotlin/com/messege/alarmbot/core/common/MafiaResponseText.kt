package com.messege.alarmbot.core.common

import com.messege.alarmbot.contents.mafia.Player

object MafiaText {
    const val GAME_RULE = "마피아 게임 설명입니다.\n\n" +
            "- .마피아 : 마피아를 시작하거나 게임 진행상황을 확인\n" +
            "- .마피아종료 : 마피아 게임을 종료합니다 (참여자만 가능)\n\n" +
            "- .미션 : 마피아 미션 목록이 나열됩니다.\n\n" +
            "* 단계\n" +
            "- 인원 참여\n" +
            "- 게임 진행\n" +
            "-- 1 대화 : 3분\n" +
            "-- 2 투표 : 마피아를 투표\n" +
            "-- 3 암살 : 마피아가 암살\n" +
            "-- 4 조사 : 영매나 경찰이 조사\n\n" +
            "* 세력\n" +
            "* 시민 세력 직업은 랜덤이지만 중복되지 않습니다.\n" +
            "1. 시민 : 마피아를 제거하여 승리\n" +
            "1.1 경찰 : 조사 시간에 생존자 마피아 여부 확인\n" +
            "1.2 영매 : 조사 시간에 사망자 직업 확인\n" +
            "1.3 의사 : 암살 대상을 보호\n" +
            "1.4 보디가드 : 투표 대상을 보호\n" +
            "1.5 정치인 : 투표 한번이 2개로 적용\n" +
            "1.6 국정원 : 마피아의 대화를 엿들음\n" +
            "1.7 시민\n" +
            "2. 마피아 : 마피아 많으면 승리\n" +
            "3. 바보 : 투표로 죽으면 승리\n\n" +
            "* 인원 배합\n" +
            "4: 마피아1, 시민3\n" +
            "5: 마피아1, 바보1, 시민3\n" +
            "6: 마피아1, 바보1, 시민4\n" +
            "7: 마피아2, 바보1, 시민4\n" +
            "8: 마피아2, 바보1, 시민5"

    const val GAME_END_COMMAND = "[마피아를 종료합니다.]"
    const val GAME_ALREADY_START = "[이미 게임 진행 중입니다.]"
    const val GAME_ALREADY_USER = "[이미 같은 이름의 플레이어가 있습니다.]"
    const val GAME_WAIT_END = "[인원이 마감되었거나 게임 진행중이 아닙니다.]"
    const val GAME_NOT_START_MORE_PLAYER = "[인원이 부족하여 게임을 시작할 수 없습니다.]"
    const val GAME_ASSIGN_JOB = "[직업 할당중 ...]"

    const val ASSIGN_JOB_CITIZEN = "[당신은 시민입니다. 마피아를 모두 찾아 죽이면 승리합니다.]"
    const val ASSIGN_JOB_POLICE  = "[당신은 경찰입니다. 마피아를 모두 찾아 죽이면 승리합니다.]\n* 조사시간에 한명을 지목하여 마피아 여부를 확인할 수 있어요"
    const val ASSIGN_JOB_SHAMAN  = "[당신은 영매입니다. 마피아를 모두 찾아 죽이면 승리합니다.]\n* 조사시간에 죽은 사람을 지목하여 직업을 확인할 수 있어요"
    const val ASSIGN_JOB_MAFIA   = "[당신은 마피아입니다. 시민보다 마피아가 많으면 승리합니다.]"
    const val ASSIGN_JOB_FOOL    = "[당신은 바보입니다. 투표시간에 지목당해 죽으면 승리합니다.]"

    const val ASSIGN_JOB_DOCTOR         = "[당신은 의사입니다. 마피아를 모두 찾아 죽이면 승리합니다.]\n" +
        "* 능력 : 개인톡으로 지목한 사람을 암살에서 구해냅니다. (대상은 언제든 바꿀 수 있으며 능력이 아침마다 리셋됩니다.)"
    const val ASSIGN_JOB_BODYGUARD      = "[당신은 보디가드입니다. 마피아를 모두 찾아 죽이면 승리합니다.]\n" +
        "* 능력 : 개인톡으로 지목한 사람을 투표에서 구해냅니다. (대상은 언제든 바꿀 수 있으며 능력이 아침마다 리셋됩니다.)"


    const val ASSIGN_JOB_POLITICIAN = "[당신은 정치인입니다. 마피아를 모두 찾아 죽이면 승리합니다.]\n" +
        "* 능력 : 당신의 투표는 투표 결과에 2표로 적용됩니다.)"
    const val ASSIGN_JOB_AGENT = "[당신은 국정원 요원입니다. 마피아를 모두 찾아 죽이면 승리합니다.]\n" +
        "* 능력 : 마피아의 대화를 엿들을 수 있습니다.)"

    const val VOTE_RESULT_NOT    = "[누구도 투표로 죽지 않았습니다.]"
    const val KILL_RESULT_NOT    = "[누구도 암살로 죽지 않았습니다.]"

    fun winFool(name : String, players: List<Player>) = "[죽은 $name 님은 바보입니다. 투표로 죽은 바보의 단독 승리입니다!]\n${playerProgressToText(players, true)}"

    fun winCitizen(name : String, players: List<Player>) = "[죽은 $name 님은 마피아입니다. 마피아가 모두 죽어 시민이 승리합니다!]\n${playerProgressToText(players, true)}"

    fun winMafia(name : String, players: List<Player>) = "[죽은 $name 님은 시민입니다. 시민 수가 적어 마피아가 이겼습니다!]\n${playerProgressToText(players, true)}"

    fun mafiaVoted(name : String, players: List<Player>) = "[죽은 $name 님은 마피아입니다.]\n${playerProgressToText(players)}"

    fun citizenVoted(name : String, players: List<Player>) = "[죽은 $name 님은 마피아가 아닙니다.]\n${playerProgressToText(players)}"

    fun gameStateTalk(time: Int) = "[아침이 되었습니다. 모두 마피아를 찾기 위해 대화를 나누세요. ($time 초)]\n호스트가 \"스킵\"이라고 치면 대화가 넘어갑니다."

    fun gameStateVote(time: Int) = "[투표 시간입니다. 전체 채팅에 마피아로 파악되는 유저 이름을 정확하게 톡해주세요. 앞에 @를 붙여도 됩니다. ($time 초)]"

    fun gameStateKill(time: Int) = "[밤이 되었습니다. 마피아는 봇에게 개인톡을 하면 서로 대화가 가능합니다. 봇 개인톡으로 죽일 이름을 말해주세요. ($time 초)]"

    fun gameStatePoliceTime(time: Int, players: List<Player>)
    = "[새벽의 조사시간입니다. 경찰이나 영매는 봇 개인톡으로 조사 대상을 말해주세요. ($time 초)]\n바보는 시민으로 뜹니다.\n" + playerProgressToText(players)

    fun voteKillUser(voteName : String) = "[투표로 인해 $voteName 님이 죽었습니다.]"

    fun mafiaKillUser(targetName : String) = "[$targetName 님이 마피아에 의해 암살당했습니다.]"

    fun policeMessage(name: String, isMafia : Boolean) = "[$name 는 마피아 편${ if(isMafia) "입니다." else "이 아닙니다." }]"

    fun shamanMessage(name: String, job : String) = "[$name 의 직업은 $job 입니다.]"

    fun gameRemainingTime(state: String, total: Int, remain: Int) = "[마피아 ($state) 단계 남은 시간 : $remain / $total 초]"

    fun gameEndWaitTimeOut(num : Int) = "[시간 만료. 인원이 부족하여 게임을 시작할 수 없습니다. ($num 명) ]"

    fun gameWaitTimeOutGoToCheck(players : List<Player>) = "[시간 만료. (${players.size} 명) ]\n* 참여인원\n${playerProgressToText(players)}"

    fun gameEndCheckTimeOut(num : Int) = "[시간 만료. 확인된 인원이 게임을 시작하기에 부족합니다. ($num 명) ]"

    fun gameCheckTimeOutGoToAssign(players : List<Player>) = "[시간 만료. (${players.size} 명) ]\n* 확인인원\n${playerProgressToText(players)}"

    fun userVote(userName: String, voteName: String) = "[$userName 님이 $voteName 님을 투표했습니다.]"

    fun doctorMessage(name: String, target : String) = "[$name 님이 $target 님을 암살로부터 보호합니다.]"
    fun bodyguardMessage(name: String, target : String) = "[$name 님이 $target 님을 투표로부터 보호합니다.]"

    fun doctorSaveMan(target : String) = "[의사가 $target 님을 암살로부터 보호합니다.]"
    fun bodyguardSaveMan(target : String) = "[보디가드가 $target 님을 투표로부터 보호합니다.]"

    fun mafiaMessage(name: String, text : String) =
        "[$name 마피아님이 전달한 메시지]\n$text"

    fun hostStartGame(hostName: String) =
        "[마피아 게임 시작]\n\n" +
                "* 호스트: $hostName\n"+
                "!! 마피아를 하려면 채팅에 \"참여\"라고 톡해주세요.\n" +
                "!! 인원이 충분하면 $hostName 님은 \".참여종료\"라고 톡해주세요."

    fun checkPlayer(hostName: String, players : List<Player>) =
        "[참여 확인중]\n\n" +
                "* 호스트: $hostName\n"+
                "!! ${players.size}인원이 참여했습니다.\n" +
                "!! 개인톡으로 봇에게 \"확인\"이라고 톡해주세요.\n" +
                playerProgressToText(players)

    fun userInviteGame(userName: String, players : List<Player>) = "[$userName 가 게임에 참여했습니다]\n${playerProgressToText(players)}"

    fun userCheckGame(userName: String) = "[$userName 님의 개인톡이 확인되었습니다]"

    private fun playerProgressToText(
        players: List<Player>, isJob : Boolean = false): String {

        val playersText = players.joinToString("\n") {
            "- ${it.name} : ${
                if (it is Player.Assign) {
                    val job = if(isJob) it.job.korName else ""
                    if (it.isSurvive) "생존 $job" else "사망 $job"
                } else {
                    "직업 미할당"
                }
            }"
        }
        return playersText
    }
}
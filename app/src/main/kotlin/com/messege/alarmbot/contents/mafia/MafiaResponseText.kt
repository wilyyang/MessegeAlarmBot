package com.messege.alarmbot.contents.mafia

import com.messege.alarmbot.core.common.FOLDING_TEXT

val arrayOfMafiaMissions = arrayOf(
    "➊ 헛기침 3번, 한숨1번",
    "➌ 상대방 말끊기 3번",
    "➍ 노래 흥얼거리기",
    "➎ 마이크 껐다켰다 3번하기",
    "➐ 자연스럽게 반말하기",
    "➑ 다른 사람 칭찬하기",
    "➒ 영어단어 섞어서 3개 말하기",
    "➓ 상대방 말끊기 3번",
    "⓫ 한 사람 닉네임 3번이상 호명하며 대화하기",
    "⓬ 오늘 먹은 음식 한가지 말하기",
    "⓭ TMI 말하기",
    "⓮ 말할때 “인생” 넣어서 2번 말하기",
    "⓯ 말할때 ”오늘” 넣어서 5번 말하기",
    "⓰ 감탄사 5번하기 (아하, 오호, 우와..)",
    "⓱ 날씨 얘기하기",
    "⓲ 말할때 ”아니 근҉데҉ 진짜” 3번 말하기",
    "⓳ 말할때 “솔직히” 3번 말하기"
)

object MafiaText {
    const val GAME_END_COMMAND = "[마피아를 종료합니다.]"
    const val GAME_ALREADY_START = "[이미 게임 진행 중입니다.]"
    const val GAME_ALREADY_USER = "[이미 같은 이름의 플레이어가 있습니다.]"
    const val GAME_WAIT_END = "[인원이 마감되었거나 게임 진행중이 아닙니다.]"
    const val GAME_NOT_START_MORE_PLAYER = "[인원이 부족하여 게임을 시작할 수 없습니다.]"
    const val GAME_ASSIGN_JOB = "[직업 할당중 ...]"

    const val ASSIGN_JOB_CITIZEN = "[당신은 시민입니다. 마피아를 모두 찾아 죽이면 승리합니다.]"
    const val ASSIGN_JOB_POLICE  = "[당신은 경찰입니다. 마피아를 모두 찾아 죽이면 승리합니다.]\n* 밤시간에 한명을 지목하여 마피아 여부를 확인할 수 있어요"
    const val ASSIGN_JOB_SHAMAN  = "[당신은 영매입니다. 마피아를 모두 찾아 죽이면 승리합니다.]\n* 밤시간에 죽은 사람을 지목하여 직업을 확인할 수 있어요"
    const val ASSIGN_JOB_SOLDIER = "[당신은 군인입니다. 마피아를 모두 찾아 죽이면 승리합니다.]\n* 마피아에게 암살당하면, 마피아 한 명을 죽입니다"
    const val ASSIGN_JOB_MAGICIAN = "[당신은 마술사입니다. 대화시간에 선택한 직업이 됩니다.(2번째 날부터 능력 사용 가능)]\n* 선택 대상의 경우, 마피아는 사망, 시민 직업은 시민이 됨"
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

    fun magicianKillMafia(mafia: String) = "[$mafia 님이 마술사에 의해 죽었습니다]"
    fun magicianHaveJob(job: String) = "[당신은 이제 $job 입니다. $job 으로 행동해주세요.]"

    const val MAGICIAN_GET_YOUR_JOB = "[마술사가 당신의 직업을 가져갔습니다. 당신은 이제 시민입니다.]"

    fun winFool(name : String, players: List<Player>) = "[죽은 $name 님은 바보입니다. 투표로 죽은 바보의 단독 승리입니다!]\n${playerProgressToText(players, true)}"

    fun winCitizen(name : String, players: List<Player>) = "[죽은 $name 님은 마피아입니다. 마피아가 모두 죽어 시민이 승리합니다!]\n${playerProgressToText(players, true)}"

    fun winCitizenWithSoldier(name : String, players: List<Player>) = "[죽은 $name 님에 의해 마피아가 모두 죽어 시민이 승리합니다!]\n${playerProgressToText(players, true)}"

    fun winMafia(name : String, players: List<Player>) = "[죽은 $name 님은 시민입니다. 시민 수가 적어 마피아가 이겼습니다!]\n${playerProgressToText(players, true)}"
    fun winMagic(players: List<Player>) = "[마술사는 마피아가 되었습니다. 시민 수가 적어 마피아가 이겼습니다!]\n${playerProgressToText(players, true)}"

    fun mafiaVoted(name : String, players: List<Player>) = "[죽은 $name 님은 마피아입니다.]\n${playerProgressToText(players)}"

    fun citizenVoted(name : String, players: List<Player>) = "[죽은 $name 님은 마피아가 아닙니다.]\n${playerProgressToText(players)}"

    fun gameStateTalk(time: Int) = "[아침이 되었습니다. 모두 마피아를 찾기 위해 대화를 나누세요. ($time 초)]\n호스트가 \"스킵\"이라고 치면 대화가 넘어갑니다."

    fun gameStateVote(time: Int) = "[투표 시간입니다. 전체 채팅에 마피아로 파악되는 유저 이름을 정확하게 톡해주세요. 앞에 @를 붙여도 됩니다. ($time 초)]"

    fun gameVoteCompleteSoon() = "[투표 종료 10초 전입니다!]"

    fun gameStateKill(time: Int) = "[밤이 되었습니다. 마피아는 봇에게 개인톡을 하면 서로 대화가 가능합니다. 봇 개인톡으로 죽일 이름을 말해주세요. 경찰이나 영매도 봇 개인톡으로 조사 대상을 말해주세요. ($time 초)]"

    fun gameKillCompleteSoon() = "[밤 시간 종료 10초 전입니다!]"

    fun gameStatePoliceTime(time: Int, players: List<Player>)
    = "[새벽의 조사시간입니다. 경찰이나 영매는 봇 개인톡으로 조사 대상을 말해주세요. ($time 초)]\n바보는 시민으로 뜹니다.\n" + playerProgressToText(players)

    fun voteKillUser(voteName : String) = "[투표로 인해 $voteName 님이 죽었습니다.]"

    fun mafiaKillUser(targetName : String) = "[$targetName 님이 마피아에 의해 암살당했습니다.]"

    fun policeMessage(name: String, isMafia : Boolean) = "[$name 는 마피아 편${ if(isMafia) "입니다." else "이 아닙니다." }]"

    fun shamanMessage(name: String, job : String) = "[$name 의 직업은 $job 입니다.]"

    fun gameRemainingTime(state: String, total: Int, remain: Int, players: List<Player>?)
    = "[마피아 ($state) 단계 남은 시간 : $remain / $total 초]" +
        if(players == null) { "" }
        else {
            FOLDING_TEXT + "\n" +
                playerProgressToText(players = players, isJob = false)
        }

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

    fun soldierKilledMessage(name: String, soldier: String) =
        "[마피아 $name 님이 군인 $soldier 님에 의해 죽었습니다]"

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
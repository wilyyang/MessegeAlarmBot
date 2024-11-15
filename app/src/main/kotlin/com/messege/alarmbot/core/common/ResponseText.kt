package com.messege.alarmbot.core.common

import com.messege.alarmbot.contents.UserKey

object CommonText{
    const val HELP = "안녕하세요? 빵구봇입니다.\n\n" +
            "* 현재 사용 가능한 명령어\n\n" +
            "- .다섯고개\n" +
            "- .다섯고개규칙\n" +
            "- .다섯고개종료\n" +
            "- .마피아\n" +
            "- .마피아규칙\n" +
            "- .마피아종료\n"

    fun alreadyUser(name: String, alreadyUsers : List<String>) : String{
        val alreadyText = alreadyUsers.joinToString(", ")
        return "${name}님 안녕하세요. 이전에 $alreadyText 닉네임으로 오셨군요."
    }
}

object QuestionGameText{
    const val GAME_RULE = "[다섯 고개 규칙]\n\n" +
            "1. 10개의 단어가 나열된다\n" +
            "2. 호스트는 나열된 단어 중 하나를 선택한다. (갠톡)\n" +
            "3. 5번의 질문과 5번의 정답 확인을 할 수 있다\n" +
            "4. 호스트는 5개의 질문 중 1번 거짓말 할 수 있다."

    const val GAME_END = "[다섯 고개 종료]"
    const val GAME_NOT_START = "[게임이 없습니다.]"
    const val NOT_ANSWER = "[정답 후보에 들어있지 않아요.]"

    fun hostStartGame(hostName: String, totalWords: List<String>) =
        "[다섯 고개 게임 시작]\n\n" +
                "* 호스트: $hostName\n\n" +
                "단어 : ${totalWords.joinToString(", ")}\n\n" +
                "!! $hostName 님은 봇에게 갠톡으로 나열된 단어 중 하나를 선택하여 알려주세요!"

    fun hostSelectCompleteAnswer(hostName: String) = "$hostName 님이 정답을 정했습니다. \n$hostName 님에게 질문하고 채팅에 답을 올려주세요!"

    fun alreadyGameProgress(
        userWords: List<Pair<UserKey, String>>,
        notSelectWords: List<String>
    ) = "[이미 게임 진행 중입니다.]\n\n${wordProgressToText(userWords, notSelectWords)}"

    fun userSelectAnswer(
        userName: String,
        answer: String,
        userWords: List<Pair<UserKey, String>>,
        notSelectWords: List<String>
    ) = "[${userName} 님이 정답을 맞추셨습니다! 정답은 $answer 입니다.]\n\n${wordProgressToText(userWords, notSelectWords)}"

    fun userSelectAlreadyWord(
        userName: String,
        text: String,
        userWords: List<Pair<UserKey, String>>,
        notSelectWords: List<String>
    ) = "[${userName} 님이 말한 $text 는 이미 말한 단어입니다.]\n\n${wordProgressToText(userWords, notSelectWords)}"

    fun userSelectNotAnswer(
        userName: String,
        text: String,
        userWords: List<Pair<UserKey, String>>,
        notSelectWords: List<String>
    ) = "[${userName}님의 $text 는 정답이 아닙니다.]\n\n${wordProgressToText(userWords, notSelectWords)}"

    fun userSelectNotAnswerAndHostWin(
        userName: String,
        text: String,
        hostName: String,
        answer: String,
        userWords: List<Pair<UserKey, String>>,
        notSelectWords: List<String>
    ) = "[${userName}님의 $text 는 정답이 아닙니다.]\n" +
            "모든 기회를 소진하여 $hostName 님이 이겼습니다. 정답은 $answer 입니다." +
            "\n\n${wordProgressToText(userWords, notSelectWords)}"

    private fun wordProgressToText(
        userWords: List<Pair<UserKey, String>>,
        notSelectWords: List<String>
    ): String {
        val result = "오답 : \n" +
                userWords.joinToString("\n") { "- ${it.first.name} : ${it.second}" } +
                "\n\n후보 : " +
                notSelectWords.joinToString(", ")
        return result
    }
}
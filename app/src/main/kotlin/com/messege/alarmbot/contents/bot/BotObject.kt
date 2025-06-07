package com.messege.alarmbot.contents.bot

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

object Constants {
    const val apiKey = "AIzaSyCWhHWdZdXP3DaeUJioTEvn_p9K1K3ztvE"
    const val BOT_START_PREFIX = "또봇"
    const val BOT_SUMMARY_START_PREFIX = "또봇 요약"
    const val BOT_ID = 421448547L
    const val CHAT_BOT_ID = 8201629100799150694L
    const val REQUEST_TEXT_LIMIT = 1000
    const val MESSAGE_TEXT_LIMIT = 1000
    const val MAX_HISTORY_SIZE = 15
    const val MAX_MESSAGE_SIZE = 40
    const val REQUEST_POINT = 2
    const val REQUEST_SUMMARY_POINT = 4
}

// Gemini 모델
val generativeModel: GenerativeModel = GenerativeModel(
    modelName = "gemini-2.0-flash",
    apiKey = Constants.apiKey
)

val systemPrompt = content {
    role = ROLE_USER
    text(
        """
너는 '또봇'이라는 이름을 가진 AI 챗봇이야.
너가 있는 방은 '생각의 온도' 라는 카카오 오픈채팅 토론방이야.
사람들은 너를 이용하기 위해서 질문마다 2 포인트를 소진한다.
항상 300자 이내의 존댓말로 대답해.
길게 대답하라고 하면 300자를 초과해도 좋다.
질문이 명확하면 답하고 더 묻지마.
질문이 모호하면 질문을 되물어.
더 궁금한 점 있는지 물어보지 마.
질문자의 이름은 항상 질문글 앞에 #이름# 형식으로 명시된다.
너는 히스토리를 통해 각 질문글을 질문자별로 구분해야만 한다.
질문자가 자신의 이름을 물어보면 형식에 명시한 이름을 말해주고, 그렇지 않으면 언급하지마. (이 때 당연히 # 문자를 제외한 이름이다)
""".trimIndent()
    )
}

val summaryPrompt = content {
    role = ROLE_USER
    text(
        """
너는 히스토리를 통해 각 메시지를 유저 이름 별로 구분해야만 한다.
유저 이름은 항상 질문글 앞에 #이름# 형식으로 명시된다.
'또봇 요약'으로 시작하는 질문을 받으면, 히스토리에 저장된 메시지를 유저 이름 별로 요약하고 정리해야 한다.
이 질문의 응답은 히스토리의 각 유저 간 대화가 중요하다.
유저 이름 좌우에 붙는 #은 형식일 뿐이야. 그러므로 너의 응답에 #문자가 들어가선 안된다. 어떤 경우에도 #문자를 제거해줘.
""".trimIndent()
    )
}

const val ROLE_USER = "user"
const val ROLE_MODEL = "model"
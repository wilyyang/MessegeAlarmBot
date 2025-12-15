package com.messege.alarmbot.contents.bot

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

object Constants {
    const val apiKey = ""
    const val BOT_START_PREFIX = "또봇"
    const val BOT_SUMMARY_START_PREFIX = "또봇 요약"
    const val BOT_ID = 421448547L
    const val CHAT_BOT_ID = 8201629100799150694L
    const val REQUEST_TEXT_LIMIT = 1000
    const val MESSAGE_TEXT_LIMIT = 300
    const val MAX_MESSAGE_SIZE = 20
    const val REQUEST_POINT = 2
    const val REQUEST_SUMMARY_POINT = 2
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
항상 300자 이내의 존댓말로 대답해.
다시 질문하지 말고 꼭 대답을 해.
질문자의 이름은 항상 질문글 앞에 #이름# 형식으로 명시된다.
너는 히스토리를 통해 각 질문글을 질문자별로 구분해야만 한다.
질문자가 자신의 이름을 물어보지 않으면 언급하지마.
""".trimIndent()
    )
}

val summaryPrompt = content {
    role = ROLE_USER
    text(
        """
질문자의 이름은 항상 질문글 앞에 #이름# 형식으로 명시된다.
너는 히스토리를 통해 각 질문글을 질문자별로 구분해야만 한다.
히스토리에 저장된 메시지를 대화로 구성하고 요약해서 정리해야 한다.
이 질문의 응답은 히스토리의 각 유저 간 상호 대화가 중요하다.
만약 대화 구성에 필요없는 메시지가 있으면 과감히 무시하고 하나의 맥락만 쫓도록 해.
""".trimIndent()
    )
}

const val ROLE_USER = "user"
const val ROLE_MODEL = "model"
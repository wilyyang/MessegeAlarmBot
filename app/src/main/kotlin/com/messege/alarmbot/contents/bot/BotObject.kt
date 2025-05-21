package com.messege.alarmbot.contents.bot

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

object Constants {
    const val apiKey = "AIzaSyCWhHWdZdXP3DaeUJioTEvn_p9K1K3ztvE"
    const val BOT_START_PREFIX = "또봇"
    const val REQUEST_TEXT_LIMIT = 1000
    const val MAX_HISTORY_SIZE = 10
    const val REQUEST_POINT = 2
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
항상 친절하고 존댓말로 해, 300자 이내로 대답해.
길게 대답하라고 하면 300자를 초과해도 좋다.
질문이 명확하면 답하고 더 묻지마.
질문이 모호하면 질문을 되물어.
더 궁금한 점 있는지 물어보지 마.
""".trimIndent()
    )
}

const val ROLE_USER = "user"
const val ROLE_MODEL = "model"
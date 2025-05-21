package com.messege.alarmbot.contents.bot

import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.bot.Constants.REQUEST_POINT
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Group1RoomTextResponse
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.channels.Channel

class BotContent(
    override val commandChannel : Channel<Command>,
    private val memberDatabaseDao : MemberDatabaseDao
) : BaseContent {
    override val contentsName : String = "봇"
    private val historyCache : MutableList<Content> = mutableListOf()

    override suspend fun request(message : Message) {
        if (message.type == ChatRoomType.GroupRoom1 && message is Message.Talk &&
            message.text.length in (Constants.BOT_START_PREFIX.length + 1) .. Constants.REQUEST_TEXT_LIMIT &&
            message.text.startsWith(Constants.BOT_START_PREFIX)
        ) {
            val user = memberDatabaseDao.getMember(message.userId).getOrNull(0)
            if (user != null) {
                if (user.giftPoints >= REQUEST_POINT) {
                    val question = message.text.substring(Constants.BOT_START_PREFIX.length).trim()
                    if (question.isBlank()) return

                    val response = handleBotQuestion(question, user.latestName)
                    if (response != null) {
                        memberDatabaseDao.updateMemberGiftPoints(user.userId, user.giftPoints - REQUEST_POINT)
                        commandChannel.send(Group1RoomTextResponse(response))
                    }
                } else {
                    commandChannel.send(Group1RoomTextResponse("질문하려면 $REQUEST_POINT 포인트가 필요합니다. (현재 포인트 : ${user.giftPoints})"))
                }
            }
        }
    }

    private suspend fun handleBotQuestion(question : String, latestName : String) : String? {
        val userPrompt = content {
            role = ROLE_USER
            text("질문자의 이름은 $latestName 이야. 물어보면 이름을 말해주고, 그렇지 않으면 언급할 필요 없어")
        }

        try {
            val chat = generativeModel.startChat(
                history = listOf(systemPrompt, userPrompt) + historyCache
            )

            val response = chat.sendMessage(question)
            val replyText = response.text?.trim().orEmpty()

            if (replyText.isNotEmpty()) {
                // 히스토리에 추가
                historyCache += content { role = ROLE_USER; text(question) }
                historyCache += content { role = ROLE_MODEL; text(replyText) }
                while (historyCache.size > Constants.MAX_HISTORY_SIZE) {
                    historyCache.removeAt(0)
                }
                return replyText
            }
        } catch (e : Exception) {
            Logger.e("[ai.error] ${e.message}")
        }
        return null
    }
}
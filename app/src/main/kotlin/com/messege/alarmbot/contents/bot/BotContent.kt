package com.messege.alarmbot.contents.bot

import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.bot.Constants.BOT_ID
import com.messege.alarmbot.contents.bot.Constants.CHAT_BOT_ID
import com.messege.alarmbot.contents.bot.Constants.MESSAGE_TEXT_LIMIT
import com.messege.alarmbot.contents.bot.Constants.REQUEST_POINT
import com.messege.alarmbot.contents.bot.Constants.REQUEST_SUMMARY_POINT
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.core.common.FOLDING_TEXT
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
    private val messageCache : MutableList<Content> = mutableListOf()

    override suspend fun request(message : Message) {
        val user = memberDatabaseDao.getMember(message.userId).getOrNull(0)
        if (message.type == ChatRoomType.GroupRoom1 && message is Message.Talk
            && message.userId != BOT_ID && message.userId != CHAT_BOT_ID && user != null
        ) {
            messageCache += content { role = ROLE_USER; text("#${user.latestName}# ${message.text.take(MESSAGE_TEXT_LIMIT)}") }
            while (messageCache.size > Constants.MAX_MESSAGE_SIZE) {
                messageCache.removeAt(0)
            }

            if (message.text.startsWith(Constants.BOT_SUMMARY_START_PREFIX) &&
                message.text.length <= MESSAGE_TEXT_LIMIT
            ) {
                if (user.giftPoints >= REQUEST_SUMMARY_POINT) {
                    val response = handleBotSummaryQuestion("#${user.latestName}# ${message.text}")
                    if (response != null) {
                        memberDatabaseDao.updateMemberGiftPoints(user.userId, user.giftPoints - REQUEST_SUMMARY_POINT)
                        commandChannel.send(Group1RoomTextResponse("[요약]${FOLDING_TEXT}\n$response"))
                    }
                } else {
                    commandChannel.send(Group1RoomTextResponse("요약하려면 $REQUEST_SUMMARY_POINT 포인트가 필요합니다. (현재 포인트 : ${user.giftPoints})"))
                }
            } else if (message.text.startsWith(Constants.BOT_START_PREFIX) && message.text.length < Constants.REQUEST_TEXT_LIMIT
            ) {
                if (user.giftPoints >= REQUEST_POINT) {
                    val question = message.text.substring(Constants.BOT_START_PREFIX.length).trim()
                    if (question.isBlank()) return

                    val response = handleBotQuestion("#${user.latestName}# $question")
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

    private suspend fun handleBotQuestion(question : String) : String? {
        try {
            val chat = generativeModel.startChat(
                history = listOf(systemPrompt) + messageCache
            )
            val response = chat.sendMessage(question)
            val replyText = response.text?.trim().orEmpty()

            if (replyText.isNotEmpty()) {
                messageCache += content { role = ROLE_MODEL; text(replyText) }
                return replyText
            }
        } catch (e : Exception) {
            Logger.e("[ai.error] ${e.message}")
        }
        return null
    }

    private suspend fun handleBotSummaryQuestion(question : String) : String? {
        try {
            val chat = generativeModel.startChat(
                history = listOf(summaryPrompt) + messageCache
            )
            val response = chat.sendMessage(question)
            val replyText = response.text?.trim().orEmpty()
            return replyText
        } catch (e : Exception) {
            Logger.e("[ai.error] ${e.message}")
        }
        return null
    }
}
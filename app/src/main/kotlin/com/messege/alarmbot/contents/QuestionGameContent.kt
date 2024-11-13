package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.arrayOfPlaces
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.QuestionGameText
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.core.common.questionGameEnd
import com.messege.alarmbot.core.common.questionGameRule
import com.messege.alarmbot.domain.model.Command
import com.messege.alarmbot.domain.model.MainChatTextResponse
import com.messege.alarmbot.domain.model.UserTextResponse
import kotlinx.coroutines.channels.Channel

data class UserKey(val name: String, val key: String)
data class GameState(
    val isStart: Boolean = false,
    var step: Int = 0,
    var answer: String = "",
    val hostKey: UserKey? = null
)

class QuestionGameContent(override val commandChannel: Channel<Command>) : BaseContent {
    override val contentsName: String = "다섯고개"
    private var gameState: GameState = GameState()
    private val totalWords: MutableList<String> = mutableListOf()
    private val notSelectWords: MutableList<String> = mutableListOf()
    private val userWords: MutableList<Pair<UserKey, String>> = mutableListOf()

    override suspend fun request(
        postTime: Long,
        chatRoomKey: ChatRoomKey,
        user: Person,
        text: String
    ) {
        if (chatRoomKey == TARGET_KEY) {
            when (text) {
                "$hostKeyword${contentsName} $questionGameRule",
                "$hostKeyword${contentsName}${questionGameRule}" -> {
                    commandChannel.send(MainChatTextResponse(text = QuestionGameText.GAME_RULE))
                }

                hostKeyword + contentsName -> {
                    if (!gameState.isStart) {
                        startGame(host = user)
                        commandChannel.send(
                            MainChatTextResponse(
                                text = QuestionGameText.hostStartGame(
                                    hostName = "${user.name}",
                                    totalWords = totalWords
                                )
                            )
                        )

                    } else {
                        commandChannel.send(
                            MainChatTextResponse(
                                text = QuestionGameText.alreadyGameProgress(
                                    userWords = userWords,
                                    notSelectWords = notSelectWords
                                )
                            )
                        )
                    }
                }

                "$hostKeyword${contentsName} $questionGameEnd",
                "$hostKeyword${contentsName}${questionGameEnd}" -> {
                    if (gameState.isStart) {
                        clearGame()
                        commandChannel.send(MainChatTextResponse(text = QuestionGameText.GAME_END))
                    } else {
                        commandChannel.send(MainChatTextResponse(text = QuestionGameText.GAME_NOT_START))
                    }
                }


                else -> {
                    if (gameState.isStart && user.key != gameState.hostKey?.key) {
                        if (gameState.answer != text) {
                            val isAlreadyAnswer = userWords.firstOrNull { it.second == text } != null
                            if (isAlreadyAnswer) {
                                commandChannel.send(
                                    MainChatTextResponse(
                                        text = QuestionGameText.userSelectAlreadyWord(
                                            userName = "${user.name}",
                                            text = text,
                                            userWords = userWords,
                                            notSelectWords = notSelectWords
                                        )
                                    )
                                )
                            } else {
                                gameState.step++
                                userWords.add(UserKey(name = "${user.name}", key = "${user.key}") to text)

                                if (gameState.step > 4) {
                                    commandChannel.send(
                                        MainChatTextResponse(
                                            text = QuestionGameText.userSelectNotAnswerAndHostWin(
                                                userName = "${user.name}",
                                                text = text,
                                                hostName = gameState.hostKey?.name ?: "",
                                                answer = gameState.answer,
                                                userWords = userWords,
                                                notSelectWords = notSelectWords
                                            )
                                        )
                                    )
                                    clearGame()
                                } else {
                                    commandChannel.send(
                                        MainChatTextResponse(
                                            text = QuestionGameText.userSelectNotAnswer(
                                                userName = "${user.name}",
                                                text = text,
                                                userWords = userWords,
                                                notSelectWords = notSelectWords
                                            )
                                        )
                                    )
                                }
                            }
                        } else {
                            commandChannel.send(
                                MainChatTextResponse(
                                    text = QuestionGameText.userSelectAnswer(
                                        userName = "${user.name}",
                                        answer = text,
                                        userWords = userWords,
                                        notSelectWords = notSelectWords
                                    )
                                )
                            )
                            clearGame()
                        }
                    }
                }
            }
        } else {
            if (gameState.isStart && gameState.answer.isBlank() && "${gameState.hostKey?.key}" == user.key) {
                if (totalWords.contains(text)) {
                    gameState.answer = text
                    commandChannel.send(
                        MainChatTextResponse(
                            text = QuestionGameText.hostSelectCompleteAnswer(
                                "${gameState.hostKey?.name}"
                            )
                        )
                    )
                } else {
                    commandChannel.send(
                        UserTextResponse(
                            userKey = ChatRoomKey(
                                false,
                                "${user.name}",
                                "${user.key}"
                            ), text = QuestionGameText.NOT_ANSWER
                        )
                    )
                }
            }
        }
    }

    private fun startGame(host: Person) {
        gameState = GameState(
            isStart = true,
            hostKey = UserKey(name = host.name.toString(), key = host.key.toString())
        )
        totalWords.clear()
        userWords.clear()
        totalWords.addAll(arrayOfPlaces.toList().shuffled().take(10))
    }

    private fun clearGame() {
        gameState = GameState()
        totalWords.clear()
        userWords.clear()
    }
}
package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.arrayOfPlaces
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.QuestionGameText
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.core.common.questionGameEnd
import com.messege.alarmbot.core.common.questionGameRule
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
                                    hostName = gameState.hostKey!!.name,
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
                    val isExistWord = totalWords.firstOrNull { it == text } != null
                    /**
                     * * 게임 진행중 / 단어 선택됨 / 호스트 아님 / 단어 후보에 있음
                     */
                    if (gameState.isStart && gameState.answer.isNotBlank() && "${user.key}" != gameState.hostKey!!.key && isExistWord) {
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
                            /**
                             * * 최초 선택 단어
                             *
                             * - 진행 단계 증가
                             * - 유저 단어에 추가
                             * - 단어 후보 에서 제거
                             */
                            gameState.step++
                            userWords.add(
                                UserKey(
                                    name = "${user.name}",
                                    key = "${user.key}"
                                ) to text
                            )
                            notSelectWords.remove(text)

                            if (gameState.answer == text) {
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
                            } else {
                                if (gameState.step > 4) {
                                    commandChannel.send(
                                        MainChatTextResponse(
                                            text = QuestionGameText.userSelectNotAnswerAndHostWin(
                                                userName = "${user.name}",
                                                text = text,
                                                hostName = gameState.hostKey!!.name,
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
                        }
                    }
                }
            }
        } else {
            /**
             * * 게임 진행중 / 정답 선택중 / 호스트
             */
            if (gameState.isStart && gameState.answer.isBlank() && "${gameState.hostKey?.key}" == user.key) {
                if (totalWords.contains(text)) {
                    gameState.answer = text
                    commandChannel.send(
                        MainChatTextResponse(
                            text = QuestionGameText.hostSelectCompleteAnswer(
                                hostName = gameState.hostKey!!.name
                            )
                        )
                    )
                } else {
                    commandChannel.send(
                        UserTextResponse(
                            userKey = chatRoomKey, text = QuestionGameText.NOT_ANSWER
                        )
                    )
                }
            }
        }
    }

    private fun startGame(host: Person) {
        gameState = GameState(
            isStart = true,
            hostKey = UserKey(name = "${host.name}", key = "${host.key}")
        )
        totalWords.clear()
        notSelectWords.clear()
        userWords.clear()

        totalWords.addAll(arrayOfPlaces.toList().shuffled().take(10))
        notSelectWords.addAll(totalWords)
    }

    private fun clearGame() {
        gameState = GameState()
        totalWords.clear()
        notSelectWords.clear()
        userWords.clear()
    }
}
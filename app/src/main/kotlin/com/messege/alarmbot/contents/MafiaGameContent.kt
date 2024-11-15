package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.MafiaText
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.questionGameEnd
import com.messege.alarmbot.core.common.questionGameRule
import kotlinx.coroutines.channels.Channel

/**
 * * 단계
 * - 인원 참여
 * - 게임 진행
 * -- 1 대화 : 3분
 * -- 2 투표 : 대화 이후 누구를 처형할지 투표
 * -- 3 암살 : 마피아가 누구를 죽일지 투표
 * -- 4 수사 : 경찰이 한명의 직업을 확인
 *
 * 세력
 * 1. 시민 : 마피아를 제거하여 승리
 * 1.1 경찰 : 수사 시간에 직업 확인
 * 2. 마피아 : 마피아가 시민과 같거나 많으면 승리
 * 3. 바보 : 투표로 지목당해 죽으면 승리
 *
 * 인원 배합
 * - 4명 : 마피아 1, 시민 3
 * - 5명 : 마피아 1, 바보 1, 시민 3
 * - 6명 : 마피아 2, 시민 3, 경찰 1
 * - 7명 : 마피아 2, 바보 1, 시민 3, 경찰 1
 * - 8명 : 마피아 3, 시민 4, 경찰 1
 */

enum class Job {
    Citizen, Police, Mafia, Fool
}

data class MafiaGameState(
    val isStart: Boolean = false,
    var step: Int = 0,
    val hostKey: UserKey? = null
)

class MafiaGameContent(
    override val commandChannel: Channel<Command>
) : BaseContent {
    override val contentsName: String = "마피아"
    private var gameState: MafiaGameState = MafiaGameState()

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        if(chatRoomKey == TARGET_KEY){
            when(text){
                "$hostKeyword${contentsName} $questionGameRule",
                "$hostKeyword${contentsName}$questionGameRule" -> {
                    commandChannel.send(MainChatTextResponse(text = MafiaText.GAME_RULE))
                }

                hostKeyword + contentsName -> {
                    if (!gameState.isStart) {
                        startGame(host = user)
                        commandChannel.send(
                            MainChatTextResponse(
                                text = MafiaText.hostStartGame(
                                    hostName = gameState.hostKey!!.name
                                )
                            )
                        )

                    } else {
                        commandChannel.send(
                            MainChatTextResponse(
                                text = MafiaText.GAME_ALREADY_START
                            )
                        )
                    }
                }

                "$hostKeyword${contentsName} $questionGameEnd",
                "$hostKeyword${contentsName}$questionGameEnd" -> {
                    if (gameState.isStart) {
                        clearGame()
                        commandChannel.send(MainChatTextResponse(text = MafiaText.GAME_END))
                    } else {
                        commandChannel.send(MainChatTextResponse(text = MafiaText.GAME_NOT_START))
                    }
                }
                else -> {
                }
            }
        }
    }

    private fun startGame(host: Person) {
        gameState = MafiaGameState(
            isStart = true,
            hostKey = UserKey(name = "${host.name}", key = "${host.key}")
        )
    }

    private fun clearGame() {
        gameState = MafiaGameState()
    }
}
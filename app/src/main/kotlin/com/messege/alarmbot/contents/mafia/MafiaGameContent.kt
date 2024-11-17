package com.messege.alarmbot.contents.mafia

import android.app.Person
import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.Command
import com.messege.alarmbot.contents.MainChatTextResponse
import com.messege.alarmbot.contents.Timer
import com.messege.alarmbot.contents.UserTextResponse
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.MafiaText
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.core.common.gameRemainingTime
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.participateGame
import com.messege.alarmbot.core.common.questionGameRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class MafiaPlayMetaData(
    val hostName: String = "",
    val hostKey: String = "",
    val allPlayers : MutableList<Player> = mutableListOf(),
    val isStart: Boolean = false,
    var playStep : Int = 0
)

class MafiaGameContent(
    override val commandChannel: Channel<Command>,
    scope : CoroutineScope
) : BaseContent {
    override val contentsName: String = "마피아"
    private var metaData: MafiaPlayMetaData = MafiaPlayMetaData()

    private val timer = Timer()
    private val _stateFlow = MutableStateFlow<MafiaGameState>(MafiaGameState.End)

    init {
        scope.launch {
            _stateFlow.collect { state ->
                when(state){
                    is MafiaGameState.Play.Wait -> {
                        timer.start(state.time) {
                            if(state.players.size < 4){
                                MainChatTextResponse(text = MafiaText.gameEndWaitTimeOut(state.players.size))
                                endGame()
                            }else{
                                MainChatTextResponse(text = MafiaText.gameWaitTimeOutGoToCheck(state.players))
                                _stateFlow.value = MafiaGameState.Play.Check(players = state.players)
                            }
                        }

                        commandChannel.send(
                            MainChatTextResponse(
                                text = MafiaText.hostStartGame(
                                    hostName = metaData.hostName
                                )
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        val gameState: MafiaGameState = _stateFlow.value

        /**
         * 게임 규칙
         */
        if(text == "$hostKeyword${contentsName}$questionGameRule" && chatRoomKey == TARGET_KEY) {
            commandChannel.send(MainChatTextResponse(text = MafiaText.GAME_RULE))
            return
        }

        /**
         * 게임 시작
         */
        if(text == hostKeyword + contentsName && chatRoomKey != TARGET_KEY) {
            if(gameState is MafiaGameState.Play){
                commandChannel.send(
                    UserTextResponse(
                        userKey = chatRoomKey,
                        text = MafiaText.GAME_ALREADY_START
                    )
                )
            }else{
                startGame(host = user)
            }
            return
        }

        /**
         * 참여 마감
         */
        if(text == participateGame && chatRoomKey != TARGET_KEY && gameState !is MafiaGameState.Play.Wait){
            commandChannel.send(
                UserTextResponse(
                    userKey = chatRoomKey,
                    text = MafiaText.GAME_WAIT_END
                )
            )
            return
        }

        /**
         * 남은 시간
         */
        if(text == "$hostKeyword${contentsName}$gameRemainingTime" && chatRoomKey == TARGET_KEY && gameState is MafiaGameState.Play){
            val current = timer.getRemainingTime()
            if (gameState.time != 0 && current != 0) {
                commandChannel.send(
                    MainChatTextResponse(
                        text = MafiaText.gameRemainingTime(
                            state = gameState.javaClass.simpleName, total = gameState.time, remain = current
                        )
                    )
                )
            }
            return
        }

        /**
         * 마피아 대화
         */
        if(chatRoomKey != TARGET_KEY && gameState is MafiaGameState.Play.Progress){
            val userPlayer = gameState.survivors.first { user.name == it.name }

            if(userPlayer is Player.Assign.Mafia){
                gameState.survivors.filterIsInstance<Player.Assign.Mafia>().forEach { mafia ->
                    commandChannel.send(
                        UserTextResponse(
                            userKey = ChatRoomKey(isGroupConversation = false, mafia.name, mafia.name),
                            text = MafiaText.mafiaMessage("${user.name}", text)
                        )
                    )
                }
            }
        }

        when(gameState){
            is MafiaGameState.Play.Wait -> {
                if(chatRoomKey != TARGET_KEY){
                    if(text == participateGame){
                        gameState.players.let { players ->
                            val isAlreadyUser = players.firstOrNull { it.name == user.name } != null

                            if(isAlreadyUser){
                                commandChannel.send(
                                    UserTextResponse(
                                        userKey = chatRoomKey,
                                        text = MafiaText.GAME_ALREADY_USER
                                    )
                                )
                            }else{
                                if(players.size < 8){
                                    players.add(Player.None(name = "${user.name}"))
                                    commandChannel.send(
                                        MainChatTextResponse(
                                            text = MafiaText.userInviteGame(
                                                userName = "${user.name}",
                                                players = players
                                            )
                                        )
                                    )
                                    if(players.size == 8){
                                        _stateFlow.value = MafiaGameState.Play.Check(players = players)
                                    }

                                }else {
                                    commandChannel.send(
                                        UserTextResponse(
                                            userKey = chatRoomKey,
                                            text = MafiaText.GAME_WAIT_END
                                        )
                                    )
                                }
                            }
                        }

                    }
                }else{

                }

            }
            else -> {}
        }
    }

    private fun startGame(host: Person) {
        metaData = MafiaPlayMetaData(
            isStart = true,
            hostName = "${host.name}",
            hostKey = "${host.key}"
        )
        _stateFlow.value = MafiaGameState.Play.Wait()
    }

    private fun endGame() {
        metaData = MafiaPlayMetaData()
        _stateFlow.value = MafiaGameState.End
    }
}
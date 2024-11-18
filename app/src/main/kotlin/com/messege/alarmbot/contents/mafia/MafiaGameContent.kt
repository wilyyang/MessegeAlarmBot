package com.messege.alarmbot.contents.mafia

import android.app.Person
import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.Command
import com.messege.alarmbot.contents.MainChatTextResponse
import com.messege.alarmbot.contents.TimeWork
import com.messege.alarmbot.contents.Timer
import com.messege.alarmbot.contents.UserTextResponse
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.MafiaText
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_CITIZEN
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_FOOL
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_MAFIA
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_POLICE
import com.messege.alarmbot.core.common.MafiaText.GAME_ASSIGN_JOB
import com.messege.alarmbot.core.common.MafiaText.GAME_NOT_START_MORE_PLAYER
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.core.common.checkGame
import com.messege.alarmbot.core.common.gameEndText
import com.messege.alarmbot.core.common.gameRemainingTime
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.participateGame
import com.messege.alarmbot.core.common.questionGameEnd
import com.messege.alarmbot.core.common.questionGameRule
import com.messege.alarmbot.util.log.Logger
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

    private val timer = Timer(scope)
    private val _stateFlow = MutableStateFlow<MafiaGameState>(MafiaGameState.End())

    private val _timerFlow = MutableStateFlow<TimeWork>(TimeWork(0) {})

    init {
        scope.launch {
            _timerFlow.collect { timeWork ->
                timer.start(timeWork)
            }
        }

        scope.launch {

            _stateFlow.collect { state ->
                if(state is MafiaGameState.Play){
                    Logger.e("[mafia.state] : ${state.korName} / ${state.time} s")

                    when(state){
                        is MafiaGameState.Play.Wait -> {
                            state.players.add(Player.None(key = metaData.hostKey, name = metaData.hostName))
                            _timerFlow.value = TimeWork(state.time){
                                if(state.players.size < 4){
                                    MainChatTextResponse(text = MafiaText.gameEndWaitTimeOut(state.players.size))
                                    _stateFlow.value = MafiaGameState.End()
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

                        is MafiaGameState.Play.Check -> {
                            _timerFlow.value = TimeWork(state.time){
                                val checkedPlayers = state.players.filter { it.isCheck }
                                if(checkedPlayers.size < 4){
                                    MainChatTextResponse(text = MafiaText.gameEndCheckTimeOut(state.players.size))
                                    _stateFlow.value = MafiaGameState.End()
                                }else{
                                    MainChatTextResponse(text = MafiaText.gameCheckTimeOutGoToAssign(state.players))
                                    _stateFlow.value = MafiaGameState.Play.AssignJob(players = state.players)
                                }
                            }

                            commandChannel.send(
                                MainChatTextResponse(
                                    text = MafiaText.checkPlayer(
                                        hostName = metaData.hostName,
                                        players = state.players
                                    )
                                )
                            )
                        }

                        is MafiaGameState.Play.AssignJob -> {
                            _timerFlow.value = TimeWork(state.time){
                                _stateFlow.value = MafiaGameState.Play.Progress.CitizenTime.Talk(survivors = state.assignedPlayers)
                            }

                            commandChannel.send(MainChatTextResponse(text = GAME_ASSIGN_JOB))
                            state.assignJob()

                            state.assignedPlayers.forEach { player ->
                                commandChannel.send(
                                    UserTextResponse(
                                        userKey = ChatRoomKey(
                                            isGroupConversation = false,
                                            player.name,
                                            player.name
                                        ),
                                        text = when(player){
                                            is Player.Assign.Citizen -> ASSIGN_JOB_CITIZEN
                                            is Player.Assign.Police -> ASSIGN_JOB_POLICE
                                            is Player.Assign.Mafia -> ASSIGN_JOB_MAFIA
                                            is Player.Assign.Fool -> ASSIGN_JOB_FOOL
                                        }
                                    )
                                )
                            }
                        }
                        is MafiaGameState.Play.Progress -> {
                            progressStateHandle(state)
                        }
                    }
                }else{
                    Logger.e("[mafia.state] : ${state.korName}")
                    endGame()
                }
            }
        }
    }

    override suspend fun request(postTime : Long, chatRoomKey: ChatRoomKey, user : Person, text : String) {
        val localState = _stateFlow.value
        /**
         * 게임 규칙
         */
        if(chatRoomKey == TARGET_KEY && text == "$hostKeyword${contentsName}$questionGameRule") {
            commandChannel.send(MainChatTextResponse(text = MafiaText.GAME_RULE))
            return
        }

        /**
         * 게임 시작
         */
        if(chatRoomKey == TARGET_KEY && text == hostKeyword + contentsName) {
            if(localState is MafiaGameState.Play){
                commandChannel.send(MainChatTextResponse(MafiaText.GAME_ALREADY_START))
            }else{
                startGame(host = user)
            }
            return
        }

        /**
         * 남은 시간
         */
        if(chatRoomKey == TARGET_KEY && text == "$hostKeyword${contentsName}$gameRemainingTime" && localState is MafiaGameState.Play){
            val currentSeconds = timer.getRemainingTime()
            if (localState.time != 0 && currentSeconds != 0) {
                commandChannel.send(
                    MainChatTextResponse(
                        text = MafiaText.gameRemainingTime(
                            state = localState.korName, total = localState.time, remain = currentSeconds
                        )
                    )
                )
            }
            return
        }

        /**
         * 참여 마감
         */
        if(chatRoomKey == TARGET_KEY && text == participateGame && localState !is MafiaGameState.Play.Wait){
            commandChannel.send(
                MainChatTextResponse(MafiaText.GAME_WAIT_END)
            )
            return
        }

        /**
         * 마피아 대화 : return 없음!
         */
        if(chatRoomKey != TARGET_KEY && localState is MafiaGameState.Play.Progress){
            val mafias = localState.survivors.filterIsInstance<Player.Assign.Mafia>()
            val userMafia = mafias.firstOrNull { it.name == "${user.name}" }
            if(userMafia != null){
                mafias.filter { it.name != userMafia.name }.forEach { mafia ->
                    commandChannel.send(
                        UserTextResponse(
                            userKey = ChatRoomKey(isGroupConversation = false, mafia.name, mafia.name),
                            text = MafiaText.mafiaMessage(userMafia.name, text)
                        )
                    )
                }
            }
        }

        /**
         * 게임 종료
         */
        if(chatRoomKey == TARGET_KEY && text == "$hostKeyword${contentsName}$gameEndText" && "${user.key}" == metaData.hostKey) {
            if(localState is MafiaGameState.Play){
                commandChannel.send(MainChatTextResponse(MafiaText.GAME_END_COMMAND))
                _stateFlow.value = MafiaGameState.End()
            }
            return
        }

        /**
         * 게임 상태에 따른 대화
         */
        when(localState){
            is MafiaGameState.Play.Wait -> {
                if(chatRoomKey == TARGET_KEY){
                    if(text == participateGame){
                        localState.players.let { players ->
                            val isNewUser = players.firstOrNull { it.name == "${user.name}" } == null
                            if(isNewUser && players.size < 8){
                                players.add(Player.None(key = "${user.key}", name = "${user.name}"))
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
                            }
                        }
                    }else if(text == "$hostKeyword$participateGame$gameEndText" && "${user.key}" == metaData.hostKey){
                        if(localState.players.size < 4){
                            commandChannel.send(MainChatTextResponse(GAME_NOT_START_MORE_PLAYER))
                        }else{
                            _stateFlow.value = MafiaGameState.Play.Check(players = localState.players)
                        }
                    }
                }
            }

            is MafiaGameState.Play.Check -> {
                if(chatRoomKey != TARGET_KEY){
                    if(text == checkGame){
                        localState.players.let { players ->
                            players.firstOrNull { it.name == "${user.name}" }?.let { player ->
                                if(!player.isCheck){
                                    player.isCheck = true

                                    commandChannel.send(MainChatTextResponse(MafiaText.userCheckGame(player.name)))

                                    val checkedSize = players.count { it.isCheck }
                                    if(checkedSize == players.size){
                                        _stateFlow.value = MafiaGameState.Play.AssignJob(players = localState.players)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is MafiaGameState.Play.Progress -> {
                progressStateMessage(state = localState, userName = "${user.name}", text = text)
            }

            else -> {}
        }
    }


    private fun progressStateHandle(state : MafiaGameState.Play.Progress){
        when(state){
            else -> {}
        }
    }

    private fun progressStateMessage(state : MafiaGameState.Play.Progress, userName : String, text : String){
        when(state){
            else -> {}
        }
    }

    private fun startGame(host: Person) {
        timer.stop()
        metaData = MafiaPlayMetaData(
            isStart = true,
            hostName = "${host.name}",
            hostKey = "${host.key}"
        )
        _stateFlow.value = MafiaGameState.Play.Wait()
    }

    private fun endGame() {
        timer.stop()
        metaData = MafiaPlayMetaData()
    }
}
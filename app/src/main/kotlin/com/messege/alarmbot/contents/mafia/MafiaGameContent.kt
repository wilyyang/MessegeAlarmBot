package com.messege.alarmbot.contents.mafia

import android.app.Person
import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.Command
import com.messege.alarmbot.contents.MainChatTextResponse
import com.messege.alarmbot.contents.TimeWork
import com.messege.alarmbot.contents.Timer
import com.messege.alarmbot.contents.UserTextResponse
import com.messege.alarmbot.core.common.*
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_CITIZEN
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_FOOL
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_MAFIA
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_POLICE
import com.messege.alarmbot.core.common.MafiaText.GAME_ASSIGN_JOB
import com.messege.alarmbot.core.common.MafiaText.GAME_NOT_START_MORE_PLAYER
import com.messege.alarmbot.core.common.MafiaText.KILL_RESULT_NOT
import com.messege.alarmbot.core.common.MafiaText.VOTE_RESULT_NOT
import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class MafiaPlayMetaData(
    val hostName: String = "",
    val hostKey: String = "",
    val allPlayers : MutableList<Player> = mutableListOf(),
    val isStart: Boolean = false,
    var playStep : Int = 0,
    val mission: String = ""
)

fun Player.isUser(user : Person, roomKey : ChatRoomKey) : Boolean {
    return this.name == "${user.name}" || this.name == roomKey.roomName
}

class MafiaGameContent(
    override val commandChannel: Channel<Command>,
    private val scope : CoroutineScope
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
                                    commandChannel.send(MainChatTextResponse(text = MafiaText.gameEndWaitTimeOut(state.players.size)))
                                    _stateFlow.value = MafiaGameState.End()
                                }else{
                                    commandChannel.send(MainChatTextResponse(text = MafiaText.gameWaitTimeOutGoToCheck(state.players)))
                                    _stateFlow.value = state.toCheck()
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
                                    commandChannel.send(MainChatTextResponse(text = MafiaText.gameEndCheckTimeOut(state.players.size)))
                                    _stateFlow.value = MafiaGameState.End()
                                }else{
                                    commandChannel.send(MainChatTextResponse(text = MafiaText.gameCheckTimeOutGoToAssign(state.players)))
                                    _stateFlow.value = state.toAssignJob()
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
                                _stateFlow.value = state.toTalk()
                            }

                            commandChannel.send(MainChatTextResponse(text = GAME_ASSIGN_JOB))
                            state.assignJob()

                            state.assignedPlayers.forEach { player ->
                                metaData.allPlayers.add(player)
                                delay(2000)
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
                                            is Player.Assign.Mafia -> ASSIGN_JOB_MAFIA + "\n- 미션을 꼭 수행 해주세요!\n- 미션 : ${metaData.mission}"
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
            val userMafia = mafias.firstOrNull { it.isUser(user,chatRoomKey) }
            if(userMafia != null){
                mafias.filter { it.name != userMafia.name }.forEach { mafia ->
                    delay(1000)
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
                                    _stateFlow.value = localState.toCheck()
                                }
                            }
                        }
                    }else if(text == "$hostKeyword$participateGame$gameEndText" && "${user.key}" == metaData.hostKey){
                        if(localState.players.size < 4){
                            commandChannel.send(MainChatTextResponse(GAME_NOT_START_MORE_PLAYER))
                        }else{
                            _stateFlow.value = localState.toCheck()
                        }
                    }
                }
            }

            is MafiaGameState.Play.Check -> {
                if(chatRoomKey != TARGET_KEY){
                    if(text == checkGame){
                        localState.players.let { players ->
                            players.firstOrNull { it.isUser(user, chatRoomKey) }?.let { player ->
                                if(!player.isCheck){
                                    player.isCheck = true

                                    commandChannel.send(MainChatTextResponse(MafiaText.userCheckGame(player.name)))

                                    val checkedSize = players.count { it.isCheck }
                                    if(checkedSize == players.size){
                                        _stateFlow.value = localState.toAssignJob()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is MafiaGameState.Play.Progress -> {
                progressStateMessage(isMainChat = chatRoomKey == TARGET_KEY, state = localState, userName = "${user.name}", text = text)
            }

            else -> {}
        }
    }


    private suspend fun progressStateHandle(state : MafiaGameState.Play.Progress){
        when(state){
            is MafiaGameState.Play.Progress.CitizenTime.Talk -> {
                delay(1000)
                _timerFlow.value = TimeWork(state.time){
                    _stateFlow.value = state.toVote()
                }
                commandChannel.send(MainChatTextResponse(text = MafiaText.gameStateTalk(state.time)))
            }

            is MafiaGameState.Play.Progress.CitizenTime.Vote -> {
                delay(1000)
                _timerFlow.value = TimeWork(state.time){
                    _stateFlow.value = state.toVoteComplete()
                }
                commandChannel.send(MainChatTextResponse(text = MafiaText.gameStateVote(state.time)))
            }

            is MafiaGameState.Play.Progress.CitizenTime.VoteComplete -> {
                delay(1000)
                _timerFlow.value = TimeWork(state.time){}

                state.votedCount.let { counts ->
                    val vote = if(counts.isEmpty()){
                        null
                    }else if(counts.size == 1){
                        counts[0]
                    }else{
                        if(counts[0] == counts[1]){
                            null
                        }else{
                            counts[0]
                        }
                    }

                    if(vote == null){
                        commandChannel.send(MainChatTextResponse(VOTE_RESULT_NOT))
                        _stateFlow.value = state.toKill()
                    }else{
                        val votedMan = state.survivors.firstOrNull{it.name == vote.first}
                        if(votedMan == null){
                            commandChannel.send(MainChatTextResponse(VOTE_RESULT_NOT))
                            _stateFlow.value = state.toKill()
                        }else{
                            votedMan.isSurvive = false
                            state.survivors.removeIf { it.name == votedMan.name }
                            commandChannel.send(MainChatTextResponse(MafiaText.voteKillUser(vote.first, vote.second)))
                            _stateFlow.value = state.toDetermine(votedMan)
                        }
                    }
                }
            }
            is MafiaGameState.Play.Progress.CitizenTime.Determine -> {
                delay(1000)
                _timerFlow.value = TimeWork(state.time) {}

                when(state.votedMan){
                    is Player.Assign.Fool -> {
                        commandChannel.send(MainChatTextResponse(MafiaText.winFool(state.votedMan.name, metaData.allPlayers)))
                        _stateFlow.value = MafiaGameState.End()
                    }

                    is Player.Assign.Citizen,
                    is Player.Assign.Police -> {
                        val mafiaCount = state.survivors.count { it is Player.Assign.Mafia }
                        val citizenCount = state.survivors.count { it !is Player.Assign.Mafia }

                        if(mafiaCount == citizenCount){
                            commandChannel.send(MainChatTextResponse(MafiaText.winMafia(state.votedMan.name, metaData.allPlayers)))
                            _stateFlow.value = MafiaGameState.End()
                        }else{
                            commandChannel.send(MainChatTextResponse(MafiaText.citizenVoted(state.votedMan.name, metaData.allPlayers)))
                            _stateFlow.value = state.toKill()
                        }
                    }

                    is Player.Assign.Mafia -> {
                        val mafiaCount = state.survivors.count { it is Player.Assign.Mafia }
                        if(mafiaCount == 0){
                            commandChannel.send(MainChatTextResponse(MafiaText.winCitizen(state.votedMan.name, metaData.allPlayers)))
                            _stateFlow.value = MafiaGameState.End()
                        }else{
                            commandChannel.send(MainChatTextResponse(MafiaText.mafiaVoted(state.votedMan.name, metaData.allPlayers)))
                            _stateFlow.value = state.toKill()
                        }
                    }
                }
            }

            is MafiaGameState.Play.Progress.MafiaTime.Kill -> {
                delay(1000)
                _timerFlow.value = TimeWork(state.time){
                    _stateFlow.value = state.toKillComplete()
                }

                commandChannel.send(MainChatTextResponse(text = MafiaText.gameStateKill(state.time)))
            }

            is MafiaGameState.Play.Progress.MafiaTime.KillComplete -> {
                delay(1000)
                _timerFlow.value = TimeWork(state.time){}
                state.targetedCount.let { counts ->
                    val target = if(counts.isEmpty()){
                        null
                    }else if(counts.size == 1){
                        counts[0]
                    }else{
                        if(counts[0] == counts[1]){
                            null
                        }else{
                            counts[0]
                        }
                    }

                    if(target == null){
                        commandChannel.send(MainChatTextResponse(KILL_RESULT_NOT))
                        _stateFlow.value = state.toPoliceTime()
                    }else{
                        val targetedMan = state.survivors.firstOrNull{it.name == target.first}
                        if(targetedMan == null){
                            commandChannel.send(MainChatTextResponse(KILL_RESULT_NOT))
                            _stateFlow.value = state.toPoliceTime()
                        }else{
                            targetedMan.isSurvive = false
                            state.survivors.removeIf { it.name == targetedMan.name }
                            commandChannel.send(MainChatTextResponse(MafiaText.mafiaKillUser(target.first, target.second)))
                            _stateFlow.value = state.toDetermine(targetedMan)
                        }
                    }
                }
            }

            is MafiaGameState.Play.Progress.MafiaTime.Determine -> {
                delay(1000)
                _timerFlow.value = TimeWork(state.time) {}
                val mafiaCount = state.survivors.count { it is Player.Assign.Mafia }
                val citizenCount = state.survivors.count { it !is Player.Assign.Mafia }

                if(mafiaCount == citizenCount){
                    commandChannel.send(MainChatTextResponse(MafiaText.winMafia(state.targetedMan.name, metaData.allPlayers)))
                    _stateFlow.value = MafiaGameState.End()
                }else{
                    _stateFlow.value = state.toPoliceTime()
                }
            }

            is MafiaGameState.Play.Progress.PoliceTime -> {
                delay(1000)
                _timerFlow.value = TimeWork(state.time) {
                    _stateFlow.value = state.toTalk()
                }

                val police = state.survivors.firstOrNull { it is Player.Assign.Police }

                if(police != null){
                    commandChannel.send(MainChatTextResponse(MafiaText.gameStatePoliceTime(state.time, state.survivors)))
                }else{
                    _stateFlow.value = state.toTalk()
                }
            }
        }
    }

    private suspend fun progressStateMessage(isMainChat : Boolean, state : MafiaGameState.Play.Progress, userName : String, text : String){
        when(state){
            is MafiaGameState.Play.Progress.CitizenTime.Talk -> {
                if(isMainChat && text == timeSkip && userName == metaData.hostName){
                    _stateFlow.value = state.toVote()
                }
            }

            is MafiaGameState.Play.Progress.CitizenTime.Vote -> {
                val target = if(text.startsWith("@")){
                    text.substring(1).trim()
                }else{
                    text
                }

                val surviveUser = state.survivors.firstOrNull { it.name == userName && it.votedName.isBlank() }
                val isTargetSurviveAndNotUser = state.survivors.firstOrNull { it.name != userName && it.name == target} != null
                if(isMainChat && surviveUser != null && isTargetSurviveAndNotUser){
                    surviveUser.votedName = target
                    commandChannel.send(MainChatTextResponse(text = MafiaText.userVote(userName = userName, voteName = target)))

                    val voteCount = state.survivors.count { it.votedName.isNotBlank() }
                    if(voteCount == state.survivors.size){
                        _stateFlow.value = state.toVoteComplete()
                    }
                }
            }

            is MafiaGameState.Play.Progress.MafiaTime.Kill -> {
                if(!isMainChat){
                    val target = if(text.startsWith("@")){
                        text.substring(1).trim()
                    }else{
                        text
                    }

                    val mafiaUser = state.mafias.firstOrNull { it.name == userName }
                    val isTargetSurviveAndNotUser = state.survivors.firstOrNull { it !is Player.Assign.Mafia && it.name == target} != null

                    if(mafiaUser != null && mafiaUser.targetedName.isBlank() && isTargetSurviveAndNotUser){
                        mafiaUser.targetedName = target

                        val targetCount = state.mafias.count { it.targetedName.isNotBlank() }
                        if(targetCount == state.mafias.size){
                            _stateFlow.value = state.toKillComplete()
                        }
                    }
                }
            }

            is MafiaGameState.Play.Progress.PoliceTime -> {
                if(!isMainChat) {
                    val target = if (text.startsWith("@")) {
                        text.substring(1).trim()
                    } else {
                        text
                    }

                    val policeUser = state.survivors.firstOrNull { it.name == userName }

                    if(policeUser != null && policeUser is Player.Assign.Police && !policeUser.isInvestigate){

                        val targetUser = state.survivors.firstOrNull { it.name == target}

                        if(targetUser != null){
                            policeUser.isInvestigate = true
                            val job = when(targetUser){
                                is Player.Assign.Mafia -> "마피아"
                                else -> "시민"
                            }
                            commandChannel.send(
                                UserTextResponse(
                                    userKey = ChatRoomKey(isGroupConversation = false, policeUser.name, policeUser.name),
                                    text = MafiaText.policeMessage(name = target, job = job)
                                )
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private suspend fun startGame(host: Person) {
        timer.stop()
        val mission = arrayOfMafiaMissions.toList().shuffled()[0]
        metaData = MafiaPlayMetaData(
            isStart = true,
            hostName = "${host.name}",
            hostKey = "${host.key}",
            mission = mission
        )
        _stateFlow.value = MafiaGameState.Play.Wait()
    }

    private suspend fun endGame() {
        timer.stop()
        metaData = MafiaPlayMetaData()
    }
}
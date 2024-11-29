package com.messege.alarmbot.contents.mafia

import android.app.Person
import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.Command
import com.messege.alarmbot.contents.GameChatTextResponse
import com.messege.alarmbot.contents.TimeWork
import com.messege.alarmbot.contents.Timer
import com.messege.alarmbot.contents.UserTextResponse
import com.messege.alarmbot.core.common.*
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_AGENT
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_BODYGUARD
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_CITIZEN
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_DOCTOR
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_FOOL
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_MAFIA
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_MAGICIAN
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_POLICE
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_POLITICIAN
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_SHAMAN
import com.messege.alarmbot.core.common.MafiaText.ASSIGN_JOB_SOLDIER
import com.messege.alarmbot.core.common.MafiaText.GAME_ASSIGN_JOB
import com.messege.alarmbot.core.common.MafiaText.GAME_NOT_START_MORE_PLAYER
import com.messege.alarmbot.core.common.MafiaText.KILL_RESULT_NOT
import com.messege.alarmbot.core.common.MafiaText.MAGICIAN_GET_YOUR_JOB
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
    val mission: String = "",
    val missions: List<String> = listOf()
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
    private var agentUserRoomKey : ChatRoomKey? = null

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
                                    commandChannel.send(GameChatTextResponse(text = MafiaText.gameEndWaitTimeOut(state.players.size)))
                                    _stateFlow.value = MafiaGameState.End()
                                }else{
                                    commandChannel.send(GameChatTextResponse(text = MafiaText.gameWaitTimeOutGoToCheck(state.players)))
                                    _stateFlow.value = state.toCheck()
                                }
                            }
                            commandChannel.send(
                                GameChatTextResponse(
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
                                    commandChannel.send(GameChatTextResponse(text = MafiaText.gameEndCheckTimeOut(state.players.size)))
                                    _stateFlow.value = MafiaGameState.End()
                                }else{
                                    commandChannel.send(GameChatTextResponse(text = MafiaText.gameCheckTimeOutGoToAssign(state.players)))
                                    _stateFlow.value = state.toAssignJob()
                                }
                            }

                            commandChannel.send(
                                GameChatTextResponse(
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

                            commandChannel.send(GameChatTextResponse(text = GAME_ASSIGN_JOB))
                            state.assignJob()
                            state.assignedPlayers.firstOrNull{ it is Player.Assign.Agent}?.let { agent ->
                                agentUserRoomKey = ChatRoomKey(isGroupConversation = false, roomName = agent.name, roomKey = agent.name)
                            }

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
                                            is Player.Assign.Politician -> ASSIGN_JOB_POLITICIAN
                                            is Player.Assign.Agent -> ASSIGN_JOB_AGENT
                                            is Player.Assign.Soldier -> ASSIGN_JOB_SOLDIER
                                            is Player.Assign.Police -> ASSIGN_JOB_POLICE
                                            is Player.Assign.Shaman -> ASSIGN_JOB_SHAMAN
                                            is Player.Assign.Doctor -> ASSIGN_JOB_DOCTOR
                                            is Player.Assign.Bodyguard -> ASSIGN_JOB_BODYGUARD
                                            is Player.Assign.Magician -> ASSIGN_JOB_MAGICIAN
                                            is Player.Assign.Fool -> ASSIGN_JOB_FOOL
                                            is Player.Assign.Mafia -> ASSIGN_JOB_MAFIA + "\n- 미션을 꼭 수행 해주세요!\n- 미션 : ${metaData.mission}"
                                        }
                                    )
                                )
                            }
                            delay(2000)
                            commandChannel.send(GameChatTextResponse(text = "마피아 미션은 이중에 하나입니다\n" + metaData.missions.joinToString("\n")))
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
        if(chatRoomKey == GAME_KEY && text == "$hostKeyword${contentsName}$questionGameRule") {
            commandChannel.send(GameChatTextResponse(text = MafiaText.GAME_RULE))
            return
        }

        /**
         * 미션 모음
         */
        if(chatRoomKey == GAME_KEY && text == "$hostKeyword$mission") {
            commandChannel.send(GameChatTextResponse(text = arrayOfMafiaMissions.joinToString("\n")))
            return
        }

        /**
         * 게임 시작
         */
        if(chatRoomKey == GAME_KEY && text == hostKeyword + contentsName) {
            if(localState is MafiaGameState.Play){
                commandChannel.send(GameChatTextResponse(MafiaText.GAME_ALREADY_START))
            }else{
                startGame(host = user)
            }
            return
        }

        /**
         * 남은 시간
         */
        if(chatRoomKey == GAME_KEY && text == "$hostKeyword${contentsName}$gameRemainingTime" && localState is MafiaGameState.Play){
            val currentSeconds = timer.getRemainingTime()
            if (localState.time != 0 && currentSeconds != 0) {
                commandChannel.send(
                    GameChatTextResponse(
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
        if(chatRoomKey == GAME_KEY && text == participateGame && localState !is MafiaGameState.Play.Wait){
            commandChannel.send(
                GameChatTextResponse(MafiaText.GAME_WAIT_END)
            )
            return
        }

        /**
         * 마피아 대화 : return 없음!
         */
        if(!chatRoomKey.isGroupConversation && localState is MafiaGameState.Play.Progress){
            val chatUser = localState.survivors.firstOrNull{ it.isUser(user,chatRoomKey) }
            if(chatUser != null){
                when(chatUser){
                    is Player.Assign.Mafia -> {
                        val mafias = localState.survivors.filterIsInstance<Player.Assign.Mafia>()
                        mafias.filter { it.name != chatUser.name }.forEach { mafia ->
                            delay(1000)
                            commandChannel.send(
                                UserTextResponse(
                                    userKey = ChatRoomKey(isGroupConversation = false, mafia.name, mafia.name),
                                    text = MafiaText.mafiaMessage(chatUser.name, text)
                                )
                            )
                        }
                        agentUserRoomKey?.let {
                            delay(1000)
                            commandChannel.send(
                                UserTextResponse(
                                    userKey = it,
                                    text = MafiaText.mafiaMessage("???", text)
                                )
                            )
                        }
                    }
                    is Player.Assign.Doctor -> {
                        val target = if(text.startsWith("@")){
                            text.substring(1).trim()
                        }else{
                            text
                        }
                        val targetUser = localState.survivors.firstOrNull { it.name != chatUser.name && it.name == target}
                        if(targetUser != null){
                            chatUser.saveTarget = target
                            delay(1000)

                            commandChannel.send(
                                UserTextResponse(
                                    userKey = chatRoomKey,
                                    text = MafiaText.doctorMessage(chatUser.name, text)
                                )
                            )
                        }
                    }

                    is Player.Assign.Bodyguard -> {
                        val target = if(text.startsWith("@")){
                            text.substring(1).trim()
                        }else{
                            text
                        }
                        val targetUser = localState.survivors.firstOrNull { it.name != chatUser.name && it.name == target}
                        if(targetUser != null){
                            chatUser.guardTarget = target
                            delay(1000)

                            commandChannel.send(
                                UserTextResponse(
                                    userKey = chatRoomKey,
                                    text = MafiaText.bodyguardMessage(chatUser.name, text)
                                )
                            )
                        }
                    }

                    else -> {}
                }
            }
        }

        /**
         * 게임 종료
         */
        if(chatRoomKey == GAME_KEY && text == "$hostKeyword${contentsName}$gameEndText" && "${user.key}" == metaData.hostKey) {
            if(localState is MafiaGameState.Play){
                commandChannel.send(GameChatTextResponse(MafiaText.GAME_END_COMMAND))
                _stateFlow.value = MafiaGameState.End()
            }
            return
        }

        /**
         * 게임 상태에 따른 대화
         */
        when(localState){
            is MafiaGameState.Play.Wait -> {
                if(chatRoomKey == GAME_KEY){
                    if(text == participateGame){
                        localState.players.let { players ->
                            val isNewUser = players.firstOrNull { it.name == "${user.name}" } == null
                            if(isNewUser && players.size < 8){
                                players.add(Player.None(key = "${user.key}", name = "${user.name}"))
                                commandChannel.send(
                                    GameChatTextResponse(
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
                            commandChannel.send(GameChatTextResponse(GAME_NOT_START_MORE_PLAYER))
                        }else{
                            _stateFlow.value = localState.toCheck()
                        }
                    }
                }
            }

            is MafiaGameState.Play.Check -> {
                if(!chatRoomKey.isGroupConversation){
                    if(text == checkGame){
                        localState.players.let { players ->
                            players.firstOrNull { it.isUser(user, chatRoomKey) }?.let { player ->
                                if(!player.isCheck){
                                    player.isCheck = true

                                    commandChannel.send(GameChatTextResponse(MafiaText.userCheckGame(player.name)))

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
                if(chatRoomKey == GAME_KEY || !chatRoomKey.isGroupConversation){
                    progressStateMessage(isMainChat = chatRoomKey == GAME_KEY, state = localState, userName = "${user.name}", text = text)
                }
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
                commandChannel.send(GameChatTextResponse(text = MafiaText.gameStateTalk(state.time)))
            }

            is MafiaGameState.Play.Progress.CitizenTime.Vote -> {
                delay(1000)
                _timerFlow.value = TimeWork(state.time){
                    _stateFlow.value = state.toVoteComplete()
                }
                commandChannel.send(GameChatTextResponse(text = MafiaText.gameStateVote(state.time)))
            }

            is MafiaGameState.Play.Progress.CitizenTime.VoteComplete -> {
                delay(2000)
                _timerFlow.value = TimeWork(state.time){}

                state.votedCount.let { counts ->
                    val vote = if(counts.isEmpty()){
                        null
                    }else if(counts.size == 1){
                        counts[0]
                    }else{
                        if(counts[0].second == counts[1].second){
                            null
                        }else{
                            counts[0]
                        }
                    }

                    if(vote == null){
                        commandChannel.send(GameChatTextResponse(VOTE_RESULT_NOT))
                        _stateFlow.value = state.toKill()
                    }else{
                        val votedMan = state.survivors.firstOrNull{it.name == vote.first}
                        if(votedMan == null){
                            commandChannel.send(GameChatTextResponse(VOTE_RESULT_NOT))
                            _stateFlow.value = state.toKill()
                        }else{
                            _stateFlow.value = state.toDetermine(votedMan)
                        }
                    }
                }
            }
            is MafiaGameState.Play.Progress.CitizenTime.Determine -> {
                delay(2000)
                _timerFlow.value = TimeWork(state.time) {}

                val bodyguard = state.survivors.firstOrNull { it is Player.Assign.Bodyguard }
                if (bodyguard != null && bodyguard is Player.Assign.Bodyguard
                    && bodyguard.name != state.votedMan.name && bodyguard.guardTarget == state.votedMan.name)
                {
                    commandChannel.send(GameChatTextResponse(VOTE_RESULT_NOT))
                    _stateFlow.value = state.toKill()
                    return
                }

                state.votedMan.isSurvive = false
                state.survivors.removeIf { it.name == state.votedMan.name }
                commandChannel.send(GameChatTextResponse(MafiaText.voteKillUser(state.votedMan.name)))
                delay(2000)

                when(state.votedMan){
                    is Player.Assign.Fool -> {
                        commandChannel.send(GameChatTextResponse(MafiaText.winFool(state.votedMan.name, metaData.allPlayers)))
                        _stateFlow.value = MafiaGameState.End()
                    }

                    is Player.Assign.Citizen,
                    is Player.Assign.Politician,
                    is Player.Assign.Agent,
                    is Player.Assign.Doctor,
                    is Player.Assign.Bodyguard,
                    is Player.Assign.Shaman,
                    is Player.Assign.Soldier,
                    is Player.Assign.Magician,
                    is Player.Assign.Police -> {
                        val mafiaCount = state.survivors.count { it is Player.Assign.Mafia }
                        val citizenCount = state.survivors.count { it !is Player.Assign.Mafia }

                        if(mafiaCount == citizenCount){
                            commandChannel.send(GameChatTextResponse(MafiaText.winMafia(state.votedMan.name, metaData.allPlayers)))
                            _stateFlow.value = MafiaGameState.End()
                        }else{
                            _stateFlow.value = state.toKill()
                        }
                    }

                    is Player.Assign.Mafia -> {
                        val mafiaCount = state.survivors.count { it is Player.Assign.Mafia }
                        if(mafiaCount == 0){
                            commandChannel.send(GameChatTextResponse(MafiaText.winCitizen(state.votedMan.name, metaData.allPlayers)))
                            _stateFlow.value = MafiaGameState.End()
                        }else{
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

                commandChannel.send(GameChatTextResponse(text = MafiaText.gameStateKill(state.time)))
            }

            is MafiaGameState.Play.Progress.MafiaTime.KillComplete -> {
                delay(2000)
                _timerFlow.value = TimeWork(state.time){}
                state.targetedCount.let { counts ->
                    val target = if(counts.isEmpty()){
                        null
                    }else if(counts.size == 1){
                        counts[0]
                    }else{
                        if(counts[0].second == counts[1].second){
                            null
                        }else{
                            counts[0]
                        }
                    }

                    if(target == null){
                        commandChannel.send(GameChatTextResponse(KILL_RESULT_NOT))
                        _stateFlow.value = state.toInvestigateTime()
                    }else{
                        val targetedMan = state.survivors.firstOrNull{it.name == target.first}
                        if(targetedMan == null){
                            commandChannel.send(GameChatTextResponse(KILL_RESULT_NOT))
                            _stateFlow.value = state.toInvestigateTime()
                        }else{
                            _stateFlow.value = state.toDetermine(targetedMan)
                        }
                    }
                }
            }

            is MafiaGameState.Play.Progress.MafiaTime.Determine -> {
                delay(2000)
                _timerFlow.value = TimeWork(state.time) {}

                val doctor = state.survivors.firstOrNull { it is Player.Assign.Doctor }
                if(doctor != null && doctor is Player.Assign.Doctor
                    && doctor.name != state.targetedMan.name && doctor.saveTarget == state.targetedMan.name)
                {
                    commandChannel.send(GameChatTextResponse(KILL_RESULT_NOT))
                    _stateFlow.value = state.toInvestigateTime()
                    return
                }

                state.targetedMan.isSurvive = false
                state.survivors.removeIf { it.name == state.targetedMan.name }
                commandChannel.send(GameChatTextResponse(MafiaText.mafiaKillUser(state.targetedMan.name)))
                delay(2000)

                if(state.targetedMan is Player.Assign.Soldier){
                    state.mafias.shuffled().getOrNull(0)?.let { soldierTarget ->
                        soldierTarget.isSurvive = false
                        state.survivors.removeIf { it.name == soldierTarget.name }

                        commandChannel.send(
                            GameChatTextResponse(
                                MafiaText.soldierKilledMessage(
                                    name = soldierTarget.name,
                                    soldier = state.targetedMan.name
                                )
                            )
                        )
                        delay(1000)
                    }
                }

                val mafiaCount = state.survivors.count { it is Player.Assign.Mafia }
                val citizenCount = state.survivors.count { it !is Player.Assign.Mafia }

                if(mafiaCount == 0){
                    commandChannel.send(GameChatTextResponse(MafiaText.winCitizenWithSoldier(state.targetedMan.name, metaData.allPlayers)))
                    _stateFlow.value = MafiaGameState.End()
                }else if(mafiaCount == citizenCount){
                    commandChannel.send(GameChatTextResponse(MafiaText.winMafia(state.targetedMan.name, metaData.allPlayers)))
                    _stateFlow.value = MafiaGameState.End()
                }else{
                    _stateFlow.value = state.toInvestigateTime()
                }
            }

            is MafiaGameState.Play.Progress.InvestigateTime -> {
                _timerFlow.value = TimeWork(state.time) {}
                _stateFlow.value = state.toTalk()
            }
        }
    }

    private suspend fun progressStateMessage(isMainChat : Boolean, state : MafiaGameState.Play.Progress, userName : String, text : String){
        when(state){
            is MafiaGameState.Play.Progress.CitizenTime.Talk -> {
                if(isMainChat && text == timeSkip && userName == metaData.hostName){
                    _stateFlow.value = state.toVote()
                }

                if(!isMainChat){
                    val currentUser = state.survivors.firstOrNull { it.name == userName }
                    if(currentUser is Player.Assign.Magician){
                        val target = if(text.startsWith("@")){
                            text.substring(1).trim()
                        }else{
                            text
                        }

                        val targetUser = state.survivors.firstOrNull { it.name == target && currentUser.name != target}
                        metaData.allPlayers.removeIf { currentUser.name == it.name }
                        state.survivors.removeIf { currentUser.name == it.name }

                        when(targetUser){
                            is Player.Assign.Mafia -> {
                                targetUser.isSurvive = false
                                state.survivors.removeIf { targetUser.name == it.name }

                                val newJob = currentUser.toMafia()
                                metaData.allPlayers.add(newJob)
                                state.survivors.add(newJob)

                                commandChannel.send(GameChatTextResponse(MafiaText.magicianKillMafia(targetUser.name)))
                            }
                            is Player.Assign -> {
                                metaData.allPlayers.removeIf { targetUser.name == it.name }
                                state.survivors.removeIf { targetUser.name == it.name }

                                val targetInit = Player.None(targetUser.name, targetUser.key)
                                targetInit.isCheck = true
                                val targetNewJob = targetInit.toCitizen()
                                metaData.allPlayers.add(targetNewJob)
                                state.survivors.add(targetNewJob)

                                val newJob = currentUser.toCitizen(targetUser.job)
                                metaData.allPlayers.add(newJob)
                                state.survivors.add(newJob)

                                if (targetUser is Player.Assign.Agent) {
                                    agentUserRoomKey = ChatRoomKey(
                                        isGroupConversation = false,
                                        roomName = currentUser.name,
                                        roomKey = currentUser.name
                                    )
                                }

                                commandChannel.send(
                                    UserTextResponse(
                                        userKey = ChatRoomKey(isGroupConversation = false, targetUser.name, targetUser.name),
                                        text = MAGICIAN_GET_YOUR_JOB
                                    )
                                )
                            }
                            else -> {}
                        }

                        delay(1000)
                        if(targetUser is Player.Assign){
                            commandChannel.send(
                                UserTextResponse(
                                    userKey = ChatRoomKey(isGroupConversation = false, currentUser.name, currentUser.name),
                                    text = MafiaText.magicianHaveJob(targetUser.job.korName)
                                )
                            )
                        }
                    }
                }
            }

            is MafiaGameState.Play.Progress.CitizenTime.Vote -> {
                if(isMainChat && text == timeSkip && userName == metaData.hostName){
                    _stateFlow.value = state.toVoteComplete()
                    return
                }

                val target = if(text.startsWith("@")){
                    text.substring(1).trim()
                }else{
                    text
                }

                val surviveUser = state.survivors.firstOrNull { it.name == userName && it.votedName.isBlank() }
                val isTargetSurviveAndNotUser = state.survivors.firstOrNull { it.name != userName && it.name == target} != null
                if(isMainChat && surviveUser != null && isTargetSurviveAndNotUser){
                    surviveUser.votedName = target
                    commandChannel.send(GameChatTextResponse(text = MafiaText.userVote(userName = userName, voteName = target)))

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

                    val currentUser = state.survivors.firstOrNull { it.name == userName }
                    when(currentUser){
                        is Player.Assign.Mafia -> {
                            val isTargetNotMafia = state.survivors.firstOrNull { it.name == target && it !is Player.Assign.Mafia } != null

                            if(currentUser.targetedName.isBlank() && isTargetNotMafia){
                                currentUser.targetedName = target

                                commandChannel.send(
                                    UserTextResponse(
                                        userKey = ChatRoomKey(isGroupConversation = false, currentUser.name, currentUser.name),
                                        text = "${currentUser.name}님이 ${target}을 지목했습니다."
                                    )
                                )
                            }
                        }
                        is Player.Assign.Police -> {
                            if(!currentUser.isInvestigate){
                                val targetUser = state.survivors.firstOrNull { it.name == target && currentUser.name != target}

                                if(targetUser != null){
                                    currentUser.isInvestigate = true
                                    commandChannel.send(
                                        UserTextResponse(
                                            userKey = ChatRoomKey(isGroupConversation = false, currentUser.name, currentUser.name),
                                            text = MafiaText.policeMessage(name = target, isMafia = targetUser is Player.Assign.Mafia)
                                        )
                                    )
                                }
                            }
                        }
                        is Player.Assign.Shaman -> {
                            if(!currentUser.isPossess){
                                val targetUser = metaData.allPlayers.firstOrNull { it.name == target && currentUser.name != target}

                                if(targetUser != null && targetUser is Player.Assign && !targetUser.isSurvive){
                                    currentUser.isPossess = true
                                    commandChannel.send(
                                        UserTextResponse(
                                            userKey = ChatRoomKey(isGroupConversation = false, currentUser.name, currentUser.name),
                                            text = MafiaText.shamanMessage(name = target, job = targetUser.job.korName)
                                        )
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
            else -> {}
        }
    }

    private suspend fun startGame(host: Person) {
        timer.stop()
        val missions = arrayOfMafiaMissions.toList().shuffled().take(8)
        val mission = missions[0]
        metaData = MafiaPlayMetaData(
            isStart = true,
            hostName = "${host.name}",
            hostKey = "${host.key}",
            mission = mission,
            missions = missions.shuffled()
        )
        _stateFlow.value = MafiaGameState.Play.Wait()
    }

    private suspend fun endGame() {
        timer.stop()
        agentUserRoomKey = null
        commandChannel.send(GameChatTextResponse(text = "미션은 ${metaData.mission} 입니다."))
        metaData = MafiaPlayMetaData()
    }
}
package com.messege.alarmbot.contents.mafia


sealed class MafiaGameState{
    abstract val korName: String

    data class End(override val korName : String = "종료") : MafiaGameState()

    sealed class Play : MafiaGameState(){
        abstract val time: Int

        data class Wait(
            override val korName : String = "참여 대기",
            val players : MutableList<Player.None> = mutableListOf()
        ) : Play() {
            override val time = 3 * 60

            fun toCheck() = Check(players = players)
        }

        data class Check(
            override val korName : String = "참여 확인",
            val players : MutableList<Player.None>
        ) : Play() {
            override val time = players.size * 30

            fun toAssignJob() : AssignJob {
                players.removeAll { !it.isCheck }
                return AssignJob(players = players)
            }
        }

        data class AssignJob(
            override val korName : String = "직업 할당",
            val players : MutableList<Player.None>
        ) : Play() {
            override val time = players.size * 10
            val assignedPlayers : MutableList<Player.Assign> = mutableListOf()

            fun assignJob() {
                players.shuffle()
                players.getOrNull(0)?.let {
                    assignedPlayers.add(it.toMafia())
                }
                players.getOrNull(1)?.let {
                    assignedPlayers.add(it.toPolice())
                }
                players.getOrNull(2)?.let {
                    assignedPlayers.add(it.toDoctor())
                }
                players.getOrNull(3)?.let {
                    assignedPlayers.add(it.toFool())
                }
                players.getOrNull(4)?.let {
                    assignedPlayers.add(it.toBodyguard())
                }
                players.getOrNull(5)?.let {
                    assignedPlayers.add(it.toMafia())
                }
                players.getOrNull(6)?.let {
                    assignedPlayers.add(it.toCitizen())
                }
                players.getOrNull(7)?.let {
                    assignedPlayers.add(it.toMafia())
                }
            }

            fun toTalk() = Progress.CitizenTime.Talk(survivors = assignedPlayers)
        }

        sealed class Progress: Play(){
            abstract val survivors: MutableList<Player.Assign>

            sealed class CitizenTime : Progress(){
                data class Talk(
                    override val korName : String = "대화",
                    override val survivors : MutableList<Player.Assign>
                ) : CitizenTime(){
                    override val time = survivors.size * 40

                    fun toVote() = Vote(survivors = survivors)
                }

                data class Vote(
                    override val korName : String = "투표",
                    override val survivors : MutableList<Player.Assign>
                ) : CitizenTime() {
                    override val time = survivors.size * 20

                    fun toVoteComplete() : VoteComplete {
                        val votedCount = survivors
                            .filter { it.votedName.isNotBlank() }
                            .groupingBy { it.votedName }
                            .eachCount()
                            .toList()
                            .sortedByDescending { it.second }

                        return VoteComplete(votedCount = votedCount, survivors = survivors)
                    }
                }

                data class VoteComplete(
                    override val korName : String = "투표 완료",
                    val votedCount : List<Pair<String, Int>>,
                    override val survivors : MutableList<Player.Assign>
                ) : CitizenTime(){
                    override val time: Int = 0

                    fun voteResult(): Player.Assign? {
                        return when(votedCount.size){
                            0 -> null
                            1 -> survivors.first { votedCount[0].first == it.name }
                            else -> {
                                if(votedCount[0].second == votedCount[1].second){
                                    null
                                }else{
                                    survivors.first { votedCount[0].first == it.name }
                                }
                            }
                        }
                    }

                    fun toDetermine(votedMan : Player.Assign) = Determine(survivors = survivors, votedMan = votedMan)
                }

                data class Determine(
                    override val korName : String = "투표 결과",
                    override val survivors : MutableList<Player.Assign>,
                    val votedMan : Player.Assign
                ) : CitizenTime(){
                    override val time: Int = 0
                }

                fun toKill() : MafiaTime.Kill{
                    survivors.forEach {
                        it.reset()
                    }

                    return MafiaTime.Kill(survivors = survivors, mafias = survivors.filterIsInstance<Player.Assign.Mafia>())
                }
            }

            sealed class MafiaTime : Progress(){
                abstract val mafias: List<Player.Assign.Mafia>

                data class Kill(
                    override val korName : String = "암살",
                    override val survivors : MutableList<Player.Assign>,
                    override val mafias : List<Player.Assign.Mafia>
                )  : MafiaTime(){
                    override val time = mafias.size * 45

                    fun toKillComplete() : KillComplete {
                        val targetedCount = mafias
                            .filter { it.targetedName.isNotBlank() }
                            .groupingBy { it.targetedName }
                            .eachCount()
                            .toList()
                            .sortedByDescending { it.second }

                        return KillComplete(targetedCount = targetedCount, survivors = survivors, mafias = mafias)
                    }
                }

                data class KillComplete(
                    override val korName : String = "암살 완료",
                    val targetedCount : List<Pair<String, Int>>,
                    override val survivors : MutableList<Player.Assign>,
                    override val mafias : List<Player.Assign.Mafia>
                )  : MafiaTime(){
                    override val time: Int = 0

                    fun killResult(): Player.Assign? {
                        return when(targetedCount.size){
                            0 -> null
                            1 -> survivors.first { targetedCount[0].first == it.name }
                            else -> {
                                if(targetedCount[0].second == targetedCount[1].second){
                                    null
                                }else{
                                    survivors.first { targetedCount[0].first == it.name }
                                }
                            }
                        }
                    }

                    fun toDetermine(targetedMan : Player.Assign) = Determine(survivors = survivors, mafias = mafias, targetedMan = targetedMan)
                }

                data class Determine(
                    override val korName : String = "암살 결과",
                    override val survivors : MutableList<Player.Assign>,
                    override val mafias : List<Player.Assign.Mafia>,
                    val targetedMan : Player.Assign
                ) : MafiaTime(){
                    override val time: Int = 0
                }


                fun toPoliceTime() = PoliceTime(survivors = survivors)
            }

            data class PoliceTime(
                override val korName : String = "경찰 수사",
                override val survivors : MutableList<Player.Assign>
            ) : Progress() {
                override val time: Int = 120

                fun toTalk() : CitizenTime.Talk{
                    survivors.forEach {
                        it.reset()
                    }

                    return CitizenTime.Talk(survivors = survivors)
                }
            }
        }
    }
}
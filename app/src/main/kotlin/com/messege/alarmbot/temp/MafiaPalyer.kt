package com.messege.alarmbot.temp

enum class Side {
    Citizen, Mafia, Fool
}

enum class Job(val korName : String) {
    Citizen("시민"),
    Politician("정치인"),
    Agent("국정원 요원"),
    Police("경찰"),
    Shaman("영매"),
    Doctor("의사"),
    Bodyguard("보디가드"),
    Soldier("군인"),

    Magician("마술사"),
    Fool("바보"),
    Mafia("마피아")
}

sealed class Player {
    abstract val name : String
    abstract val key : String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Player) return false

        return name == other.name && key == other.key
    }

    override fun hashCode(): Int {
        return 31 * name.hashCode() + key.hashCode()
    }

    data class None(override val name : String, override var key : String) : Player() {
        var isCheck : Boolean = false

        fun toCitizen(job : Job) = when (job) {
            Job.Politician -> toPolitician()
            Job.Agent -> toAgent()
            Job.Police -> toPolice()
            Job.Shaman -> toShaman()
            Job.Doctor -> toDoctor()
            Job.Bodyguard -> toBodyguard()
            Job.Soldier -> toSoldier()
            else -> toCitizen()
        }

        fun toRandomHidden(job : Job) = when (job) {
            Job.Magician -> toMagician()
            Job.Fool -> toFool()
            else -> toFool()
        }

        fun toCitizen() = Assign.Citizen(name, key)
        fun toPolitician() = Assign.Politician(name, key)
        fun toAgent() = Assign.Agent(name, key)
        fun toPolice() = Assign.Police(name, key)
        fun toShaman() = Assign.Shaman(name, key)
        fun toMagician() = Assign.Magician(name, key)
        fun toMafia() = Assign.Mafia(name, key)
        fun toFool() = Assign.Fool(name, key)
        fun toDoctor() = Assign.Doctor(name, key)
        fun toBodyguard() = Assign.Bodyguard(name, key)
        fun toSoldier() = Assign.Soldier(name, key)
    }

    sealed class Assign(open val side : Side, open val job: Job): Player() {

        var isSurvive : Boolean = true

        var votedName : String = ""

        open fun reset() {
            votedName = ""
        }

        data class Citizen(override val name : String, override var key : String) : Assign(side = Side.Citizen, job = Job.Citizen)
        data class Politician(override val name : String, override var key : String) : Assign(side = Side.Citizen, job = Job.Politician)
        data class Agent(override val name : String, override var key : String) : Assign(side = Side.Citizen, job = Job.Agent)
        data class Soldier(override val name : String, override var key : String) : Assign(side = Side.Citizen, job = Job.Soldier)
        data class Police(override val name : String, override var key : String) : Assign(side = Side.Citizen, job = Job.Police) {
            var isInvestigate : Boolean = false

            override fun reset(){
                super.reset()
                isInvestigate = false
            }
        }
        data class Shaman(override val name : String, override var key : String) : Assign(side = Side.Citizen, job = Job.Shaman) {
            var isPossess : Boolean = false

            override fun reset(){
                super.reset()
                isPossess = false
            }
        }

        data class Doctor(override val name : String, override var key : String) : Assign(side = Side.Citizen, job = Job.Doctor) {
            var saveTarget : String = ""

            override fun reset() {
                super.reset()
                saveTarget = ""
            }
        }
        data class Bodyguard(override val name : String, override var key : String) : Assign(side = Side.Citizen, job = Job.Bodyguard) {
            var guardTarget : String = ""

            override fun reset() {
                super.reset()
                guardTarget = ""
            }
        }

        data class Magician(override val name : String, override var key : String) : Assign(side = Side.Citizen, job = Job.Magician){
            fun toMafia() : Assign {
                val newJob = None(name, key)
                newJob.isCheck = true
                return newJob.toMafia()
            }

            fun toCitizen(job : Job) : Assign {
                val newJob = None(name, key)
                newJob.isCheck = true
                return newJob.toCitizen(job)
            }
        }

        data class Fool(override val name : String, override var key : String) : Assign(side = Side.Fool, job = Job.Fool)

        data class Mafia(override val name : String, override var key : String) : Assign(side = Side.Mafia, job = Job.Mafia) {
            var targetedName : String = ""

            override fun reset() {
                super.reset()
                targetedName = ""
            }
        }
    }
}
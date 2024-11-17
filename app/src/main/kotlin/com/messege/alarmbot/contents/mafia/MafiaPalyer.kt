package com.messege.alarmbot.contents.mafia

enum class Side {
    Citizen, Mafia, Fool
}

sealed class Player {
    abstract val name : String
    open var key : String = ""

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Player) return false

        return name == other.name && key == other.key
    }

    override fun hashCode(): Int {
        return 31 * name.hashCode() + key.hashCode()
    }

    data class None(override val name : String) : Player() {
        fun toCitizen(key : String) = Assign.Citizen(name, key)
        fun toPolice(key : String) = Assign.Police(name, key)
        fun toMafia(key : String) = Assign.Mafia(name, key)
        fun toFool(key : String) = Assign.Fool(name, key)
    }

    sealed class Assign(open val side : Side): Player() {
        var isSurvive : Boolean = true

        var votedName : String = ""
        var isTalkSkip : Boolean = false

        open fun reset() {
            votedName = ""
            isTalkSkip = false
        }

        data class Citizen(override val name : String, override var key : String) : Assign(side = Side.Citizen)
        data class Police(override val name : String, override var key : String) : Assign(side = Side.Citizen) {
            var isInvestigate : Boolean = false

            override fun reset(){
                super.reset()
                isInvestigate = false
            }
        }

        data class Mafia(override val name : String, override var key : String) : Assign(side = Side.Mafia) {
            var targetedName : String = ""

            override fun reset() {
                super.reset()
                targetedName = ""
            }
        }

        data class Fool(override val name : String, override var key : String) : Assign(side = Side.Fool)
    }
}
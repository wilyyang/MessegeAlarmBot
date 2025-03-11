package com.messege.alarmbot.data.database.party.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.messege.alarmbot.core.common.PartyLogType
import com.messege.alarmbot.core.common.PartyState

@Entity(tableName = "PartyData")
@Keep
data class PartyData(
    val foundingTime: Long,
    val name: String,
    val partyPoints: Long,
    val leaderId: Long,
    val partyState: PartyState,
    val memberCount: Int,
    val description: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}


@Entity(tableName = "PartyRule")
@Keep
data class PartyRule(
    val time: Long,
    val partyId: Long,
    val memberId: Long,
    val rule: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "PartyLog")
@Keep
data class PartyLog(
    val time: Long,
    val partyId: Long,
    val memberId: Long,
    val logType: PartyLogType
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}
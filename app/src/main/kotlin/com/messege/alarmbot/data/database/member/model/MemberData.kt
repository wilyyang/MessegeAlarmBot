package com.messege.alarmbot.data.database.member.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MemberData")
@Keep
data class MemberData(
    val userId: Long,
    val createAt: Long,
    val profileType: Int,
    val latestName: String,
    val isSuperAdmin: Boolean,
    val isAdmin: Boolean,
    val chatProfileCount: Long,
    val talkCount: Long,
    val deleteTalkCount: Long,
    val enterCount: Long,
    val kickCount: Long,
    val sanctionCount: Long,
    val likes: Long,
    val dislikes: Long,
    val likesWeekly: Long,
    val dislikesWeekly: Long,
    val giftPoints: Long,
    val resetPoints: Long,
    val rank: String,
    val partyId: Long,
    val isPartyLeader: Boolean
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "NicknameData")
@Keep
data class NicknameData(
    val userId: Long,
    val changeAt: Long,
    val nickName: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "AdminLogData")
@Keep
data class AdminLogData(
    val userId: Long,
    val changeAt: Long,
    val isAdmin: Boolean
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "ChatProfileData")
@Keep
data class ChatProfileData(
    val userId: Long,
    val changeAt: Long,
    val isAvailable: Boolean
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "DeleteTalkData")
@Keep
data class DeleteTalkData(
    val userId: Long,
    val deleteAt: Long,
    val deleteText: String
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "EnterData")
@Keep
data class EnterData(
    val userId: Long,
    val enterAt: Long
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "KickData")
@Keep
data class KickData(
    val userId: Long,
    val kickAt: Long
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "SanctionData")
@Keep
data class SanctionData(
    val userId: Long,
    val eventAt: Long,
    val giverId: Long,
    val reason: String,
    val sanctionCount: Long
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "LikeData")
@Keep
data class LikeData(
    val userId: Long,
    val takeAt: Long,
    val giverId: Long,
    val point: Long
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "DislikeData")
@Keep
data class DislikeData(
    val userId: Long,
    val takeAt: Long,
    val giverId: Long,
    val point: Long
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

@Entity(tableName = "PartyChangeData")
@Keep
data class PartyChangeData(
    val userId: Long,
    val changeAt: Long,
    val partyId: Long,
    val isPartyLeader: Boolean
) {
    @PrimaryKey(autoGenerate = true) var idx: Long = 0
}

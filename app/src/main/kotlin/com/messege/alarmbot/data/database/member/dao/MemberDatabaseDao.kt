package com.messege.alarmbot.data.database.member.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.messege.alarmbot.data.database.member.model.*

@Dao
interface MemberDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberData)

    @Query("SELECT * FROM MemberData ORDER BY createAt DESC")
    suspend fun getMembersAll() : List<MemberData>

    @Query("SELECT * FROM MemberData WHERE isAdmin = true ORDER BY createAt DESC")
    suspend fun getAdminMembers() : List<MemberData>

    @Query("SELECT * FROM MemberData WHERE userId = :userId")
    suspend fun getMember(userId: Long) : List<MemberData>

    @Query("SELECT latestName FROM MemberData WHERE userId = :userId")
    suspend fun getMemberName(userId: Long) : List<String>

    @Query("UPDATE MemberData SET profileType = :profileType WHERE userId = :userId")
    suspend fun updateProfileType(userId: Long, profileType: Int)

    @Query("UPDATE MemberData SET latestName = :latestName WHERE userId = :userId")
    suspend fun updateLatestName(userId: Long, latestName: String)

    @Query("UPDATE MemberData SET isSuperAdmin = :isSuperAdmin WHERE userId = :userId")
    suspend fun updateSuperAdmin(userId: Long, isSuperAdmin: Boolean)

    @Query("UPDATE MemberData SET isAdmin = :isAdmin WHERE userId = :userId")
    suspend fun updateAdmin(userId: Long, isAdmin: Boolean)

    @Query("UPDATE MemberData SET chatProfileCount = chatProfileCount + 1 WHERE userId = :userId")
    suspend fun incrementChatProfile(userId: Long)

    @Query("UPDATE MemberData SET talkCount = talkCount + 1 WHERE userId = :userId")
    suspend fun incrementTalkCount(userId: Long)

    @Query("UPDATE MemberData SET deleteTalkCount = deleteTalkCount + 1 WHERE userId = :userId")
    suspend fun incrementDeleteTalkCount(userId: Long)

    @Query("UPDATE MemberData SET enterCount = enterCount + 1 WHERE userId = :userId")
    suspend fun incrementEnterCount(userId: Long)

    @Query("UPDATE MemberData SET kickCount = kickCount + 1 WHERE userId = :userId")
    suspend fun incrementKickCount(userId: Long)

    // Log Data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNickNameData(nickNameData: NicknameData)

    @Query("SELECT * FROM NicknameData WHERE userId = :userId ORDER BY changeAt DESC")
    suspend fun getNicknameDataAll(userId: Long) : List<NicknameData>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminLogData(adminLogData: AdminLogData)

    @Query("SELECT * FROM AdminLogData WHERE userId = :userId ORDER BY changeAt DESC")
    suspend fun getAdminLogDataAll(userId: Long) : List<AdminLogData>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatProfileData(chatProfileData: ChatProfileData)

    @Query("SELECT * FROM ChatProfileData WHERE userId = :userId ORDER BY changeAt DESC")
    suspend fun getChatProfileDataAll(userId: Long) : List<ChatProfileData>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeleteTalkData(deleteTalkData: DeleteTalkData)

    @Query("SELECT * FROM DeleteTalkData WHERE userId = :userId ORDER BY deleteAt DESC")
    suspend fun getDeleteTalkDataAll(userId: Long) : List<DeleteTalkData>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnterData(enterData: EnterData)

    @Query("SELECT * FROM EnterData WHERE userId = :userId ORDER BY enterAt DESC")
    suspend fun getEnterDataAll(userId: Long) : List<EnterData>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKickData(kickData: KickData)

    @Query("SELECT * FROM KickData WHERE userId = :userId ORDER BY kickAt DESC")
    suspend fun getKickDataAll(userId: Long) : List<KickData>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSanctionData(sanctionData: SanctionData)

    @Query("SELECT * FROM SanctionData WHERE userId = :userId ORDER BY eventAt DESC")
    suspend fun getSanctionDataAll(userId: Long) : List<SanctionData>

    @Query("UPDATE MemberData SET sanctionCount = :sanctionCount WHERE userId = :userId")
    suspend fun updateMemberSanctionCount(userId: Long, sanctionCount: Long)


    // Like, Dislike Data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikeData(likeData: LikeData)

    @Query("UPDATE MemberData SET likes = likes + :point, likesWeekly = likesWeekly + :point WHERE userId = :userId")
    suspend fun increaseLikes(userId: Long, point: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDislikeData(dislikeData: DislikeData)

    @Query("UPDATE MemberData SET dislikes = dislikes + :point, dislikesWeekly = dislikesWeekly + :point WHERE userId = :userId")
    suspend fun increaseDislikes(userId: Long, point: Long)

    @Query("UPDATE MemberData SET likes = 0, dislikes = 0")
    suspend fun resetAllMembersLikesAndDislikes()

    @Query("UPDATE MemberData SET likesWeekly = 0, dislikesWeekly = 0")
    suspend fun resetAllMembersWeeklyLikesAndDislikes()

    @Query("SELECT * FROM MemberData ORDER BY likes DESC LIMIT 10")
    suspend fun getTop10MembersByLikes(): List<MemberData>

    @Query("SELECT * FROM MemberData ORDER BY dislikes DESC LIMIT 10")
    suspend fun getTop10MembersByDislikes(): List<MemberData>

    @Query("SELECT * FROM MemberData ORDER BY likesWeekly DESC LIMIT 10")
    suspend fun getTop10MembersByLikesWeekly(): List<MemberData>

    @Query("SELECT * FROM MemberData ORDER BY dislikesWeekly DESC LIMIT 10")
    suspend fun getTop10MembersByDislikesWeekly(): List<MemberData>

    @Query("SELECT * FROM MemberData ORDER BY (likes - dislikes) DESC LIMIT 11")
    suspend fun getTop11MembersByRequirePoint(): List<MemberData>

    // Rank Data
    @Query("UPDATE MemberData SET giftPoints = resetPoints + partyResetPoints")
    suspend fun resetAllMembersGiftPoints()

    @Query("UPDATE MemberData SET giftPoints = :giftPoints WHERE userId = :userId")
    suspend fun updateMemberGiftPoints(userId: Long, giftPoints: Long)

    @Query("UPDATE MemberData SET rank = :rank, resetPoints = :resetPoints WHERE userId = :userId")
    suspend fun updateMemberRank(userId: Long, rank: String, resetPoints: Long)

    @Query("SELECT * FROM MemberData WHERE rank = :rank")
    suspend fun getMembersByRank(rank: String): List<MemberData>

    // Party Query
    @Query("SELECT * FROM MemberData WHERE partyId = :partyId AND partyState = 'PartyMember' ORDER BY joinTime ASC")
    suspend fun getPartyMembers(partyId : Long) : List<MemberData>

    @Query("SELECT * FROM MemberData WHERE partyId = :partyId AND partyState = 'Applicant' ORDER BY joinTime ASC")
    suspend fun getPartyApplicants(partyId : Long) : List<MemberData>


    @Query("UPDATE MemberData SET partyId = 0, partyState = 'None', joinTime = -1, partyResetPoints = 0 WHERE userId = :userId")
    suspend fun updatePartyStateNone(userId: Long)

    @Query("UPDATE MemberData SET partyId = :partyId, partyState = 'Applicant', joinTime = :joinTime WHERE userId = :userId")
    suspend fun updatePartyStateApplicant(userId: Long, partyId: Long, joinTime: Long)

    @Query("UPDATE MemberData SET partyId = :partyId, partyState = 'PartyMember', joinTime = :joinTime, partyResetPoints = :partyResetPoints WHERE userId = :userId")
    suspend fun updatePartyStateMember(userId: Long, partyId: Long, joinTime: Long, partyResetPoints: Long)

    @Query("UPDATE MemberData SET partyId = :partyId, partyState = 'PartyLeader', partyResetPoints = :partyResetPoints WHERE userId = :userId")
    suspend fun updatePartyStateLeader(userId: Long, partyId: Long, partyResetPoints: Long)


    @Query("UPDATE MemberData SET partyResetPoints = :partyResetPoints WHERE partyId = :partyId AND partyState = 'PartyMember'")
    suspend fun updatePartyMemberPoints(partyId: Long, partyResetPoints: Long)

    @Query("UPDATE MemberData SET partyId = 0, partyState = 'None', joinTime = -1, partyResetPoints = 0 WHERE partyId = :partyId")
    suspend fun resetPartyMemberByDissolution(partyId: Long)

    @Query("SELECT * FROM MemberData ORDER BY partyResetPoints DESC LIMIT 11")
    suspend fun getTop11MembersByPartyPoint(): List<MemberData>

    // Only Reset
    @Query("UPDATE MemberData SET likes = 0, dislikes = 0, likesWeekly = 0, dislikesWeekly = 0")
    suspend fun resetAllMembersLikesAndDislikesToZero()

    @Query("UPDATE MemberData SET rank = 'Unemployed', resetPoints = 10")
    suspend fun updateAllMemberRankDefault()

    @Query("UPDATE MemberData SET rank = 'Minister', resetPoints = 50 WHERE isAdmin = true")
    suspend fun updateMemberRankAdmin()

    @Query("UPDATE MemberData SET rank = 'President', resetPoints = 60 WHERE isSuperAdmin = true")
    suspend fun updateMemberRankSuperAdmin()
}
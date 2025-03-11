package com.messege.alarmbot.data.database.party.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.messege.alarmbot.data.database.party.model.PartyData
import com.messege.alarmbot.data.database.party.model.PartyLog
import com.messege.alarmbot.data.database.party.model.PartyRule

@Dao
interface PartyDatabaseDao {

    // PartyData
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyData)

    @Query("SELECT * FROM PartyData WHERE idx = :partyId LIMIT 1")
    suspend fun getParty(partyId : Long): PartyData?

    @Query("SELECT * FROM PartyData WHERE name = :partyName LIMIT 1")
    suspend fun getPartyByName(partyName: String): PartyData?

    @Query("SELECT * FROM PartyData ORDER BY partyPoints DESC LIMIT 10")
    suspend fun getTop10PartiesByPartyPoints(): List<PartyData>


    @Query("UPDATE PartyData SET name = :newName WHERE idx = :partyId")
    suspend fun updatePartyName(partyId: Long, newName: String)

    @Query("UPDATE PartyData SET partyPoints = :newPoints WHERE idx = :partyId")
    suspend fun updatePartyPoints(partyId: Long, newPoints: Long)

    @Query("UPDATE PartyData SET leaderId = :newLeaderId WHERE idx = :partyId")
    suspend fun updatePartyLeader(partyId: Long, newLeaderId: Long)

    @Query("UPDATE PartyData SET partyState = :newState WHERE idx = :partyId")
    suspend fun updatePartyState(partyId: Long, newState: String)

    @Query("UPDATE PartyData SET memberCount = :newMemberCount WHERE idx = :partyId")
    suspend fun updatePartyMemberCount(partyId: Long, newMemberCount: Int)

    @Query("UPDATE PartyData SET description = :newDescription WHERE idx = :partyId")
    suspend fun updatePartyDescription(partyId: Long, newDescription: String)


    // PartyRule
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartyRule(partyRule: PartyRule)

    @Query("SELECT * FROM PartyRule WHERE partyId = :partyId")
    suspend fun getPartyRules(partyId: Long): List<PartyRule>

    @Query("""
    DELETE FROM PartyRule 
    WHERE idx = (
        SELECT idx FROM PartyRule
        WHERE partyId = :partyId 
        ORDER BY time ASC 
        LIMIT 1 OFFSET :orderNumber
    )
    """)
    suspend fun deletePartyRuleOrderNumber(partyId: Long, orderNumber: Int)

    @Query("DELETE FROM PartyRule WHERE partyId = :partyId")
    suspend fun deleteAllPartyRules(partyId: Long)

    // PartyLog
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartyLog(partyLog: PartyLog)

}
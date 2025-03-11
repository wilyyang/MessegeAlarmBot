package com.messege.alarmbot.data.database.party.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.messege.alarmbot.data.database.party.model.PartyData
import com.messege.alarmbot.data.database.party.model.PartyLog
import com.messege.alarmbot.data.database.party.model.PartyRule

@Dao
interface PartyDatabaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: PartyData)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartyRule(partyRule: PartyRule)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartyLog(partyLog: PartyLog)

}
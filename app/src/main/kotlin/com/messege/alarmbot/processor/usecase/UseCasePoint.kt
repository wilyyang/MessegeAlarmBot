package com.messege.alarmbot.processor.usecase

import com.messege.alarmbot.core.common.Rank
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.member.model.MemberData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UseCasePoint(
    private val dispatcher : CoroutineDispatcher = Dispatchers.IO,
    private val memberDatabaseDao: MemberDatabaseDao
) {
    suspend fun resetAllMembersGiftPoints(){
        withContext(dispatcher) {
            memberDatabaseDao.resetAllMembersGiftPoints()
        }
    }

    suspend fun resetAllMembersWeeklyLikesAndDislikes(){
        withContext(dispatcher) {
            memberDatabaseDao.resetAllMembersWeeklyLikesAndDislikes()
        }
    }

    suspend fun getTop10MembersByLikesAndDislikes() : Pair<List<MemberData>, List<MemberData>> {
        return withContext(dispatcher) {
            val likes = memberDatabaseDao.getTop10MembersByLikes()
            val dislikes = memberDatabaseDao.getTop10MembersByDislikes()
            likes to dislikes
        }
    }

    suspend fun getTop10MembersByLikesAndDislikesWeekly() : Pair<List<MemberData>, List<MemberData>> {
        return withContext(dispatcher) {
            val likes = memberDatabaseDao.getTop10MembersByLikesWeekly()
            val dislikes = memberDatabaseDao.getTop10MembersByDislikesWeekly()
            likes to dislikes
        }
    }

    suspend fun giveLikePoint(giver : MemberData, givePoint : Long, targetId : Long) : MemberData? {
        return withContext(dispatcher) {
            if(giver.giftPoints >= givePoint){
                memberDatabaseDao.updateMemberGiftPoints(giver.userId, giver.giftPoints - givePoint)
                memberDatabaseDao.increaseLikes(targetId, givePoint)

                memberDatabaseDao.getMember(targetId).getOrNull(0)
            }else {
                null
            }
        }
    }

    suspend fun giveDislikePoint(giver : MemberData, givePoint : Long, targetId : Long) : MemberData? {
        return withContext(dispatcher) {
            if(giver.giftPoints >= givePoint){
                memberDatabaseDao.updateMemberGiftPoints(giver.userId, giver.giftPoints - givePoint)
                memberDatabaseDao.increaseDislikes(targetId, givePoint)

                memberDatabaseDao.getMember(targetId).getOrNull(0)
            }else {
                null
            }
        }
    }

    suspend fun updateMemberRank(member: MemberData) : Rank {
        val currentRank = Rank.getRankByName(member.rank)
        if(currentRank.tier > 4){
            return currentRank
        }

        val point = member.likes - member.dislikes
        val newRank = Rank.getRankByPoint(point)

        if(member.rank != newRank.name){
            memberDatabaseDao.updateMemberRank(member.userId, newRank.name, newRank.resetPoints)
        }
        return newRank
    }

    suspend fun updatePrimeMinister() : MemberData? {
        return withContext(dispatcher) {
            // 1. 야당대표 리셋
            memberDatabaseDao.getMembersByRank(Rank.PrimeMinister.name).forEach { member ->
                val newRank = Rank.getRankByPoint(member.likes - member.dislikes)
                memberDatabaseDao.updateMemberRank(member.userId, newRank.name, newRank.resetPoints)
            }

            // 2. 야당대표 선출
            val primeMinister = memberDatabaseDao.getTop11MembersByPartyPoint().firstOrNull { Rank.getRankByName(it.rank).tier < 5 }
            if(primeMinister != null){
                memberDatabaseDao.updateMemberRank(primeMinister.userId, Rank.PrimeMinister.name, Rank.PrimeMinister.resetPoints)
            }
            primeMinister
        }
    }

    suspend fun updateSeoulMayor() : MemberData? {
        return withContext(dispatcher) {
            // 1. 서울시장 리셋
            memberDatabaseDao.getMembersByRank(Rank.SeoulMayor.name).forEach { member ->
                val newRank = Rank.getRankByPoint(member.likes - member.dislikes)
                memberDatabaseDao.updateMemberRank(member.userId, newRank.name, newRank.resetPoints)
            }

            // 2. 서울시장 선출
            val seoulMayor = memberDatabaseDao.getTop11MembersByRequirePoint().firstOrNull { Rank.getRankByName(it.rank).tier < 5 }
            if(seoulMayor != null){
                memberDatabaseDao.updateMemberRank(seoulMayor.userId, Rank.SeoulMayor.name, Rank.SeoulMayor.resetPoints)
            }
            seoulMayor
        }
    }
}
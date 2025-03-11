package com.messege.alarmbot.processor.usecase

import com.messege.alarmbot.core.common.PartyLogType
import com.messege.alarmbot.core.common.PartyMemberState
import com.messege.alarmbot.core.common.PartyState
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.member.model.MemberData
import com.messege.alarmbot.data.database.party.dao.PartyDatabaseDao
import com.messege.alarmbot.data.database.party.model.PartyData
import com.messege.alarmbot.data.database.party.model.PartyLog
import com.messege.alarmbot.data.database.party.model.PartyRule
import com.messege.alarmbot.processor.usecase.PartyCreateResult.AlreadyPartyMember
import com.messege.alarmbot.processor.usecase.PartyCreateResult.AlreadyPartyNameExist
import com.messege.alarmbot.processor.usecase.PartyCreateResult.CreateSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class PartyCreateResult {
    data object AlreadyPartyMember : PartyCreateResult()
    data object AlreadyPartyNameExist : PartyCreateResult()
    data class CreateSuccess(val partyId: Long) : PartyCreateResult()
}

sealed class PartyDissolveResult {
    data object DissolveFail : PartyDissolveResult()
    data class DissolveSuccess(val party: PartyData) : PartyDissolveResult()
}

data class PartyInfo(
    val partyInfo : PartyData,
    val partyLeader : MemberData,
    val partyMembers : List<MemberData>
)

sealed class PartyRenameResult {
    data object RenameFail : PartyRenameResult()
    data class RenameSuccess(val originName: String, val newName: String) : PartyRenameResult()
}

sealed class PartyDescriptionResult {
    data object DescriptionFail : PartyDescriptionResult()
    data class DescriptionSuccess(val partyName: String) : PartyDescriptionResult()
}

sealed class PartyRuleResult {
    data object PartyRuleFail : PartyRuleResult()
    data class PartyRuleSuccess(val partyName: String) : PartyRuleResult()
}

sealed class DelegateLeaderResult {
    data object DelegateLeaderFail : DelegateLeaderResult()
    data class DelegateLeaderSuccess(val newLeader: MemberData) : DelegateLeaderResult()
}

sealed class PartyEvent {
    data object Fail : PartyEvent()
    data class Success(val party: PartyData) : PartyEvent()
}


class UseCaseParty(
    private val dispatcher : CoroutineDispatcher = Dispatchers.IO,
    private val memberDatabaseDao: MemberDatabaseDao,
    private val partyDatabaseDao: PartyDatabaseDao
) {

    // 창당
    suspend fun createParty(member : MemberData, partyName : String) : PartyCreateResult{
        return withContext(dispatcher) {
            if(member.partyId == 0L){
                if(partyDatabaseDao.getPartyByName(partyName) == null){
                    val time = System.currentTimeMillis()
                    val partyId = partyDatabaseDao.insertParty(
                        PartyData(
                            foundingTime = time,
                            name = partyName,
                            partyPoints = 6L,
                            leaderId = member.userId,
                            partyState = PartyState.Active,
                            memberCount = 1,
                            description = ""
                        )
                    )

                    memberDatabaseDao.updatePartyStateLeader(member.userId, partyId, 11L)

                    partyDatabaseDao.insertPartyLog(PartyLog(
                        time = time,
                        partyId = partyId,
                        memberId = member.userId,
                        logType = PartyLogType.Founding
                    ))
                    CreateSuccess(partyId)

                }else {
                    AlreadyPartyNameExist
                }
            }else {
                AlreadyPartyMember
            }
        }
    }

    // 해산
    suspend fun dissolveParty(member : MemberData) : PartyDissolveResult {
        return withContext(dispatcher) {
            if (member.partyId != 0L && member.partyState == PartyMemberState.PartyLeader) {
                val time = System.currentTimeMillis()
                val party = partyDatabaseDao.getParty(member.partyId)

                if(party != null){
                    partyDatabaseDao.updatePartyState(member.partyId, PartyState.Dissolution.name)
                    memberDatabaseDao.resetPartyMemberByDissolution(member.partyId)
                    partyDatabaseDao.deleteAllPartyRules(member.partyId)

                    partyDatabaseDao.insertPartyLog(PartyLog(
                        time = time,
                        partyId = member.partyId,
                        memberId = member.userId,
                        logType = PartyLogType.Dissolution
                    ))

                    PartyDissolveResult.DissolveSuccess(party)
                }else{
                    PartyDissolveResult.DissolveFail
                }
            }else {
                PartyDissolveResult.DissolveFail
            }
        }
    }

    // 조회
    suspend fun getPartyInfo(partyName: String): PartyInfo?{
        return withContext(dispatcher) {
            partyDatabaseDao.getPartyByName(partyName)?.let { party ->
                memberDatabaseDao.getMember(party.leaderId).getOrNull(0)?.let { leader ->
                    val members = memberDatabaseDao.getPartyMembers(party.idx)
                    PartyInfo(party, leader, members)
                }
            }
        }
    }

    // 랭킹 조회
    suspend fun getPartyRanking(): List<PartyData> {
        return withContext(dispatcher) {
            partyDatabaseDao.getTop10PartiesByPartyPoints()
        }
    }


    // 당 이름 변경
    suspend fun renameParty(member : MemberData, newName: String) : PartyRenameResult{
        return withContext(dispatcher) {
            if (member.partyId != 0L && member.partyState == PartyMemberState.PartyLeader) {
                val party = partyDatabaseDao.getParty(member.partyId)
                if(party != null){
                    partyDatabaseDao.updatePartyName(party.idx, newName)
                    PartyRenameResult.RenameSuccess(party.name, newName)
                }else{
                    PartyRenameResult.RenameFail
                }
            }else{
                PartyRenameResult.RenameFail
            }
        }
    }

    // 당 소개 변경
    suspend fun updatePartyDescription(member : MemberData, newDescription: String) : PartyDescriptionResult{
        return withContext(dispatcher) {
            if (member.partyId != 0L && member.partyState == PartyMemberState.PartyLeader) {
                val party = partyDatabaseDao.getParty(member.partyId)
                if(party != null){
                    partyDatabaseDao.updatePartyDescription(party.idx, newDescription)
                    PartyDescriptionResult.DescriptionSuccess(party.name)
                }else{
                    PartyDescriptionResult.DescriptionFail
                }
            }else{
                PartyDescriptionResult.DescriptionFail
            }
        }
    }


    // 당규 추가
    suspend fun addPartyRule(member : MemberData, ruleContent: String) : PartyRuleResult{
        return withContext(dispatcher) {
            if (member.partyId != 0L && member.partyState == PartyMemberState.PartyLeader) {
                val party = partyDatabaseDao.getParty(member.partyId)
                if(party != null){
                    partyDatabaseDao.insertPartyRule(PartyRule(
                        partyId = party.idx,
                        time = System.currentTimeMillis(),
                        memberId = member.userId,
                        rule = ruleContent
                    ))
                    PartyRuleResult.PartyRuleSuccess(party.name)
                }else{
                    PartyRuleResult.PartyRuleFail
                }
            }else{
                PartyRuleResult.PartyRuleFail
            }
        }
    }

    // 당규 삭제
    suspend fun removePartyRule(member : MemberData, number: Int) : PartyRuleResult{
        return withContext(dispatcher) {
            if (member.partyId != 0L && member.partyState == PartyMemberState.PartyLeader) {
                val party = partyDatabaseDao.getParty(member.partyId)
                if(party != null){
                    partyDatabaseDao.deletePartyRuleOrderNumber(party.idx, number)
                    PartyRuleResult.PartyRuleSuccess(party.name)
                }else{
                    PartyRuleResult.PartyRuleFail
                }
            }else{
                PartyRuleResult.PartyRuleFail
            }
        }
    }

    // 당규 조회
    suspend fun getPartyRules(partyName: String): List<PartyRule>? {
        return withContext(dispatcher) {
            val party = partyDatabaseDao.getPartyByName(partyName)
            if(party != null){
                partyDatabaseDao.getPartyRules(party.idx)
            }else{
                null
            }
        }
    }


    // 위임 (리더 변경)
    suspend fun delegateLeader(member : MemberData, newLeaderId: Long) : DelegateLeaderResult {
        return withContext(dispatcher) {
            if (member.partyId != 0L && member.partyState == PartyMemberState.PartyLeader) {
                val time = System.currentTimeMillis()
                val party = partyDatabaseDao.getParty(member.partyId)
                val newLeader = memberDatabaseDao.getMember(newLeaderId).getOrNull(0)

                if(party != null && newLeader != null){
                    partyDatabaseDao.updatePartyLeader(party.idx, newLeader.userId)
                    memberDatabaseDao.updatePartyStateMember(member.userId, party.idx, time, party.partyPoints)
                    memberDatabaseDao.updatePartyStateLeader(newLeader.userId, party.idx, 5 + party.partyPoints)

                    partyDatabaseDao.insertPartyLog(
                        PartyLog(
                            time = time,
                            partyId = member.partyId,
                            memberId = member.userId,
                            logType = PartyLogType.Delegation
                        )
                    )

                    DelegateLeaderResult.DelegateLeaderSuccess(newLeader)
                }else{
                    DelegateLeaderResult.DelegateLeaderFail
                }
            }else {
                DelegateLeaderResult.DelegateLeaderFail
            }
        }
    }

    // 가입 신청
    suspend fun requestToJoinParty(member : MemberData, partyName: String) : PartyEvent{
        return withContext(dispatcher) {
            if (member.partyId == 0L && member.partyState == PartyMemberState.None) {
                val time = System.currentTimeMillis()
                val party = partyDatabaseDao.getPartyByName(partyName)
                if(party != null){
                    memberDatabaseDao.updatePartyStateApplicant(member.userId, party.idx, time)

                    partyDatabaseDao.insertPartyLog(
                        PartyLog(
                            time = time,
                            partyId = member.partyId,
                            memberId = member.userId,
                            logType = PartyLogType.Application
                        )
                    )

                    PartyEvent.Success(party)
                }else{
                    PartyEvent.Fail
                }
            } else {
                PartyEvent.Fail
            }
        }
    }

    // 가입 신청 취소
    suspend fun cancelJoinRequest(member : MemberData) : PartyEvent{
        return withContext(dispatcher) {
            if (member.partyId != 0L && member.partyState == PartyMemberState.Applicant) {
                val time = System.currentTimeMillis()
                val party = partyDatabaseDao.getParty(member.partyId)
                if(party != null){
                    memberDatabaseDao.updatePartyStateNone(member.userId)

                    partyDatabaseDao.insertPartyLog(
                        PartyLog(
                            time = time,
                            partyId = member.partyId,
                            memberId = member.userId,
                            logType = PartyLogType.Cancellation
                        )
                    )

                    PartyEvent.Success(party)
                }else{
                    PartyEvent.Fail
                }
            } else {
                PartyEvent.Fail
            }
        }
    }
}
package com.messege.alarmbot.contents.common

import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.core.common.Rank
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.member.model.AdminLogData
import com.messege.alarmbot.data.database.member.model.SanctionData
import com.messege.alarmbot.processor.model.AdminRoomTextResponse
import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Group1RoomTextResponse
import com.messege.alarmbot.util.format.toTimeFormatDate
import kotlinx.coroutines.channels.Channel

class CommonContent(
    override val commandChannel: Channel<Command>,
    private val memberDatabaseDao : MemberDatabaseDao
) : BaseContent {
    override val contentsName: String = "기본"

    override suspend fun request(message : Message) {
        if(message.type == ChatRoomType.GroupRoom1 && message is Message.Talk) {
            val user = memberDatabaseDao.getMember(message.userId).getOrNull(0)
            val isSuperAdmin = user?.isSuperAdmin ?: false
            val isAdmin = isSuperAdmin || user?.isAdmin ?: false
            val rank = if(user != null) Rank.getRankByName(user.rank) else Rank.Unemployed

            if(message.text == ".?"){
                commandChannel.send(Group1RoomTextResponse(COMMAND_HELP))
            }else if(message.text.startsWith(".조회")){
                val targetId = message.mentionIds.getOrNull(0)
                if(targetId != null){
                    val targetMember = memberDatabaseDao.getMember(targetId).getOrNull(0)
                    val allNames = memberDatabaseDao.getNicknameDataAll(targetId).joinToString(",") { it.nickName }
                    if(targetMember != null){
                        var profileInfo = "닉네임 : ${targetMember.latestName} " +
                            "${if(targetMember.isSuperAdmin)"[슈퍼관리자]" else if(targetMember.isAdmin) "[관리자]" else ""}\n\n" +

                        "직업 : ${Rank.getRankByName(targetMember.rank).korName}\n" +
                        "포인트 : ${targetMember.giftPoints}\n" +
                        "좋아요 : ${targetMember.likes}\n" +
                        "싫어요 : ${targetMember.dislikes}\n" +
                        "주간 좋아요 : ${targetMember.likesWeekly}\n" +
                        "주간 싫어요 : ${targetMember.dislikesWeekly}"

                        if (rank.tier >= 4) {
                            profileInfo +=
                                "\n\n톡 횟수 : ${targetMember.talkCount}\n" +
                                    "입장 횟수 : ${targetMember.enterCount}\n" +
                                    "강퇴 횟수 : ${targetMember.kickCount}\n" +
                                    "제재 횟수 : ${targetMember.sanctionCount}\n" +
                                    "톡 삭제 횟수 : ${targetMember.deleteTalkCount}\n" +
                                    "1:1 톡 변경 횟수 : ${targetMember.chatProfileCount}\n\n" +
                                    "닉네임 변경 이력 : $allNames"
                        }


                        if(isAdmin){
                            commandChannel.send(AdminRoomTextResponse(profileInfo))
                        }else{
                            commandChannel.send(Group1RoomTextResponse(profileInfo))
                        }
                    }
                }
            } else if(message.text.startsWith(".제재내역") && isAdmin){
                val targetId = message.mentionIds.getOrNull(0)
                if(targetId != null){
                    val targetMember = memberDatabaseDao.getMember(targetId).getOrNull(0)
                    if(targetMember != null){

                        var sanctionText = "${targetMember.latestName} 제재 (${targetMember.sanctionCount})\n"
                        memberDatabaseDao.getSanctionDataAll(targetMember.userId).forEach { sanction ->
                            val sanctionDate = (sanction.eventAt * 1000).toTimeFormatDate()
                            val giverName = memberDatabaseDao.getMemberName(sanction.giverId)
                            sanctionText += "$sanctionDate $giverName : ${sanction.sanctionCount} (${sanction.reason})\n"
                        }
                        commandChannel.send(AdminRoomTextResponse(sanctionText))
                    }
                }
            }else if((message.text.startsWith(SanctionType.Sanction.prefix) ||
                    message.text.startsWith(SanctionType.Warning.prefix) ||
                    message.text.startsWith(SanctionType.Remission.prefix)) && isAdmin
            ) {
                val sanctionType = SanctionType.entries.find { message.text.startsWith(it.prefix) }
                val targetId = message.mentionIds.getOrNull(0)
                if (sanctionType != null && targetId != null) {
                    val targetMember = memberDatabaseDao.getMember(targetId).getOrNull(0)
                    if (targetMember != null) {
                        parseSanction(
                            input = message.text,
                            sanctionType = sanctionType,
                            targetMemberName = targetMember.latestName
                        )?.let { sanction ->
                            val tempResult = targetMember.sanctionCount + sanction.sanctionCount
                            val result = if(tempResult > 0) tempResult else 0
                            memberDatabaseDao.updateMemberSanctionCount(
                                userId = targetMember.userId,
                                sanctionCount = result
                            )
                            memberDatabaseDao.insertSanctionData(
                                SanctionData(
                                    userId = targetMember.userId, eventAt = message.time,
                                    giverId = message.userId, reason = sanction.reason?:"",
                                    sanctionCount = sanction.sanctionCount.toLong()
                                )
                            )
                            val responseText = "${targetMember.latestName} 님이 ${sanctionType.title}로 " +
                                "제재 $result 회 상태 입니다."
                            commandChannel.send(AdminRoomTextResponse(responseText))
                        }
                    }
                }
            }else if(message.text.startsWith(".임명") && isSuperAdmin){
                val targetId = message.mentionIds.getOrNull(0)
                if(targetId != null){
                    val target = memberDatabaseDao.getMember(targetId).getOrNull(0)
                    if(target != null){
                        memberDatabaseDao.insertAdminLogData(AdminLogData(targetId, message.time, true))
                        memberDatabaseDao.updateAdmin(targetId, true)
                        memberDatabaseDao.updateMemberRank(target.userId, Rank.Minister.name, Rank.Minister.resetPoints)
                        commandChannel.send(Group1RoomTextResponse("${target.latestName}님이 부방으로 임명되었습니다."))
                    }
                }
            }else if(message.text.startsWith(".해제") && isSuperAdmin){
                val targetId = message.mentionIds.getOrNull(0)
                if(targetId != null){
                    val target = memberDatabaseDao.getMember(targetId).getOrNull(0)
                    if(target != null){
                        memberDatabaseDao.insertAdminLogData(AdminLogData(targetId, message.time, false))
                        memberDatabaseDao.updateAdmin(targetId, false)

                        val point = target.likes - target.dislikes
                        val newRank = Rank.getRankByPoint(point)
                        memberDatabaseDao.updateMemberRank(target.userId, newRank.name, newRank.resetPoints)
                        commandChannel.send(Group1RoomTextResponse("${target.latestName}님이 부방에서 해제되었습니다."))
                    }
                }
            }
        }
    }
}
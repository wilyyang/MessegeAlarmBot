package com.messege.alarmbot.contents.common

import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.member.model.AdminLogData
import com.messege.alarmbot.processor.model.AdminRoomTextResponse
import com.messege.alarmbot.processor.model.Command
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

            if(message.text.startsWith(".조회") && isAdmin){
                val targetId = message.mentionIds.getOrNull(0)
                if(targetId != null){
                    val targetMember = memberDatabaseDao.getMember(targetId).getOrNull(0)
                    val allNames = memberDatabaseDao.getNicknameDataAll(targetId).joinToString(",") { it.nickName }
                    if(targetMember != null){
                        val profileInfo = "닉네임 : ${targetMember.latestName} " +
                            "${if(targetMember.isSuperAdmin)"[슈퍼관리자]" else if(targetMember.isAdmin) "[관리자]" else ""}\n" +
                        "톡 횟수 : ${targetMember.talkCount}\n" +
                        "입장 횟수 : ${targetMember.enterCount}\n" +
                        "강퇴 횟수 : ${targetMember.kickCount}\n" +
                        "톡 삭제 횟수 : ${targetMember.deleteTalkCount}\n" +
                        "1:1 톡 변경 횟수 : ${targetMember.chatProfileCount}\n\n" +
                        "닉네임 변경 이력 : $allNames"

                        commandChannel.send(AdminRoomTextResponse(profileInfo))
                    }
                }
            }else if(message.text.startsWith(".임명") && isSuperAdmin){
                val targetId = message.mentionIds.getOrNull(0)
                if(targetId != null){
                    memberDatabaseDao.insertAdminLogData(AdminLogData(targetId, message.time, true))
                    memberDatabaseDao.updateAdmin(targetId, true)
                }
            }else if(message.text.startsWith(".해제") && isSuperAdmin){
                val targetId = message.mentionIds.getOrNull(0)
                if(targetId != null){
                    memberDatabaseDao.insertAdminLogData(AdminLogData(targetId, message.time, false))
                    memberDatabaseDao.updateAdmin(targetId, false)
                }
            }
        }
    }
}
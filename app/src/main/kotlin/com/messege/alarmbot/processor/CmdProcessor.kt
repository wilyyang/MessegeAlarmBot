package com.messege.alarmbot.processor

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.messege.alarmbot.contents.*
import com.messege.alarmbot.contents.common.CommonContent
import com.messege.alarmbot.contents.point.PointContent
import com.messege.alarmbot.contents.topic.TopicContent
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.core.common.PartyMemberState
import com.messege.alarmbot.core.common.Rank
import com.messege.alarmbot.core.common.SUPER_ADMIN_AUTOJU
import com.messege.alarmbot.core.common.SUPER_ADMIN_ME
import com.messege.alarmbot.core.common.TEMP_PROFILE_TYPE
import com.messege.alarmbot.core.common.inNotTalkType
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.member.model.*
import com.messege.alarmbot.data.database.party.dao.PartyDatabaseDao
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
import com.messege.alarmbot.kakao.ChatLogsObserver
import com.messege.alarmbot.kakao.ChatMembersObserver
import com.messege.alarmbot.kakao.model.ChatMember
import com.messege.alarmbot.processor.model.AdminRoomTextResponse
import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Group1RoomTextResponse
import com.messege.alarmbot.processor.model.Group2RoomTextResponse
import com.messege.alarmbot.processor.model.IndividualRoomTextResponse
import com.messege.alarmbot.processor.model.LikeWeeklyRanking
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.processor.model.None
import com.messege.alarmbot.processor.model.ResetMemberPoint
import com.messege.alarmbot.processor.usecase.UseCasePoint
import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class CmdProcessor(
    private val applicationContext: Context,
    private val memberDatabaseDao: MemberDatabaseDao,
    private val topicDatabaseDao: TopicDatabaseDao,
    private val partyDatabaseDao: PartyDatabaseDao
) {
    private var groupRoom1OpenChatRoomAction : Notification.Action? = null
    private var groupRoom2OpenChatRoomAction : Notification.Action? = null
    private var adminOpenChatRoomAction : Notification.Action? = null
    private val userChatRoomMap = mutableMapOf<ChatRoomKey, Notification.Action>()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val commandChannel : Channel<Command> = Channel()
    private val channelFlow = commandChannel.receiveAsFlow().shareIn(scope = scope, started = WhileSubscribed())

    private val useCasePoint = UseCasePoint(memberDatabaseDao = memberDatabaseDao)

    private val commonContent : CommonContent = CommonContent(
        commandChannel = commandChannel,
        memberDatabaseDao = memberDatabaseDao
    )

    private var contents: Array<BaseContent> = arrayOf(
        commonContent,
        PointContent(
            commandChannel = commandChannel,
            memberDatabaseDao = memberDatabaseDao,
            useCasePoint = useCasePoint
        ),
        TopicContent(
            commandChannel = commandChannel,
            topicDatabaseDao = topicDatabaseDao,
            memberDatabaseDao = memberDatabaseDao
        )
    )

    init{
        scope.launch {
            // Member Update
            val chatMembersObserver = ChatMembersObserver()

            chatMembersObserver.observeChatMembers().collect { members ->
                val savedMap = memberDatabaseDao.getMembersAll().associateBy { it.userId }
                members.forEach { talkMember ->
                    savedMap[talkMember.userId]?.let { savedMember ->
                        if(talkMember.nickName != savedMember.latestName){
                            useCaseUpdateMemberNickName(talkMember.userId, talkMember.nickName)
                            val allNames = memberDatabaseDao.getNicknameDataAll(talkMember.userId).joinToString(",") { it.nickName }
                            handleCommand(AdminRoomTextResponse("닉네임 변경 : $allNames"))
                        }

                        if(talkMember.type != savedMember.profileType){
                            useCaseUpdateMemberProfileType(talkMember.userId, talkMember.type, savedMember.profileType){
                                handleCommand(AdminRoomTextResponse("1:1 톡 가능 프로필로 변경됨 (${talkMember.nickName})"))
                            }
                        }

                    }?:also {
                        useCaseInsertNewMember(talkMember)
                    }
                }
            }
        }

        scope.launch {
            // Message Receive
            val chatLogsObserver = ChatLogsObserver(
                getName = {
                    memberDatabaseDao.getMemberName(it).getOrNull(0)?:""
                }
            )

            chatLogsObserver.observeChatLogs().collect { message ->

                // Member Database Update
                when(message){
                    is Message.Event.DeleteMessage -> {
                        Logger.d("[message.delete][${message.type.roomKey}] ${message.userName} ${message.deleteMessage}")
                        memberDatabaseDao.insertDeleteTalkData(DeleteTalkData(message.userId, message.time, message.deleteMessage))
                        memberDatabaseDao.incrementDeleteTalkCount(message.userId)
                        handleCommand(Group2RoomTextResponse("메시지가 삭제됨 : ${message.deleteMessage} (${message.userName})"))
                    }
                    is Message.Event.ManageEvent.AppointManagerEvent -> {
                        Logger.d("[message.manager][${message.type.roomKey}] ${message.targetName}, appointment")

                        val target = memberDatabaseDao.getMember(message.targetId).getOrNull(0)
                        if(target != null){
                            memberDatabaseDao.insertAdminLogData(AdminLogData(target.userId, message.time, true))
                            memberDatabaseDao.updateAdmin(target.userId, true)

                            memberDatabaseDao.updateMemberRank(target.userId, Rank.Minister.name, Rank.Minister.resetPoints)
                        }
                    }
                    is Message.Event.ManageEvent.ReleaseManagerEvent -> {
                        Logger.d("[message.manager][${message.type.roomKey}] ${message.targetName}, release")

                        val target = memberDatabaseDao.getMember(message.targetId).getOrNull(0)
                        if(target != null){
                            memberDatabaseDao.insertAdminLogData(AdminLogData(target.userId, message.time, false))
                            memberDatabaseDao.updateAdmin(target.userId, false)

                            val point = target.likes - target.dislikes
                            val newRank = Rank.getRankByPoint(point)
                            memberDatabaseDao.updateMemberRank(target.userId, newRank.name, newRank.resetPoints)
                        }
                    }
                    is Message.Event.ManageEvent.EnterEvent -> {
                        Logger.d("[message.enter][${message.type.roomKey}] ${message.targetName}")
                        memberDatabaseDao.insertEnterData(EnterData(message.targetId, message.time))
                        memberDatabaseDao.incrementEnterCount(message.targetId)
                        val allNames = memberDatabaseDao.getNicknameDataAll(message.targetId).joinToString(",") { it.nickName }
                        handleCommand(AdminRoomTextResponse("유저가 입장함 : $allNames (${message.targetName})"))
                    }
                    is Message.Event.ManageEvent.KickEvent -> {
                        Logger.d("[message.kick][${message.type.roomKey}] ${message.targetName}")
                        memberDatabaseDao.insertKickData(KickData(message.targetId, message.time))
                        memberDatabaseDao.incrementKickCount(message.targetId)

                        memberDatabaseDao.getMember(message.targetId).getOrNull(0)?.let { member ->
                            val kickPoint = if(member.sanctionCount > 4) 4 else member.sanctionCount
                            memberDatabaseDao.updateMemberSanctionCount(member.userId, member.sanctionCount - kickPoint)
                            memberDatabaseDao.insertSanctionData(
                                SanctionData(
                                    userId = member.userId, eventAt = message.time,
                                    giverId = message.userId, reason = "강퇴당함",
                                    sanctionCount = -kickPoint
                                )
                            )
                        }
                    }
                    is Message.Talk -> {
                        Logger.d("[message.talk][${message.type.roomKey}] ${message.userName} ${message.text}")
                        memberDatabaseDao.incrementTalkCount(message.userId)
                    }
                    else -> {}
                }

                // Send Message
                if(message is Message.Talk){
                    contents.forEach { content ->
                        content.request(message)
                    }
                }
            }
        }

        scope.launch {
            channelFlow.collect { command ->
                handleCommand(command)
            }
        }
    }

    fun deliverNotification(chatRoomKey: ChatRoomKey, user: Person, action : Notification.Action, text : String){
        if(!chatRoomKey.isGroupConversation){
            userChatRoomMap[chatRoomKey] = action
        }
        if(chatRoomKey.roomKey.toLong() == ChatRoomType.GroupRoom1.roomKey){
            groupRoom1OpenChatRoomAction = action
        }

        if(chatRoomKey.roomKey.toLong() == ChatRoomType.GroupRoom2.roomKey){
            groupRoom2OpenChatRoomAction = action
        }

        if(chatRoomKey.roomKey.toLong() == ChatRoomType.AdminRoom.roomKey){
            adminOpenChatRoomAction = action
        }
    }

    fun sendCommand(command: Command){
        scope.launch {
            commandChannel.send(command)
        }
    }

    private fun handleCommand(command: Command){
        if(command !is None) {
            Logger.v("[command] $command")
        }
        when(command){
            is Group1RoomTextResponse -> {
                groupRoom1OpenChatRoomAction?.let { action ->
                    sendActionText(applicationContext, action, command.text)
                }
            }
            is Group2RoomTextResponse -> {
                groupRoom2OpenChatRoomAction?.let { action ->
                    sendActionText(applicationContext, action, command.text)
                }
            }
            is AdminRoomTextResponse -> {
                adminOpenChatRoomAction?.let { action ->
                    sendActionText(applicationContext, action, command.text)
                }
            }
            is IndividualRoomTextResponse -> {
                userChatRoomMap[command.userKey]?.let { action ->
                    sendActionText(applicationContext, action, command.text)
                }
            }
            is ResetMemberPoint -> {
                scope.launch {
                    useCasePoint.resetAllMembersGiftPoints()

                    groupRoom1OpenChatRoomAction?.let { action ->
                        sendActionText(applicationContext, action, "데일리 포인트가 지급되었습니다!")
                    }
                }
            }

            is LikeWeeklyRanking -> {
                scope.launch {
                    val (likesWeekly, dislikesWeekly) = useCasePoint.getTop10MembersByLikesAndDislikesWeekly()

                    var responseText = "* 한주간 랭킹 입니다! *\n\n"
                    responseText += "[주간 좋아요! 랭킹]\n"
                    responseText += likesWeekly.mapIndexed { index, it ->
                        if(it.likesWeekly > 0){
                            "${index + 1}. ${it.latestName} : ${it.likesWeekly}\n"
                        }else ""
                    }.joinToString("")

                    responseText += "\n\n[주간 싫어요! 랭킹]\n"
                    responseText += dislikesWeekly.mapIndexed { index, it ->
                        if(it.dislikesWeekly > 0){
                            "${index + 1}. ${it.latestName} : ${it.dislikesWeekly}\n"
                        }else ""
                    }.joinToString("")

                    useCasePoint.resetAllMembersWeeklyLikesAndDislikes()

                    groupRoom1OpenChatRoomAction?.let { action ->
                        sendActionText(applicationContext, action, responseText)
                    }
                }
            }
            else -> {}
        }
    }

    private suspend fun useCaseInsertNewMember(talkMember: ChatMember){
        memberDatabaseDao.insertNickNameData(
            NicknameData(
                userId = talkMember.userId,
                changeAt = System.currentTimeMillis(),
                nickName = talkMember.nickName
            )
        )

        val isTalkAvailable = !inNotTalkType(talkMember.type)
        if (isTalkAvailable){
            memberDatabaseDao.insertChatProfileData(
                ChatProfileData(
                    userId = talkMember.userId,
                    changeAt = System.currentTimeMillis(),
                    isAvailable = true
                )
            )
        }

        memberDatabaseDao.insertMember(
            MemberData(
                userId = talkMember.userId,
                createAt = System.currentTimeMillis(),
                profileType = talkMember.type,
                latestName = talkMember.nickName,
                isSuperAdmin = talkMember.userId == SUPER_ADMIN_ME || talkMember.userId == SUPER_ADMIN_AUTOJU,
                isAdmin = false,
                chatProfileCount = if(isTalkAvailable) 1 else 0,
                talkCount = 0,
                deleteTalkCount = 0,
                enterCount = 1,
                kickCount = 0,
                sanctionCount = 0,
                likes = 0,
                dislikes = 0,
                likesWeekly = 0,
                dislikesWeekly = 0,
                giftPoints = 10,
                resetPoints = 10,
                rank = if (talkMember.userId == SUPER_ADMIN_ME || talkMember.userId == SUPER_ADMIN_AUTOJU) {
                    Rank.President.name
                } else {
                    Rank.Unemployed.name
                },
                partyId = 0,
                partyState = PartyMemberState.None,
                joinTime = -1L,
                partyResetPoints = 0L
            )
        )
    }

    private suspend fun useCaseUpdateMemberNickName(userId: Long, newNickName: String){
        memberDatabaseDao.insertNickNameData(
            NicknameData(
                userId = userId,
                changeAt = System.currentTimeMillis(),
                nickName = newNickName
            )
        )
        memberDatabaseDao.updateLatestName(userId, newNickName)
    }

    private suspend fun useCaseUpdateMemberProfileType(userId: Long, profileType: Int, saveProfileType: Int, alarmTalkProfile: () -> Unit){
        if(profileType != TEMP_PROFILE_TYPE){
            memberDatabaseDao.updateProfileType(userId, profileType)
            val isCurrentChatAvailable = !inNotTalkType(profileType)
            val isSavedChatAvailable = !inNotTalkType(saveProfileType)
            if(isCurrentChatAvailable != isSavedChatAvailable){
                memberDatabaseDao.insertChatProfileData(ChatProfileData(userId, System.currentTimeMillis(), isCurrentChatAvailable))
                if(isCurrentChatAvailable){
                    memberDatabaseDao.incrementChatProfile(userId)
                    alarmTalkProfile()
                }
            }
        }
    }
}

fun sendActionText(context: Context, action: Notification.Action, text: String) {
    val firstInput = action.remoteInputs?.firstOrNull() ?: return
    val intent = Intent().apply {
        val bundle = Bundle().apply {
            putCharSequence(firstInput.resultKey, text)
        }
        RemoteInput.addResultsToIntent(arrayOf(firstInput), this, bundle)
    }

    try {
        action.actionIntent.send(context, 0, intent)
    } catch (e: PendingIntent.CanceledException) {
        e.printStackTrace()
    }
}
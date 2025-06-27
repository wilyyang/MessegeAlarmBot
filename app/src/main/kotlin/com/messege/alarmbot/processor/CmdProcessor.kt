package com.messege.alarmbot.processor

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.messege.alarmbot.contents.*
import com.messege.alarmbot.contents.bot.BotContent
import com.messege.alarmbot.contents.common.CommonContent
import com.messege.alarmbot.contents.party.PartyContent
import com.messege.alarmbot.contents.point.PointContent
import com.messege.alarmbot.contents.topic.TopicContent
import com.messege.alarmbot.contents.mafia.MafiaGameContent
import com.messege.alarmbot.core.common.*
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.member.model.*
import com.messege.alarmbot.data.database.party.dao.PartyDatabaseDao
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
import com.messege.alarmbot.kakao.ChatLogsObserver
import com.messege.alarmbot.kakao.ChatMembersObserver
import com.messege.alarmbot.kakao.model.ChatMember
import com.messege.alarmbot.processor.model.*
import com.messege.alarmbot.processor.usecase.UseCaseParty
import com.messege.alarmbot.processor.usecase.UseCasePoint
import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    private val useCaseParty = UseCaseParty(partyDatabaseDao = partyDatabaseDao, memberDatabaseDao = memberDatabaseDao)

    private lateinit var chatMembersObserver : ChatMembersObserver

    private val commonContent : CommonContent = CommonContent(
        commandChannel = commandChannel,
        memberDatabaseDao = memberDatabaseDao,
        partyDatabaseDao = partyDatabaseDao
    )

    private var contents: Array<BaseContent> = arrayOf(
        commonContent,
        BotContent(
            commandChannel = commandChannel,
            memberDatabaseDao = memberDatabaseDao
        ),
        PointContent(
            commandChannel = commandChannel,
            memberDatabaseDao = memberDatabaseDao,
            useCasePoint = useCasePoint
        ),
        TopicContent(
            commandChannel = commandChannel,
            topicDatabaseDao = topicDatabaseDao,
            memberDatabaseDao = memberDatabaseDao
        ),
        PartyContent(
            commandChannel = commandChannel,
            memberDatabaseDao = memberDatabaseDao,
            useCaseParty = useCaseParty
        ),
        MafiaGameContent(
            commandChannel = commandChannel,
            scope = scope
        )
    )

    init{
        scope.launch {
            // reset all
            // memberDatabaseDao.updateAllMemberRankDefault()
            // memberDatabaseDao.updateMemberRank(userId = SUPER_ADMIN_ME, rank = Rank.President.name, resetPoints = Rank.President.resetPoints)
            // memberDatabaseDao.resetAllMembersGiftPoints()
            // memberDatabaseDao.resetAllMembersLikesAndDislikesToZero()

            // Member Update
            chatMembersObserver = ChatMembersObserver()

            chatMembersObserver.observeChatMembers().collect { members ->
                val savedMap = memberDatabaseDao.getMembersAll().associateBy { it.userId }
                members.forEach { talkMember ->
                    savedMap[talkMember.userId]?.let { savedMember ->
                        if(talkMember.nickName != savedMember.latestName){
                            useCaseUpdateMemberNickName(talkMember.userId, talkMember.nickName)
                            val allNames = memberDatabaseDao.getNicknameDataAll(talkMember.userId).joinToString(",") { it.nickName }
                            commandChannel.send(AdminRoomTextResponse("닉네임 변경 : $allNames"))
                        }

                        if(talkMember.type != savedMember.profileType){
                            useCaseUpdateMemberProfileType(talkMember.userId, talkMember.type, savedMember.profileType){
                                sendCommand(AdminRoomTextResponse("1:1 톡 가능 프로필로 변경됨 (${talkMember.nickName})"))
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
                    memberDatabaseDao.getMemberName(it).getOrNull(0)
                        ?:chatMembersObserver.getChatMember(it)?.nickName
                        ?:"알수없음"
                }
            )

            chatLogsObserver.observeChatLogs().collect { message ->

                // Member Database Update
                when(message){
                    is Message.Event.DeleteMessage -> {
                        Logger.d("[message.delete][${message.type.roomKey}] ${message.userName} ${message.deleteMessage}")
                        memberDatabaseDao.insertDeleteTalkData(DeleteTalkData(message.userId, message.time, message.deleteMessage))
                        memberDatabaseDao.incrementDeleteTalkCount(message.userId)
                        commandChannel.send(AdminRoomTextResponse("메시지가 삭제됨 : ${message.deleteMessage} (${message.userName})"))
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
                        commandChannel.send(AdminRoomTextResponse("유저가 입장함 : $allNames (${message.targetName})"))
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
                        Logger.d("[message.talk][${message.type.roomKey}][${message.userId}] ${message.userName} ${message.text}")
                        memberDatabaseDao.incrementTalkCount(message.userId)
                        contents.forEach { content ->
                            content.request(message)
                        }
                    }
                    else -> {}
                }
            }
        }

        scope.launch {
            channelFlow.collect { command ->
                handleCommand(command)
                delay(command.delayMilliSeconds)
            }
        }
    }

    fun deliverNotification(chatRoomKey: ChatRoomKey, user: Person, action : Notification.Action, text : String){
        var isCheckedRoom = false
        if(chatRoomKey.roomKey.toLongOrNull() == ChatRoomType.GroupRoom1.roomKey){
            isCheckedRoom = true
            groupRoom1OpenChatRoomAction = action
        }

        if(chatRoomKey.roomKey.toLongOrNull() == ChatRoomType.GroupRoom2.roomKey){
            isCheckedRoom = true
            groupRoom2OpenChatRoomAction = action
        }

        if(chatRoomKey.roomKey.toLongOrNull() == ChatRoomType.AdminRoom.roomKey){
            isCheckedRoom = true
            adminOpenChatRoomAction = action
        }

        if(!isCheckedRoom){
            userChatRoomMap[chatRoomKey] = action
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
                    val primeMinister = useCasePoint.updatePrimeMinister()
                    val seoulMayor = useCasePoint.updateSeoulMayor()

                    var responseText = ""
                    primeMinister?.let {
                        responseText += "* 새로운 야당대표는 ${primeMinister.latestName}님 입니다!\n" +
                            "\n"
                    }
                    seoulMayor?.let {
                        responseText += "* 새로운 서울시장은 ${seoulMayor.latestName}님 입니다!\n" +
                            "\n"
                    }

                    val (likesWeekly, dislikesWeekly) = useCasePoint.getTop10MembersByLikesAndDislikesWeekly()
                    responseText += "* 한주간 랭킹 입니다! * $FOLDING_TEXT\n\n"
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
            is UpdateKakaoMembers -> {
                scope.launch {
                    val result = chatMembersObserver.walCheckpoint()
                    val textResult = if(result) "성공" else "실패"
                    groupRoom1OpenChatRoomAction?.let { action ->
                        sendActionText(applicationContext, action, "유저 업데이트 $textResult")
                    }
                }
            }
            is MacroKakaoTalkRoomNews -> {
                val shPath = "/storage/emulated/0/Documents/ChatBot/news_macro.sh"
                val cmd = arrayOf("su", "-c", "sh \"$shPath\"")

                try {
                    val process = Runtime.getRuntime().exec(cmd)
                    val resultCode = process.waitFor()

                    Logger.d("[macro] 스크립트 종료 코드: $resultCode")
                } catch (e: Exception) {
                    Logger.e("[macro] 스크립트 실행 실패 ${e.message}")
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
                isSuperAdmin = talkMember.userId == SUPER_ADMIN_ME || talkMember.userId == SUPER_ADMIN_ME_2,
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
                rank = if (talkMember.userId == SUPER_ADMIN_ME  || talkMember.userId == SUPER_ADMIN_ME_2) {
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
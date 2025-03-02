package com.messege.alarmbot.processor

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.messege.alarmbot.contents.*
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.contents.mafia.MafiaGameContent
import com.messege.alarmbot.contents.topic.TopicContent
import com.messege.alarmbot.core.common.GAME_KEY
import com.messege.alarmbot.core.common.HOST_KEY
import com.messege.alarmbot.core.common.TEMP_PROFILE_TYPE
import com.messege.alarmbot.core.common.inNotTalkType
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.member.model.AdminLogData
import com.messege.alarmbot.data.database.member.model.ChatProfileData
import com.messege.alarmbot.data.database.member.model.MemberData
import com.messege.alarmbot.data.database.member.model.NicknameData
import com.messege.alarmbot.data.database.user.dao.UserDatabaseDao
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
import com.messege.alarmbot.kakao.ChatLogsObserver
import com.messege.alarmbot.kakao.ChatMembersObserver
import com.messege.alarmbot.kakao.model.ChatMember
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
    private val userDatabaseDao: UserDatabaseDao,
    private val topicDatabaseDao: TopicDatabaseDao
) {

    private var mainOpenChatRoomAction : Notification.Action? = null
    private var gameOpenChatRoomAction : Notification.Action? = null
    private var hostOpenChatRoomAction : Notification.Action? = null
    private val userChatRoomMap = mutableMapOf<ChatRoomKey, Notification.Action>()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val commandChannel : Channel<Command> = Channel()
    private val channelFlow = commandChannel.receiveAsFlow().shareIn(scope = scope, started = WhileSubscribed())

    private var contents: Array<BaseContent> = arrayOf(
        CommonContent(
            commandChannel = commandChannel,
            insertUser = userDatabaseDao::insertUser,
            getLatestUserName = userDatabaseDao::getLatestUserName,
            getUserNameList = userDatabaseDao::getUserNames
        ),
        TopicContent(
            commandChannel = commandChannel,
            getLatestUserName = userDatabaseDao::getLatestUserName,
            insertTopic = topicDatabaseDao::insertTopic,
            recommendTopic = topicDatabaseDao::getRandomTopic,
            selectTopic = topicDatabaseDao::getSelectTopic,
            deleteTopic = topicDatabaseDao::deleteTopic
        ),
         QuestionGameContent(commandChannel),
         MafiaGameContent(commandChannel, scope)
    )

    init{
        scope.launch {
            val chatLogsObserver = ChatLogsObserver(
                getName = {
                    memberDatabaseDao.getMemberName(it).getOrNull(0)?:""
                }
            )

            chatLogsObserver.observeChatLogs().collect { message ->
                Logger.i("[chat_log] $message")
            }
        }

        scope.launch {
            val chatMembersObserver = ChatMembersObserver()

            chatMembersObserver.observeChatMembers().collect { members ->
                val savedMap = memberDatabaseDao.getMembersAll().associateBy { it.userId }
                members.forEach { talkMember ->
                    savedMap[talkMember.userId]?.let { savedMember ->
                        if(talkMember.nickName != savedMember.latestName){
                            useCaseUpdateMemberNickName(talkMember.userId, talkMember.nickName)
                        }

                        val isTalkAdmin = talkMember.privilege == -1
                        if(isTalkAdmin != savedMember.isAdmin){
                            useCaseUpdateMemberAdmin(talkMember.userId, isTalkAdmin)
                        }

                        if(talkMember.type != savedMember.profileType){
                            useCaseUpdateMemberProfileType(talkMember.userId, talkMember.type, savedMember.profileType)
                        }

                    }?:also {
                        useCaseInsertNewMember(talkMember)
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

    suspend fun deliverNotification(postTime : Long, chatRoomKey: ChatRoomKey, user: Person, action : Notification.Action, text : String){
        if(!chatRoomKey.isGroupConversation){
            Logger.w("[deliver.user] key : $chatRoomKey")
            Logger.w("[deliver.user] userName : ${user.name} / text : $text")
            userChatRoomMap[chatRoomKey] = action
        }
        if(chatRoomKey == TARGET_KEY){
            Logger.d("[deliver.main] key : $chatRoomKey")
            Logger.d("[deliver.main] userName : ${user.name} / text : $text")
            mainOpenChatRoomAction = action
        }
        if(chatRoomKey == GAME_KEY){
            Logger.d("[deliver.game] key : $chatRoomKey")
            Logger.d("[deliver.game] userName : ${user.name} / text : $text")
            gameOpenChatRoomAction = action
        }
        if(chatRoomKey == HOST_KEY){
            Logger.d("[deliver.host] key : $chatRoomKey")
            Logger.d("[deliver.host] userName : ${user.name} / text : $text")
            hostOpenChatRoomAction = action
        }

        for(content in contents) content.request(postTime = postTime, chatRoomKey = chatRoomKey, user = user, text = text)
    }

    private fun handleCommand(command: Command){
        if(command !is None) {
            Logger.v("[command] $command")
        }
        when(command){
            is MainChatTextResponse -> {
                mainOpenChatRoomAction?.let { action ->
                    sendActionText(applicationContext, action, command.text)
                }
            }
            is GameChatTextResponse -> {
                gameOpenChatRoomAction?.let { action ->
                    sendActionText(applicationContext, action, command.text)
                }
            }
            is HostChatTextResponse -> {
                hostOpenChatRoomAction?.let { action ->
                    sendActionText(applicationContext, action, command.text)
                }
            }
            is UserTextResponse -> {
                userChatRoomMap[command.userKey]?.let { action ->
                    sendActionText(applicationContext, action, command.text)
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

        if (talkMember.privilege == -1) {
            memberDatabaseDao.insertAdminLogData(
                AdminLogData(
                    userId = talkMember.userId,
                    changeAt = System.currentTimeMillis(),
                    isAdmin = true
                )
            )
        }

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
                isAdmin = talkMember.privilege == -1,
                chatProfileCount = if(isTalkAvailable) 1 else 0,
                talkCount = 0,
                deleteTalkCount = 0,
                enterCount = 1,
                kickCount = 0,
                sanctionCount = 0,
                likes = 0,
                dislikes = 0,
                partyId = 0,
                isPartyLeader = false
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

    private suspend fun useCaseUpdateMemberAdmin(userId: Long, isAdmin: Boolean){
        memberDatabaseDao.insertAdminLogData(
            AdminLogData(
                userId = userId,
                changeAt = System.currentTimeMillis(),
                isAdmin = isAdmin
            )
        )
        memberDatabaseDao.updateAdmin(userId, isAdmin)
    }

    private suspend fun useCaseUpdateMemberProfileType(userId: Long, profileType: Int, saveProfileType: Int){
        if(profileType != TEMP_PROFILE_TYPE){
            memberDatabaseDao.updateProfileType(userId, profileType)
            val isCurrentChatAvailable = !inNotTalkType(profileType)
            val isSavedChatAvailable = !inNotTalkType(saveProfileType)
            if(isCurrentChatAvailable != isSavedChatAvailable){
                memberDatabaseDao.insertChatProfileData(ChatProfileData(userId, System.currentTimeMillis(), isCurrentChatAvailable))
                if(isCurrentChatAvailable){
                    memberDatabaseDao.incrementChatProfile(userId)
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
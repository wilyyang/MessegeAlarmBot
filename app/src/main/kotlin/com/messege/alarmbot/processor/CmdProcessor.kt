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
import com.messege.alarmbot.data.database.user.dao.UserDatabaseDao
import com.messege.alarmbot.data.database.topic.dao.TopicDatabaseDao
import com.messege.alarmbot.kakao.ChatLogsObserver
import com.messege.alarmbot.kakao.ChatMembersObserver
import com.messege.alarmbot.util.format.toTimeFormat
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
            val chatLogsObserver = ChatLogsObserver()

            chatLogsObserver.observeChatLogs().collect { log ->
                Logger.i("[chat_log] $log")
            }
        }

        scope.launch {
            val chatMembersObserver = ChatMembersObserver()

            chatMembersObserver.observeChatMembers().collect { members ->
                members.map { "${it.type} ${it.chatId} ${it.userId} ${it.privilege} ${it.nickName}" }.joinToString { ", " }
                Logger.i("[chat_member] $members")
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
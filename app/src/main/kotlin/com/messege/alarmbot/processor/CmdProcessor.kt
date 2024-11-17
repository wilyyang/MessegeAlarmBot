package com.messege.alarmbot.processor

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.CommonContent
import com.messege.alarmbot.contents.QuestionGameContent
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.core.common.TARGET_KEY
import com.messege.alarmbot.contents.Command
import com.messege.alarmbot.contents.mafia.MafiaGameContent
import com.messege.alarmbot.contents.MainChatTextResponse
import com.messege.alarmbot.contents.None
import com.messege.alarmbot.contents.UserTextResponse
import com.messege.alarmbot.data.database.message.dao.MessageDatabaseDao
import com.messege.alarmbot.data.database.message.model.MessageData
import com.messege.alarmbot.data.database.user.dao.UserDatabaseDao
import com.messege.alarmbot.util.format.replyFormat
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
    private val messageDatabaseDao: MessageDatabaseDao
) {

    private var mainOpenChatRoomAction : Notification.Action? = null
    private val userChatRoomMap = mutableMapOf<ChatRoomKey, Notification.Action>()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val commandChannel : Channel<Command> = Channel()
    private val channelFlow = commandChannel.receiveAsFlow().shareIn(scope = scope, started = WhileSubscribed())

    private var contents: Array<BaseContent> = arrayOf(
        CommonContent(
            commandChannel = commandChannel,
            insertUser = userDatabaseDao::insertUser,
            getUserNameList = userDatabaseDao::getUserNames
        ),

         QuestionGameContent(commandChannel),
         MafiaGameContent(commandChannel, scope)
    )

    init{

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
        }else if(chatRoomKey == TARGET_KEY){
            Logger.d("[deliver.main] key : $chatRoomKey")
            Logger.d("[deliver.main] userName : ${user.name} / text : $text")
            mainOpenChatRoomAction = action
        }

        messageDatabaseDao.insertMessage(
            MessageData(
                postTime = postTime,
                message = text,
                userName = "${user.name}",
                userKey = "$${user.key}",
                roomName = chatRoomKey.roomName,
                roomKey = chatRoomKey.roomKey
            )
        )

        for(content in contents) content.request(postTime = postTime, chatRoomKey = chatRoomKey, user = user, text = text)
    }

    private fun handleCommand(command: Command){
        if(command !is None) {
            Logger.v("[command] $command")
        }
        when(command){
            is MainChatTextResponse -> {
                mainOpenChatRoomAction?.let { action ->
                    sendActionText(applicationContext, action, replyFormat(command.text))
                }
            }
            is UserTextResponse -> {
                userChatRoomMap[command.userKey]?.let { action ->
                    sendActionText(applicationContext, action, replyFormat(command.text))
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
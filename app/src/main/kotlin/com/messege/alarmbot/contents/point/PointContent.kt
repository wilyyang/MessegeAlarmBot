package com.messege.alarmbot.contents.point

import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Group1RoomTextResponse
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.processor.usecase.UseCasePoint
import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.channels.Channel

class PointContent (
    override val commandChannel: Channel<Command>,
    private val memberDatabaseDao : MemberDatabaseDao,
    private val useCasePoint : UseCasePoint
) : BaseContent {
    override val contentsName : String = "포인트"

    override suspend fun request(message : Message) {
        if(message.type == ChatRoomType.GroupRoom1 && message is Message.Talk) {
            when(message.text){
                ".랭킹" -> {
                    val (likes, dislikes) = useCasePoint.getTop10MembersByLikesAndDislikes()

                    var responseText = "[좋아요! 랭킹]\n"
                    responseText += likes.mapIndexed { index, it ->
                        if(it.likes > 0){
                            "${index + 1}. ${it.latestName} : ${it.likes}\n"
                        }else ""

                    }.joinToString("")

                    responseText += "\n\n[싫어요! 랭킹]\n"
                    responseText += dislikes.mapIndexed { index, it ->
                        if(it.dislikes > 0){
                            "${index + 1}. ${it.latestName} : ${it.dislikes}\n"
                        }else ""
                    }.joinToString("")

                    commandChannel.send(Group1RoomTextResponse(responseText))
                }

                ".랭킹 주간" -> {
                    val (likes, dislikes) = useCasePoint.getTop10MembersByLikesAndDislikesWeekly()

                    var responseText = "[주간 좋아요! 랭킹]\n"
                    responseText += likes.mapIndexed { index, it ->
                        if(it.likes > 0){
                            "${index + 1}. ${it.latestName} : ${it.likes}\n"
                        }else ""
                    }.joinToString("")

                    responseText += "\n\n[주간 싫어요! 랭킹]\n"
                    responseText += dislikes.mapIndexed { index, it ->
                        if(it.dislikes > 0){
                            "${index + 1}. ${it.latestName} : ${it.dislikes}\n"
                        }else ""
                    }.joinToString("")

                    commandChannel.send(Group1RoomTextResponse(responseText))
                }

                else -> {
                    if(message.text.startsWith(".좋아 ") || message.text.startsWith(".싫어 ")){
                        val user = memberDatabaseDao.getMember(message.userId).getOrNull(0)
                        val targetId = message.mentionIds.getOrNull(0)
                        if(user != null && targetId != null){
                            val point = parseLikeCommand(message.text)
                            if(point != null){
                                val isLike = message.text.startsWith(".좋아 ")
                                val isLikeText = if(isLike) "좋아" else "싫어"
                                val target = if(isLike){
                                    useCasePoint.giveLikePoint(user, point, targetId)
                                }else{
                                    useCasePoint.giveDislikePoint(user, point, targetId)
                                }

                                if(target != null){
                                    val responseText = "${user.latestName}님이 ${target.latestName}님에게 " +
                                            "$isLikeText +${point}! " +
                                            "(${target.latestName}의 현재 $isLikeText : ${target.likes})"
                                    commandChannel.send(Group1RoomTextResponse(responseText))
                                }else{
                                    val responseText = "${user.latestName}님의 증정 포인트가 없거나 대상이 없습니다. (현재 포인트 : ${user.giftPoints})"
                                    commandChannel.send(Group1RoomTextResponse(responseText))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseLikeCommand(input: String): Long? {
        val parts = input.split(" ").filter { it.isNotBlank() }
        return if (parts.size < 2) null else parts[1].toLongOrNull()?:10
    }
}
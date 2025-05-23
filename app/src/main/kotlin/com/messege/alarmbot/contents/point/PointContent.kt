package com.messege.alarmbot.contents.point

import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.core.common.FOLDING_TEXT
import com.messege.alarmbot.core.common.Rank
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

                    var responseText = "\uD83C\uDFC6 유저 전체 랭킹!\n$FOLDING_TEXT\n"
                    responseText += "[좋아요! 랭킹]\n"
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
                    val (likesWeekly, dislikesWeekly) = useCasePoint.getTop10MembersByLikesAndDislikesWeekly()

                    var responseText = "\uD83C\uDFC6 유저 주간 랭킹!\n$FOLDING_TEXT\n"
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

                    commandChannel.send(Group1RoomTextResponse(responseText))
                }

                else -> {
                    if(message.text.startsWith(".좋아 ") || message.text.startsWith(".싫어 ")){
                        val user = memberDatabaseDao.getMember(message.userId).getOrNull(0)
                        val targetId = message.mentionIds.getOrNull(0)
                        if(user != null && targetId != null){
                            if(user.userId == targetId){
                                commandChannel.send(Group1RoomTextResponse("자기 자신에게 좋아요나 싫어요를 할 수 없습니다."))
                                return
                            }

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
                                    val newRank = useCasePoint.updateMemberRank(target)

                                    val responseText = "${user.latestName}님이 ${target.latestName}님에게 " +
                                            "$isLikeText +${point}! " +
                                            "(${target.latestName}의 현재 $isLikeText : ${if(isLike) target.likes else target.dislikes})" +
                                            if(target.rank != newRank.name) "\n계급이 변경되었습니다. (${Rank.getRankByName(target.rank).korName} -> ${newRank.korName})" else ""
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
        return if (parts.size < 2) null else {
            val point = parts[1].toLongOrNull()
            if(point == null){
                10
            }else{
                if(point <= 0 || point > 10000){
                    null
                }else{
                    point
                }
            }
        }
    }
}
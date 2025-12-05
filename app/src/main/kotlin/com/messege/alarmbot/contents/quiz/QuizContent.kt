package com.messege.alarmbot.contents.quiz

import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.bot.Constants.REQUEST_POINT
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.quiz.dao.QuizDatabaseDao
import com.messege.alarmbot.data.database.quiz.model.QuizData
import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Group1RoomTextResponse
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.util.format.toTimeFormatDate
import kotlinx.coroutines.channels.Channel

class QuizContent(
    override val commandChannel: Channel<Command>,
    private val quizDatabaseDao: QuizDatabaseDao,
    private val memberDatabaseDao : MemberDatabaseDao
) : BaseContent {
    override val contentsName: String = "퀴즈"
    private var isQuiz = false
    private var currentQuiz : QuizData? = null

    override suspend fun request(message : Message) {
        val user = memberDatabaseDao.getMember(message.userId).getOrNull(0)
        val isAdmin = user?.isSuperAdmin ?: false || user?.isAdmin ?: false

        if(message.type == ChatRoomType.GroupRoom1) {
            if(message is Message.Talk && user != null){
                if(isAdmin) {
                    when {
                        message.text.startsWith(".퀴즈추가 ") -> {
                            val updateTime = System.currentTimeMillis()
                            val cleaned = message.text.removePrefix(".퀴즈추가").trim()

                            val (quiz, answer) = cleaned.split(":", limit = 2)
                                .map { it.trim() }
                                .let {
                                    it.getOrNull(0).orEmpty() to it.getOrNull(1).orEmpty()
                                }

                            if (quiz.isEmpty() || answer.isEmpty()) {
                                commandChannel.send(Group1RoomTextResponse(text = "퀴즈 추가에 실패했습니다."))
                            } else {
                                val key = quizDatabaseDao.insertQuiz(
                                    QuizData(
                                        updateTime = updateTime,
                                        userKey = message.userId,
                                        quiz = quiz,
                                        answer = answer
                                    )
                                )
                                commandChannel.send(Group1RoomTextResponse(text = "퀴즈가 추가되었습니다. (key = $key)"))
                            }
                        }

                        message.text.startsWith("퀴즈 ") -> {
                            val number = message.text.substringAfter(" ", "").toIntOrNull()?.toLong()
                            val quiz = number?.let { quizDatabaseDao.getSelectQuiz(it) }

                            val quizResultText = quiz?.let {
                                val latestName = memberDatabaseDao.getMember(it.userKey).getOrNull(0)?.latestName?:"-"
                                "- 질문글 : ${it.quiz}\n- 정답 : ${it.answer}\n\n⏱${it.updateTime.toTimeFormatDate()} - $latestName"
                            } ?: "등록된 퀴즈가 없습니다."
                            commandChannel.send(Group1RoomTextResponse(text = quizResultText))
                        }

                        message.text.startsWith("퀴즈삭제 ") -> {
                            val number = message.text.substringAfter(" ", "").toIntOrNull()?.toLong()
                            val quiz = number?.let { quizDatabaseDao.getSelectQuiz(it) }
                            val quizDeleteText = if(quiz != null){
                                quizDatabaseDao.deleteQuiz(number)
                                "퀴즈가 삭제되었습니다. (key = $number)"
                            }else{
                                "등록된 퀴즈가 없습니다."
                            }
                            commandChannel.send(Group1RoomTextResponse(text = quizDeleteText))
                        }
                    }
                }else{
                    if(isQuiz){
                        currentQuiz?.let { quiz ->
                            if(message.text.trim() == quiz.answer.trim()){
                                memberDatabaseDao.updateMemberGiftPoints(user.userId, user.giftPoints + 1)
                                resetQuiz()
                                val quizText = "${quiz.quiz}\n\n ${user.latestName}님 정답입니다. (포인트 추가 + 1)"
                                commandChannel.send(Group1RoomTextResponse(text = quizText))
                            }else{
                                commandChannel.send(Group1RoomTextResponse(text = "틀렸습니다."))
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun sendQuizStart(){
        currentQuiz = quizDatabaseDao.getRandomQuiz()
        currentQuiz?.let { quiz ->
            isQuiz = true

            val circles = "○".repeat(quiz.answer.length)
            val quizText = "${quiz.quiz}\n\n힌트 : $circles"
            commandChannel.send(Group1RoomTextResponse(text = quizText))
        }
    }

    suspend fun sendQuizEnd() {
        if(isQuiz){
            currentQuiz?.let { quiz ->
                val quizText = "${quiz.quiz}\n\n${"퀴즈를 맞춘 사람이 없습니다."}"
                commandChannel.send(Group1RoomTextResponse(text = quizText))

                resetQuiz()
            }
        }
    }

    private fun resetQuiz(){
        isQuiz = false
        currentQuiz = null
    }
}
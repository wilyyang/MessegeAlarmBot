package com.messege.alarmbot.contents.quiz

import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.data.database.quiz.dao.QuizDatabaseDao
import com.messege.alarmbot.data.database.quiz.model.QuizData
import com.messege.alarmbot.processor.model.AdminRoomTextResponse
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
    private var isActive = true

    override suspend fun request(message : Message) {
        val user = memberDatabaseDao.getMember(message.userId).getOrNull(0)
        if(message.type == ChatRoomType.GroupRoom1 && isActive) {
            if(message is Message.Talk && user != null){
                if(isQuiz){
                    currentQuiz?.let { quiz ->
                        if(message.text.trim() == quiz.answer.trim()){
                            memberDatabaseDao.updateMemberGiftPoints(user.userId, user.giftPoints + 1)
                            resetQuiz()
                            val quizText = "${quiz.quiz}\n\n${user.latestName}님 정답입니다. (포인트 추가 + 1)"
                            commandChannel.send(Group1RoomTextResponse(text = quizText))
                        }
                    }
                }
            }
        }else if(message.type == ChatRoomType.AdminRoom){
            if(message is Message.Talk) {
                when {
                    message.text.startsWith(".퀴즈추가 ") -> addQuiz(message)
                    message.text.startsWith(".퀴즈 ") -> showQuiz(message)
                    message.text.startsWith(".퀴즈삭제 ") -> deleteQuiz(message)
                    message.text == ".퀴즈중지" -> {
                        isActive = false
                        commandChannel.send(AdminRoomTextResponse(text = "퀴즈가 중지됩니다."))
                    }
                    message.text == ".퀴즈재개" -> {
                        isActive = true
                        commandChannel.send(AdminRoomTextResponse(text = "퀴즈가 재개됩니다."))
                    }
                }
            }
        }
    }

    private suspend fun addQuiz(message : Message.Talk){
        val updateTime = System.currentTimeMillis()
        val cleaned = message.text.removePrefix(".퀴즈추가").trim()

        val lastColonIndex = cleaned.lastIndexOf(':')
        val (quiz, answer) =
            if (lastColonIndex != -1) {
                cleaned.substring(0, lastColonIndex).trim() to
                    cleaned.substring(lastColonIndex + 1).trim()
            } else {
                cleaned to ""   // 콜론이 없는 경우
            }

        if (quiz.isEmpty() || answer.isEmpty()) {
            commandChannel.send(AdminRoomTextResponse(text = "퀴즈 추가에 실패했습니다."))
        } else {
            val key = quizDatabaseDao.insertQuiz(
                QuizData(
                    updateTime = updateTime,
                    userKey = message.userId,
                    category = "일반",
                    difficulty = 1,
                    quiz = quiz,
                    answer = answer
                )
            )
            commandChannel.send(AdminRoomTextResponse(text = "퀴즈가 추가되었습니다. (key = $key)"))
        }
    }

    private suspend fun showQuiz(message : Message.Talk){
        val number = message.text.substringAfter(" ", "").toIntOrNull()?.toLong()
        val quiz = number?.let { quizDatabaseDao.getSelectQuiz(it) }

        val quizResultText = quiz?.let {
            val latestName = memberDatabaseDao.getMember(it.userKey).getOrNull(0)?.latestName?:"-"
            "${it.quiz}\n- 정답 : ${it.answer}\n\n⏱ ${it.updateTime.toTimeFormatDate()} - $latestName"
        } ?: "등록된 퀴즈가 없습니다."
        commandChannel.send(AdminRoomTextResponse(text = quizResultText))
    }

    private suspend fun deleteQuiz(message : Message.Talk){
        val number = message.text.substringAfter(" ", "").toIntOrNull()?.toLong()
        val quiz = number?.let { quizDatabaseDao.getSelectQuiz(it) }
        val quizDeleteText = if(quiz != null){
            quizDatabaseDao.deleteQuiz(number)
            "퀴즈가 삭제되었습니다. (key = $number)"
        }else{
            "등록된 퀴즈가 없습니다."
        }
        commandChannel.send(AdminRoomTextResponse(text = quizDeleteText))
    }


    suspend fun sendQuizStart(){
        if(!isActive) return

        currentQuiz = quizDatabaseDao.getRandomQuiz()
        currentQuiz?.let { quiz ->
            isQuiz = true

            val circles = quiz.answer.map { ch ->
                if (ch == ' ') ' ' else '○'
            }.joinToString("")
            val quizText = "[퀴즈]\n${quiz.quiz}\n\n힌트 : $circles"
            commandChannel.send(Group1RoomTextResponse(text = quizText))
        }
    }

    suspend fun sendQuizEnd() {
        if(isQuiz && isActive){
            currentQuiz?.let { quiz ->
                val quizText = "퀴즈를 맞춘 사람이 없습니다."
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
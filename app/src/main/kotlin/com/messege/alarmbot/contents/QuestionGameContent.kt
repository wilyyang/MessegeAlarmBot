package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.arrayOfPlaces
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.domain.model.ChatRoomKey
import com.messege.alarmbot.domain.model.Command
import com.messege.alarmbot.domain.model.GroupTextResponse
import com.messege.alarmbot.domain.model.None
import com.messege.alarmbot.domain.model.UserTextResponse
import kotlinx.coroutines.channels.Channel

data class UserKey(val name: String, val key: String)
class QuestionGameContent(override val commandChannel : Channel<Command>) : BaseContent{
    private var step = 0
    override val contentsName : String = "다섯고개"
    private var isStart = false
    private var hostName = ""
    private var hostKey = ""
    private var answer = ""
    private val randomJobs : MutableList<String> = mutableListOf()
    private val userAnswers : MutableList<Pair<UserKey, String>> = mutableListOf()

    override suspend fun request(chatRoomKey: ChatRoomKey, user : Person, text : String) {
        var command : Command = None
        if(!chatRoomKey.isGroupConversation){
            if(isStart && answer.isBlank() && chatRoomKey.roomName == hostName){
                command = if(randomJobs.contains(text)){
                    answer = text
                    GroupTextResponse(text = "$hostName 님이 정답을 정했습니다. \n$hostName 님에게 질문하고 채팅에 답을 올려주세요!")
                } else {
                    UserTextResponse(userName = hostName, text = "정답 후보에 들어있지 않아요.")
                }
            }
        }else{
            when(text){
                hostKeyword + contentsName -> {
                    command = if(isStart){
                        GroupTextResponse(text = "[이미 게임 진행 중입니다.]\n\n${answerProgressToString()}")
                    }else{
                        startGame(host = user)
                        GroupTextResponse(text = "[다섯 고개 게임 시작]\n\n" +
                                "* 호스트: ${user.name}\n\n" +
                                "정답 후보 : ${randomJobs.joinToString(", ")}\n\n" +
                                "* $hostName 님은 저에게 갠톡으로 정답을 정해주세요!")
                    }
                }
                "$hostKeyword${contentsName} 종료",
                "$hostKeyword${contentsName}종료" -> {
                    command = if(isStart){
                        clearGame()
                        GroupTextResponse(text = "[다섯 고개 종료]")
                    }else{
                        GroupTextResponse(text = "[게임이 없습니다.]")
                    }
                }
                "$hostKeyword${contentsName} 규칙",
                "$hostKeyword${contentsName}규칙" -> {
                    command = GroupTextResponse(
                        text = "[다섯 고개 규칙]\n\n" +
                                "1. 6명의 인원이 참가한다.\n" +
                                "2. 같은 카테고리의 10개 단어가 나열된다.\n" +
                                "3. 호스트는 10개 중 하나를 선택한다.\n" +
                                "4. 5명이 순서대로 질문과 정답확인을 할 수 있다.\n" +
                                "5. 질문과 정답확인 기회는 1회뿐이다.\n" +
                                "6. 호스트가 5개의 질문 중 최대 1번 거짓말을 할수있다."
                    )
                }

                else -> {
                    val isAlreadyUser = false//userAnswers.firstOrNull { it.first == userName } != null
                    val isAlreadyAnswer = userAnswers.firstOrNull{ it.second == text } != null
                    if (answer.isNotBlank() && (randomJobs.contains(text) || isAlreadyAnswer)
                        && !isAlreadyUser
                        && user.key != hostKey) {

                        command = if(answer == text){
                            val response = "[${user.name} 님이 정답을 맞추셨습니다! 정답은 $text 입니다.]\n\n${answerProgressToString()}"
                            clearGame()
                            GroupTextResponse(text = response)
                        }else if(isAlreadyAnswer){
                            GroupTextResponse(text = "[${user.name} 님이 말한 $text 는 이미 말한 답입니다.]\n\n${answerProgressToString()}")
                        }else {
                            ++step
                            userAnswers.add(UserKey(name = "${user.name}", key = "${user.key}") to text)
                            var response = "${user.name} 님의 $text 는 정답이 아닙니다."
                            if(step > 4){
                                response += "\n 모든 기회를 소진하여 $hostName 님이 이겼습니다. 정답은 $answer 입니다.\n\n${answerProgressToString()}"
                                clearGame()
                                GroupTextResponse(text = response)
                            }else{
                                GroupTextResponse(text = "$response\n\n${answerProgressToString()}")
                            }
                        }
                    }
                }
            }
        }
        commandChannel.send(command)
    }

    private fun answerProgressToString() : String{
        var result = "오답 : \n"
        result += userAnswers.map { "- ${it.first.name} : ${it.second}" }.joinToString("\n")
        randomJobs.removeAll(userAnswers.map { it.second })
        val current = randomJobs.joinToString(", ")
        result += "\n\n후보 : $current\n"
        return result
    }

    private fun startGame(host : Person){
        this.step = 0
        this.isStart = true
        this.hostName = host.name.toString()
        this.hostKey = host.key.toString()
        this.randomJobs.clear()
        this.randomJobs.addAll(arrayOfPlaces.toList().shuffled().take(10))
        this.userAnswers.clear()
    }

    private fun clearGame(){
        step = 0
        isStart = false
        hostName = ""
        hostKey = ""
        answer = ""
        randomJobs.clear()
        userAnswers.clear()
    }
}
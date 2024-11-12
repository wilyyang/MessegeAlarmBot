package com.messege.alarmbot.contents

import android.app.Person
import com.messege.alarmbot.core.common.helpKeyword
import com.messege.alarmbot.core.common.hostKeyword
import com.messege.alarmbot.core.common.ChatRoomKey
import com.messege.alarmbot.domain.model.Command
import com.messege.alarmbot.domain.model.MainChatTextResponse
import com.messege.alarmbot.domain.model.None
import kotlinx.coroutines.channels.Channel

class CommonContent(override val commandChannel : Channel<Command>) : BaseContent{
    override val contentsName : String = "기본"
    private val userMap : MutableMap<String, MutableList<String>> = mutableMapOf()

    override suspend fun request(chatRoomKey: ChatRoomKey, user : Person, text : String) {
        val command = when(text){
            hostKeyword + helpKeyword -> MainChatTextResponse(text = "안녕하세요? 빵구봇입니다.\n\n" +
                "* 현재 사용 가능한 명령어\n\n" +
                "- t : 3분 측정 (1분 전 알림)\n"+
                "- t {분} : 분만큼 측정\n"+
                "- t @{닉네임}\n"+
                "- t @{닉네임} {분}\n"+
                "- t e : 측정 종료(측정 중일 때)\n\n"+
                "- .다섯고개\n"+
                "- .다섯고개규칙\n"+
                "- .다섯고개종료\n")
            else -> {
                val currentName = "${user.name}"
                val currentKey = "${user.key}"
                val userNameList = userMap[currentKey]
                if(userNameList.isNullOrEmpty()){
                    userMap[currentKey] = mutableListOf(currentName)
                    None
                }else if(userNameList.last() != currentName){
                    var test = ""
                    for(temp in userNameList){
                        test += temp+ ", "
                    }
                    userMap[currentKey]!!.add(currentName)
                    MainChatTextResponse(text = "${user.name}님 안녕하세요. ${user.name}님은 이전에 ${test}로 오셨군요")
                }else{
                    None
                }
            }
        }
        commandChannel.send(command)
    }
}
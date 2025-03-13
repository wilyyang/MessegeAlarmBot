package com.messege.alarmbot.contents.party

import com.messege.alarmbot.contents.BaseContent
import com.messege.alarmbot.contents.point.PartyKeyword
import com.messege.alarmbot.core.common.ChatRoomType
import com.messege.alarmbot.core.common.Rank
import com.messege.alarmbot.data.database.member.dao.MemberDatabaseDao
import com.messege.alarmbot.processor.model.Command
import com.messege.alarmbot.processor.model.Group1RoomTextResponse
import com.messege.alarmbot.processor.model.Message
import com.messege.alarmbot.processor.usecase.PartyCreateResult
import com.messege.alarmbot.processor.usecase.PartyDissolveResult
import com.messege.alarmbot.processor.usecase.PartyMemberEvent
import com.messege.alarmbot.processor.usecase.UseCaseParty
import com.messege.alarmbot.util.format.toTimeFormatDate
import com.messege.alarmbot.util.log.Logger
import kotlinx.coroutines.channels.Channel

class PartyContent (
    override val commandChannel: Channel<Command>,
    private val memberDatabaseDao : MemberDatabaseDao,
    private val useCaseParty: UseCaseParty
) : BaseContent {
    override val contentsName : String = "정당"

    override suspend fun request(message : Message) {
        if(message.type == ChatRoomType.GroupRoom1 && message is Message.Talk) {
            val member = memberDatabaseDao.getMember(message.userId).getOrNull(0)
            if(member == null || Rank.getRankByName(member.rank).rank < 0){
                commandChannel.send(Group1RoomTextResponse("랭크가 낮아서 기능을 사용할 수 없습니다."))
            }else{
                when{
                    // Party
                    message.text.startsWith(".${PartyKeyword.PARTY_CREATE} ") -> {
                        val args = message.text.split(" ", limit = 2)
                        if (args.size > 1) {
                            val partyName = args[1]
                            val result = useCaseParty.createParty(member, partyName)
                            val response = when(result){
                                is PartyCreateResult.CreateSuccess -> {
                                    "창당에 성공했습니다!! 정당 이름은 $partyName 입니다. (help : .? 정당)"
                                }
                                is PartyCreateResult.AlreadyPartyMember -> {
                                    "이미 정당에 가입해 있습니다. 탈당 후 창당하세요."
                                }

                                is PartyCreateResult.AlreadyPartyNameExist -> {
                                    "기존 정당 이름에 $partyName 이 있습니다. 다른 이름을 사용하세요."
                                }
                            }
                            commandChannel.send(Group1RoomTextResponse(response))

                        } else responseTextFormatInvalid()
                    }
                    message.text == ".${PartyKeyword.PARTY_DISSOLVE}" -> {
                        val result = useCaseParty.dissolveParty(member)
                        val response = when(result){
                            is PartyDissolveResult.DissolveSuccess -> {
                                "${result.party.name} 정당을 해산했습니다. 모든 당원의 당적이 초기화되었습니다."
                            }
                            is PartyDissolveResult.DissolveFail -> {
                                "정당을 해산할 권리가 없습니다."
                            }
                        }
                        commandChannel.send(Group1RoomTextResponse(response))

                    }
                    message.text.startsWith(".${PartyKeyword.PARTY_INFO} ") -> {
                        val args = message.text.split(" ", limit = 2)
                        if (args.size > 1) {
                            val partyName = args[1]
                            val result = useCaseParty.getPartyInfo(partyName)


                            val response = if(result != null){
                                val members = result.partyMembers.joinToString(", "){
                                    it.latestName
                                }

                                "[${result.partyInfo.name} 정보]\n" +
                                        "- 창당일 : ${result.partyInfo.foundingTime.toTimeFormatDate()}\n" +
                                        "- 당대표 : ${result.partyLeader.latestName}\n" +
                                        "- 당원수 : ${result.partyInfo.memberCount}명\n" +
                                        "- 당소개 : ${result.partyInfo.description}\n" +
                                        "- 당원 : $members"

                            }else{
                                "정당 정보가 없습니다."
                            }

                            commandChannel.send(Group1RoomTextResponse(response))
                        } else responseTextFormatInvalid()
                    }
                    message.text == ".${PartyKeyword.PARTY_RANKING}" -> {
                        val result = useCaseParty.getPartyRanking()

                    }
                    message.text.startsWith(".${PartyKeyword.PARTY_RENAME} ") -> {
                        val args = message.text.split(" ", limit = 2)
                        if (args.size > 1) {
                            val newName = args[1]
                            val result = useCaseParty.renameParty(member, newName)

                        } else responseTextFormatInvalid()
                    }
                    message.text.startsWith(".${PartyKeyword.PARTY_DESCRIPTION} ") -> {
                        val args = message.text.split(" ", limit = 2)
                        if (args.size > 1) {
                            val description = args[1]
                            val result = useCaseParty.updatePartyDescription(member, description)
                        } else responseTextFormatInvalid()
                    }

                    // Party Rule
                    message.text.startsWith(".${PartyKeyword.PARTY_RULE_ADD} ") -> {
                        val args = message.text.split(" ", limit = 2)
                        if (args.size > 1) {
                            val rule = args[1]
                            val result = useCaseParty.addPartyRule(member, rule)


                        } else responseTextFormatInvalid()
                    }
                    message.text.startsWith(".${PartyKeyword.PARTY_RULE_REMOVE} ") -> {
                        val args = message.text.split(" ", limit = 2)
                        if (args.size > 1) {
                            val ruleNumber = args[1].toIntOrNull()
                            if (ruleNumber == null) {
                                responseTextFormatInvalid()
                            } else{
                                val result = useCaseParty.removePartyRule(member, ruleNumber)
                            }

                        } else responseTextFormatInvalid()
                    }
                    message.text.startsWith(".${PartyKeyword.PARTY_RULES} ") -> {
                        val args = message.text.split(" ", limit = 2)
                        if (args.size > 1) {
                            val partyName = args[1]
                            val result = useCaseParty.getPartyRules(partyName)

                        } else responseTextFormatInvalid()
                    }

                    // Party Member
                    message.text.startsWith(".${PartyKeyword.PARTY_MEM_DELEGATE} ") -> {
                        val mentionId = message.mentionIds.getOrNull(0) // targetId
                        if (mentionId == null) {
                            responseTextFormatInvalid()
                        } else{
                            val result = useCaseParty.delegateLeader(member, mentionId)
                            val response = when(result){
                                is PartyMemberEvent.Success -> {
                                    "[${PartyKeyword.PARTY_MEM_DELEGATE}] 했습니다."
                                }
                                else -> {
                                    "[${PartyKeyword.PARTY_MEM_DELEGATE}] 실패 했습니다."
                                }
                            }
                            commandChannel.send(Group1RoomTextResponse(response))
                        }
                    }
                    message.text.startsWith(".${PartyKeyword.PARTY_MEM_JOIN} ") -> {
                        val args = message.text.split(" ", limit = 2)
                        if (args.size > 1) {
                            val partyName = args[1]
                            val result = useCaseParty.requestToJoinParty(member, partyName)
                            val response = when(result){
                                is PartyMemberEvent.Success -> {
                                    "[${PartyKeyword.PARTY_MEM_JOIN}] 했습니다."
                                }
                                else -> {
                                    "[${PartyKeyword.PARTY_MEM_JOIN}] 실패 했습니다."
                                }
                            }
                            commandChannel.send(Group1RoomTextResponse(response))
                        } else responseTextFormatInvalid()
                    }
                    message.text == ".${PartyKeyword.PARTY_MEM_CANCEL}" -> {
                        val result = useCaseParty.cancelJoinRequest(member)
                        val response = when(result){
                            is PartyMemberEvent.Success -> {
                                "[${PartyKeyword.PARTY_MEM_CANCEL}] 했습니다."
                            }
                            else -> {
                                "[${PartyKeyword.PARTY_MEM_CANCEL}] 실패 했습니다."
                            }
                        }
                        commandChannel.send(Group1RoomTextResponse(response))
                    }
                    message.text.startsWith(".${PartyKeyword.PARTY_MEM_APPROVE} ") -> {
                        val mentionId = message.mentionIds.getOrNull(0) // targetId
                        if (mentionId == null) {
                            responseTextFormatInvalid()
                        }else {
                            val result = useCaseParty.approveMemberRequest(member, mentionId)
                            val response = when(result){
                                is PartyMemberEvent.Success -> {
                                    "[${PartyKeyword.PARTY_MEM_APPROVE}] 했습니다."
                                }
                                else -> {
                                    "[${PartyKeyword.PARTY_MEM_APPROVE}] 실패 했습니다."
                                }
                            }
                            commandChannel.send(Group1RoomTextResponse(response))
                        }
                    }
                    message.text.startsWith(".${PartyKeyword.PARTY_MEM_REJECT} ") -> {
                        val mentionId = message.mentionIds.getOrNull(0) // targetId
                        if (mentionId == null) {
                            responseTextFormatInvalid()
                        } else {
                            val result = useCaseParty.rejectMemberRequest(member, mentionId)
                            val response = when(result){
                                is PartyMemberEvent.Success -> {
                                    "[${PartyKeyword.PARTY_MEM_REJECT}] 했습니다."
                                }
                                else -> {
                                    "[${PartyKeyword.PARTY_MEM_REJECT}] 실패 했습니다."
                                }
                            }
                            commandChannel.send(Group1RoomTextResponse(response))
                        }
                    }
                    message.text == ".${PartyKeyword.PARTY_MEM_LEAVE}" -> {
                        val result = useCaseParty.leaveParty(member)
                        val response = when(result){
                            is PartyMemberEvent.Success -> {
                                "[${PartyKeyword.PARTY_MEM_LEAVE}] 했습니다."
                            }
                            else -> {
                                "[${PartyKeyword.PARTY_MEM_LEAVE}] 실패 했습니다."
                            }
                        }
                        commandChannel.send(Group1RoomTextResponse(response))

                    }
                    message.text.startsWith(".${PartyKeyword.PARTY_MEM_EXPULSION} ") -> {
                        val mentionId = message.mentionIds.getOrNull(0) // targetId
                        if (mentionId == null) {
                            responseTextFormatInvalid()
                        } else {
                            val result = useCaseParty.expelMember(member, mentionId)
                            val response = when(result){
                                is PartyMemberEvent.Success -> {
                                    "[${PartyKeyword.PARTY_MEM_EXPULSION}] 했습니다."
                                }
                                else -> {
                                    "[${PartyKeyword.PARTY_MEM_EXPULSION}] 실패 했습니다."
                                }
                            }
                            commandChannel.send(Group1RoomTextResponse(response))
                        }
                    }

                    // Party Request
                    message.text == ".${PartyKeyword.PARTY_JOIN_REQUEST} " -> {
                        val args = message.text.split(" ", limit = 2)
                        if (args.size > 1) {
                            val partyName = args[1]
                            val result = useCaseParty.getJoinRequests(partyName)

                        } else responseTextFormatInvalid()
                    }
                    else -> {}
                }
            }
        }
    }

    suspend fun responseTextFormatInvalid(){
        commandChannel.send(Group1RoomTextResponse("입력 텍스트가 올바르지 않습니다."))
    }
}
package com.messege.alarmbot.contents.common

enum class SanctionType(val prefix : String, val count : Int, val isRemission : Boolean = false){
    Sanction(".제재", 1),
    Warning(".경고", 2),
    Remission(".감면", -1, true),
    Kick(".강퇴", -4, true)
}

data class SanctionWrapper(
    val targetUser: String,
    val sanctionCount: Int,
    val reason: String?
)

fun parseSanction(input: String, sanctionType: SanctionType, targetMemberName: String): SanctionWrapper? {
    if(!input.contains(sanctionType.prefix)) return null
    if(!input.contains("@${targetMemberName}")) return null

    var sanctionCount = sanctionType.count
    var reason: String? = null

    val replaceText = input.replace(sanctionType.prefix, "").replace("@$targetMemberName", "")
    val parts = replaceText.split(" ").filter { it.isNotBlank() }

    if (parts.isNotEmpty()) {
        val possibleNumber = parts[0].toIntOrNull()
        if (possibleNumber != null && possibleNumber > 0) {
            sanctionCount = possibleNumber * sanctionType.count
            reason = parts.drop(1).joinToString(" ").ifBlank { null }
        } else {
            // 숫자가 없으면 그대로 reason으로 설정
            reason = parts.joinToString(" ").ifBlank { null }
        }
    }

    return SanctionWrapper(
        targetUser = targetMemberName,
        sanctionCount = sanctionCount,
        reason = reason
    )
}
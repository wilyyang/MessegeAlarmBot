package com.messege.alarmbot.util.format

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toTimeFormat() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(this))

fun Long.toTimeFormatHHmm() = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()).format(Date(this))

fun replyFormat(text : String) = "┏ 빵구봇 ━━━━━━\n\n$text\n\n┗━━━━━━━━━"
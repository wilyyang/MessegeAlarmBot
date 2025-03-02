package com.messege.alarmbot.util.format

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toTimeFormat() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(this))

fun Long.toTimeFormatDate() = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(this))
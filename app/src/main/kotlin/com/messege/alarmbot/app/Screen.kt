package com.messege.alarmbot.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.saveable.rememberSaveable

private const val DEFAULT_GROUP_1_KEY = 18470291424463783L
private const val DEFAULT_GROUP_2_KEY = 18470291424463783L
private const val DEFAULT_ADMIN_KEY   = 18470647988050317L
private const val DEFAULT_SUPER_ADMIN_ME = 6155898534804297193L

// =========================
// 2) 전역 "적용된 값" (토글 ON이면 여기 값이 바뀜)
//    확장성 필요 없다 했으니 전역으로 그냥 둠
// =========================
@Volatile var GROUP_1_KEY_APPLIED: Long = DEFAULT_GROUP_1_KEY
@Volatile var GROUP_2_KEY_APPLIED: Long = DEFAULT_GROUP_2_KEY
@Volatile var ADMIN_KEY_APPLIED: Long   = DEFAULT_ADMIN_KEY
@Volatile var SUPER_ADMIN_ME_APPLIED: Long = DEFAULT_SUPER_ADMIN_ME

// =========================
// 3) 수집 메시지(최대 10개만 보관/표시)
// =========================
val lastMessages = mutableStateListOf<String>()

@Composable
fun BotConfigScreen() {
    // 입력값(문자열) + 적용 토글
    var group1Text by rememberSaveable { mutableStateOf(DEFAULT_GROUP_1_KEY.toString()) }
    var group2Text by rememberSaveable { mutableStateOf(DEFAULT_GROUP_2_KEY.toString()) }
    var adminText  by rememberSaveable { mutableStateOf(DEFAULT_ADMIN_KEY.toString()) }
    var superText  by rememberSaveable { mutableStateOf(DEFAULT_SUPER_ADMIN_ME.toString()) }

    var applyGroup1 by rememberSaveable { mutableStateOf(true) }
    var applyGroup2 by rememberSaveable { mutableStateOf(true) }
    var applyAdmin  by rememberSaveable { mutableStateOf(true) }
    var applySuper  by rememberSaveable { mutableStateOf(true) }

    // 토글/입력값이 바뀔 때마다 전역 적용값 갱신
    LaunchedEffect(applyGroup1) {
        if (applyGroup1) {
            GROUP_1_KEY_APPLIED = group1Text.toLongOrNull() ?: 0L
        }
    }

    LaunchedEffect(applyGroup2) {
        if (applyGroup2) {
            GROUP_2_KEY_APPLIED = group2Text.toLongOrNull() ?: 0L
        }
    }

    LaunchedEffect(applyAdmin) {
        if (applyAdmin) {
            ADMIN_KEY_APPLIED = adminText.toLongOrNull() ?: 0L
        }
    }

    LaunchedEffect(applySuper) {
        if (applySuper) {
            SUPER_ADMIN_ME_APPLIED = superText.toLongOrNull() ?: 0L
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Message Alarm Bot", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        ConfigRow(
            label = "GROUP_1_KEY",
            text = group1Text,
            onTextChange = { group1Text = it },
            applied = applyGroup1,
            onAppliedChange = { applyGroup1 = it },
        )
        Spacer(Modifier.height(8.dp))

        ConfigRow(
            label = "GROUP_2_KEY",
            text = group2Text,
            onTextChange = { group2Text = it },
            applied = applyGroup2,
            onAppliedChange = { applyGroup2 = it },
        )
        Spacer(Modifier.height(8.dp))

        ConfigRow(
            label = "ADMIN_KEY",
            text = adminText,
            onTextChange = { adminText = it },
            applied = applyAdmin,
            onAppliedChange = { applyAdmin = it },
        )
        Spacer(Modifier.height(8.dp))

        ConfigRow(
            label = "SUPER_ADMIN_ME",
            text = superText,
            onTextChange = { superText = it },
            applied = applySuper,
            onAppliedChange = { applySuper = it },
        )

        Spacer(Modifier.height(16.dp))

        // 현재 적용값 요약(디버그용)
        Text(
            text = "APPLIED: G1=$GROUP_1_KEY_APPLIED, G2=$GROUP_2_KEY_APPLIED, ADMIN=$ADMIN_KEY_APPLIED, SUPER=$SUPER_ADMIN_ME_APPLIED",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(16.dp))

        Divider()
        Spacer(Modifier.height(8.dp))

        Text("Last 10 Messages", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(lastMessages) { msg ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    SelectionContainer {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigRow(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    applied: Boolean,
    onAppliedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = text,
            enabled = !applied,
            onValueChange = onTextChange,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("APPLY", style = MaterialTheme.typography.labelSmall)
            Switch(
                checked = applied,
                onCheckedChange = onAppliedChange
            )
        }
    }
}
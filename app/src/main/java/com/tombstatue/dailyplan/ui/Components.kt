package com.tombstatue.dailyplan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tombstatue.dailyplan.ui.theme.Accent
import com.tombstatue.dailyplan.ui.theme.CardBorder
import com.tombstatue.dailyplan.ui.theme.SheetBg
import com.tombstatue.dailyplan.ui.theme.TextDim
import com.tombstatue.dailyplan.ui.theme.TextMain
import com.tombstatue.dailyplan.ui.theme.UndoneRed

/** 底部文字输入弹层：今天页添加计划、日详情页添加日程/事件共用 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputSheet(
    title: String,
    placeholder: String = "写下你的计划…",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SheetBg) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .imePadding()
        ) {
            Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = TextDim) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain,
                    cursorColor = Accent
                )
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = TextDim) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(text) },
                    enabled = text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) { Text("添加") }
            }
        }
    }
}

/** 删除确认框：今天页与日详情页共用 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        title = { Text(title, color = TextMain, fontSize = 17.sp) },
        text = { Text(content, color = TextDim) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除", color = UndoneRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextDim) }
        }
    )
}

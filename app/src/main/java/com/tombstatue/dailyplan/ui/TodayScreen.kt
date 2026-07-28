package com.tombstatue.dailyplan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tombstatue.dailyplan.data.Period
import com.tombstatue.dailyplan.data.Task
import com.tombstatue.dailyplan.ui.theme.Accent
import com.tombstatue.dailyplan.ui.theme.AllDoneGreen
import com.tombstatue.dailyplan.ui.theme.CardBg
import com.tombstatue.dailyplan.ui.theme.CardBorder
import com.tombstatue.dailyplan.ui.theme.SheetBg
import com.tombstatue.dailyplan.ui.theme.TextDim
import com.tombstatue.dailyplan.ui.theme.TextMain
import com.tombstatue.dailyplan.ui.theme.TextStruck
import com.tombstatue.dailyplan.ui.theme.UndoneRed
import com.tombstatue.dailyplan.ui.theme.emoji
import com.tombstatue.dailyplan.ui.theme.label
import com.tombstatue.dailyplan.ui.theme.tint
import java.time.LocalDate

/** "2026-07-19" → "7月19日 · 周日" */
fun formatChineseDate(isoDate: String): String {
    val d = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val week = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[d.dayOfWeek.value - 1]
    return "${d.monthValue}月${d.dayOfMonth}日 · $week"
}

@Composable
fun TodayScreen(vm: PlanViewModel, padding: PaddingValues, onStartPomodoro: (Task) -> Unit = {}) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val today = ui.today
    var addTarget by remember { mutableStateOf<Period?>(null) }   // 弹层要添加到哪个板块
    var actionTask by remember { mutableStateOf<Task?>(null) }    // 长按待操作的任务

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(formatChineseDate(today.logicalDate), color = TextDim, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("今日计划", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                val done = today.tasks.count { it.done }
                if (today.tasks.isNotEmpty()) {
                    Text(
                        "$done / ${today.tasks.size}",
                        color = if (done == today.tasks.size) AllDoneGreen else Accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // 当天的重要事件：顶部加粗横幅
            today.events.forEach { event ->
                Text(
                    "📌 ${event.text}",
                    color = UndoneRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        Period.entries.forEach { period ->
            item(key = period.name) {
                SectionCard(
                    period = period,
                    tasks = today.tasks.filter { it.period == period },
                    onAdd = { addTarget = period },
                    onToggle = vm::toggle,
                    onLongPress = { actionTask = it }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    addTarget?.let { period ->
        TextInputSheet(
            title = "添加到 ${period.emoji} ${period.label}",
            onConfirm = { text ->
                vm.add(period, text)
                addTarget = null
            },
            onDismiss = { addTarget = null }
        )
    }

    actionTask?.let { task ->
        AlertDialog(
            onDismissRequest = { actionTask = null },
            containerColor = SheetBg,
            title = { Text(task.text, color = TextMain, fontSize = 16.sp) },
            text = {
                Column {
                    if (!task.done) {
                        TextButton(
                            onClick = {
                                onStartPomodoro(task)
                                actionTask = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("🍅 开始专注", color = Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                    }
                    TextButton(
                        onClick = {
                            vm.delete(task.id)
                            actionTask = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("删除", color = UndoneRed, fontSize = 15.sp) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { actionTask = null }) { Text("取消", color = TextDim) } }
        )
    }
}

@Composable
private fun SectionCard(
    period: Period,
    tasks: List<Task>,
    onAdd: () -> Unit,
    onToggle: (String) -> Unit,
    onLongPress: (Task) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${period.emoji} ${period.label}",
                    color = period.tint,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text("＋", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (tasks.isEmpty()) {
                Text(
                    "点右上角 ＋ 添加计划",
                    color = TextDim.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
            } else {
                tasks.forEach { task -> TaskRow(task, onToggle, onLongPress) }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(task: Task, onToggle: (String) -> Unit, onLongPress: (Task) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onToggle(task.id) },     // 点击：划掉 / 恢复
                onLongClick = { onLongPress(task) }  // 长按：删除
            )
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (task.done) "✓" else "○",
            color = if (task.done) TextStruck else Accent,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            task.text,
            color = if (task.done) TextStruck else TextMain,
            fontSize = 15.sp,
            // 提前规划的日程加粗显示，与当天临时添加的区分
            fontWeight = if (task.fromPlan) FontWeight.Bold else FontWeight.Normal,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
    }
}

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tombstatue.dailyplan.data.DayPlan
import com.tombstatue.dailyplan.data.Event
import com.tombstatue.dailyplan.data.Period
import com.tombstatue.dailyplan.data.Task
import com.tombstatue.dailyplan.ui.theme.Accent
import com.tombstatue.dailyplan.ui.theme.AllDoneGreen
import com.tombstatue.dailyplan.ui.theme.CardBg
import com.tombstatue.dailyplan.ui.theme.CardBorder
import com.tombstatue.dailyplan.ui.theme.TextDim
import com.tombstatue.dailyplan.ui.theme.TextMain
import com.tombstatue.dailyplan.ui.theme.TextStruck
import com.tombstatue.dailyplan.ui.theme.UndoneRed
import com.tombstatue.dailyplan.ui.theme.emoji
import com.tombstatue.dailyplan.ui.theme.label
import com.tombstatue.dailyplan.ui.theme.tint

/**
 * 日详情页（全屏子页面）：
 * - 未来日期：可添加/长按删除重要事件与三时段日程，不能打勾
 * - 过去日期：只读，未完成在上、已完成在下
 */
@Composable
fun DayDetailScreen(
    vm: PlanViewModel,
    ui: UiState,
    date: String,
    padding: PaddingValues,
    onBack: () -> Unit
) {
    val isFuture = date > ui.today.logicalDate
    val plan = ui.plans.find { it.date == date } ?: DayPlan(date)
    val record = ui.history.find { it.date == date }

    // 未来日期的编辑状态
    var addEvent by remember { mutableStateOf(false) }
    var addPeriod by remember { mutableStateOf<Period?>(null) }
    var delEvent by remember { mutableStateOf<Event?>(null) }
    var delTask by remember { mutableStateOf<Task?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("‹ 返回日历", color = Accent, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatChineseDate(date),
                    color = TextMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (!isFuture && record != null && record.tasks.isNotEmpty()) {
                    val done = record.tasks.count { it.done }
                    Text(
                        "$done / ${record.tasks.size}",
                        color = if (done == record.tasks.size) AllDoneGreen else TextDim,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (isFuture) {
            // ---------- 未来日期：可编辑 ----------
            item {
                EventCard(
                    events = plan.events,
                    onAdd = { addEvent = true },
                    onLongPress = { delEvent = it }
                )
                Spacer(Modifier.height(10.dp))
            }
            Period.entries.forEach { period ->
                item(key = period.name) {
                    PlanSectionCard(
                        period = period,
                        tasks = plan.tasks.filter { it.period == period },
                        onAdd = { addPeriod = period },
                        onLongPress = { delTask = it }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            item {
                Text(
                    "长按可删除 · 未来的日程不能打勾",
                    color = TextDim.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        } else {
            // ---------- 过去日期：只读 ----------
            if (record == null) {
                item { Text("这一天没有记录", color = TextDim, fontSize = 14.sp) }
            } else {
                item {
                    record.events.forEach { event ->
                        Text(
                            "📌 ${event.text}",
                            color = UndoneRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    if (record.events.isNotEmpty()) Spacer(Modifier.height(6.dp))

                    val undone = record.tasks.filter { !it.done }
                    val done = record.tasks.filter { it.done }
                    if (undone.isNotEmpty()) {
                        PastGroupCard(title = "未完成 (${undone.size})", titleColor = UndoneRed) {
                            undone.forEach { t ->
                                Text(
                                    "○ ${t.text} ${t.period.emoji}",
                                    color = Color(0xFF8A8A9A),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    if (done.isNotEmpty()) {
                        PastGroupCard(title = "已完成 (${done.size})", titleColor = AllDoneGreen) {
                            done.forEach { t ->
                                Text(
                                    "✓ ${t.text} ${t.period.emoji}",
                                    color = TextStruck,
                                    fontSize = 14.sp,
                                    textDecoration = TextDecoration.LineThrough,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                    if (record.tasks.isEmpty()) {
                        Text("这一天只有事件，没有日程记录", color = TextDim, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // ---------- 弹层与确认框 ----------
    if (addEvent) {
        TextInputSheet(
            title = "添加重要事件（会显示在日历上）",
            placeholder = "例如：高数期中考试",
            onConfirm = { vm.addPlanEvent(date, it); addEvent = false },
            onDismiss = { addEvent = false }
        )
    }
    addPeriod?.let { period ->
        TextInputSheet(
            title = "添加到 ${period.emoji} ${period.label}",
            onConfirm = { vm.addPlanTask(date, period, it); addPeriod = null },
            onDismiss = { addPeriod = null }
        )
    }
    delEvent?.let { event ->
        ConfirmDeleteDialog(
            title = "删除这个事件？",
            content = event.text,
            onConfirm = { vm.deletePlanEvent(date, event.id); delEvent = null },
            onDismiss = { delEvent = null }
        )
    }
    delTask?.let { task ->
        ConfirmDeleteDialog(
            title = "删除这条日程？",
            content = task.text,
            onConfirm = { vm.deletePlanTask(date, task.id); delTask = null },
            onDismiss = { delTask = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventCard(
    events: List<Event>,
    onAdd: () -> Unit,
    onLongPress: (Event) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, UndoneRed)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "📌 重要事件",
                    color = UndoneRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onAdd, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text("＋", color = Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (events.isEmpty()) {
                Text(
                    "点右上角 ＋ 添加，事件会显示在日历上",
                    color = TextDim.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
            } else {
                events.forEach { event ->
                    Text(
                        event.text,
                        color = TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {}, onLongClick = { onLongPress(event) })
                            .padding(vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanSectionCard(
    period: Period,
    tasks: List<Task>,
    onAdd: () -> Unit,
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
                    "点 ＋ 添加日程",
                    color = TextDim.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
            } else {
                tasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {}, onLongClick = { onLongPress(task) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("·", color = Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(task.text, color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PastGroupCard(
    title: String,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(title, color = titleColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            content()
        }
    }
}

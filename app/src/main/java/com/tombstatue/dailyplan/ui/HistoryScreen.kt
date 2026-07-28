package com.tombstatue.dailyplan.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tombstatue.dailyplan.data.Task
import com.tombstatue.dailyplan.ui.theme.Accent
import com.tombstatue.dailyplan.ui.theme.AllDoneGreen
import com.tombstatue.dailyplan.ui.theme.CardBg
import com.tombstatue.dailyplan.ui.theme.CardBorder
import com.tombstatue.dailyplan.ui.theme.TextDim
import com.tombstatue.dailyplan.ui.theme.TextMain
import com.tombstatue.dailyplan.ui.theme.TextStruck
import com.tombstatue.dailyplan.ui.theme.UndoneBg
import com.tombstatue.dailyplan.ui.theme.UndoneRed
import com.tombstatue.dailyplan.ui.theme.emoji
import java.time.LocalDate

@Composable
fun HistoryScreen(vm: PlanViewModel, padding: PaddingValues) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val today = ui.today
    val reversedHistory = remember(ui.history) { ui.history.reversed() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("已完成", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
        }
        // 今天：只显示已划掉的任务
        item {
            DayCard(
                title = "${formatChineseDate(today.logicalDate)} · 今天",
                tasks = today.tasks,
                onlyDone = true,
                highlight = true,
                emptyHint = "今天还没有完成的计划"
            )
            Spacer(Modifier.height(12.dp))
        }
        // 历史：按日期倒序，完整显示（含未完成）
        if (ui.history.isEmpty()) {
            item {
                Text(
                    "还没有历史记录，明天再来看看",
                    color = TextDim.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        } else {
            items(reversedHistory, key = { it.date }) { rec ->
                val title =
                    if (isYesterday(rec.date, today.logicalDate)) "${formatChineseDate(rec.date)} · 昨天"
                    else formatChineseDate(rec.date)
                DayCard(title = title, tasks = rec.tasks, onlyDone = false, highlight = false, emptyHint = null)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun isYesterday(date: String, todayDate: String): Boolean = runCatching {
    LocalDate.parse(date).plusDays(1) == LocalDate.parse(todayDate)
}.getOrDefault(false)

@Composable
private fun DayCard(
    title: String,
    tasks: List<Task>,
    onlyDone: Boolean,
    highlight: Boolean,
    emptyHint: String?
) {
    val done = tasks.count { it.done }
    val total = tasks.size
    val allDone = total > 0 && done == total

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, if (highlight) Accent else CardBorder)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row {
                Text(
                    title,
                    color = if (highlight) Accent else TextDim,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                if (total > 0) {
                    Text(
                        if (allDone) "$done / $total 🎉" else "$done / $total",
                        color = if (allDone) AllDoneGreen else TextDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            val shown = if (onlyDone) tasks.filter { it.done } else tasks
            if (shown.isEmpty() && emptyHint != null) {
                Text(
                    emptyHint,
                    color = TextDim.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            shown.forEach { task ->
                Row(
                    Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.done) {
                        Text(
                            "✓ ${task.text}",
                            color = TextStruck,
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(task.period.emoji, fontSize = 11.sp)
                    } else {
                        Text(
                            "○ ${task.text}",
                            color = TextDim,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(color = UndoneBg, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                "未完成",
                                color = UndoneRed,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

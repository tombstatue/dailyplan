package com.tombstatue.dailyplan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tombstatue.dailyplan.ui.theme.Accent
import com.tombstatue.dailyplan.ui.theme.TextDim
import com.tombstatue.dailyplan.ui.theme.TextMain
import com.tombstatue.dailyplan.ui.theme.UndoneBg
import com.tombstatue.dailyplan.ui.theme.UndoneRed
import java.time.LocalDate
import java.time.YearMonth

/** 非本月日期的灰显色 */
private val OutMonth = Color(0xFF3A3A4A)

/**
 * 日历页：月视图网格。
 * 点未来/过去日期 → 日详情；点今天 → 切回"今天"标签页。
 */
@Composable
fun CalendarScreen(vm: PlanViewModel, padding: PaddingValues, onGoToday: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val todayDate = ui.today.logicalDate
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }

    val selected = selectedDate
    if (selected == null) {
        MonthGrid(
            ui = ui,
            todayDate = todayDate,
            padding = padding,
            onSelect = { date ->
                if (date == todayDate) onGoToday() else selectedDate = date
            }
        )
    } else {
        BackHandler { selectedDate = null } // 系统返回键回到日历网格
        DayDetailScreen(vm, ui, selected, padding, onBack = { selectedDate = null })
    }
}

@Composable
private fun MonthGrid(
    ui: UiState,
    todayDate: String,
    padding: PaddingValues,
    onSelect: (String) -> Unit
) {
    var month by rememberSaveable { mutableStateOf(todayDate.substring(0, 7)) } // "2026-07"
    val ym = YearMonth.parse(month)

    // 各日期的事件文字：未来看规划、今天看今日状态、过去看归档
    val eventsByDate = remember(ui) {
        buildMap<String, List<String>> {
            ui.plans.forEach { p -> if (p.events.isNotEmpty()) put(p.date, p.events.map { it.text }) }
            ui.history.forEach { r -> if (r.events.isNotEmpty()) put(r.date, r.events.map { it.text }) }
            if (ui.today.events.isNotEmpty()) put(ui.today.logicalDate, ui.today.events.map { it.text })
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 标题栏：‹ 2026年7月 ›
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = ym.minusMonths(1).toString() }) {
                Text("‹", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "${ym.year}年${ym.monthValue}月",
                color = TextMain,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { month = ym.plusMonths(1).toString() }) {
                Text("›", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        // 星期表头，周一起始
        Row {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Text(
                    w,
                    color = TextDim,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // 6 行 × 7 列，首尾用相邻月补齐
        val first = ym.atDay(1)
        val start = first.minusDays((first.dayOfWeek.value - 1).toLong())
        val cells = (0 until 42).map { start.plusDays(it.toLong()) }
        cells.chunked(7).forEach { week ->
            Row {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        inMonth = YearMonth.from(day) == ym,
                        isToday = day.toString() == todayDate,
                        events = eventsByDate[day.toString()].orEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(day.toString()) }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "点日期查看/规划 · 今天会跳回「今天」页",
            color = TextDim.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    events: List<String>,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${day.dayOfMonth}",
            color = if (isToday) Color.White else if (inMonth) TextMain else OutMonth,
            fontSize = 13.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = if (isToday) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Accent)
                    .padding(horizontal = 8.dp, vertical = 1.dp)
            } else {
                Modifier.padding(vertical = 1.dp)
            }
        )
        // 事件文字条（B 方案）：显示第一条，多于一条时显示 +n
        if (events.isNotEmpty()) {
            Text(
                events.first(),
                color = UndoneRed,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.dp, vertical = 1.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(UndoneBg)
                    .padding(horizontal = 2.dp)
            )
            if (events.size > 1) {
                Text("+${events.size - 1}", color = TextDim, fontSize = 8.sp)
            }
        }
    }
}

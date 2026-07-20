package com.tombstatue.dailyplan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * 日历页：月视图网格 + 批量规划入口。
 */
@Composable
fun CalendarScreen(vm: PlanViewModel, padding: PaddingValues, onGoToday: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val todayDate = ui.today.logicalDate
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var showBatchSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 批量操作结果 → Snackbar + 撤销
    val batchResult by vm.batchResult.collectAsStateWithLifecycle()
    LaunchedEffect(batchResult) {
        if (batchResult != null) {
            val r = batchResult!!
            val action = snackbarHostState.showSnackbar(
                message = "已添加「${r.text}」至 ${r.count} 天",
                actionLabel = "撤销",
                duration = SnackbarDuration.Indefinite
            )
            if (action == SnackbarResult.ActionPerformed) {
                vm.undoBatch()
            }
            // 超时或撤销后清除
            vm.clearBatchResult()
        }
    }

    val selected = selectedDate
    Scaffold(
        modifier = Modifier.padding(padding),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (selected == null) {
            MonthGrid(
                ui = ui,
                todayDate = todayDate,
                onBatchClick = { showBatchSheet = true },
                onSelect = { date ->
                    if (date == todayDate) onGoToday() else selectedDate = date
                }
            )
        } else {
            BackHandler { selectedDate = null }
            DayDetailScreen(vm, ui, selected, innerPadding, onBack = { selectedDate = null })
        }
    }

    if (showBatchSheet) {
        BatchAddSheet(
            todayDate = todayDate,
            onConfirm = { dates, period, text ->
                vm.batchAdd(dates, period, text)
                showBatchSheet = false
            },
            onDismiss = { showBatchSheet = false }
        )
    }
}

@Composable
private fun MonthGrid(
    ui: UiState,
    todayDate: String,
    onBatchClick: () -> Unit,
    onSelect: (String) -> Unit
) {
    var month by rememberSaveable { mutableStateOf(todayDate.substring(0, 7)) }
    val ym = YearMonth.parse(month)

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
        // 批量规划按钮
        TextButton(
            onClick = onBatchClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text("📦 批量规划", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        // 星期表头
        Row {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                Text(w, color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        // 6 行 × 7 列
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
            "点日期查看/规划 · 「📦 批量规划」可在多天添加同一日程",
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

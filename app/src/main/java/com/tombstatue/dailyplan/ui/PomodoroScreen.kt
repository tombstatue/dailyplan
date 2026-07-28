package com.tombstatue.dailyplan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tombstatue.dailyplan.data.Task
import com.tombstatue.dailyplan.pomodoro.PomodoroMode
import com.tombstatue.dailyplan.pomodoro.PomodoroService
import com.tombstatue.dailyplan.pomodoro.TimerState
import com.tombstatue.dailyplan.ui.theme.Accent
import com.tombstatue.dailyplan.ui.theme.CardBg
import com.tombstatue.dailyplan.ui.theme.CardBorder
import com.tombstatue.dailyplan.ui.theme.SheetBg
import com.tombstatue.dailyplan.ui.theme.TextDim
import com.tombstatue.dailyplan.ui.theme.TextMain
import com.tombstatue.dailyplan.ui.theme.emoji
import com.tombstatue.dailyplan.ui.theme.label
import com.tombstatue.dailyplan.ui.theme.tint

private val WorkRed = Color(0xFFE94560)
private val BreakGreen = Color(0xFF4ECCA3)
private val PauseAmber = Color(0xFFF0A500)
private val RingBg = Color(0xFF2A2A3A)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PomodoroScreen(padding: PaddingValues, todayVm: PlanViewModel) {
    val vm: PomodoroViewModel = viewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    val ui by todayVm.ui.collectAsStateWithLifecycle()
    var showTaskPicker by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf<PomodoroMode?>(null) }
    var pickedMode by remember { mutableStateOf<PomodoroMode?>(null) }

    val ringColor = if (s.mode == PomodoroMode.WORK) WorkRed else BreakGreen

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 今日汇总
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("今日专注", color = TextDim, fontSize = 12.sp)
                Text(
                    formatDuration(s.focusSecToday),
                    color = ringColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("完成", color = TextDim, fontSize = 12.sp)
                Text("${s.sessionsToday} 次", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))

        // 任务绑定标签
        val task = vm.boundTask
        if (task != null) {
            Surface(
                color = CardBg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(task.period.emoji, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        task.text,
                        color = TextMain,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (s.sessionsToday > 0) {
                        Text("🍅×${s.sessionsToday}", color = TextDim, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                    }
                    TextButton(onClick = { vm.unbindTask() }, contentPadding = PaddingValues(4.dp)) {
                        Text("✕", color = TextDim, fontSize = 16.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 进度圆环
        val progress = if (s.totalSec > 0) s.remainingSec.toFloat() / s.totalSec else 0f
        BoxWithCircle(progress = progress, color = ringColor, size = 200.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    PomodoroService.formatTime(s.remainingSec),
                    color = TextMain,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    if (s.finished) {
                        if (s.mode == PomodoroMode.WORK) "完成！☕" else "休息结束 💪"
                    } else s.mode.label,
                    color = ringColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        // 模式切换
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PomodoroMode.entries.forEach { mode ->
                val selected = s.mode == mode && !s.finished
                Surface(
                    onClick = {
                        vm.setMode(mode)
                        pickedMode = mode
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) ringColor else CardBg,
                    border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = {
                                vm.setMode(mode)
                                pickedMode = mode
                            },
                            onLongClick = { showTimeDialog = mode }
                        )
                ) {
                    Text(
                        "${mode.label} ${mode.defaultMin}",
                        color = if (selected) Color.White else TextDim,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        // 按钮
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val isFinished = s.finished
            Surface(
                onClick = {
                    if (isFinished) {
                        // 完成后点按钮 → 重置该模式计时并从头开始
                        vm.reset()
                        vm.start()
                    } else if (s.running) vm.pause() else vm.start()
                },
                shape = RoundedCornerShape(12.dp),
                color = if (s.running) PauseAmber else ringColor
            ) {
                Text(
                    if (isFinished) "再来一次" else if (s.running) "暂停" else "开始",
                    color = if (s.running) Color(0xFF12121A) else Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 12.dp)
                )
            }
            Surface(
                onClick = { vm.reset() },
                shape = RoundedCornerShape(12.dp),
                color = CardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Text(
                    "重置",
                    color = TextDim,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // 选择任务入口（未绑定时）
        if (task == null) {
            TextButton(onClick = { showTaskPicker = true }) {
                Text("📋 选择今天的计划开始专注…", color = Accent, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.weight(1f))
    }

    // 任务选择器弹窗
    if (showTaskPicker) {
        val undone = ui.today.tasks.filter { !it.done }
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            containerColor = SheetBg,
            title = { Text("选择要专注的任务", color = TextMain, fontSize = 17.sp) },
            text = {
                if (undone.isEmpty()) {
                    Text("今天没有未完成的计划", color = TextDim)
                } else {
                    LazyColumn {
                        items(undone, key = { it.id }) { t ->
                            TextButton(
                                onClick = {
                                    vm.bindTask(t)
                                    showTaskPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "${t.period.emoji} ${t.text}",
                                    color = TextMain,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTaskPicker = false }) { Text("取消", color = TextDim) }
            }
        )
    }

    // 长按模式 → 自定义时长
    showTimeDialog?.let { mode ->
        var sliderValue by remember { mutableFloatStateOf(mode.defaultMin.toFloat()) }
        AlertDialog(
            onDismissRequest = { showTimeDialog = null },
            containerColor = SheetBg,
            title = { Text("自定义${mode.label}时长", color = TextMain, fontSize = 17.sp) },
            text = {
                Column {
                    Text("${sliderValue.toInt()} 分钟", color = ringColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 1f..120f,
                        steps = 23, // (120-1)/5 - 1 = ~23
                        colors = SliderDefaults.colors(thumbColor = ringColor, activeTrackColor = ringColor)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val min = sliderValue.toInt().coerceIn(1, 120)
                    vm.setMode(mode, min)
                    showTimeDialog = null
                }) { Text("确定", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showTimeDialog = null }) { Text("取消", color = TextDim) }
            }
        )
    }
}

@Composable
private fun BoxWithCircle(
    progress: Float,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    val stroke = 8.dp
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val strokePx = stroke.toPx()
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(w - strokePx, h - strokePx)
            // 背景环
            drawArc(
                color = RingBg,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // 进度弧
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

private fun formatDuration(totalSec: Int): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

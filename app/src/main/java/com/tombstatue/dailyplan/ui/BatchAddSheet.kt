package com.tombstatue.dailyplan.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tombstatue.dailyplan.data.Period
import com.tombstatue.dailyplan.ui.theme.Accent
import com.tombstatue.dailyplan.ui.theme.CardBg
import com.tombstatue.dailyplan.ui.theme.CardBorder
import com.tombstatue.dailyplan.ui.theme.SheetBg
import com.tombstatue.dailyplan.ui.theme.TextDim
import com.tombstatue.dailyplan.ui.theme.TextMain
import com.tombstatue.dailyplan.ui.theme.UndoneRed
import com.tombstatue.dailyplan.ui.theme.emoji
import com.tombstatue.dailyplan.ui.theme.label
import com.tombstatue.dailyplan.ui.theme.tint
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd")

private fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()) =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/**
 * 批量添加面板：日期范围 / 每周重复，两个标签切换。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchAddSheet(
    todayDate: String,  // 逻辑日期，开始日期不能早于这个
    onConfirm: (dates: List<String>, period: Period, text: String) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.parse(todayDate)
    var tab by remember { mutableStateOf(0) }
    // 日期状态
    var startMillis by remember { mutableStateOf(today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
    var endMillis by remember { mutableStateOf(today.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    // 每周重复：选中的星期
    var selectedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }
    // 时段 + 文字
    var period by remember { mutableStateOf(Period.MORNING) }
    var text by remember { mutableStateOf("") }
    // 确认中的错误提示
    var error by remember { mutableStateOf<String?>(null) }

    val startDate = startMillis.toLocalDate()
    val endDate = endMillis.toLocalDate()

    // 计算预览天数
    val previewDates = remember(tab, startDate, endDate, selectedDays) {
        if (startDate > endDate) emptyList()
        else {
            var d = startDate; val list = mutableListOf<LocalDate>()
            while (!d.isAfter(endDate)) {
                if (tab == 0 || selectedDays.contains(d.dayOfWeek)) list.add(d)
                d = d.plusDays(1)
            }
            list
        }
    }

    fun validate(): Boolean {
        if (startDate < today) { error = "开始日期不能早于今天"; return false }
        if (text.isBlank()) { error = "请输入日程内容"; return false }
        if (tab == 1 && selectedDays.isEmpty()) { error = "请至少选择一个星期"; return false }
        if (previewDates.isEmpty()) { error = "所选范围内没有匹配的日期"; return false }
        if (previewDates.size > 180) { error = "最多支持 180 天（约半年），请缩小范围"; return false }
        error = null
        return true
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SheetBg) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .imePadding()
        ) {
            Text("📦 批量规划", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = tab,
                containerColor = CardBg,
                contentColor = Accent
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("日期范围", modifier = Modifier.padding(10.dp)) }
                Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("每周重复", modifier = Modifier.padding(10.dp)) }
            }
            Spacer(Modifier.height(10.dp))

            // 日期选择行
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { showStartPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "开始 ${dateFmt.format(startDate)}",
                        color = TextMain, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
                Surface(
                    onClick = { showEndPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "结束 ${dateFmt.format(endDate)}",
                        color = TextMain, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // 每周重复：星期多选
            if (tab == 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val days = listOf(DayOfWeek.MONDAY to "周一", DayOfWeek.TUESDAY to "周二",
                        DayOfWeek.WEDNESDAY to "周三", DayOfWeek.THURSDAY to "周四",
                        DayOfWeek.FRIDAY to "周五", DayOfWeek.SATURDAY to "周六", DayOfWeek.SUNDAY to "周日")
                    days.forEach { (d, label) ->
                        FilterChip(
                            selected = d in selectedDays,
                            onClick = {
                                selectedDays = if (d in selectedDays) selectedDays - d else selectedDays + d
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent,
                                selectedLabelColor = TextMain
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // 时段选择
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Period.entries.forEach { p ->
                    Surface(
                        onClick = { period = p },
                        shape = RoundedCornerShape(8.dp),
                        color = if (period == p) p.tint.copy(alpha = 0.25f) else CardBg,
                        border = BorderStroke(1.dp, if (period == p) p.tint else CardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "${p.emoji} ${p.label}",
                            color = if (period == p) p.tint else TextDim,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 文字输入
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("日程内容（如\"背单词30个\"）", color = TextDim) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain,
                    cursorColor = Accent
                )
            )
            Spacer(Modifier.height(6.dp))

            // 预览
            Text(
                if (previewDates.isEmpty()) "请选择有效的日期范围"
                else "将添加至 ${dateFmt.format(previewDates.first())} ~ ${dateFmt.format(previewDates.last())} · 共 ${previewDates.size} 天",
                color = TextDim, fontSize = 12.sp
            )

            error?.let { Spacer(Modifier.height(4.dp)); Text(it, color = UndoneRed, fontSize = 12.sp) }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = TextDim) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (validate()) {
                            onConfirm(previewDates.map { it.toString() }, period, text)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) { Text(if (previewDates.isEmpty()) "确认" else "确认 · ${previewDates.size} 天") }
            }
        }
    }

    // DatePicker dialogs
    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startMillis = it }
                    showStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { endMillis = it }
                    showEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}

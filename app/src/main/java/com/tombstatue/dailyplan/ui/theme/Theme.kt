package com.tombstatue.dailyplan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tombstatue.dailyplan.data.Period

// 配色来自设计文档 §5.1（深色夜间风）
val Bg = Color(0xFF12121A)
val CardBg = Color(0xFF1E1E2A)
val CardBorder = Color(0xFF2A2A3A)
val TextMain = Color(0xFFE8E8F0)
val TextDim = Color(0xFF6B6B80)
val TextStruck = Color(0xFF55556A)
val Accent = Color(0xFF7C6CFF)
val UndoneRed = Color(0xFFE08A8A)
val UndoneBg = Color(0xFF3A2A2A)
val AllDoneGreen = Color(0xFF8BE9A0)
val SheetBg = Color(0xFF22222E)

/** 各时段的显示名 */
val Period.label: String
    get() = when (this) {
        Period.MORNING -> "早晨"
        Period.AFTERNOON -> "下午"
        Period.EVENING -> "晚上"
    }

/** 各时段的图标 */
val Period.emoji: String
    get() = when (this) {
        Period.MORNING -> "🌅"
        Period.AFTERNOON -> "☀️"
        Period.EVENING -> "🌙"
    }

/** 各时段的标题颜色 */
val Period.tint: Color
    get() = when (this) {
        Period.MORNING -> Color(0xFFFFB86C)
        Period.AFTERNOON -> Color(0xFF8BE9FD)
        Period.EVENING -> Color(0xFFBD93F9)
    }

@Composable
fun DailyPlanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            background = Bg,
            onBackground = TextMain,
            surface = CardBg,
            onSurface = TextMain,
            surfaceContainer = CardBg,
            onSurfaceVariant = TextDim
        ),
        content = content
    )
}

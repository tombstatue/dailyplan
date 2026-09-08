package com.tombstatue.dailyplan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tombstatue.dailyplan.data.Period
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// 配色来自设计文档 §6（v1.4 主题系统）
data class ThemeSpec(
    val id: String,
    val isDark: Boolean,
    val bg: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val textMain: Color,
    val textDim: Color,
    val textStruck: Color,
    val accent: Color,
    val undoneRed: Color,
    val undoneBg: Color,
    val allDoneGreen: Color,
    val sheetBg: Color,
    val label: String,
    val customBgPath: String? = null,
    val scrimAlpha: Float = 0f
)

object ThemeHolder {

    const val ID_NIGHT = "night"
    const val ID_FRESH = "fresh"
    const val ID_CALM = "calm"
    const val ID_CUSTOM = "custom"

    val NIGHT = ThemeSpec(
        id = ID_NIGHT,
        isDark = true,
        bg = Color(0xFF12121A),
        cardBg = Color(0xFF1E1E2A),
        cardBorder = Color(0xFF2A2A3A),
        textMain = Color(0xFFE8E8F0),
        textDim = Color(0xFF6B6B80),
        textStruck = Color(0xFF55556A),
        accent = Color(0xFF7C6CFF),
        undoneRed = Color(0xFFE08A8A),
        undoneBg = Color(0xFF3A2A2A),
        allDoneGreen = Color(0xFF8BE9A0),
        sheetBg = Color(0xFF22222E),
        label = "夜间紫"
    )

    val FRESH = ThemeSpec(
        id = ID_FRESH,
        isDark = false,
        bg = Color(0xFFF6F4EE),
        cardBg = Color(0xFFFFFFFF),
        cardBorder = Color(0xFFE4E0D6),
        textMain = Color(0xFF2B2B33),
        textDim = Color(0xFF8E8E9A),
        textStruck = Color(0xFFB4B4BC),
        accent = Color(0xFF3D7BF4),
        undoneRed = Color(0xFFD46A6A),
        undoneBg = Color(0xFFFBE9E9),
        allDoneGreen = Color(0xFF4C9B6B),
        sheetBg = Color(0xFFF0EDE6),
        label = "浅色清新"
    )

    val CALM = ThemeSpec(
        id = ID_CALM,
        isDark = true,
        bg = Color(0xFF0E1621),
        cardBg = Color(0xFF17202E),
        cardBorder = Color(0xFF22304A),
        textMain = Color(0xFFD9E4F0),
        textDim = Color(0xFF7E93A8),
        textStruck = Color(0xFF5B6B7E),
        accent = Color(0xFF5AA7FF),
        undoneRed = Color(0xFFE08A8A),
        undoneBg = Color(0xFF232C3A),
        allDoneGreen = Color(0xFF7FD8A4),
        sheetBg = Color(0xFF1C2836),
        label = "冷蓝静谧"
    )

    private val _current = MutableStateFlow(NIGHT)
    val current: StateFlow<ThemeSpec> = _current

    fun apply(id: String) {
        _current.value = fromId(id)
    }

    fun fromId(id: String): ThemeSpec = when (id) {
        ID_FRESH -> FRESH
        ID_CALM -> CALM
        else -> NIGHT
    }

    /** 自定义图片主题：由图片分析（亮度/Palette）构建后调用 */
    fun setCustom(spec: ThemeSpec) {
        _current.value = spec
    }
}

val LocalThemeSpec = staticCompositionLocalOf { ThemeHolder.NIGHT }

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

// ---------- 全局同名色板（页面零改动，动态读主题） ----------

val Bg: Color @Composable get() = LocalThemeSpec.current.bg
val CardBg: Color @Composable get() = LocalThemeSpec.current.cardBg
val CardBorder: Color @Composable get() = LocalThemeSpec.current.cardBorder
val TextMain: Color @Composable get() = LocalThemeSpec.current.textMain
val TextDim: Color @Composable get() = LocalThemeSpec.current.textDim
val TextStruck: Color @Composable get() = LocalThemeSpec.current.textStruck
val Accent: Color @Composable get() = LocalThemeSpec.current.accent
val UndoneRed: Color @Composable get() = LocalThemeSpec.current.undoneRed
val UndoneBg: Color @Composable get() = LocalThemeSpec.current.undoneBg
val AllDoneGreen: Color @Composable get() = LocalThemeSpec.current.allDoneGreen
val SheetBg: Color @Composable get() = LocalThemeSpec.current.sheetBg

@Composable
fun DailyPlanTheme(content: @Composable () -> Unit) {
    val spec by ThemeHolder.current.collectAsState()
    CompositionLocalProvider(LocalThemeSpec provides spec) {
        MaterialTheme(
            colorScheme = if (spec.isDark) darkColorScheme(
                primary = spec.accent,
                onPrimary = Color.White,
                background = spec.bg,
                onBackground = spec.textMain,
                surface = spec.cardBg,
                onSurface = spec.textMain,
                surfaceContainer = spec.cardBg,
                onSurfaceVariant = spec.textDim
            ) else lightColorScheme(
                primary = spec.accent,
                onPrimary = Color.White,
                background = spec.bg,
                onBackground = spec.textMain,
                surface = spec.cardBg,
                onSurface = spec.textMain,
                surfaceContainer = spec.cardBg,
                onSurfaceVariant = spec.textDim
            ),
            content = content
        )
    }
}

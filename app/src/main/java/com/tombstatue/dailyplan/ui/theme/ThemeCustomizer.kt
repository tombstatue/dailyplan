package com.tombstatue.dailyplan.ui.theme

import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.palette.graphics.Palette
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 自定义图片主题构建：下采样 32x32 求平均亮度 → 判定深/浅文字系；
 * Palette 提取主色做强调色。纯计算用于单测（亮度阈值）。
 */
object ThemeCustomizer {

    /** 亮度阈值：暗图 → 浅色文字系，亮图 → 深色文字系 */
    fun textIsDark(avgLuminance: Double): Boolean = avgLuminance >= 128

    /** 平均亮度（0-255），对下采样后的像素取 RGB 加权平均 */
    fun averageLuminance(bitmap: android.graphics.Bitmap): Double {
        var sum = 0.0
        var count = 0
        for (y in 0 until bitmap.height step 2) {
            for (x in 0 until bitmap.width step 2) {
                val p = bitmap.getPixel(x, y)
                val g = 0.299 * AndroidColor.red(p) + 0.587 * AndroidColor.green(p) + 0.114 * AndroidColor.blue(p)
                sum += g
                count++
            }
        }
        return if (count == 0) 128.0 else sum / count
    }

    @Suppress("DEPRECATION")
    private fun downsample(path: String, target: Int = 32): android.graphics.Bitmap? {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth <= 0) return null
        var sample = 1
        while (opts.outWidth / (sample * 2) >= target) sample *= 2
        val d = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(path, d)
    }

    /** 加载图片并构建自定义主题 spec（IO 线程；失败返回 null 由调用方回退） */
    suspend fun buildCustomTheme(path: String): ThemeSpec? = withContext(Dispatchers.IO) {
        val bmp = downsample(path) ?: return@withContext null
        val lum = averageLuminance(bmp)
        val dark = !textIsDark(lum)
        val palette = Palette.from(bmp).generate()
        val accentPx = palette?.vibrantSwatch?.rgb
            ?: palette?.mutedSwatch?.rgb
            ?: if (dark) AndroidColor.rgb(105, 190, 255) else AndroidColor.rgb(60, 120, 240)
        val accent = Color(accentPx)

        if (dark) {
            ThemeSpec(
                id = ThemeHolder.ID_CUSTOM,
                isDark = true,
                // 深图：深色底 + 白色系文字；卡片用半透明白叠层保证可读
                bg = Color(0x00000000),
                cardBg = Color.White.copy(alpha = 0.10f),
                cardBorder = Color.White.copy(alpha = 0.25f),
                textMain = Color.White,
                textDim = Color(0xFFC9C9D8),
                textStruck = Color(0xFF9A9AAF),
                accent = accent,
                undoneRed = Color(0xFFFF9E9E),
                undoneBg = Color.White.copy(alpha = 0.12f),
                allDoneGreen = Color(0xFFA9F0C0),
                sheetBg = Color(0xE6000000),
                label = "自定义图片",
                customBgPath = path,
                scrimAlpha = 0.35f
            )
        } else {
            ThemeSpec(
                id = ThemeHolder.ID_CUSTOM,
                isDark = false,
                bg = Color(0xFF000000),
                cardBg = Color.Black.copy(alpha = 0.08f),
                cardBorder = Color.Black.copy(alpha = 0.18f),
                textMain = Color(0xFF1C1C22),
                textDim = Color(0xFF55555E),
                textStruck = Color(0xFF8A8A92),
                accent = accent,
                undoneRed = Color(0xFFC85050),
                undoneBg = Color.Black.copy(alpha = 0.07f),
                allDoneGreen = Color(0xFF2E8A55),
                sheetBg = Color(0xF2FFFFFF),
                label = "自定义图片",
                customBgPath = path,
                scrimAlpha = 0.12f
            )
        }
    }
}

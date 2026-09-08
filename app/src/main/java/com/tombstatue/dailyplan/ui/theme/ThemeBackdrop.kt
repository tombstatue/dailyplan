package com.tombstatue.dailyplan.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * 自定义图片背景层：图片全屏 + 自适应 scrim；无图片时原样。
 * 页面 Scaffold/背景色用半透明 spec.bg 时背景图透出。
 */
@Composable
fun ThemeBackdrop(bgPath: String? = null, content: @Composable () -> Unit) {
    val spec by ThemeHolder.current.collectAsState()
    val path = bgPath ?: spec.customBgPath
    val bgImage = remember(path) {
        path?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
    }
    Box {
        if (bgImage != null) {
            Image(
                bitmap = bgImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            val scrim = if (spec.isDark) Color.Black.copy(alpha = spec.scrimAlpha)
            else Color.White.copy(alpha = 0.35f)
            Box(Modifier.fillMaxSize().background(scrim))
        }
        content()
    }
}

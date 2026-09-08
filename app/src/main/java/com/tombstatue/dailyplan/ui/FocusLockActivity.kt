package com.tombstatue.dailyplan.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tombstatue.dailyplan.pomodoro.PomodoroEngine
import com.tombstatue.dailyplan.pomodoro.PomodoroMode
import com.tombstatue.dailyplan.pomodoro.PomodoroService
import com.tombstatue.dailyplan.pomodoro.SettingsStore
import com.tombstatue.dailyplan.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 专注页：计时期间全屏。白名单应用可一键打开；
 * 退出专注按设置开关决定是否输入 20 位随机验证码（默认关闭）。
 */
class FocusLockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在锁屏上方显示
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 不隐藏状态栏，允许下拉查看消息通知

        setContent {
            DailyPlanTheme {
                FocusLockScreen(onFinish = { finish() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 屏幕固定：阻止 Home 键 / 手势返回桌面
        try {
            startLockTask()
        } catch (_: SecurityException) {
            // 屏幕固定未启用，fallback 到 MainActivity.onResume 重检测机制
        }
    }

    override fun finish() {
        try { stopLockTask() } catch (_: Exception) {}
        super.finish()
    }

    // 禁止返回键
    override fun onBackPressed() {}

    // 保留状态栏（可下拉看消息），仅隐藏导航栏
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
        }
    }
}

/** 白名单应用信息（图标+名称） */
private data class WhitelistApp(val pkg: String, val label: String, val icon: Drawable?)

private suspend fun loadWhitelistApps(context: Context): List<WhitelistApp> {
    val pkgs = SettingsStore.whitelist()
    if (pkgs.isEmpty()) return emptyList()
    val pm = context.packageManager
    return pkgs.mapNotNull { pkg ->
        runCatching {
            val info = pm.getApplicationInfo(pkg, 0)
            WhitelistApp(pkg, pm.getApplicationLabel(info).toString(), pm.getApplicationIcon(info))
        }.getOrNull()
    }
}

@Composable
private fun FocusLockScreen(onFinish: () -> Unit) {
    val s by PomodoroEngine.state.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var unlockCode by remember { mutableStateOf("") }
    var userInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }
    var showCompletion by remember { mutableStateOf(false) }
    var forceCode by remember { mutableStateOf(false) }
    var whitelistApps by remember { mutableStateOf<List<WhitelistApp>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        forceCode = SettingsStore.forceExitCode()
        whitelistApps = loadWhitelistApps(context)
    }

    // 监听状态变化：暂停 → 解除锁屏；完成 → 展示完成页后解除
    LaunchedEffect(s.running, s.finished) {
        if (!s.running && !s.finished) {
            onFinish() // 用户从外部暂停
        }
        if (s.finished && !showCompletion) {
            showCompletion = true
            delay(4000)
            onFinish() // 计时完成，自动解除
        }
    }

    val ringColor = if (s.mode == PomodoroMode.WORK) Color(0xFFE94560) else Color(0xFF4ECCA3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D15))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showCompletion) {
                // 完成页面
                Spacer(Modifier.weight(1f))
                Text("🎉", fontSize = 56.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "专注完成！",
                    color = ringColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text("做得很棒，休息一下吧 ☕", color = TextDim, fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatDuration(s.focusSecToday),
                            color = ringColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("今日专注", color = TextDim, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${s.sessionsToday} 次",
                            color = Accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("完成", color = TextDim, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.weight(1.2f))
            } else {
                // 计时页面
                Spacer(Modifier.weight(0.6f))
                Text("🔒", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text("专注中", color = TextDim, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                Text(
                    PomodoroService.formatTime(s.remainingSec),
                    color = TextMain,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
                Text(s.mode.label, color = ringColor, fontSize = 16.sp)

                if (!s.boundTaskText.isNullOrBlank()) {
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        color = CardBg,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "📋 ${s.boundTaskText}",
                            color = TextDim,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // 白名单应用入口
                if (whitelistApps.isNotEmpty()) {
                    Text("白名单应用", color = TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(whitelistApps) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        (context as? ComponentActivity)?.stopLockTask()
                                        val launch = context.packageManager.getLaunchIntentForPackage(app.pkg)
                                        if (launch != null) {
                                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(launch)
                                        }
                                    }
                                    .padding(4.dp)
                            ) {
                                val bmp = app.icon?.let {
                                    it.toBitmap(48, 48).asImageBitmap()
                                }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = app.label,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(app.label, color = TextDim, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatDuration(s.focusSecToday),
                            color = ringColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("今日专注", color = TextDim, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${s.sessionsToday} 次",
                            color = Accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("完成", color = TextDim, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (forceCode) {
                            unlockCode = generateUnlockCode()
                            userInput = ""
                            inputError = false
                            showExitDialog = true
                        } else {
                            showExitConfirm = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("退出专注模式", color = TextDim, fontSize = 15.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "坚持就是胜利 ✊",
                    color = TextDim.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.weight(1.2f))
            }
        }
    }

    // 简单确认退出（验证码开关关闭时）
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            containerColor = SheetBg,
            title = {
                Text("中断专注？", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = { Text("退出后计时将暂停并保留剩余时间，可随时继续。", color = TextDim, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        PomodoroEngine.pause()
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ringColor)
                ) {
                    Text("确认退出", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("继续专注", color = TextDim)
                }
            }
        )
    }

    // 验证码退出弹窗（开关打开时）
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = SheetBg,
            title = {
                Text(
                    "确认退出专注",
                    color = TextMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("请输入以下验证码以退出：", color = TextDim, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))

                    // 20 位验证码（分组显示，不可选中复制）
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        unlockCode.chunked(4).forEach { group ->
                            Text(
                                group,
                                color = TextMain,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = {
                            if (it.length <= 20 && it.all { c -> c.isDigit() }) {
                                userInput = it
                                inputError = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入 20 位验证码", color = TextDim) },
                        singleLine = true,
                        isError = inputError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ringColor,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain,
                            cursorColor = Accent,
                            errorBorderColor = Color(0xFFE94560)
                        ),
                        supportingText = if (inputError) {
                            { Text("验证码错误，请核对后重新输入", color = Color(0xFFE94560), fontSize = 11.sp) }
                        } else null
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "坚持就是胜利 ✊",
                        color = TextDim.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (userInput == unlockCode) {
                            showExitDialog = false
                            PomodoroEngine.pause()
                            onFinish()
                        } else {
                            inputError = true
                            userInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ringColor)
                ) {
                    Text("确认退出", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("继续专注", color = TextDim)
                }
            }
        )
    }
}

private fun generateUnlockCode(): String =
    (1..20).map { (0..9).random() }.joinToString("")

private fun formatDuration(totalSec: Int): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

package com.tombstatue.dailyplan.pomodoro

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import com.tombstatue.dailyplan.ui.FocusLockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 前台应用监测（白名单核心）：计时运行中，用户打开非白名单应用 → 自动拉回专注页。
 * 检测延迟 1~3 秒（UsageStats 事件回读），拉回依赖 SYSTEM_ALERT_WINDOW 豁免后台启动限制。
 */
object AppUsageWatcher {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private var lastPullbackAt = 0L
    private const val PULLBACK_INTERVAL_MS = 30_000L

    fun start(context: Context) {
        job?.cancel()
        job = scope.launch {
            while (true) {
                runCatching { checkAndPullback(context.applicationContext) }
                delay(2_000)
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null
    }

    private suspend fun checkAndPullback(context: Context) {
        if (!PomodoroEngine.state.value.running) return
        if (!usagePermissionGranted(context)) return
        val fg = foregroundPackage(context) ?: return
        if (fg == context.packageName) return
        if (fg in SettingsStore.whitelist()) return
        val now = System.currentTimeMillis()
        if (now - lastPullbackAt < PULLBACK_INTERVAL_MS) return
        lastPullbackAt = now
        context.startActivity(
            Intent(context, FocusLockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        )
    }

    fun usagePermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    private fun foregroundPackage(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = runCatching { usm.queryEvents(now - 60_000, now) }.getOrNull() ?: return null
        var latest: String? = null
        var latestTime = 0L
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND && e.timeStamp >= latestTime) {
                latest = e.packageName
                latestTime = e.timeStamp
            }
        }
        return latest
    }
}

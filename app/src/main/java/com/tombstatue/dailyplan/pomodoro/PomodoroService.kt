package com.tombstatue.dailyplan.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.tombstatue.dailyplan.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PomodoroService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var observeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        observeJob?.cancel()
        observeJob = scope.launch {
            PomodoroEngine.state.collectLatest { s ->
                if (s.running || s.finished) {
                    val n = buildNotification(s)
                    startForeground(NOTIF_ID, n)
                    if (s.finished && s.mode == PomodoroMode.WORK) {
                        vibrate()
                    }
                }
                if (!s.running && !s.finished) {
                    // 用户暂停——移除前台但保留服务
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        observeJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(s: TimerState): Notification {
        val content = if (s.finished) {
            if (s.mode == PomodoroMode.WORK) "工作完成！休息一下吧 ☕" else "休息结束！开始新的番茄钟吧 💪"
        } else {
            val label = s.boundTaskText ?: "专注中"
            "${formatTime(s.remainingSec)} · $label"
        }
        val title = if (s.finished) "🍅 番茄钟" else "🍅 ${s.mode.label}"
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply {
                putExtra("goto_tab", 2) // 番茄钟 tab
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(!s.finished)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun vibrate() {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    companion object {
        const val CHANNEL_ID = "pomodoro_timer"
        private const val NOTIF_ID = 1001

        fun formatTime(seconds: Int): String {
            val m = seconds / 60; val s = seconds % 60
            return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        }

        private fun createChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "番茄钟计时", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "番茄钟后台计时通知"
                        setShowBadge(false)
                    }
                )
            }
        }

        fun createChannelStatic(context: Context) = createChannel(context)
    }
}

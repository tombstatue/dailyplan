package com.tombstatue.dailyplan.pomodoro

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

/** 精确闹钟兜底：进程被杀时保证"计时结束"提醒不丢失 */
object PomodoroAlarm {

    private const val ACTION_FINISH = "com.tombstatue.dailyplan.POMODORO_FINISH"
    private const val REQUEST_CODE = 42
    const val CHANNEL_ID = "pomodoro_finish"
    private const val NOTIF_ID = 1002

    fun schedule(context: Context, endAtMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setWindow(AlarmManager.RTC_WAKEUP, endAtMillis, 60_000, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
            }
        } catch (se: SecurityException) {
            am.setWindow(AlarmManager.RTC_WAKEUP, endAtMillis, 60_000, pi)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, PomodoroAlarmReceiver::class.java).setAction(ACTION_FINISH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class PomodoroAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (PomodoroEngine.state.value.running) return // 进程活着：Service 会接管完成通知
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(PomodoroAlarm.CHANNEL_ID, "番茄钟结束", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.notify(
            PomodoroAlarm.NOTIF_ID,
            NotificationCompat.Builder(context, PomodoroAlarm.CHANNEL_ID)
                .setContentTitle("🍅 番茄钟")
                .setContentText("计时结束！休息一下吧 ☕")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
        )
        vibrate(context)
    }

    private fun vibrate(context: Context) {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

package com.tombstatue.dailyplan.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tombstatue.dailyplan.data.Task
import com.tombstatue.dailyplan.pomodoro.PomodoroEngine
import com.tombstatue.dailyplan.pomodoro.PomodoroMode
import com.tombstatue.dailyplan.pomodoro.PomodoroService
import com.tombstatue.dailyplan.pomodoro.TimerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PomodoroViewModel(app: Application) : AndroidViewModel(app) {

    val state: StateFlow<TimerState> = PomodoroEngine.state
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PomodoroEngine.state.value
        )

    /** 绑定的任务（null = 独立模式） */
    var boundTask: Task? = null
        private set

    /** 长按模式按钮 → 自定义分钟；null = 使用默认 */
    var customModeMin: Int? = null

    fun bindTask(task: Task) {
        boundTask = task
        PomodoroEngine.setTaskLabel(task.text)
    }

    fun unbindTask() {
        boundTask = null
        PomodoroEngine.setTaskLabel(null)
    }

    fun start() {
        PomodoroEngine.start()
        startService()
    }

    fun pause() {
        PomodoroEngine.pause()
        stopServiceIfIdle()
    }

    fun setMode(mode: PomodoroMode, customMin: Int? = null) {
        customModeMin = customMin
        PomodoroEngine.setMode(mode, customMin)
        stopServiceIfIdle()
    }

    fun reset() {
        PomodoroEngine.reset()
        stopServiceIfIdle()
    }

    fun clearFinished() {
        // UI 在展示"完成"后清除 finished 标记（由 Engine 下一次 setMode/start 也会清除）
    }

    private fun startService() {
        try {
            val ctx = getApplication<Application>()
            PomodoroService.createChannelStatic(ctx)
            ctx.startForegroundService(Intent(ctx, PomodoroService::class.java))
        } catch (e: Exception) {
            android.util.Log.e("PomodoroVM", "启动前台服务失败", e)
        }
    }

    private fun stopServiceIfIdle() {
        try {
            if (!PomodoroEngine.state.value.running) {
                getApplication<Application>().stopService(
                    Intent(getApplication(), PomodoroService::class.java)
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PomodoroVM", "停止服务失败", e)
        }
    }
}

package com.tombstatue.dailyplan.pomodoro

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tombstatue.dailyplan.logic.DayLogic

/**
 * 番茄钟引擎（单例）：墙钟制持有计时状态，进程被杀后由 PomodoroStore 恢复。
 */
object PomodoroEngine {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var hydrated = false

    /** App/Service 启动时调用：注入存储并恢复上次计时状态 */
    suspend fun hydrate(context: Context) {
        PomodoroStore.init(context)
        SettingsStore.init(context)
        if (hydrated) return
        hydrated = true
        _state.value = TimerLogic.refreshDayCounters(
            TimerLogic.hydrate(PomodoroStore.load(), System.currentTimeMillis()),
            DayLogic.logicalDate(System.currentTimeMillis())
        )
        PomodoroStore.save(_state.value)
        if (_state.value.finished) _state.update { it.copy(finished = false) }
        PomodoroStore.context?.let { ctx ->
            val endAt = _state.value.endAtMillis
            if (_state.value.running && endAt != null) PomodoroAlarm.schedule(ctx, endAt)
            else PomodoroAlarm.cancel(ctx)
        }
        if (_state.value.running) startTicking()
    }

    /** 启动/恢复计时 */
    fun start() {
        val today = DayLogic.logicalDate(System.currentTimeMillis())
        _state.update {
            TimerLogic.refreshDayCounters(TimerLogic.onStarted(it, System.currentTimeMillis()), today)
        }
        _state.value.endAtMillis?.let { endAt ->
            PomodoroStore.context?.let { PomodoroAlarm.schedule(it, endAt) }
        }
        saveState()
        startTicking()
    }

    /** 暂停（保留剩余秒数，可继续） */
    fun pause() {
        tickJob?.cancel(); tickJob = null
        _state.update { TimerLogic.onPaused(it, System.currentTimeMillis()) }
        PomodoroStore.context?.let { PomodoroAlarm.cancel(it) }
        saveState()
    }

    /** 切换模式（重置该模式总时长） */
    fun setMode(mode: PomodoroMode, customMin: Int? = null) {
        tickJob?.cancel(); tickJob = null
        val min = customMin ?: mode.defaultMin
        _state.update {
            it.copy(mode = mode, totalSec = min * 60, remainingSec = min * 60, endAtMillis = null, running = false, finished = false)
        }
        PomodoroStore.context?.let { PomodoroAlarm.cancel(it) }
        saveState()
    }

    /** 重置当前模式 */
    fun reset() {
        tickJob?.cancel(); tickJob = null
        _state.update { it.copy(endAtMillis = null, remainingSec = it.totalSec, running = false, finished = false) }
        PomodoroStore.context?.let { PomodoroAlarm.cancel(it) }
        saveState()
    }

    /** 设置任务绑定标签（仅用于通知文字） */
    fun setTaskLabel(text: String?) {
        _state.update { it.copy(boundTaskText = text) }
        saveState()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (_state.value.running) {
                delay(1000)
                val now = System.currentTimeMillis()
                val (next, done) = TimerLogic.onTick(_state.value, now)
                _state.value = next
                if (done) {
                    PomodoroStore.context?.let { PomodoroAlarm.cancel(it) }
                    saveState()
                    break
                } // 仅状态跃迁写盘；endAt 为基准，快照丢失无碍
            }
        }
    }

    private fun saveState() {
        scope.launch { PomodoroStore.save(_state.value) }
    }
}

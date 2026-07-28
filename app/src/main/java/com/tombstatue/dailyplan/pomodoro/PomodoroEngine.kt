package com.tombstatue.dailyplan.pomodoro

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
 * 番茄钟引擎（单例）：持有计时状态，管理倒数协程。
 * 进程被杀状态丢失——不做持久化（番茄钟数据不跨天保留）。
 */
object PomodoroEngine {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var lastDate: String = DayLogic.logicalDate(System.currentTimeMillis())

    /** 启动/恢复计时 */
    fun start() {
        val today = DayLogic.logicalDate(System.currentTimeMillis())
        if (today != lastDate) {
            _state.update { it.copy(sessionsToday = 0, focusSecToday = 0) }
            lastDate = today
        }
        _state.update { it.copy(running = true, finished = false) }
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(1000)
                tick()
            }
        }
    }

    /** 暂停 */
    fun pause() {
        tickJob?.cancel(); tickJob = null
        _state.update { it.copy(running = false) }
    }

    /** 切换模式（重置该模式总时长） */
    fun setMode(mode: PomodoroMode, customMin: Int? = null) {
        tickJob?.cancel(); tickJob = null
        val min = customMin ?: mode.defaultMin
        _state.update { it.copy(mode = mode, totalSec = min * 60, remainingSec = min * 60, running = false, finished = false) }
    }

    /** 重置当前模式 */
    fun reset() {
        tickJob?.cancel(); tickJob = null
        _state.update { it.copy(remainingSec = it.totalSec, running = false, finished = false) }
    }

    /** 设置任务绑定标签（仅用于通知文字） */
    fun setTaskLabel(text: String?) {
        _state.update { it.copy(boundTaskText = text) }
    }

    private suspend fun tick() {
        _state.update { s ->
            if (s.remainingSec > 0) s.copy(remainingSec = s.remainingSec - 1)
            else s
        }
        if (_state.value.remainingSec <= 0) {
            tickJob?.cancel(); tickJob = null
            _state.update { s ->
                val isWork = s.mode == PomodoroMode.WORK
                s.copy(
                    remainingSec = 0, running = false, finished = true,
                    sessionsToday = if (isWork) s.sessionsToday + 1 else s.sessionsToday,
                    focusSecToday = if (isWork) s.focusSecToday + s.totalSec else s.focusSecToday
                )
            }
        }
    }
}

package com.tombstatue.dailyplan.pomodoro

import kotlinx.serialization.Serializable

/** 番茄钟模式 */
@Serializable
enum class PomodoroMode(val label: String, val defaultMin: Int) {
    WORK("工作中", 25),
    SHORT_BREAK("短休息", 5),
    LONG_BREAK("长休息", 15)
}

/**
 * 墙钟制计时状态。running=true 时以 endAtMillis 为唯一时间基准，
 * remainingSec 只是显示快照 / 暂停后保留的剩余值。
 */
@Serializable
data class TimerState(
    val mode: PomodoroMode = PomodoroMode.WORK,
    val totalSec: Int = 25 * 60,
    val endAtMillis: Long? = null,     // 墙钟截止时间戳；running=true 时必非空
    val remainingSec: Int = 25 * 60,   // 运行中=显示快照；不在运行=剩余值
    val running: Boolean = false,
    val finished: Boolean = false,     // 刚完成，UI 用
    val boundTaskText: String? = null, // 关联的任务文字，用于通知显示
    val sessionsToday: Int = 0,        // 今日完成的工作番茄数
    val focusSecToday: Int = 0,        // 今日累计专注秒数
    val dayKey: String? = null         // 计数归属日期（跨天清零用）
)

/** 纯计算逻辑：不依赖 Android，便于单元测试 */
object TimerLogic {

    /** 剩余秒数：墙钟优先；endAt 为空时返回 fallback */
    fun remainingAtMillis(endAtMillis: Long?, nowMillis: Long, fallbackSec: Int): Int {
        if (endAtMillis == null) return fallbackSec
        val remain = (endAtMillis - nowMillis).toDouble() / 1000.0
        return if (remain <= 0) 0 else kotlin.math.ceil(remain).toInt()
    }

    /** 开始/恢复：以 remainingSec 为基准设置墙钟截止 */
    fun onStarted(state: TimerState, nowMillis: Long): TimerState {
        val base = if (state.remainingSec <= 0) state.totalSec else state.remainingSec
        return state.copy(
            endAtMillis = nowMillis + base * 1000L,
            running = true,
            finished = false,
            remainingSec = base
        )
    }

    /** 暂停：把墙钟剩余结算成固定值 */
    fun onPaused(state: TimerState, nowMillis: Long): TimerState {
        if (state.endAtMillis == null) return state
        val remain = remainingAtMillis(state.endAtMillis, nowMillis, state.remainingSec)
        return state.copy(remainingSec = remain, endAtMillis = null, running = false)
    }

    /** 每秒钟的显示快照；返回 null 表示还在计，返回终态表示已到点结算 */
    fun onTick(state: TimerState, nowMillis: Long): Pair<TimerState, Boolean> {
        val remain = remainingAtMillis(state.endAtMillis, nowMillis, state.remainingSec)
        if (remain > 0) return state.copy(remainingSec = remain) to false
        return finalize(state) to true
    }

    /** 到点结清：工作模式计入完整番茄，休息模式仅结束 */
    fun finalize(state: TimerState): TimerState {
        val isWork = state.mode == PomodoroMode.WORK
        return state.copy(
            remainingSec = 0,
            endAtMillis = null,
            running = false,
            finished = true,
            sessionsToday = if (isWork) state.sessionsToday + 1 else state.sessionsToday,
            focusSecToday = if (isWork) state.focusSecToday + state.totalSec else state.focusSecToday
        )
    }

    /**
     * 启动水合（App/服务恢复）：运行中且截止已过 → 直接结清；
     * 运行中且未到 → 恢复墙钟；其他原样。
     */
    fun hydrate(state: TimerState, nowMillis: Long): TimerState {
        if (!state.running || state.endAtMillis == null) return state
        val remain = remainingAtMillis(state.endAtMillis, nowMillis, state.remainingSec)
        return if (remain <= 0) finalize(state) else state.copy(remainingSec = remain)
    }

    /** 跨天清零今日计数 */
    fun refreshDayCounters(state: TimerState, todayKey: String): TimerState =
        if (state.dayKey == todayKey) state
        else state.copy(sessionsToday = 0, focusSecToday = 0, dayKey = todayKey)
}

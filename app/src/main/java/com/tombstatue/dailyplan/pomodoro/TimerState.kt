package com.tombstatue.dailyplan.pomodoro

/** 番茄钟模式 */
enum class PomodoroMode(val label: String, val defaultMin: Int) {
    WORK("工作中", 25),
    SHORT_BREAK("短休息", 5),
    LONG_BREAK("长休息", 15)
}

data class TimerState(
    val mode: PomodoroMode = PomodoroMode.WORK,
    val remainingSec: Int = 25 * 60,
    val totalSec: Int = 25 * 60,
    val running: Boolean = false,
    val finished: Boolean = false,     // 刚完成，UI 用
    val boundTaskText: String? = null, // 关联的任务文字，用于通知显示
    val sessionsToday: Int = 0,        // 今天完成的工作番茄数
    val focusSecToday: Int = 0         // 今天累计专注秒数
)

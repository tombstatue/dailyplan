package com.tombstatue.dailyplan.logic

import com.tombstatue.dailyplan.data.DayRecord
import com.tombstatue.dailyplan.data.TodayState
import java.time.Instant
import java.time.ZoneId

/**
 * 每日重置的核心逻辑（纯函数，便于单元测试）。
 * 一天的分界线是凌晨 4:00：凌晨 0:00–3:59 仍算前一天。
 */
object DayLogic {

    private const val DAY_START_HOUR = 4L

    /** 当前时刻对应的逻辑日期（减 4 小时后取日历日），格式 yyyy-MM-dd */
    fun logicalDate(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(nowMillis)
            .atZone(zone)
            .minusHours(DAY_START_HOUR)
            .toLocalDate()
            .toString()

    /**
     * 结算：若日期已跨天，返回（清空的新状态, 归档记录）；
     * 未跨天返回（原状态, null）；跨天但当天无任务时归档记录为 null。
     */
    fun rollover(
        state: TodayState,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<TodayState, DayRecord?> {
        val today = logicalDate(nowMillis, zone)
        if (state.logicalDate == today) return state to null
        val record = if (state.tasks.isNotEmpty()) DayRecord(state.logicalDate, state.tasks) else null
        return TodayState(today, emptyList()) to record
    }
}

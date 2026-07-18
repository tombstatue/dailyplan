package com.tombstatue.dailyplan.logic

import com.tombstatue.dailyplan.data.DayPlan
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

    /** 结算结果 */
    data class RolloverResult(
        val newToday: TodayState,
        val archived: List<DayRecord>,    // 需追加进历史的记录，按日期升序
        val remainingPlans: List<DayPlan> // 消费/清理后的未来规划
    )

    /**
     * 结算 v2：
     * - 未跨天：原样返回
     * - 跨天：归档原今天 → 被跳过的中间日按"全部未完成"归档 → 新今天并入该日期的规划
     *   （规划日程标记 fromPlan、事件进 events）→ 移除所有日期 ≤ 新日期的规划
     */
    fun rollover(
        state: TodayState,
        plans: List<DayPlan>,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): RolloverResult {
        val today = logicalDate(nowMillis, zone)
        if (state.logicalDate == today) return RolloverResult(state, emptyList(), plans)

        val archived = mutableListOf<DayRecord>()
        if (state.tasks.isNotEmpty() || state.events.isNotEmpty()) {
            archived += DayRecord(state.logicalDate, state.tasks, state.events)
        }
        // 几天没打开 app：被跳过的日子若有规划，以"全部未完成"归档，不凭空消失
        plans.filter { it.date > state.logicalDate && it.date < today }
            .sortedBy { it.date }
            .forEach { p ->
                if (p.tasks.isNotEmpty() || p.events.isNotEmpty()) {
                    archived += DayRecord(
                        date = p.date,
                        tasks = p.tasks.map { it.copy(done = false, fromPlan = true) },
                        events = p.events
                    )
                }
            }
        // 新今天 = 该日期的规划（可能为空）
        val todayPlan = plans.find { it.date == today }
        val newToday = TodayState(
            logicalDate = today,
            tasks = todayPlan?.tasks?.map { it.copy(fromPlan = true) } ?: emptyList(),
            events = todayPlan?.events ?: emptyList()
        )
        // 防御性清理：所有 ≤ 今天的规划条目一律移除
        val remaining = plans.filter { it.date > today }
        return RolloverResult(newToday, archived, remaining)
    }
}

package com.tombstatue.dailyplan.data

import kotlinx.serialization.Serializable

/** 一天中的时段 */
enum class Period { MORNING, AFTERNOON, EVENING }

/** 一条计划 */
@Serializable
data class Task(
    val id: String,               // UUID
    val text: String,             // 计划内容
    val period: Period,           // 所属时段
    val done: Boolean,            // 是否已划掉
    val createdAt: Long,          // 创建时间（epoch millis）
    val fromPlan: Boolean = false // 是否来自提前规划（置顶加粗显示）；默认值保证旧数据兼容
)

/** 重要事件：属于某一天的大事（考试、截止等），无时段 */
@Serializable
data class Event(
    val id: String,
    val text: String
)

/** 未来某天的规划：重要事件 + 提前安排的日程 */
@Serializable
data class DayPlan(
    val date: String,                          // yyyy-MM-dd
    val events: List<Event> = emptyList(),
    val tasks: List<Task> = emptyList()
)

/** 今天页的完整状态 */
@Serializable
data class TodayState(
    val logicalDate: String,                   // 逻辑日期，如 "2026-07-19"
    val tasks: List<Task>,
    val events: List<Event> = emptyList()      // 当天的重要事件；默认值保证旧数据兼容
)

/** 历史归档：某一天的快照（含未完成任务与事件） */
@Serializable
data class DayRecord(
    val date: String,
    val tasks: List<Task>,
    val events: List<Event> = emptyList()      // 默认值保证旧数据兼容
)

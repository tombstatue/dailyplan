package com.tombstatue.dailyplan.data

import kotlinx.serialization.Serializable

/** 一天中的时段 */
enum class Period { MORNING, AFTERNOON, EVENING }

/** 一条计划 */
@Serializable
data class Task(
    val id: String,          // UUID
    val text: String,        // 计划内容
    val period: Period,      // 所属时段
    val done: Boolean,       // 是否已划掉
    val createdAt: Long      // 创建时间（epoch millis）
)

/** 今天页的完整状态 */
@Serializable
data class TodayState(
    val logicalDate: String,     // 逻辑日期，如 "2026-07-19"
    val tasks: List<Task>
)

/** 历史归档：某一天的任务快照（含未完成项） */
@Serializable
data class DayRecord(
    val date: String,
    val tasks: List<Task>
)

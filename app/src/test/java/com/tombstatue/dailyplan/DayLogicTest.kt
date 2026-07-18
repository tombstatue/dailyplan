package com.tombstatue.dailyplan

import com.tombstatue.dailyplan.data.DayRecord
import com.tombstatue.dailyplan.data.Period
import com.tombstatue.dailyplan.data.Task
import com.tombstatue.dailyplan.data.TodayState
import com.tombstatue.dailyplan.logic.DayLogic
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayLogicTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun task(text: String, done: Boolean) =
        Task(id = "id-$text", text = text, period = Period.MORNING, done = done, createdAt = 0L)

    // ---------- 逻辑日期（凌晨 4 点分界） ----------

    @Test
    fun 凌晨4点前算前一天() {
        assertEquals("2026-07-18", DayLogic.logicalDate(at(2026, 7, 19, 3, 59), zone))
    }

    @Test
    fun 凌晨4点整算当天() {
        assertEquals("2026-07-19", DayLogic.logicalDate(at(2026, 7, 19, 4, 0), zone))
    }

    @Test
    fun 白天归属当天() {
        assertEquals("2026-07-19", DayLogic.logicalDate(at(2026, 7, 19, 12, 0), zone))
    }

    @Test
    fun 跨月边界() {
        assertEquals("2026-04-30", DayLogic.logicalDate(at(2026, 5, 1, 3, 0), zone))
    }

    @Test
    fun 跨年边界() {
        assertEquals("2025-12-31", DayLogic.logicalDate(at(2026, 1, 1, 2, 0), zone))
    }

    // ---------- 结算规则 ----------

    @Test
    fun 同一天不结算() {
        val st = TodayState("2026-07-19", listOf(task("a", false)))
        val (newState, record) = DayLogic.rollover(st, at(2026, 7, 19, 10, 0), zone)
        assertEquals(st, newState)
        assertNull(record)
    }

    @Test
    fun 凌晨使用不触发重置() {
        // 7月19日凌晨2点，逻辑日期仍是 7月18日 → 不结算
        val st = TodayState("2026-07-18", listOf(task("a", false)))
        val (newState, record) = DayLogic.rollover(st, at(2026, 7, 19, 2, 0), zone)
        assertEquals(st, newState)
        assertNull(record)
    }

    @Test
    fun 跨天清空并归档含未完成() {
        val st = TodayState("2026-07-18", listOf(task("a", true), task("b", false)))
        val (newState, record) = DayLogic.rollover(st, at(2026, 7, 19, 10, 0), zone)
        assertEquals("2026-07-19", newState.logicalDate)
        assertTrue(newState.tasks.isEmpty())
        assertNotNull(record)
        assertEquals("2026-07-18", record!!.date)
        assertEquals(2, record.tasks.size)
        assertTrue(record.tasks[0].done)    // 完成状态原样保留
        assertFalse(record.tasks[1].done)
    }

    @Test
    fun 空任务日不产生历史() {
        val st = TodayState("2026-07-18", emptyList())
        val (newState, record) = DayLogic.rollover(st, at(2026, 7, 19, 10, 0), zone)
        assertEquals("2026-07-19", newState.logicalDate)
        assertNull(record)
    }

    @Test
    fun 隔多天也正常结算() {
        // 几天没打开 app：归档的是最后使用那天，今天页清空
        val st = TodayState("2026-07-15", listOf(task("a", true)))
        val (newState, record) = DayLogic.rollover(st, at(2026, 7, 19, 10, 0), zone)
        assertEquals("2026-07-19", newState.logicalDate)
        assertEquals("2026-07-15", record!!.date)
    }

    // ---------- 序列化 ----------

    @Test
    fun 序列化往返() {
        val json = Json { ignoreUnknownKeys = true }
        val st = TodayState(
            "2026-07-19",
            listOf(task("背单词", true), Task("x", "跑步 3 公里", Period.EVENING, false, 123L))
        )
        assertEquals(st, json.decodeFromString<TodayState>(json.encodeToString(st)))

        val history = listOf(DayRecord("2026-07-18", st.tasks))
        assertEquals(history, json.decodeFromString<List<DayRecord>>(json.encodeToString(history)))
    }
}

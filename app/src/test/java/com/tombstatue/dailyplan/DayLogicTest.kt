package com.tombstatue.dailyplan

import com.tombstatue.dailyplan.data.DayPlan
import com.tombstatue.dailyplan.data.DayRecord
import com.tombstatue.dailyplan.data.Event
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayLogicTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val json = Json { ignoreUnknownKeys = true }

    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun task(text: String, done: Boolean, fromPlan: Boolean = false) =
        Task(id = "id-$text", text = text, period = Period.MORNING, done = done, createdAt = 0L, fromPlan = fromPlan)

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

    // ---------- 结算 v2 ----------

    @Test
    fun 同一天不结算() {
        val st = TodayState("2026-07-19", listOf(task("a", false)))
        val plans = listOf(DayPlan("2026-07-20", tasks = listOf(task("p", false))))
        val r = DayLogic.rollover(st, plans, at(2026, 7, 19, 10, 0), zone)
        assertEquals(st, r.newToday)
        assertTrue(r.archived.isEmpty())
        assertEquals(plans, r.remainingPlans)
    }

    @Test
    fun 凌晨使用不触发重置() {
        val st = TodayState("2026-07-18", listOf(task("a", false)))
        val r = DayLogic.rollover(st, emptyList(), at(2026, 7, 19, 2, 0), zone)
        assertEquals(st, r.newToday)
        assertTrue(r.archived.isEmpty())
    }

    @Test
    fun 跨天清空并归档含未完成与事件() {
        val st = TodayState(
            "2026-07-18",
            listOf(task("a", true), task("b", false)),
            listOf(Event("e1", "考试"))
        )
        val r = DayLogic.rollover(st, emptyList(), at(2026, 7, 19, 10, 0), zone)
        assertEquals("2026-07-19", r.newToday.logicalDate)
        assertTrue(r.newToday.tasks.isEmpty())
        assertTrue(r.newToday.events.isEmpty())
        assertEquals(1, r.archived.size)
        assertEquals("2026-07-18", r.archived[0].date)
        assertEquals(2, r.archived[0].tasks.size)
        assertTrue(r.archived[0].tasks[0].done)
        assertFalse(r.archived[0].tasks[1].done)
        assertEquals("考试", r.archived[0].events[0].text)
    }

    @Test
    fun 空任务日不产生历史() {
        val st = TodayState("2026-07-18", emptyList())
        val r = DayLogic.rollover(st, emptyList(), at(2026, 7, 19, 10, 0), zone)
        assertEquals("2026-07-19", r.newToday.logicalDate)
        assertTrue(r.archived.isEmpty())
    }

    @Test
    fun 次日规划自动并入并置顶加粗标记() {
        val st = TodayState("2026-07-18", listOf(task("old", true)))
        val plans = listOf(
            DayPlan(
                "2026-07-19",
                events = listOf(Event("e1", "高数期中")),
                tasks = listOf(task("复习错题", false))
            ),
            DayPlan("2026-07-25", tasks = listOf(task("远期", false)))
        )
        val r = DayLogic.rollover(st, plans, at(2026, 7, 19, 10, 0), zone)
        assertEquals("2026-07-19", r.newToday.logicalDate)
        assertEquals(1, r.newToday.tasks.size)
        assertTrue(r.newToday.tasks[0].fromPlan)              // 规划标记
        assertEquals("高数期中", r.newToday.events[0].text)     // 事件横幅
        assertEquals(listOf(plans[1]), r.remainingPlans)       // 已消费的被移除，远期保留
    }

    @Test
    fun 跳过多天时被跳过的规划逐日归档为全部未完成() {
        // 7月15日后没打开 app，7月16/17 都有规划，7月19日才打开
        val st = TodayState("2026-07-15", listOf(task("old", true)))
        val plans = listOf(
            DayPlan("2026-07-17", tasks = listOf(task("十七日的事", false))),
            DayPlan("2026-07-16", tasks = listOf(task("十六日的事", true))), // 存储中即便误标 done 也按未完成归档
            DayPlan("2026-07-19", tasks = listOf(task("今天的事", false)))
        )
        val r = DayLogic.rollover(st, plans, at(2026, 7, 19, 10, 0), zone)
        // 归档按日期升序：15（原今天）、16、17
        assertEquals(listOf("2026-07-15", "2026-07-16", "2026-07-17"), r.archived.map { it.date })
        assertFalse(r.archived[1].tasks[0].done)               // 跳过日强制未完成
        assertTrue(r.archived[1].tasks[0].fromPlan)
        assertEquals("今天的事", r.newToday.tasks[0].text)
        assertTrue(r.remainingPlans.isEmpty())
    }

    @Test
    fun 无规划的新一天为空白页() {
        val st = TodayState("2026-07-18", listOf(task("a", false)))
        val plans = listOf(DayPlan("2026-07-25", tasks = listOf(task("远期", false))))
        val r = DayLogic.rollover(st, plans, at(2026, 7, 19, 10, 0), zone)
        assertTrue(r.newToday.tasks.isEmpty())
        assertEquals(plans, r.remainingPlans)
    }

    // ---------- 序列化与升级兼容 ----------

    @Test
    fun 序列化往返() {
        val st = TodayState(
            "2026-07-19",
            listOf(task("背单词", true), Task("x", "跑步 3 公里", Period.EVENING, false, 123L, fromPlan = true)),
            listOf(Event("e1", "考试"))
        )
        assertEquals(st, json.decodeFromString<TodayState>(json.encodeToString(st)))

        val plans = listOf(DayPlan("2026-07-25", listOf(Event("e2", "截止")), listOf(task("p", false))))
        assertEquals(plans, json.decodeFromString<List<DayPlan>>(json.encodeToString(plans)))
    }

    @Test
    fun 旧版本JSON可正常读取() {
        // v1.0 存储的旧格式：Task 无 fromPlan，TodayState/DayRecord 无 events
        val oldTask = """{"id":"a","text":"背单词","period":"MORNING","done":true,"createdAt":1}"""
        val t = json.decodeFromString<Task>(oldTask)
        assertFalse(t.fromPlan)
        assertEquals("", t.batchId)   // v1.2 新增字段，旧数据默认空

        val oldToday = """{"logicalDate":"2026-07-18","tasks":[$oldTask]}"""
        val st = json.decodeFromString<TodayState>(oldToday)
        assertTrue(st.events.isEmpty())
        assertEquals(1, st.tasks.size)

        val oldRecord = """[{"date":"2026-07-17","tasks":[$oldTask]}]"""
        val history = json.decodeFromString<List<DayRecord>>(oldRecord)
        assertTrue(history[0].events.isEmpty())
    }
}

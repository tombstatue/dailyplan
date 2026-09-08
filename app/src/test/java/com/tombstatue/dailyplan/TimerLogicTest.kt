package com.tombstatue.dailyplan

import com.tombstatue.dailyplan.pomodoro.PomodoroMode
import com.tombstatue.dailyplan.pomodoro.TimerLogic
import com.tombstatue.dailyplan.pomodoro.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerLogicTest {

    private val now = 1_900_000_000_000L

    // ---------- remainingAtMillis ----------

    @Test
    fun `无墙钟基准时返回备用剩余`() {
        assertEquals(600, TimerLogic.remainingAtMillis(null, now, 600))
    }

    @Test
    fun `截止已过返回0`() {
        assertEquals(0, TimerLogic.remainingAtMillis(now - 1000, now, 600))
    }

    @Test
    fun `未到期向上取整计算剩余`() {
        assertEquals(61, TimerLogic.remainingAtMillis(now + 60_500, now, 600))
    }

    // ---------- start / pause ----------

    @Test
    fun `开始计时以剩余值为基准设置墙钟截止`() {
        val s = TimerState(remainingSec = 300)
        val started = TimerLogic.onStarted(s, now)
        assertEquals(now + 300_000, started.endAtMillis)
        assertTrue(started.running)
        assertFalse(started.finished)
    }

    @Test
    fun `剩余为0时开始使用总时长`() {
        val s = TimerState(remainingSec = 0, totalSec = 300, finished = true)
        val started = TimerLogic.onStarted(s, now)
        assertEquals(now + 300_000, started.endAtMillis)
        assertEquals(300, started.remainingSec)
        assertFalse(started.finished)
    }

    @Test
    fun `暂停结算剩余并清除墙钟`() {
        val s = TimerState(endAtMillis = now + 90_500, running = true)
        val paused = TimerLogic.onPaused(s, now)
        assertEquals(91, paused.remainingSec)
        assertNull(paused.endAtMillis)
        assertFalse(paused.running)
    }

    // ---------- tick ----------

    @Test
    fun `到点结清并计入工作番茄`() {
        val s = TimerState(mode = PomodoroMode.WORK, totalSec = 300, endAtMillis = now, running = true, sessionsToday = 2, focusSecToday = 100)
        val (next, done) = TimerLogic.onTick(s, now)
        assertTrue(done)
        assertEquals(3, next.sessionsToday)
        assertEquals(400, next.focusSecToday)
        assertTrue(next.finished)
        assertFalse(next.running)
        assertNull(next.endAtMillis)
        assertEquals(0, next.remainingSec)
    }

    @Test
    fun `休息模式到点不计数`() {
        val s = TimerState(mode = PomodoroMode.LONG_BREAK, totalSec = 900, endAtMillis = now, running = true, sessionsToday = 2, focusSecToday = 100)
        val (next, done) = TimerLogic.onTick(s, now)
        assertTrue(done)
        assertEquals(2, next.sessionsToday)
        assertEquals(100, next.focusSecToday)
    }

    @Test
    fun `未到点只刷新快照`() {
        val s = TimerState(endAtMillis = now + 70_000, running = true)
        val (next, done) = TimerLogic.onTick(s, now)
        assertFalse(done)
        assertEquals(70, next.remainingSec)
        assertTrue(next.running)
    }

    // ---------- hydrate（进程恢复） ----------

    @Test
    fun `恢复时已过期则直接结清并计入完成`() {
        val s = TimerState(mode = PomodoroMode.WORK, totalSec = 300, endAtMillis = now - 5_000, running = true, sessionsToday = 0, focusSecToday = 0)
        val h = TimerLogic.hydrate(s, now)
        assertTrue(h.finished)
        assertEquals(1, h.sessionsToday)
        assertEquals(300, h.focusSecToday)
    }

    @Test
    fun `恢复时未过期继续墙钟`() {
        val s = TimerState(endAtMillis = now + 30_000, running = true, remainingSec = 999)
        val h = TimerLogic.hydrate(s, now)
        assertTrue(h.running)
        assertEquals(s.endAtMillis, h.endAtMillis)
        assertEquals(30, h.remainingSec)
    }

    @Test
    fun `暂停状态恢复原样`() {
        val s = TimerState(running = false, remainingSec = 420)
        assertEquals(s, TimerLogic.hydrate(s, now))
    }

    // ---------- 跨天计数 ----------

    @Test
    fun `同一天不重置计数`() {
        val s = TimerState(sessionsToday = 3, focusSecToday = 1500, dayKey = "2026-09-08")
        assertEquals(3, TimerLogic.refreshDayCounters(s, "2026-09-08").sessionsToday)
    }

    @Test
    fun `跨天清零计数`() {
        val s = TimerState(sessionsToday = 3, focusSecToday = 1500, dayKey = "2026-09-08")
        val r = TimerLogic.refreshDayCounters(s, "2026-09-09")
        assertEquals(0, r.sessionsToday)
        assertEquals(0, r.focusSecToday)
        assertEquals("2026-09-09", r.dayKey)
    }
}

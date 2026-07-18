package com.tombstatue.dailyplan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tombstatue.dailyplan.data.DayPlan
import com.tombstatue.dailyplan.data.DayRecord
import com.tombstatue.dailyplan.data.Period
import com.tombstatue.dailyplan.data.PlanRepository
import com.tombstatue.dailyplan.data.TodayState
import com.tombstatue.dailyplan.logic.DayLogic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val today: TodayState,
    val history: List<DayRecord>,
    val plans: List<DayPlan>
)

class PlanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PlanRepository(app)

    val ui: StateFlow<UiState> = repo.state
        .map { UiState(it.today, it.history, it.plans) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UiState(
                TodayState(DayLogic.logicalDate(System.currentTimeMillis()), emptyList()),
                emptyList(),
                emptyList()
            )
        )

    init {
        checkRollover()
    }

    /** 启动/回前台时调用：跨天则结算归档并并入规划 */
    fun checkRollover() = viewModelScope.launch { repo.rolloverIfNeeded() }

    // ---------- 今天页 ----------

    fun add(period: Period, text: String) = viewModelScope.launch {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            repo.rolloverIfNeeded() // 防止跨天瞬间把新任务加进昨天
            repo.addTask(period, trimmed)
        }
    }

    fun toggle(id: String) = viewModelScope.launch { repo.toggleTask(id) }

    fun delete(id: String) = viewModelScope.launch { repo.deleteTask(id) }

    // ---------- 未来规划（日详情页） ----------

    fun addPlanTask(date: String, period: Period, text: String) = viewModelScope.launch {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) repo.addPlanTask(date, period, trimmed)
    }

    fun addPlanEvent(date: String, text: String) = viewModelScope.launch {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) repo.addPlanEvent(date, trimmed)
    }

    fun deletePlanTask(date: String, id: String) = viewModelScope.launch { repo.deletePlanTask(date, id) }

    fun deletePlanEvent(date: String, id: String) = viewModelScope.launch { repo.deletePlanEvent(date, id) }
}

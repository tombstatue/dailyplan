package com.tombstatue.dailyplan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    val history: List<DayRecord>
)

class PlanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PlanRepository(app)

    val ui: StateFlow<UiState> = repo.state
        .map { (today, history) -> UiState(today, history) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UiState(
                TodayState(DayLogic.logicalDate(System.currentTimeMillis()), emptyList()),
                emptyList()
            )
        )

    init {
        checkRollover()
    }

    /** 启动/回前台时调用：跨天则结算归档 */
    fun checkRollover() = viewModelScope.launch { repo.rolloverIfNeeded() }

    fun add(period: Period, text: String) = viewModelScope.launch {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            repo.rolloverIfNeeded() // 防止跨天瞬间把新任务加进昨天
            repo.addTask(period, trimmed)
        }
    }

    fun toggle(id: String) = viewModelScope.launch { repo.toggleTask(id) }

    fun delete(id: String) = viewModelScope.launch { repo.deleteTask(id) }
}

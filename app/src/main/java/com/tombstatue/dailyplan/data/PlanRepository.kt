package com.tombstatue.dailyplan.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tombstatue.dailyplan.logic.DayLogic
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "dailyplan")

/** 本地存储仓库：今天状态 + 历史归档，全部 JSON 存入 DataStore */
class PlanRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val keyToday = stringPreferencesKey("today_state")
    private val keyHistory = stringPreferencesKey("history")

    /** (今天状态, 历史记录) 数据流，UI 层订阅 */
    val state: Flow<Pair<TodayState, List<DayRecord>>> = context.dataStore.data.map { prefs ->
        decodeToday(prefs[keyToday]) to decodeHistory(prefs[keyHistory])
    }

    private fun decodeToday(raw: String?): TodayState =
        raw?.let { runCatching { json.decodeFromString<TodayState>(it) }.getOrNull() }
            ?: TodayState(DayLogic.logicalDate(System.currentTimeMillis()), emptyList())

    private fun decodeHistory(raw: String?): List<DayRecord> =
        raw?.let { runCatching { json.decodeFromString<List<DayRecord>>(it) }.getOrNull() }
            ?: emptyList()

    /** 启动/回前台时调用：跨天则归档昨天并清空今天页 */
    suspend fun rolloverIfNeeded() {
        context.dataStore.edit { prefs ->
            val today = decodeToday(prefs[keyToday])
            val (newState, record) = DayLogic.rollover(today, System.currentTimeMillis())
            if (record != null) {
                val history = decodeHistory(prefs[keyHistory]) + record
                prefs[keyHistory] = json.encodeToString(history)
            }
            if (newState != today) {
                prefs[keyToday] = json.encodeToString(newState)
            }
        }
    }

    suspend fun addTask(period: Period, text: String) = mutateToday { st ->
        st.copy(
            tasks = st.tasks + Task(
                id = UUID.randomUUID().toString(),
                text = text,
                period = period,
                done = false,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleTask(id: String) = mutateToday { st ->
        st.copy(tasks = st.tasks.map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    suspend fun deleteTask(id: String) = mutateToday { st ->
        st.copy(tasks = st.tasks.filterNot { it.id == id })
    }

    private suspend fun mutateToday(transform: (TodayState) -> TodayState) {
        context.dataStore.edit { prefs ->
            prefs[keyToday] = json.encodeToString(transform(decodeToday(prefs[keyToday])))
        }
    }
}

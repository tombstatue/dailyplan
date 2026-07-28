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

/** 仓库对外的完整存储状态 */
data class StoredState(
    val today: TodayState,
    val history: List<DayRecord>,
    val plans: List<DayPlan>
)

/** 本地存储仓库：今天状态 + 历史归档 + 未来规划，全部 JSON 存入 DataStore */
class PlanRepository(private val context: Context) {

    companion object { private const val TAG = "PlanRepository" }

    private val json = Json { ignoreUnknownKeys = true }
    private val keyToday = stringPreferencesKey("today_state")
    private val keyHistory = stringPreferencesKey("history")
    private val keyPlans = stringPreferencesKey("future_plans")

    val state: Flow<StoredState> = context.dataStore.data.map { prefs ->
        StoredState(
            today = decodeToday(prefs[keyToday]),
            history = decodeHistory(prefs[keyHistory]),
            plans = decodePlans(prefs[keyPlans])
        )
    }

    private fun decodeToday(raw: String?): TodayState =
        raw?.let { runCatching { json.decodeFromString<TodayState>(it) }.onFailure { android.util.Log.e(TAG, "today JSON 解析失败，将重置为空白", it) }.getOrNull() }
            ?: TodayState(DayLogic.logicalDate(System.currentTimeMillis()), emptyList())

    private fun decodeHistory(raw: String?): List<DayRecord> =
        raw?.let { runCatching { json.decodeFromString<List<DayRecord>>(it) }.onFailure { android.util.Log.e(TAG, "history JSON 解析失败，将重置为空", it) }.getOrNull() }
            ?: emptyList()

    private fun decodePlans(raw: String?): List<DayPlan> =
        raw?.let { runCatching { json.decodeFromString<List<DayPlan>>(it) }.onFailure { android.util.Log.e(TAG, "plans JSON 解析失败，将重置为空", it) }.getOrNull() }
            ?: emptyList()

    /** 启动/回前台时调用：跨天则结算（归档 + 并入规划），单事务完成 */
    suspend fun rolloverIfNeeded() {
        context.dataStore.edit { prefs ->
            val today = decodeToday(prefs[keyToday])
            val plans = decodePlans(prefs[keyPlans])
            val r = DayLogic.rollover(today, plans, System.currentTimeMillis())
            if (r.archived.isNotEmpty()) {
                prefs[keyHistory] = json.encodeToString(decodeHistory(prefs[keyHistory]) + r.archived)
            }
            if (r.newToday != today) {
                prefs[keyToday] = json.encodeToString(r.newToday)
            }
            if (r.remainingPlans != plans) {
                prefs[keyPlans] = json.encodeToString(r.remainingPlans)
            }
        }
    }

    // ---------- 今天页操作 ----------

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

    // ---------- 未来规划操作（日详情页） ----------

    suspend fun addPlanTask(date: String, period: Period, text: String) = mutatePlans { plans ->
        plans.upsert(date) {
            it.copy(
                tasks = it.tasks + Task(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    period = period,
                    done = false,
                    createdAt = System.currentTimeMillis(),
                    fromPlan = true
                )
            )
        }
    }

    suspend fun addPlanEvent(date: String, text: String) = mutatePlans { plans ->
        plans.upsert(date) {
            it.copy(events = it.events + Event(UUID.randomUUID().toString(), text))
        }
    }

    suspend fun deletePlanTask(date: String, id: String) = mutatePlans { plans ->
        plans.upsert(date) { it.copy(tasks = it.tasks.filterNot { t -> t.id == id }) }
    }

    suspend fun deletePlanEvent(date: String, id: String) = mutatePlans { plans ->
        plans.upsert(date) { it.copy(events = it.events.filterNot { e -> e.id == id }) }
    }

    /**
     * 批量添加同一条日程到多个日期。
     * @return 用于撤销的 batchId（UUID）
     */
    suspend fun batchAdd(dateRange: Iterable<String>, period: Period, text: String): String {
        val batchId = UUID.randomUUID().toString()
        mutatePlans { plans ->
            var result = plans
            for (date in dateRange) {
                result = result.upsert(date) { dp ->
                    // 跳过完全相同（text+period）的日程
                    val exists = dp.tasks.any { it.text == text && it.period == period }
                    if (exists) dp
                    else dp.copy(tasks = dp.tasks + Task(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        period = period,
                        done = false,
                        createdAt = System.currentTimeMillis(),
                        fromPlan = true,
                        batchId = batchId
                    ))
                }
            }
            result
        }
        return batchId
    }

    /** 撤销批量添加：移除所有 batchId 匹配的日程并清理空 DayPlan */
    suspend fun undoBatch(batchId: String) {
        mutatePlans { plans ->
            plans
                .map { it.copy(tasks = it.tasks.filterNot { t -> t.batchId == batchId }) }
                .filter { it.tasks.isNotEmpty() || it.events.isNotEmpty() }
                .sortedBy { it.date }
        }
    }

    /** 更新某日期的规划；更新后事件与日程都为空的条目自动清除 */
    private fun List<DayPlan>.upsert(date: String, transform: (DayPlan) -> DayPlan): List<DayPlan> {
        val updated = transform(find { it.date == date } ?: DayPlan(date))
        val rest = filterNot { it.date == date }
        return if (updated.tasks.isEmpty() && updated.events.isEmpty()) rest
        else (rest + updated).sortedBy { it.date }
    }

    private suspend fun mutatePlans(transform: (List<DayPlan>) -> List<DayPlan>) {
        context.dataStore.edit { prefs ->
            prefs[keyPlans] = json.encodeToString(transform(decodePlans(prefs[keyPlans])))
        }
    }
}

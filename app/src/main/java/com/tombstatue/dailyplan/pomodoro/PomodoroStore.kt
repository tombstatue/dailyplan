package com.tombstatue.dailyplan.pomodoro

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.pomoDataStore by preferencesDataStore(name = "dailyplan_pomodoro")

/** 番茄钟状态的 DataStore 持久化：进程被杀后恢复计时 */
object PomodoroStore {

    private const val TAG = "PomodoroStore"
    private val key = stringPreferencesKey("timer_state")
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var appContext: Context? = null

    val context: Context? get() = appContext

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun load(): TimerState {
        val ctx = appContext ?: return TimerState()
        return runCatching {
            ctx.pomoDataStore.data
                .first()[key]
                ?.let { json.decodeFromString<TimerState>(it) }
        }.onFailure { android.util.Log.e(TAG, "计时状态解析失败，重置为默认", it) }
            .getOrNull() ?: TimerState()
    }

    suspend fun save(state: TimerState) {
        val ctx = appContext ?: return
        runCatching { ctx.pomoDataStore.edit { it[key] = json.encodeToString(state) } }
            .onFailure { android.util.Log.e(TAG, "计时状态写盘失败", it) }
    }
}

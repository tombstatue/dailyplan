package com.tombstatue.dailyplan.pomodoro

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** 用户偏好设置（白名单 / 主题 / 验证码开关），独立于计时状态 */
object SettingsStore {

    private val Context.dataStore by preferencesDataStore(name = "dailyplan_settings")
    private val keyWhitelist = stringSetPreferencesKey("whitelist")
    private val keyTheme = stringPreferencesKey("theme_id")
    private val keyCustomBg = stringPreferencesKey("custom_bg_path")
    private val keyFocusBg = stringPreferencesKey("focus_bg_path")
    private val keyForceCode = booleanPreferencesKey("force_exit_code")

    @Volatile private var appContext: Context? = null
    fun init(context: Context) { appContext = context.applicationContext }

    suspend fun whitelist(): Set<String> {
        val ctx = appContext ?: return emptySet()
        return ctx.dataStore.data.first()[keyWhitelist] ?: emptySet()
    }

    suspend fun setWhitelist(pkgs: Set<String>) {
        appContext?.dataStore?.edit { it[keyWhitelist] = pkgs }
    }

    suspend fun themeId(): String {
        val ctx = appContext ?: return "night"
        return ctx.dataStore.data.first()[keyTheme] ?: "night"
    }

    suspend fun setThemeId(id: String) {
        appContext?.dataStore?.edit { it[keyTheme] = id }
    }

    suspend fun customBgPath(): String? {
        val ctx = appContext ?: return null
        return ctx.dataStore.data.first()[keyCustomBg]
    }

    suspend fun setCustomBgPath(path: String?) {
        appContext?.dataStore?.edit {
            if (path != null) it[keyCustomBg] = path else it.remove(keyCustomBg)
        }
    }

    suspend fun focusBgPath(): String? {
        val ctx = appContext ?: return null
        return ctx.dataStore.data.first()[keyFocusBg]
    }

    suspend fun setFocusBgPath(path: String?) {
        appContext?.dataStore?.edit {
            if (path != null) it[keyFocusBg] = path else it.remove(keyFocusBg)
        }
    }

    suspend fun forceExitCode(): Boolean {
        val ctx = appContext ?: return false
        return ctx.dataStore.data.first()[keyForceCode] ?: false
    }

    suspend fun setForceExitCode(on: Boolean) {
        appContext?.dataStore?.edit { it[keyForceCode] = on }
    }
}

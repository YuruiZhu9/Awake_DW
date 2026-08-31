package com.awakedw.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** 键名契约（逐字对应设计文档，改动需同步设计 §5.3）。 */
internal object PrefKeys {
    const val GOAL_ML = "goal_ml"
    const val CUP_ML = "cup_ml"
    const val WINDOW_START_MIN = "window_start_min"
    const val WINDOW_END_MIN = "window_end_min"
    const val INTERVAL_MIN = "interval_min"
    const val REMINDERS_ENABLED = "reminders_enabled"
    const val THEME_MODE = "theme_mode"
    const val ONBOARDING_DONE = "onboarding_done"
    const val CELEBRATED_DAY_KEY = "celebrated_day_key"
}

/** DataStore 实现：读取时缺省回落 [UserSettings] 默认值，未知主题名回落 FOLLOW_TIME。 */
class UserPreferencesRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : UserPreferencesRepository {
        override val settings: Flow<UserSettings> = dataStore.data.map { it.toUserSettings() }

        override suspend fun setGoalMl(v: Int) = edit { it[intPreferencesKey(PrefKeys.GOAL_ML)] = v }

        override suspend fun setCupMl(v: Int) = edit { it[intPreferencesKey(PrefKeys.CUP_ML)] = v }

        override suspend fun setWindow(
            startMin: Int,
            endMin: Int,
        ) = edit {
            it[intPreferencesKey(PrefKeys.WINDOW_START_MIN)] = startMin
            it[intPreferencesKey(PrefKeys.WINDOW_END_MIN)] = endMin
        }

        override suspend fun setIntervalMin(v: Int) =
            edit {
                it[intPreferencesKey(PrefKeys.INTERVAL_MIN)] = v
            }

        override suspend fun setRemindersEnabled(v: Boolean) =
            edit {
                it[booleanPreferencesKey(PrefKeys.REMINDERS_ENABLED)] = v
            }

        /** ThemeChoice 以枚举名字符串持久化。 */
        override suspend fun setThemeChoice(v: ThemeChoice) = edit { it[stringPreferencesKey(PrefKeys.THEME_MODE)] = v.name }

        override suspend fun markCelebrated(dayKey: String) =
            edit {
                it[stringPreferencesKey(PrefKeys.CELEBRATED_DAY_KEY)] = dayKey
            }

        override suspend fun celebratedDayKey(): String? = dataStore.data.first()[stringPreferencesKey(PrefKeys.CELEBRATED_DAY_KEY)]

        override suspend fun markOnboardingDone() = edit { it[booleanPreferencesKey(PrefKeys.ONBOARDING_DONE)] = true }

        override suspend fun onboardingDone(): Boolean = dataStore.data.first()[booleanPreferencesKey(PrefKeys.ONBOARDING_DONE)] ?: false

        private suspend fun edit(block: (MutablePreferences) -> Unit) {
            dataStore.edit(block)
        }
    }

private fun Preferences.toUserSettings(): UserSettings {
    val defaults = UserSettings()
    return UserSettings(
        goalMl = this[intPreferencesKey(PrefKeys.GOAL_ML)] ?: defaults.goalMl,
        cupMl = this[intPreferencesKey(PrefKeys.CUP_ML)] ?: defaults.cupMl,
        windowStartMin = this[intPreferencesKey(PrefKeys.WINDOW_START_MIN)] ?: defaults.windowStartMin,
        windowEndMin = this[intPreferencesKey(PrefKeys.WINDOW_END_MIN)] ?: defaults.windowEndMin,
        intervalMin = this[intPreferencesKey(PrefKeys.INTERVAL_MIN)] ?: defaults.intervalMin,
        remindersEnabled = this[booleanPreferencesKey(PrefKeys.REMINDERS_ENABLED)] ?: defaults.remindersEnabled,
        themeChoice = themeChoiceOrDefault(this[stringPreferencesKey(PrefKeys.THEME_MODE)]),
    )
}

private fun themeChoiceOrDefault(raw: String?): ThemeChoice =
    runCatching { ThemeChoice.valueOf(requireNotNull(raw)) }.getOrDefault(ThemeChoice.FOLLOW_TIME)

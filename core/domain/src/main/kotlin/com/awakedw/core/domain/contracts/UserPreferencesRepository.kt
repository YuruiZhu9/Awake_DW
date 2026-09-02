package com.awakedw.core.domain.contracts

import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** 用户设置仓储：DataStore Preferences 持久化（键名严格按设计 §5.3）。（原 :core:data 接口按依赖倒置下沉，成员签名不变） */
interface UserPreferencesRepository {
    val settings: Flow<UserSettings>

    suspend fun setGoalMl(v: Int)

    suspend fun setCupMl(v: Int)

    suspend fun setWindow(
        startMin: Int,
        endMin: Int,
    )

    suspend fun setIntervalMin(v: Int)

    suspend fun setRemindersEnabled(v: Boolean)

    suspend fun setThemeChoice(v: ThemeChoice)

    /** 记录「达成目标的日键」，用于当日只庆祝一次。 */
    suspend fun markCelebrated(dayKey: String)

    suspend fun celebratedDayKey(): String?

    suspend fun markOnboardingDone()

    suspend fun onboardingDone(): Boolean

    val soundEnabled: Flow<Boolean>

    suspend fun setSoundEnabled(v: Boolean)
}

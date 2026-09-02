package com.awakedw.feature.onboarding

import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** 内存版用户设置仓储：记录 onboarding 置位次数与调用轨迹，供 mark-once 断言。 */
class FakePrefsRepository(
    initial: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
) : UserPreferencesRepository {
    private val _settings = MutableStateFlow(initial)
    private val sound = MutableStateFlow(true)

    override val settings: Flow<UserSettings> = _settings

    /** markOnboardingDone 的调用次数：mark-once 语义下多次 complete 只应落一次。 */
    var markOnboardingCount: Int = 0
        private set

    override suspend fun setGoalMl(v: Int) = Unit

    override suspend fun setCupMl(v: Int) = Unit

    override suspend fun setWindow(
        startMin: Int,
        endMin: Int,
    ) = Unit

    override suspend fun setIntervalMin(v: Int) = Unit

    override suspend fun setRemindersEnabled(v: Boolean) = Unit

    override suspend fun setThemeChoice(v: ThemeChoice) = Unit

    override suspend fun markCelebrated(dayKey: String) = Unit

    override suspend fun celebratedDayKey(): String? = null

    override suspend fun markOnboardingDone() {
        markOnboardingCount += 1
    }

    override suspend fun onboardingDone(): Boolean = markOnboardingCount > 0

    override val soundEnabled: Flow<Boolean> = sound

    override suspend fun setSoundEnabled(v: Boolean) {
        sound.value = v
    }
}

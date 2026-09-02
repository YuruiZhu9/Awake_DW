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

    // —— v0.2 画廊与音效（内存版，仅满足契约加宽） ——
    private val unlocked = MutableStateFlow(emptySet<String>())
    private val unseen = MutableStateFlow(emptySet<String>())
    private val pinned = MutableStateFlow<String?>(null)
    private var daily: Pair<String, String>? = null
    private val sound = MutableStateFlow(true)

    override val unlockedOutfits: Flow<Set<String>> = unlocked

    override suspend fun markOutfitsUnlocked(ids: Collection<String>) {
        unlocked.value = unlocked.value + ids.toSet()
    }

    override val unseenOutfits: Flow<Set<String>> = unseen

    override suspend fun markOutfitsUnseen(ids: Collection<String>) {
        unseen.value = unseen.value + ids.toSet()
    }

    override suspend fun markOutfitsSeen(ids: Collection<String>) {
        unseen.value = unseen.value - ids.toSet()
    }

    override val pinnedOutfitId: Flow<String?> = pinned

    override suspend fun setPinnedOutfit(id: String?) {
        pinned.value = id
    }

    override suspend fun dailyOutfit(): Pair<String, String>? = daily

    override suspend fun setDailyOutfit(
        dayKey: String,
        outfitId: String,
    ) {
        daily = dayKey to outfitId
    }

    override val soundEnabled: Flow<Boolean> = sound

    override suspend fun setSoundEnabled(v: Boolean) {
        sound.value = v
    }
}

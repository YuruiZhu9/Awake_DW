package com.awakedw.feature.gallery

import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 内存版用户设置仓储（与 feature:home 的 FakePrefsRepository 同款假件模式，仅满足契约加宽）：
 * 画廊三管（unlockedOutfits / pinnedOutfitId / setPinnedOutfit）为真实现语义——
 * setter 即改 StateFlow，供 combine 管道与 Turbine 断言；其余成员为空转桩。
 */
class FakeGalleryPrefs(
    initial: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
) : UserPreferencesRepository {
    private val _settings = MutableStateFlow(initial)

    override val settings = _settings

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

    override suspend fun markOnboardingDone() = Unit

    override suspend fun onboardingDone(): Boolean = true

    // —— v0.2 画廊与音效（内存版） ——
    private val unlocked = MutableStateFlow(emptySet<String>())
    private val pinned = MutableStateFlow<String?>(null)
    private var daily: Pair<String, String>? = null
    private val sound = MutableStateFlow(true)

    override val unlockedOutfits: Flow<Set<String>> = unlocked

    override suspend fun markOutfitsUnlocked(ids: Collection<String>) {
        unlocked.value = unlocked.value + ids.toSet()
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

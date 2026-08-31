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

    /** —— v0.2 画廊与音效（键名：unlocked_outfits / pinned_outfit_id / daily_outfit_day / daily_outfit_id / sound_enabled）—— */
    val unlockedOutfits: Flow<Set<String>>

    /** 幂等合并写入已解锁 outfit id 集。 */
    suspend fun markOutfitsUnlocked(ids: Collection<String>)

    /** 用户手动指定的「今日之裙」；null = 跟随每日随机。 */
    val pinnedOutfitId: Flow<String?>

    suspend fun setPinnedOutfit(id: String?)

    /** (dayKey, outfitId)；无记录返回 null。 */
    suspend fun dailyOutfit(): Pair<String, String>?

    suspend fun setDailyOutfit(
        dayKey: String,
        outfitId: String,
    )

    /** 音效开关，默认 true。 */
    val soundEnabled: Flow<Boolean>

    suspend fun setSoundEnabled(v: Boolean)
}

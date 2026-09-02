package com.awakedw.core.domain

import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.Outfit
import com.awakedw.core.model.OutfitCatalog
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 把当前连胜对照目录，落库新解锁；返回本次新解锁的目录件（幂等，重复调用返回空）。 */
class UnlockOutfitsUseCase
    @Inject
    constructor(
        private val prefs: UserPreferencesRepository,
    ) {
        suspend operator fun invoke(currentStreakDays: Int): List<Outfit> {
            val have = prefs.unlockedOutfits.first()
            // 走 unlockedBy 升序契约（unlockDay 升序），差集过滤既有已解锁。
            val new = OutfitCatalog.unlockedBy(currentStreakDays).filter { it.id !in have }
            if (new.isNotEmpty()) {
                prefs.markOutfitsUnlocked(new.map { it.id })
                // 新裙入柜同步并入未看集（「新裙提示」数据源）：差集已滤既有件，
                // 重复解锁不会重复入集；进画廊后由 markOutfitsSeen 统一清账。
                prefs.markOutfitsUnseen(new.map { it.id })
            }
            return new
        }
    }

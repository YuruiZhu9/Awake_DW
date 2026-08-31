package com.awakedw.core.domain

import com.awakedw.core.common.AppClock
import com.awakedw.core.common.toDayKey
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.model.Outfit
import com.awakedw.core.model.OutfitCatalog
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

/** 今日之裙：钉选优先；否则对已解锁池按 dayKey 稳定随机（同日重启不变），落库。 */
class ResolveDailyOutfitUseCase
    @Inject
    constructor(
        private val prefs: UserPreferencesRepository,
        private val clock: AppClock,
    ) {
        suspend operator fun invoke(): Outfit {
            prefs.pinnedOutfitId.first()?.let { pinnedId -> OutfitCatalog.byId(pinnedId) }?.let { return it }
            val dayKey = clock.nowEpochMs().toDayKey(clock.zone())
            prefs.dailyOutfit()?.takeIf { it.first == dayKey }?.let { OutfitCatalog.byId(it.second) }?.let { return it }
            val unlockedIds = prefs.unlockedOutfits.first()
            val pool = OutfitCatalog.all.filter { it.id in unlockedIds }
            val picked = if (pool.isEmpty()) OutfitCatalog.byId("dress_00")!! else pickForDay(dayKey, pool)
            prefs.setDailyOutfit(dayKey, picked.id)
            return picked
        }
    }

/**
 * 纯函数（供测试与复用）：dayKey 稳定随机挑选，同池同 key 永远同结果。
 * 前置条件：[pool] 非空（空池由调用方回退 dress_00，不进入本函数）。
 */
fun pickForDay(
    dayKey: String,
    pool: List<Outfit>,
): Outfit = Random(dayKey.hashCode()).let { pool[it.nextInt(pool.size)] }

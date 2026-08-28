package com.awakedw.core.domain

import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 连续达标天数：以今天收尾、每日总量 ≥ 当前目标的最长连续天数。
 * 今天尚未达标只视为「还没算」，不打破此前连续性；目标改动即时生效。
 */
class GetStreakUseCase
    @Inject
    constructor(
        private val water: WaterRepository,
        private val prefs: UserPreferencesRepository,
    ) {
        suspend operator fun invoke(): Int {
            val goalMl = prefs.settings.first().goalMl
            val bars = water.weekBars(daysBack = STREAK_LOOKBACK_DAYS)
            var index = bars.lastIndex
            // 今天未达标则从昨天起算（今天的量还在路上，不能既作数又不算数）。
            if (bars.last().totalMl < goalMl && index > 0) {
                index -= 1
            }
            var streak = 0
            while (index >= 0 && bars[index].totalMl >= goalMl) {
                streak += 1
                index -= 1
            }
            return streak
        }

        companion object {
            /** 连胜回看窗口上限（天）：再久远的历史对打卡激励已无意义。 */
            const val STREAK_LOOKBACK_DAYS = 366
        }
    }

package com.awakedw.core.domain

import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.DailyStats
import com.awakedw.core.model.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** 首页一屏所需聚合快照：今日统计 + 目标 + 杯容 + 当前主题 + 连胜天数。 */
data class HomeSnapshot(
    val stats: DailyStats,
    val goalMl: Int,
    val cupMl: Int,
    val themeId: ThemeId,
    val streakDays: Int,
)

/**
 * 首页快照流：用户设置变化或水库变化任一触发，即重算统计/目标/主题/连胜。
 * 主题解析必填：调用方（或 Hilt）显式注入 [ResolveThemeUseCase]，无隐式默认。
 */
class ObserveHomeUseCase
    @Inject
    constructor(
        private val water: WaterRepository,
        private val prefs: UserPreferencesRepository,
        private val theme: ResolveThemeUseCase,
    ) {
        private val themeFlow: Flow<ThemeId> = theme()

        private val getStreak = GetStreakUseCase(water, prefs)

        operator fun invoke(): Flow<HomeSnapshot> =
            combine(prefs.settings, water.changes, themeFlow) { settings, _, themeId ->
                HomeSnapshot(
                    stats = water.todayStats(),
                    goalMl = settings.goalMl,
                    cupMl = settings.cupMl,
                    themeId = themeId,
                    streakDays = getStreak(),
                )
            }
    }

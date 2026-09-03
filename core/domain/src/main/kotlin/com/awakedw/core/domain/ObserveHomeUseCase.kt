package com.awakedw.core.domain

import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.model.DailyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Home snapshot for the primary water logging screen. */
data class HomeSnapshot(
    val stats: DailyStats,
    val goalMl: Int,
    val cupMl: Int,
)

/** Recomputes the current water state when settings or records change. */
class ObserveHomeUseCase
    @Inject
    constructor(
        private val water: WaterRepository,
        private val prefs: UserPreferencesRepository,
    ) {
        operator fun invoke(): Flow<HomeSnapshot> =
            combine(prefs.settings, water.changes) { settings, _ ->
                HomeSnapshot(
                    stats = water.todayStats(),
                    goalMl = settings.goalMl,
                    cupMl = settings.cupMl,
                )
            }
    }

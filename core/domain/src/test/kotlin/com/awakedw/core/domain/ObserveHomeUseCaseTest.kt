package com.awakedw.core.domain

import com.awakedw.core.model.DailyStats
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ObserveHomeUseCaseTest {
    private lateinit var clock: FakeClock
    private lateinit var water: FakeWaterRepository
    private lateinit var prefs: FakeUserPreferencesRepository
    private lateinit var useCase: ObserveHomeUseCase

    @Before
    fun setUp() {
        clock = FakeClock(ZonedDateTime.of(2026, 8, 27, 12, 0, 0, 0, ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli())
        water = FakeWaterRepository(clock)
        prefs = FakeUserPreferencesRepository()
        useCase = ObserveHomeUseCase(water, prefs)
    }

    @Test
    fun `snapshot recomputes when settings and water change`() =
        runTest {
            water.addCup(250)
            val snapshots = mutableListOf<HomeSnapshot>()
            backgroundScope.launch {
                useCase().collect {
                    snapshots += it
                    if (snapshots.size >= 3) this.coroutineContext.cancel()
                }
            }
            runCurrent()
            prefs.setGoalMl(240)
            runCurrent()
            water.emitChange()
            runCurrent()

            val expected =
                HomeSnapshot(
                    stats = DailyStats(totalMl = 250, cupCount = 1, avgIntervalMin = null),
                    goalMl = 1600,
                    cupMl = 250,
                )
            val afterGoal = expected.copy(goalMl = 240)
            assertEquals(listOf(expected, afterGoal, afterGoal), snapshots)
        }

    @Test
    fun `snapshot contains current water settings`() =
        runTest {
            water.addCup(250)
            assertEquals(
                HomeSnapshot(
                    stats = DailyStats(totalMl = 250, cupCount = 1, avgIntervalMin = null),
                    goalMl = 1600,
                    cupMl = 250,
                ),
                useCase().first(),
            )
        }
}

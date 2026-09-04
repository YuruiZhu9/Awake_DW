package com.awakedw.feature.stats

import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.UserSettings
import com.awakedw.core.model.WaterRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

internal val BASE_TIME: Long =
    LocalDateTime.of(2026, 8, 27, 10, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()

internal val BASE_DAY_KEY = "2026-08-27"

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun harness(
        scheduler: TestCoroutineScheduler,
        settings: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
    ): Harness {
        val clock = FakeClock(BASE_TIME)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(settings)
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        val viewModel = StatsViewModel(clock = clock, water = water, prefs = prefs)
        return Harness(clock, water, prefs, viewModel)
    }

    private class Harness(
        val clock: FakeClock,
        val water: FakeWaterRepository,
        val prefs: FakePrefsRepository,
        val viewModel: StatsViewModel,
    )

    @Test
    fun `weekly bars include today and remain seven days`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedToday()
            runCurrent()

            assertEquals(1, h.viewModel.uiState.value.badges.cupCount)
            assertEquals(7, h.viewModel.uiState.value.bars.size)
            assertEquals(BASE_DAY_KEY, h.viewModel.uiState.value.bars.last().dayKey)
            assertEquals(250, h.viewModel.uiState.value.bars.last().totalMl)
        }

    @Test
    fun `goal changes from settings flow into statistics`() =
        runTest {
            val h = harness(testScheduler)
            assertEquals(1600, h.viewModel.uiState.value.goalMl)
            h.prefs.setGoalMl(2000)
            runCurrent()
            assertEquals(2000, h.viewModel.uiState.value.goalMl)
        }

    @Test
    fun `empty timeline becomes visible after first record`() =
        runTest {
            val h = harness(testScheduler)
            assertTrue(h.viewModel.uiState.value.timeline.isEmpty())
            h.water.seedToday()
            runCurrent()
            assertEquals(1, h.viewModel.uiState.value.timeline.size)
        }

    @Test
    fun `today count and average interval are factual summaries`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedToday(60, 30)
            runCurrent()
            val badges = h.viewModel.uiState.value.badges
            assertEquals(750, badges.totalMl)
            assertEquals(3, badges.cupCount)
            assertEquals("45 分钟", badges.avgIntervalLabel)
        }

    @Test
    fun `timeline is ordered by drinking time`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedToday(90, 30)
            runCurrent()
            val times: List<Long> = h.viewModel.uiState.value.timeline.map(WaterRecord::drankAtEpochMs)
            assertEquals(times.sorted(), times)
        }

    @Test
    fun `average interval uses a dash with zero or one record`() =
        runTest {
            val empty = harness(testScheduler)
            assertEquals("—", empty.viewModel.uiState.value.badges.avgIntervalLabel)
            val single = harness(testScheduler)
            single.water.seedToday()
            runCurrent()
            assertEquals("—", single.viewModel.uiState.value.badges.avgIntervalLabel)
        }
}

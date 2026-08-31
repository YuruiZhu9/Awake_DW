package com.awakedw.feature.stats

import com.awakedw.core.domain.GetStreakUseCase
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

/** 测试统一锚点：2026-08-27 10:00（Asia/Shanghai）。 */
internal val BASE_TIME: Long =
    LocalDateTime
        .of(2026, 8, 27, 10, 0)
        .atZone(ZoneId.of("Asia/Shanghai"))
        .toInstant()
        .toEpochMilli()

internal val BASE_DAY_KEY = "2026-08-27"

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：假钟 + 假仓储 + 真实连胜用例（与生产同一条解析路径）。 */
    private fun harness(
        scheduler: TestCoroutineScheduler,
        settings: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
    ): Harness {
        val clock = FakeClock(BASE_TIME)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(settings)
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        val viewModel =
            StatsViewModel(
                clock = clock,
                water = water,
                prefs = prefs,
                streak = GetStreakUseCase(water, prefs),
            )
        return Harness(clock, water, prefs, viewModel)
    }

    private class Harness(
        val clock: FakeClock,
        val water: FakeWaterRepository,
        val prefs: FakePrefsRepository,
        val viewModel: StatsViewModel,
    )

    @Test
    fun `连胜徽章经真实用例穿透——昨天起连两天而今天未达标`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedTotal("2026-08-25", totalMl = 1600)
            h.water.seedTotal("2026-08-26", totalMl = 1600)
            runCurrent()

            val badges = h.viewModel.uiState.value.badges
            assertEquals(2, badges.streakDays)
            // 今天还没喝：杯数徽章为 0，周条目仍含今天共 7 列。
            assertEquals(0, badges.cupCount)
            assertEquals(7, h.viewModel.uiState.value.bars.size)
            assertEquals(BASE_DAY_KEY, h.viewModel.uiState.value.bars.last().dayKey)
        }

    @Test
    fun `今日实时达标后连胜尾端翻转含今天`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedTotal("2026-08-25", totalMl = 1600)
            h.water.seedTotal("2026-08-26", totalMl = 1600)
            runCurrent()
            assertEquals(2, h.viewModel.uiState.value.badges.streakDays)

            // 今日这一大杯落库即达标：changes 流触发重算，连胜含今天翻成 3。
            h.water.seedToday(amountMl = 1600)
            runCurrent()

            assertEquals(3, h.viewModel.uiState.value.badges.streakDays)
            assertEquals(1600, h.viewModel.uiState.value.bars.last().totalMl)
        }

    @Test
    fun `目标在设置页改动后即时反映到统计页`() =
        runTest {
            val h = harness(testScheduler)
            assertEquals(1600, h.viewModel.uiState.value.goalMl)

            h.prefs.setGoalMl(2000)
            runCurrent()

            assertEquals(2000, h.viewModel.uiState.value.goalMl)
        }

    @Test
    fun `今日一杯未喝时时间线保持空态`() =
        runTest {
            val h = harness(testScheduler)

            val state = h.viewModel.uiState.value
            assertTrue(state.timeline.isEmpty())
            assertEquals(0, state.badges.cupCount)

            // 第一杯出现后空态解除，时间线升序可见。
            h.water.seedToday()
            runCurrent()

            assertEquals(1, h.viewModel.uiState.value.timeline.size)
        }

    @Test
    fun `今日杯数与平均间隔徽章复刻首页语义`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedToday(60, 30)
            runCurrent()

            val badges = h.viewModel.uiState.value.badges
            assertEquals(3, badges.cupCount)
            assertEquals("45 分钟", badges.avgIntervalLabel)
        }

    @Test
    fun `时间线按喝水时刻升序排列`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedToday(90, 30)
            runCurrent()

            val times: List<Long> = h.viewModel.uiState.value.timeline.map(WaterRecord::drankAtEpochMs)
            assertEquals(times.sorted(), times)
        }

    @Test
    fun `单杯与无杯时平均间隔徽章显示破折号`() =
        runTest {
            val empty = harness(testScheduler)
            assertEquals("—", empty.viewModel.uiState.value.badges.avgIntervalLabel)

            val single = harness(testScheduler)
            single.water.seedToday()
            runCurrent()
            assertEquals("—", single.viewModel.uiState.value.badges.avgIntervalLabel)
        }
}

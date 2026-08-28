package com.awakedw.feature.home

import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.TimeSlot
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** 测试统一锚点：2026-08-27 10:00（Asia/Shanghai，MORNING 时段）。 */
internal val BASE_TIME: Long =
    LocalDateTime.of(2026, 8, 27, 10, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：假钟 + 假仓储 + 真实用例链（与生产同一条解析路径）。 */
    private fun harness(
        scheduler: TestCoroutineScheduler,
        settings: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
    ): Harness {
        val clock = FakeClock(BASE_TIME)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(settings)
        val copies = FakeCopyLibraryRepository()
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        val viewModel =
            HomeViewModel(
                clock = clock,
                observeHome = ObserveHomeUseCase(water, prefs, ResolveThemeUseCase(prefs, clock)),
                logWater = LogWaterUseCase(water, prefs, clock),
                copies = copies,
            )
        return Harness(clock, water, prefs, copies, viewModel)
    }

    private class Harness(
        val clock: FakeClock,
        val water: FakeWaterRepository,
        val prefs: FakePrefsRepository,
        val copies: FakeCopyLibraryRepository,
        val viewModel: HomeViewModel,
    )

    @Test
    fun `防抖窗口内的连续两次点击只记一杯`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            advanceTimeBy(DEBOUNCE_MIDWAY_MS)
            runCurrent()
            h.viewModel.tapLogButton()
            advanceTimeBy(LOG_DEBOUNCE_MS + 500)
            runCurrent()

            assertEquals(1, h.water.addCount)
            assertEquals(250, h.viewModel.uiState.value.totalMl)
        }

    @Test
    fun `环区点按与按钮共用同一防抖闸门`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            h.viewModel.tapRing(null)
            h.viewModel.tapLogButton()
            advanceTimeBy(LOG_DEBOUNCE_MS + 500)
            runCurrent()

            assertEquals(1, h.water.addCount)
        }

    @Test
    fun `防抖窗口之外的两次点击各自成杯`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            advanceTimeBy(LOG_DEBOUNCE_MS + 500)
            runCurrent()
            h.viewModel.tapLogButton()
            advanceTimeBy(LOG_DEBOUNCE_MS + 500)
            runCurrent()

            assertEquals(2, h.water.addCount)
            assertEquals(500, h.viewModel.uiState.value.totalMl)
        }

    @Test
    fun `平均间隔徽章——无杯与单杯都显示破折号`() =
        runTest {
            val empty = harness(testScheduler)
            assertEquals("—", empty.viewModel.uiState.value.avgIntervalLabel)

            val single = harness(testScheduler)
            single.water.seedToday()
            runCurrent()
            val state = single.viewModel.uiState.value
            assertEquals(1, state.cupCount)
            assertEquals("—", state.avgIntervalLabel)
        }

    @Test
    fun `平均间隔徽章——三杯间隔60与30分钟显示45分钟`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedToday(60, 30)
            runCurrent()

            assertEquals("45 分钟", h.viewModel.uiState.value.avgIntervalLabel)
        }

    @Test
    fun `平均间隔徽章——间隔不小于90分钟切换为小时文案`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedToday(96)
            runCurrent()

            assertEquals("1.6h", h.viewModel.uiState.value.avgIntervalLabel)
        }

    @Test
    fun `达到目标瞬间进入庆祝态且当日后续打卡保持普通反馈`() =
        runTest {
            val h = harness(testScheduler)

            // 目标 1600ml ÷ 一杯 250ml：第 7 杯首次越过目标线。
            repeat(6) {
                h.viewModel.tapLogButton()
                advanceTimeBy(LOG_DEBOUNCE_MS + 100)
                runCurrent()
            }
            assertEquals(false, h.viewModel.uiState.value.celebrating)

            h.viewModel.tapLogButton()
            advanceTimeBy(LOG_DEBOUNCE_MS + 100)
            runCurrent()
            assertEquals(true, h.viewModel.uiState.value.celebrating)
            assertEquals(BASE_DAY_KEY, h.prefs.celebratedKeyValue)

            // 夸夸语 1.4s 收场；庆祝横幅撑满 2.5s 后自动收敛。
            advanceTimeBy(PRAISE_HOLD_MS)
            runCurrent()
            assertEquals(null, h.viewModel.uiState.value.praiseLine)
            assertEquals(true, h.viewModel.uiState.value.celebrating)

            advanceTimeBy(CELEBRATION_HOLD_MS - PRAISE_HOLD_MS)
            runCurrent()
            assertEquals(false, h.viewModel.uiState.value.celebrating)

            // 同日再打卡：celebrated_day_key 已记录，不再触发庆祝。
            h.viewModel.tapLogButton()
            advanceTimeBy(LOG_DEBOUNCE_MS + 100)
            runCurrent()
            assertEquals(false, h.viewModel.uiState.value.celebrating)
            assertEquals(8, h.water.addCount)
        }

    @Test
    fun `打卡瞬间从当前时段文案组抽取夸夸语`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            advanceTimeBy(LOG_DEBOUNCE_MS + 100)
            runCurrent()

            assertEquals(listOf(TimeSlot.MORNING), h.copies.requestedSlots)
            assertEquals("早安短句", h.viewModel.uiState.value.praiseLine)
        }

    @Test
    fun `进度越过目标后截断为满环`() =
        runTest {
            // 两「杯」各 1600ml：总量 3200 已超 1600 目标，progress 必须 clamp 到 1。
            val h = harness(testScheduler, UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD))
            h.water.seedToday(120, amountMl = 1600)
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(3200, state.totalMl)
            assertEquals(1f, state.progress, 0f)
        }

    @Test
    fun `进度按总量除以目标取小数`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedToday(60, 30)
            runCurrent()

            val state = h.viewModel.uiState.value
            assertEquals(750, state.totalMl)
            assertEquals(750f / 1600f, state.progress, 0f)
        }

    private companion object {
        const val DEBOUNCE_MIDWAY_MS = 300L
        const val PRAISE_HOLD_MS = 1_400L
        const val CELEBRATION_HOLD_MS = 2_500L
        val BASE_DAY_KEY = "2026-08-27"
    }
}

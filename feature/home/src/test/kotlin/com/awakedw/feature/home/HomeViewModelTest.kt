package com.awakedw.feature.home

import com.awakedw.core.domain.GetStreakUseCase
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.ResolveDailyOutfitUseCase
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.domain.UnlockOutfitsUseCase
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
                unlockOutfits = UnlockOutfitsUseCase(prefs),
                resolveDailyOutfit = ResolveDailyOutfitUseCase(prefs, clock),
                streakOf = GetStreakUseCase(water, prefs),
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
    fun `首触立即成笔且窗口内的连续点击只记一杯`() =
        runTest {
            val h = harness(testScheduler)

            // 立即性：不做任何时间推进，第一笔当场已成（规格 §4.1「按钮=立即记录」）。
            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(1, h.water.addCount)
            assertEquals(250, h.viewModel.uiState.value.totalMl)

            // 假钟未动——同刻连点落在 800ms 窗口内，合并忽略。
            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(1, h.water.addCount)
        }

    @Test
    fun `环区点按与按钮共用同一防抖闸门`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            h.viewModel.tapRing(null)
            h.viewModel.tapLogButton()
            runCurrent()

            assertEquals(1, h.water.addCount)
        }

    @Test
    fun `防抖窗口之外的两次点击各自成杯`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            h.clock.ms += WINDOW_GAP_MS
            h.viewModel.tapLogButton()
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

            // 目标 1600ml ÷ 一杯 250ml：第 7 杯首次越过目标线（每杯拨钟跨出防抖窗）。
            repeat(6) {
                h.clock.ms += CUP_SPACING_MS
                h.viewModel.tapLogButton()
                runCurrent()
            }
            assertEquals(false, h.viewModel.uiState.value.celebrating)

            h.clock.ms += CUP_SPACING_MS
            h.viewModel.tapLogButton()
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
            h.clock.ms += CUP_SPACING_MS
            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(false, h.viewModel.uiState.value.celebrating)
            assertEquals(8, h.water.addCount)
        }

    @Test
    fun `打卡瞬间从当前时段文案组抽取夸夸语`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            runCurrent()

            // 第 1 抽是 init 的顶部问候语，第 2 抽才是本次打卡的夸夸语。
            assertEquals(listOf(TimeSlot.MORNING, TimeSlot.MORNING), h.copies.requestedSlots)
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

    @Test
    fun `进首页即从文案库当前时段抽问候语_每次新建VM都再抽一次`() =
        runTest {
            val h = harness(testScheduler)
            runCurrent()
            // 10:00 属 MORNING：init 首抽已完成，问候语即文案库该时段的句子。
            assertEquals(listOf(TimeSlot.MORNING), h.copies.requestedSlots)
            assertEquals("早安短句", h.viewModel.uiState.value.greeting)

            // 每次进入首页 = 新建导航条目 = 重建 VM = 再抽一次（设计 §9.2 每次加载有区别）。
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val second =
                HomeViewModel(
                    clock = h.clock,
                    observeHome = ObserveHomeUseCase(h.water, h.prefs, ResolveThemeUseCase(h.prefs, h.clock)),
                    logWater = LogWaterUseCase(h.water, h.prefs, h.clock),
                    copies = h.copies,
                    unlockOutfits = UnlockOutfitsUseCase(h.prefs),
                    resolveDailyOutfit = ResolveDailyOutfitUseCase(h.prefs, h.clock),
                    streakOf = GetStreakUseCase(h.water, h.prefs),
                )
            runCurrent()
            assertEquals(listOf(TimeSlot.MORNING, TimeSlot.MORNING), h.copies.requestedSlots)
            assertEquals("早安短句", second.uiState.value.greeting)
        }

    @Test
    fun `快捷量走同一闸门并写入自定义量`() =
        runTest {
            val h = harness(testScheduler)

            // 小口 125ml：立即成笔，总量即该量。
            h.viewModel.quickLog(125)
            runCurrent()
            assertEquals(125, h.viewModel.uiState.value.totalMl)

            // 窗内连点（主按钮/快捷量任意混用）合并为一笔。
            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(1, h.water.addCount)

            // 跨出防抖窗后标准杯正常成笔，总量 125 + 250。
            h.clock.ms += WINDOW_GAP_MS
            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(2, h.water.addCount)
            assertEquals(375, h.viewModel.uiState.value.totalMl)
        }

    @Test
    fun `最近一杯时刻浮出徽章数据`() =
        runTest {
            val h = harness(testScheduler)
            h.water.seedToday(30) // 10:00 与 10:30 两杯
            runCurrent()

            assertEquals(2, h.viewModel.uiState.value.cupCount)
            assertEquals("10:30", h.viewModel.uiState.value.lastDrinkLabel)
        }

    private companion object {
        /** 夸夸语停留 1.4s、庆祝横幅 2.5s：反馈时序断言用（与生产常量同值）。 */
        const val PRAISE_HOLD_MS = 1_400L
        const val CELEBRATION_HOLD_MS = 2_500L

        /** 相邻两杯的假钟间隔：跨出 800ms 防抖窗。 */
        const val CUP_SPACING_MS = 900L
        const val WINDOW_GAP_MS = 1_300L
        val BASE_DAY_KEY = "2026-08-27"
    }
}

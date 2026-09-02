package com.awakedw.feature.home

import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.model.CatMood
import com.awakedw.core.model.ThemeChoice
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
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** 深夜锚点：2026-08-27 22:00（Asia/Shanghai，猫安睡窗起点）。 */
internal val NIGHT_BASE_TIME: Long =
    LocalDateTime.of(2026, 8, 27, 22, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()

/**
 * 记一杯回应编排——胆大王的反馈（moodboard §6.2 首页接线）：
 * - 打卡成功：猫短暂 HAPPY + 猫语气泡命中 `randomCatLine` 抽句；气泡按 [CAT_LINE_HOLD_MS]（2.0s）
 *   收场（独立于夸夸语 1.4s），心情按当前小时落回（白天 IDLE）；
 * - 同日已达标后再打卡（celebrated=false）猫仍 HAPPY 一次——回应每次成笔；
 * - init 按当前小时定初态：22 点后安睡（SLEEPY）；
 * - `petCat()`：摸猫即抽一句猫语，同 2.0s 收场；猫序列换代不殃及夸夸语/庆祝的收场（epoch 分家）；
 * - 猫配饰随连胜刷新：init 结算一次，打卡成功后再结算（streak 过门槛即披挂）。
 *
 * 猫语停留时长经构造器缺省参注入（生产 [CAT_LINE_HOLD_MS]，测试缩窗）——与 logDebounceMs 同款。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelCatTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：与 HomeViewModelTest 同一条真实用例链，外加连胜用例与本任务的猫时长缩窗。 */
    private fun harness(
        scheduler: TestCoroutineScheduler,
        settings: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
        clockMs: Long = BASE_TIME,
        catLineHoldMs: Long = CAT_LINE_HOLD_MS,
        catLines: List<String> = listOf("喵，喝水啦"),
    ): Harness {
        val clock = FakeClock(clockMs)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(settings)
        val copies = FakeCopyLibraryRepository(catLines = catLines)
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        val viewModel =
            HomeViewModel(
                clock = clock,
                observeHome = ObserveHomeUseCase(water, prefs, ResolveThemeUseCase(prefs, clock)),
                logWater = LogWaterUseCase(water, prefs, clock),
                copies = copies,
                sound = FakeSoundPlayer(),
                catLineHoldMs = catLineHoldMs,
            )
        return Harness(clock, viewModel)
    }

    private class Harness(
        val clock: FakeClock,
        val viewModel: HomeViewModel,
    )

    @Test
    fun `打卡成功猫升HAPPY抽猫语且气泡按2秒独立收场`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(CatMood.HAPPY, h.viewModel.uiState.value.catMood)
            assertEquals("喵，喝水啦", h.viewModel.uiState.value.catLine)

            // 夸夸语 1.4s 先收场，猫气泡不受牵连（独立时长）。
            advanceTimeBy(PRAISE_HOLD_MS)
            runCurrent()
            assertEquals(null, h.viewModel.uiState.value.praiseLine)
            assertEquals("喵，喝水啦", h.viewModel.uiState.value.catLine)
            assertEquals(CatMood.HAPPY, h.viewModel.uiState.value.catMood)

            // 撑满 2.0s：气泡清空，心情按当前小时（10 点）回落 IDLE。
            advanceTimeBy(CAT_LINE_HOLD_MS - PRAISE_HOLD_MS)
            runCurrent()
            assertNull(h.viewModel.uiState.value.catLine)
            assertEquals(CatMood.IDLE, h.viewModel.uiState.value.catMood)
        }

    @Test
    fun `同日已达标后再打卡猫仍HAPPY一次`() =
        runTest {
            // 目标压到 100ml：首杯即庆祝；第二杯 celebrated=false——猫仍要 HAPPY（回应每次成笔）。
            val h =
                harness(
                    testScheduler,
                    settings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD, goalMl = 100),
                )

            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(true, h.viewModel.uiState.value.celebrating)
            assertEquals(CatMood.HAPPY, h.viewModel.uiState.value.catMood)

            // 第一轮全部收场（庆祝 2.5s 为最后一拍）。
            advanceTimeBy(CELEBRATION_HOLD_MS)
            runCurrent()
            assertEquals(false, h.viewModel.uiState.value.celebrating)
            assertEquals(CatMood.IDLE, h.viewModel.uiState.value.catMood)

            h.clock.ms += WINDOW_GAP_MS
            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(false, h.viewModel.uiState.value.celebrating)
            assertEquals(CatMood.HAPPY, h.viewModel.uiState.value.catMood)
            assertEquals("喵，喝水啦", h.viewModel.uiState.value.catLine)
        }

    @Test
    fun `init时深夜22点猫为安睡态`() =
        runTest {
            val h = harness(testScheduler, clockMs = NIGHT_BASE_TIME)
            runCurrent()

            assertEquals(CatMood.SLEEPY, h.viewModel.uiState.value.catMood)
            assertNull(h.viewModel.uiState.value.catLine)
        }

    @Test
    fun `摸猫抽一句新猫语并按停留时长收场`() =
        runTest {
            val h =
                harness(
                    testScheduler,
                    catLineHoldMs = SHRUNK_CAT_HOLD_MS,
                    catLines = listOf("喵一句", "喵二句", "喵三句"),
                )
            runCurrent()
            assertNull(h.viewModel.uiState.value.catLine)

            h.viewModel.petCat()
            runCurrent()
            assertEquals("喵一句", h.viewModel.uiState.value.catLine)
            // 摸猫只添气泡，不动心情（10 点白天保持 IDLE）。
            assertEquals(CatMood.IDLE, h.viewModel.uiState.value.catMood)

            // 立刻再摸：新一句当场顶掉旧一句（猫序列防串场，同一气泡位）。
            h.viewModel.petCat()
            runCurrent()
            assertEquals("喵二句", h.viewModel.uiState.value.catLine)

            // 撑满停留时长：气泡收场（旧一摸的收场被换代拦下，只有最新守卫生效）。
            advanceTimeBy(SHRUNK_CAT_HOLD_MS)
            runCurrent()
            assertNull(h.viewModel.uiState.value.catLine)
        }

    @Test
    fun `摸猫不殃及夸夸语与庆祝的定时收场`() =
        runTest {
            val h =
                harness(
                    testScheduler,
                    settings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD, goalMl = 100),
                )

            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(true, h.viewModel.uiState.value.celebrating)

            // 打卡后立刻摸猫：猫序列换代，但夸夸/庆祝归 feedbackEpoch 管，照常收场。
            h.viewModel.petCat()
            runCurrent()

            advanceTimeBy(PRAISE_HOLD_MS)
            runCurrent()
            assertEquals(null, h.viewModel.uiState.value.praiseLine)

            advanceTimeBy(CELEBRATION_HOLD_MS - PRAISE_HOLD_MS)
            runCurrent()
            assertEquals(false, h.viewModel.uiState.value.celebrating)

            // 猫气泡按摸猫时刻起的 2.0s 收场（此刻已被覆盖经过）。
            assertNull(h.viewModel.uiState.value.catLine)
        }

    private companion object {
        /** 夸夸语 1.4s、庆祝横幅 2.5s：与生产常量同值，用于跨序列收场节奏断言。 */
        const val PRAISE_HOLD_MS = 1_400L
        const val CELEBRATION_HOLD_MS = 2_500L

        /** 相邻两次成笔的假钟间隔：跨出 800ms 防抖窗。 */
        const val WINDOW_GAP_MS = 1_300L

        /** 缩窗后的猫语停留时长：只验证「注入时长被遵守」，不与夸夸语节奏耦合。 */
        const val SHRUNK_CAT_HOLD_MS = 300L
    }
}

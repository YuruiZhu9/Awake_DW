package com.awakedw.feature.home

import com.awakedw.core.domain.DeleteWaterRecordUseCase
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/** 深夜锚点：2026-08-27 22:00（Asia/Shanghai，猫安睡窗起点）。 */
internal val NIGHT_BASE_TIME: Long =
    LocalDateTime.of(2026, 8, 27, 22, 0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()

/** 常驻入口文案随猫状态一致（0.9.0 轨道三）：安睡态换安睡邀请，其余保持原入口。 */
class CatHintTest {
    @Test
    fun `安睡态入口换安睡邀请且保持常驻语义`() {
        assertEquals("嘘，我在睡~", catHintOf(CatMood.SLEEPY))
    }

    @Test
    fun `清醒与开心态维持原常驻入口`() {
        assertEquals("点击我试试~", catHintOf(CatMood.IDLE))
        assertEquals("点击我试试~", catHintOf(CatMood.HAPPY))
    }
}

/**
 * 记一杯回应编排——胆大王的反馈（moodboard §6.2 首页接线）：
 * - 打卡成功：猫短暂 HAPPY + 猫语气泡命中**内置猫语池**；气泡按 [CAT_LINE_HOLD_MS]（3.0s）
 *   收场——与环心打卡引文同拍（视觉基线 §12.2），心情按当前小时落回（白天 IDLE）；
 * - 同日已达标后再打卡（celebrated=false）猫仍 HAPPY 一次——回应每次成笔；
 * - init 按当前小时定初态：22 点后安睡（SLEEPY）；
 * - `petCat()`：摸猫即抽一句猫语，同 3.0s 收场；猫序列换代不殃及环心确认与达标横幅的收场；
 *
 * 猫气泡停留时长经构造器缺省参注入（生产 [CAT_LINE_HOLD_MS]，测试缩窗）——与 logDebounceMs 同款。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelCatTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：与 HomeViewModelTest 同一条真实用例链，外加猫反馈的缩窗时长。 */
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
                observeHome = ObserveHomeUseCase(water, prefs),
                logWater = LogWaterUseCase(water, prefs, clock),
                deleteWater = DeleteWaterRecordUseCase(water),
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
    fun `打卡成功猫升HAPPY抽猫语且与环心引文同拍收场`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(CatMood.HAPPY, h.viewModel.uiState.value.catMood)
            assertEquals("喵，喝水啦", h.viewModel.uiState.value.catLine)
            assertNotNull(h.viewModel.uiState.value.centerNote)

            // 同屏同拍：打卡引文与猫语在同一刻一起收场（视觉基线 §12.2），
            // 心情按当前小时（10 点）回落 IDLE。
            advanceTimeBy(CAT_LINE_HOLD_MS)
            runCurrent()
            assertNull(h.viewModel.uiState.value.centerNote)
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

            // 第一轮全部收场：庆祝横幅 2.5s 先收，打卡引文与猫语 3.0s 同拍后收。
            advanceTimeBy(CELEBRATION_HOLD_MS)
            runCurrent()
            assertEquals(false, h.viewModel.uiState.value.celebrating)

            advanceTimeBy(CAT_LINE_HOLD_MS - CELEBRATION_HOLD_MS)
            runCurrent()
            assertEquals(CatMood.IDLE, h.viewModel.uiState.value.catMood)
            assertNull(h.viewModel.uiState.value.catLine)

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
    fun `摸猫抽一句心意文案并按停留时长收场`() =
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
    fun `摸猫不殃及环心确认与达标横幅的定时收场`() =
        runTest {
            val h =
                harness(
                    testScheduler,
                    settings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD, goalMl = 100),
                )

            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(true, h.viewModel.uiState.value.celebrating)

            // 打卡后立刻摸猫：猫序列换代，但环心确认/庆祝归各自的代次管，照常收场。
            h.viewModel.petCat()
            runCurrent()

            // 庆祝横幅 2.5s 先收；环心引文 3.0s 仍留在环心。
            advanceTimeBy(CELEBRATION_HOLD_MS)
            runCurrent()
            assertEquals(false, h.viewModel.uiState.value.celebrating)
            assertNotNull(h.viewModel.uiState.value.centerNote)

            advanceTimeBy(PRAISE_HOLD_MS - CELEBRATION_HOLD_MS)
            runCurrent()
            assertNull(h.viewModel.uiState.value.centerNote)

            // 猫气泡按摸猫时刻起的 3.0s 收场（此刻已被覆盖经过）。
            assertNull(h.viewModel.uiState.value.catLine)
        }

    private companion object {
        /** 环心引文 3.0s、猫语 3.0s（两者同拍）、庆祝横幅 2.5s：与生产常量同值。 */
        const val PRAISE_HOLD_MS = 3_000L
        const val CAT_LINE_HOLD_MS = 3_000L
        const val CELEBRATION_HOLD_MS = 2_500L

        /** 相邻两次成笔的假钟间隔：跨出 800ms 防抖窗。 */
        const val WINDOW_GAP_MS = 1_300L

        /** 缩窗后的猫语停留时长：只验证「注入时长被遵守」，不与环心确认节奏耦合。 */
        const val SHRUNK_CAT_HOLD_MS = 300L
    }
}

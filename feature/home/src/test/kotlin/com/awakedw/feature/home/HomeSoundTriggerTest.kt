package com.awakedw.feature.home

import com.awakedw.core.domain.GetStreakUseCase
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.ResolveDailyOutfitUseCase
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.domain.UnlockOutfitsUseCase
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.UserSettings
import com.awakedw.core.sound.SoundEvent
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

/**
 * 声音三触发点（任务 12）：
 * - 打卡成功 → 随机 DROP_A/B/C 之一（防抖合并的连点只响一声）；
 * - 当日首次达标（celebrated=true）→ 掉落音之后**追加** GOAL_MELODY；
 *   同日再打卡只掉落音，不重复旋律；
 * - 摸猫（petCat）→ PURR。
 *
 * 假 [com.awakedw.core.sound.AwakeSoundPlayer] 记录调用序列，按序断言；
 * 随机一档不钉死种子——断言「命中三档之一」即可（微变调等播放细节归 :core:sound 自己的测试）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeSoundTriggerTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：与 HomeViewModelTest 同一条真实用例链，外加假声音门面记录调用序列。 */
    private fun harness(
        scheduler: TestCoroutineScheduler,
        settings: UserSettings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD),
    ): Harness {
        val clock = FakeClock(BASE_TIME)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(settings)
        val copies = FakeCopyLibraryRepository()
        val sound = FakeSoundPlayer()
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        val viewModel =
            HomeViewModel(
                clock = clock,
                observeHome = ObserveHomeUseCase(water, prefs, ResolveThemeUseCase(prefs, clock)),
                logWater = LogWaterUseCase(water, prefs, clock),
                copies = copies,
                prefs = prefs,
                unlockOutfits = UnlockOutfitsUseCase(prefs),
                resolveDailyOutfit = ResolveDailyOutfitUseCase(prefs, clock),
                streakOf = GetStreakUseCase(water, prefs),
                sound = sound,
            )
        return Harness(clock, sound, viewModel)
    }

    private class Harness(
        val clock: FakeClock,
        val sound: FakeSoundPlayer,
        val viewModel: HomeViewModel,
    )

    @Test
    fun `打卡成功触发三档掉落音之一`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.tapLogButton()
            runCurrent()

            assertEquals(1, h.sound.events.size)
            assertTrue(h.sound.events.single() in DROP_EVENTS)
        }

    @Test
    fun `防抖合并的连点只响一声`() =
        runTest {
            val h = harness(testScheduler)

            // 同刻三连点：只有首触成笔，声音只跟成笔走——不叠三声水滴。
            h.viewModel.tapLogButton()
            h.viewModel.tapRing(null)
            h.viewModel.tapLogButton()
            runCurrent()

            assertEquals(1, h.sound.events.size)
            assertTrue(h.sound.events.single() in DROP_EVENTS)
        }

    @Test
    fun `当日首次达标在掉落音之后追加达标旋律`() =
        runTest {
            // 目标压到 100ml：首杯即达标，序列应为「掉落音 → 旋律」两句。
            val h =
                harness(
                    testScheduler,
                    settings = UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD, goalMl = 100),
                )

            h.viewModel.tapLogButton()
            runCurrent()

            assertEquals(2, h.sound.events.size)
            assertTrue(h.sound.events[0] in DROP_EVENTS)
            assertEquals(SoundEvent.GOAL_MELODY, h.sound.events[1])

            // 同日再打卡（celebrated=false）：只掉落音，不重复旋律。
            h.clock.ms += WINDOW_GAP_MS
            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(3, h.sound.events.size)
            assertTrue(h.sound.events[2] in DROP_EVENTS)
        }

    @Test
    fun `摸猫触发呼噜声`() =
        runTest {
            val h = harness(testScheduler)

            h.viewModel.petCat()
            runCurrent()

            assertEquals(listOf(SoundEvent.PURR), h.sound.events)
        }

    private companion object {
        /** 相邻两次成笔的假钟间隔：跨出 800ms 防抖窗。 */
        const val WINDOW_GAP_MS = 1_300L

        /** 与被测实现同一份掉落音候选集（断言「命中其一」用）。 */
        val DROP_EVENTS = listOf(SoundEvent.DROP_A, SoundEvent.DROP_B, SoundEvent.DROP_C)
    }
}

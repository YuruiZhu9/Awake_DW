package com.awakedw.feature.home

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.awakedw.core.domain.GetStreakUseCase
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.ResolveDailyOutfitUseCase
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.domain.UnlockOutfitsUseCase
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
import org.junit.Test

/**
 * 画卷上屏与解锁轻提示（moodboard §5.1 / §5.2 首页接线）：
 * - init 即解析今日之裙灌入 `todayOutfit`（解析前为 null，表现层不画卷不显签）；
 * - VM 存活期间画廊改钉选（首页 VM 不重建）：pin 变化回流重解析——有 pin 换成 pin 件、
 *   取消 pin 落回当日已定记录（与画廊「今日之裙」签同屏一致，无矛盾态）；
 * - 打卡成功后按最新连胜结算解锁：新解锁命中则 `newUnlock` 浮出，停留 [NEW_UNLOCK_HOLD_MS]
 *   后被 feedbackEpoch 同款收场清空；无新解锁保持 null；
 * - 新一轮打卡当场以本轮结果覆盖旧提示（防串场），旧轮的定时收场被 epoch 拦下。
 *
 * 停留时长经构造器缺省参注入（生产 [NEW_UNLOCK_HOLD_MS]，测试缩窗）——与 logDebounceMs 同款。
 * 打卡会同时触发快照回流（统计字段更新）与反馈序列两条发射，断言一律按「目标字段是否变化」
 * 等待（[awaitNewUnlock]/[awaitPraiseCleared]），不对无关过渡值的出现次序作假设。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelOutfitTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：与 HomeViewModelTest 同一条真实用例链，外加本任务的画廊两用例。 */
    private fun harness(
        scheduler: TestCoroutineScheduler,
        newUnlockHoldMs: Long = NEW_UNLOCK_HOLD_MS,
        catLineHoldMs: Long = CAT_HOLD_OUT_OF_WINDOW_MS,
    ): Harness {
        val clock = FakeClock(BASE_TIME)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD))
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
                sound = FakeSoundPlayer(),
                newUnlockHoldMs = newUnlockHoldMs,
                catLineHoldMs = catLineHoldMs,
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

    /** 等到 newUnlock 变为 [id]（null = 收场清空）的状态；跳过快照回流等不改动该字段的过渡值。 */
    private suspend fun TurbineTestContext<HomeUiState>.awaitNewUnlock(id: String?): HomeUiState {
        while (true) {
            val state = awaitItem()
            if (state.newUnlock?.id == id) return state
        }
    }

    /** 等到夸夸语收场（praiseLine 清空）的状态；跳过不改动该字段的过渡值。 */
    private suspend fun TurbineTestContext<HomeUiState>.awaitPraiseCleared(): HomeUiState {
        while (true) {
            val state = awaitItem()
            if (state.praiseLine == null) return state
        }
    }

    @Test
    fun `init后今日之裙就绪且等于resolve结果`() =
        runTest {
            val h = harness(testScheduler)

            // 解锁池为空 → resolve 走 dress_00（开局件）兜底；两处解析必须同一件。
            val expected = ResolveDailyOutfitUseCase(h.prefs, h.clock)()
            runCurrent()

            assertEquals(expected, h.viewModel.uiState.value.todayOutfit)
            assertEquals("dress_00", h.viewModel.uiState.value.todayOutfit?.id)
        }

    @Test
    fun `画廊pin后今日之裙回流为pin件`() =
        runTest {
            val h = harness(testScheduler)
            runCurrent()
            assertEquals("dress_00", h.viewModel.uiState.value.todayOutfit?.id)

            // VM 存活期间外部（画廊）改钉选：假件直调 setter 模拟 pin 变化 → 画卷/穿搭签当场换装。
            h.prefs.setPinnedOutfit("dress_02")
            runCurrent()

            assertEquals("dress_02", h.viewModel.uiState.value.todayOutfit?.id)
        }

    @Test
    fun `取消pin后今日之裙回落当日记录`() =
        runTest {
            val h = harness(testScheduler)
            runCurrent()
            // 解锁池为空 → init 解析走 dress_00（开局件）兜底，并已落库当日记录。
            assertEquals("dress_00", h.viewModel.uiState.value.todayOutfit?.id)

            h.prefs.setPinnedOutfit("dress_02")
            runCurrent()
            assertEquals("dress_02", h.viewModel.uiState.value.todayOutfit?.id)

            // 取消钉选：回落当日已定记录（同日重启同件，不重挑），且不崩溃。
            h.prefs.setPinnedOutfit(null)
            runCurrent()

            assertEquals("dress_00", h.viewModel.uiState.value.todayOutfit?.id)
        }

    @Test
    fun `打卡命中新解锁则浮出提示且停留时长后被收场清空`() =
        runTest {
            // 缩窗：停留时长 2000ms（> 夸夸语 1400ms），以便分步断言收场节奏。
            val h = harness(testScheduler, newUnlockHoldMs = SHRUNK_HOLD_MS)
            runCurrent()

            h.viewModel.uiState.test {
                assertEquals(null, awaitItem().newUnlock)

                // 首杯：解锁池为空 → dress_00（开局件）当杯入柜，提示浮出。
                h.viewModel.tapLogButton()
                awaitNewUnlock("dress_00")

                // 夸夸语 1.4s 收场时提示仍在；撑满停留时长后被 epoch 收场清空。
                advanceTimeBy(PRAISE_HOLD_MS)
                runCurrent()
                assertEquals("dress_00", awaitPraiseCleared().newUnlock?.id)
                advanceTimeBy(SHRUNK_HOLD_MS - PRAISE_HOLD_MS)
                runCurrent()
                awaitNewUnlock(null)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `无新解锁时提示保持为空`() =
        runTest {
            val h = harness(testScheduler)
            runCurrent()

            h.viewModel.uiState.test {
                assertEquals(null, awaitItem().newUnlock)

                // 首杯把 dress_00 幂等落库并浮出提示。
                h.viewModel.tapLogButton()
                awaitNewUnlock("dress_00")
                advanceTimeBy(NEW_UNLOCK_HOLD_MS)
                runCurrent()
                awaitNewUnlock(null)

                // 第二杯：解锁已落库，重复结算返回空 → 提示保持 null（含收场后的沉降态）。
                h.clock.ms += WINDOW_GAP_MS
                h.viewModel.tapLogButton()
                awaitNewUnlock(null)
                advanceTimeBy(PRAISE_HOLD_MS)
                runCurrent()
                assertEquals(null, awaitPraiseCleared().newUnlock)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `新一轮打卡当场清掉旧提示且旧轮收场被epoch拦下`() =
        runTest {
            val h = harness(testScheduler)
            runCurrent()

            h.viewModel.uiState.test {
                assertEquals(null, awaitItem().newUnlock)

                // 首杯命中 dress_00，提示浮出。
                h.viewModel.tapLogButton()
                awaitNewUnlock("dress_00")

                // 跨出防抖窗的新一轮打卡：无新解锁 → 旧提示当场以本轮空结果覆盖（防悬挂）。
                h.clock.ms += WINDOW_GAP_MS
                h.viewModel.tapLogButton()
                awaitNewUnlock(null)

                // 新一轮夸夸语照常收场（epoch 归属新轮）后状态沉降；
                // 旧轮的定时收场被 epoch 拦下：此后不再有任何状态发射。
                advanceTimeBy(PRAISE_HOLD_MS)
                runCurrent()
                awaitPraiseCleared()
                advanceTimeBy(NEW_UNLOCK_HOLD_MS)
                runCurrent()
                expectNoEvents()
            }
        }

    private companion object {
        /** 缩窗后的新解锁停留时长：须大于夸夸语 1.4s，才能分步断言两段收场。 */
        const val SHRUNK_HOLD_MS = 2_000L

        /** 与生产同值的夸夸语停留与防抖跨窗间隔（复用 HomeViewModelTest 的节奏常量）。 */
        const val PRAISE_HOLD_MS = 1_400L
        const val WINDOW_GAP_MS = 1_300L

        /**
         * 猫语停留时长拨到观察窗外：本类只验证夸夸语/新解锁（feedbackEpoch 族）的收场节奏，
         * 猫序列（catEpoch 族）2.0s 处的合法收场发射不应闯入 expectNoEvents 的观察窗。
         */
        const val CAT_HOLD_OUT_OF_WINDOW_MS = 10_000L
    }
}

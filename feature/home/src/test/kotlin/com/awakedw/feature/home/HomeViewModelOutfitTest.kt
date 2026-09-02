package com.awakedw.feature.home

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 画卷上屏与「新裙提示」（moodboard §5.1 / §5.2 首页接线，用户裁定「无声等待制」）：
 * - init 即解析今日之裙灌入 `todayOutfit`（解析前为 null，表现层不画卷不显签）；
 * - VM 存活期间画廊改钉选（首页 VM 不重建）：pin 变化回流重解析——有 pin 换成 pin 件、
 *   取消 pin 落回当日已定记录（与画廊「今日之裙」签同屏一致，无矛盾态）；
 * - 打卡成功后按最新连胜结算解锁：新解锁在用例内同步并入未看集 → `hasUnseenOutfits` 翻 true
 *   （首页不再弹任何文字，提示交给蝴蝶结圆点与画廊「新」标）；
 * - 进画廊标记已看（prefs.markOutfitsSeen）后归 false；重复解锁不重复扰动未看集。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelOutfitTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 测试装配：与 HomeViewModelTest 同一条真实用例链，可先铺未看集种子再建 VM。 */
    private fun harness(
        scheduler: TestCoroutineScheduler,
        seedUnseen: Set<String> = emptySet(),
    ): Harness {
        val clock = FakeClock(BASE_TIME)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD))
        if (seedUnseen.isNotEmpty()) runBlocking { prefs.markOutfitsUnseen(seedUnseen) }
        val copies = FakeCopyLibraryRepository()
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
                sound = FakeSoundPlayer(),
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

            // VM 存活期间外部（画廊）改钉选：假件直调 setter 模拟 pin 变化 → 画卷当场换装。
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
    fun `打卡命中新解锁后hasUnseenOutfits为true且新裙入未看集`() =
        runTest {
            val h = harness(testScheduler)
            runCurrent()
            assertFalse(h.viewModel.uiState.value.hasUnseenOutfits)

            // 首杯命中开局件 dress_00：入未看集，hasUnseenOutfits 翻 true（首页只亮圆点，不弹文字）。
            h.viewModel.tapLogButton()
            runCurrent()

            assertEquals(setOf("dress_00"), h.prefs.unseenOutfits.first())
            assertTrue(h.viewModel.uiState.value.hasUnseenOutfits)
        }

    @Test
    fun `init时已有未看集则hasUnseenOutfits直接为true`() =
        runTest {
            val h = harness(testScheduler, seedUnseen = setOf("dress_00"))
            runCurrent()

            assertTrue(h.viewModel.uiState.value.hasUnseenOutfits)
        }

    @Test
    fun `进画廊标记已看后hasUnseenOutfits归false`() =
        runTest {
            val h = harness(testScheduler)
            runCurrent()
            h.viewModel.tapLogButton()
            runCurrent()
            assertTrue(h.viewModel.uiState.value.hasUnseenOutfits)

            // 模拟画廊已读清账（GalleryViewModel init 语义）：未看集清空 → 圆点退场。
            h.prefs.markOutfitsSeen(h.prefs.unseenOutfits.first())
            runCurrent()

            assertFalse(h.viewModel.uiState.value.hasUnseenOutfits)
        }

    @Test
    fun `重复打卡无新解锁不重复扰动未看集`() =
        runTest {
            val h = harness(testScheduler)
            runCurrent()
            h.viewModel.tapLogButton()
            runCurrent()
            assertEquals(setOf("dress_00"), h.prefs.unseenOutfits.first())

            // 跨出防抖窗的第二杯：解锁已落库、无新解锁 → 未看集不重复入也不误清。
            h.clock.ms += WINDOW_GAP_MS
            h.viewModel.tapLogButton()
            runCurrent()

            assertEquals(setOf("dress_00"), h.prefs.unseenOutfits.first())
            assertTrue(h.viewModel.uiState.value.hasUnseenOutfits)
        }

    private companion object {
        /** 与生产同值的防抖跨窗间隔。 */
        const val WINDOW_GAP_MS = 1_300L
    }
}

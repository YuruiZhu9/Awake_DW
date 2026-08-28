package com.awakedw.feature.home

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.ResolveThemeUseCase
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * 首页打卡交互（Robolectric compose）：
 * 点击「记一杯」→ 环心总数更新为新一杯；防抖窗口内连点合并，只记一杯。
 *
 * 首页的漂浮粒子是永不停止的帧循环，故关闭 mainClock 自动推进、由测试显式走时：
 * advanceTimeBy 推进 Compose 帧钟（Robolectric 下与主线程调度器联动），
 * idleFor 兜底放行主线程 Handler 上挂着的 800ms 防抖延迟。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** 显式走时：先放行主线程 Handler 上的延迟任务（800ms 防抖），再推帧钟渲染新状态。 */
    private fun advanceClock(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        composeRule.mainClock.advanceTimeBy(ms)
    }

    @Test
    fun `点击打卡按钮后环心更新且窗口内连点只记一杯`() {
        val clock = FakeClock(BASE_TIME)
        val water = FakeWaterRepository(clock)
        val prefs = FakePrefsRepository(UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD))
        val copies = FakeCopyLibraryRepository()
        val viewModel =
            HomeViewModel(
                clock = clock,
                observeHome = ObserveHomeUseCase(water, prefs, ResolveThemeUseCase(prefs, clock)),
                logWater = LogWaterUseCase(water, prefs, clock),
                copies = copies,
            )

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AwakeTheme(themeId = ThemeId.EMERALD) {
                HomeScreen(viewModel = viewModel)
            }
        }
        advanceClock(FIRST_FRAME_MS)

        composeRule.onNodeWithText("0ml").assertIsDisplayed()
        composeRule.onNodeWithText("干杯一下 💧").assertIsDisplayed()

        // 防抖窗口内连点两下（两下之间不走时，同窗合并）。
        composeRule.onNodeWithText("干杯一下 💧").performClick()
        composeRule.onNodeWithText("干杯一下 💧").performClick()
        advanceClock(DEBOUNCE_AND_SETTLE_MS)

        composeRule.onNodeWithText("250ml").assertIsDisplayed()
        assertEquals(1, water.addCount)

        // 窗口外再点一杯：正常成笔。
        composeRule.onNodeWithText("干杯一下 💧").performClick()
        advanceClock(DEBOUNCE_AND_SETTLE_MS)

        composeRule.onNodeWithText("500ml").assertIsDisplayed()
        assertEquals(2, water.addCount)
    }

    private companion object {
        const val FIRST_FRAME_MS = 100L

        /** 防抖 800ms + 环推进 600ms + 数字滚动 500ms 全部走完的余量。 */
        const val DEBOUNCE_AND_SETTLE_MS = 1_500L
    }
}

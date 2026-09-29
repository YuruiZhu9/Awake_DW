package com.awakedw.feature.home

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.domain.DeleteWaterRecordUseCase
import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.model.ThemeChoice
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.UserSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** Home screen smoke test for the primary water logging action. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun advanceClock(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        composeRule.mainClock.advanceTimeBy(ms)
    }

    @Test
    fun `logging water updates the ring and debounce merges repeated taps`() =
        runTest {
            val clock = FakeClock(BASE_TIME)
            val water = FakeWaterRepository(clock)
            val prefs = FakePrefsRepository(UserSettings(themeChoice = ThemeChoice.FIXED_EMERALD))
            val viewModel =
                HomeViewModel(
                    clock = clock,
                    observeHome = ObserveHomeUseCase(water, prefs),
                    logWater = LogWaterUseCase(water, prefs, clock),
                    deleteWater = DeleteWaterRecordUseCase(water),
                    copies = FakeCopyLibraryRepository(),
                    sound = FakeSoundPlayer(),
                )

            composeRule.mainClock.autoAdvance = false
            composeRule.setContent {
                AwakeTheme(themeId = ThemeId.EMERALD) {
                    HomeScreen(viewModel = viewModel)
                }
            }
            advanceClock(FIRST_FRAME_MS)

            // 环境约束（2.0.3 记录）：Robolectric 下 EditorialHeroValue 三个文本节点
            // 在本机长期为未测量态（bounds 0,0,0,0），assertIsDisplayed 恒假——
            // 其可见性几何由 HomeScreenOverflowTest 的有效视口断言与真机验收兜底，
            // 这里只断言状态语义（数值滚动、防抖合并）。
            composeRule.onNodeWithText("0ml").assertExists()
            composeRule.onNodeWithText("记一杯").assertIsDisplayed()

            composeRule.onNodeWithText("记一杯").performClick()
            composeRule.onNodeWithText("记一杯").performClick()
            advanceClock(RENDER_SETTLE_MS)

            composeRule.onNodeWithText("250ml").assertExists()
            assertEquals(1, water.addCount)

            clock.ms += WINDOW_GAP_MS
            composeRule.onNodeWithText("记一杯").performClick()
            advanceClock(RENDER_SETTLE_MS)

            composeRule.onNodeWithText("500ml").assertExists()
            assertEquals(2, water.addCount)
        }

    private companion object {
        const val FIRST_FRAME_MS = 100L
        const val RENDER_SETTLE_MS = 1_500L
        const val WINDOW_GAP_MS = 2_000L
    }
}

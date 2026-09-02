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
                    observeHome = ObserveHomeUseCase(water, prefs, ResolveThemeUseCase(prefs, clock)),
                    logWater = LogWaterUseCase(water, prefs, clock),
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

            composeRule.onNodeWithText("0ml").assertIsDisplayed()
            composeRule.onNodeWithText("干杯一下 💧").assertIsDisplayed()

            composeRule.onNodeWithText("干杯一下 💧").performClick()
            composeRule.onNodeWithText("干杯一下 💧").performClick()
            advanceClock(RENDER_SETTLE_MS)

            composeRule.onNodeWithText("250ml").assertIsDisplayed()
            assertEquals(1, water.addCount)

            clock.ms += WINDOW_GAP_MS
            composeRule.onNodeWithText("干杯一下 💧").performClick()
            advanceClock(RENDER_SETTLE_MS)

            composeRule.onNodeWithText("500ml").assertIsDisplayed()
            assertEquals(2, water.addCount)
        }

    private companion object {
        const val FIRST_FRAME_MS = 100L
        const val RENDER_SETTLE_MS = 1_500L
        const val WINDOW_GAP_MS = 2_000L
    }
}

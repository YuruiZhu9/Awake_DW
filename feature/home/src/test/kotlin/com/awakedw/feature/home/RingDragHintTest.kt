package com.awakedw.feature.home

import android.os.Looper
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * 空态拖环提示（2.10.0）：只在今天还没有记录时露出「可拖动记录」，
 * 第一次记录后立刻收回「今日进度」——不做循环提示、不新增持久化键。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class RingDragHintTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `空首页露出拖动提示且首杯后收回`() =
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

            composeRule.onNodeWithText("可拖动记录").assertExists()

            composeRule.onNodeWithText("记一杯").performClick()
            advanceClock(RENDER_SETTLE_MS)

            composeRule.onNodeWithText("可拖动记录").assertDoesNotExist()
        }

    @Test
    fun `已有记录时不再显示拖动提示`() =
        runTest {
            val clock = FakeClock(BASE_TIME)
            val water = FakeWaterRepository(clock)
            water.seedToday(60, 30)
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
            advanceClock(RENDER_SETTLE_MS)

            composeRule.onNodeWithText("750ml").assertExists()
            composeRule.onNodeWithText("可拖动记录").assertDoesNotExist()
            composeRule.onNodeWithText("今日进度").assertExists()
        }

    private fun advanceClock(ms: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        composeRule.mainClock.advanceTimeBy(ms)
    }

    private companion object {
        const val BASE_TIME = 1_760_000_000_000L
        const val FIRST_FRAME_MS = 100L
        const val RENDER_SETTLE_MS = 1_500L
    }
}

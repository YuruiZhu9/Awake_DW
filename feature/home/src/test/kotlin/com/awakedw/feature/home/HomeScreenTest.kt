package com.awakedw.feature.home

import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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

            composeRule.onNodeWithText("0ml").assertIsDisplayed()
            composeRule.onNodeWithText("记一杯").assertIsDisplayed()
            // 今日还没有记录：没有可撤回的对象，说明行也不该出现。
            composeRule.onNodeWithText(REVERT_HINT_TEXT).assertDoesNotExist()

            composeRule.onNodeWithText("记一杯").performClick()
            composeRule.onNodeWithText("记一杯").performClick()
            advanceClock(RENDER_SETTLE_MS)

            composeRule.onNodeWithText("250ml").assertIsDisplayed()
            assertEquals(1, water.addCount)
            // 有了记录：长按「最近一杯」可撤回，这行小字必须看得见，不能只写在无障碍描述里。
            composeRule.onNodeWithText(REVERT_HINT_TEXT).assertIsDisplayed()

            clock.ms += WINDOW_GAP_MS
            composeRule.onNodeWithText("记一杯").performClick()
            advanceClock(RENDER_SETTLE_MS)

            composeRule.onNodeWithText("500ml").assertIsDisplayed()
            assertEquals(2, water.addCount)
        }

    /**
     * 撤回的完整链路：长按「最近一杯」→ 弹确认 → 取消则原地不动 → 确认才真的收回。
     *
     * ViewModel 层已有撤回语义的单测，但「长按打开确认、取消不做任何事、确认才落库」这条 UI 接线
     * 此前无人覆盖——而它守着的恰恰是四个破坏性操作（撤回/删除记录/删除文案/整库恢复默认）
     * 共用的 `AwakeConfirmDialog`。破坏性操作最怕的就是确认框形同虚设。
     */
    @Test
    fun `long press on the last cup confirms before reverting and cancel changes nothing`() =
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

            composeRule.onNodeWithText("记一杯").performClick()
            advanceClock(RENDER_SETTLE_MS)
            assertEquals(250, viewModel.uiState.value.totalMl)

            // 事实条用 clearAndSetSemantics 暴露内容描述，长按目标按描述定位。
            val lastCup = composeRule.onNodeWithContentDescription("最近一杯", substring = true)
            lastCup.performTouchInput { longClick() }
            advanceClock(RENDER_SETTLE_MS)
            composeRule.onNodeWithText("撤回这一杯").assertIsDisplayed()

            // 取消：什么都不该发生。
            composeRule.onNodeWithText("取消").performClick()
            advanceClock(RENDER_SETTLE_MS)
            composeRule.onNodeWithText("撤回这一杯").assertDoesNotExist()
            assertEquals(250, viewModel.uiState.value.totalMl)

            // 再来一次并确认：这一杯才真的被收回。
            lastCup.performTouchInput { longClick() }
            advanceClock(RENDER_SETTLE_MS)
            composeRule.onNodeWithText("撤回").performClick()
            advanceClock(RENDER_SETTLE_MS)
            assertEquals(0, viewModel.uiState.value.totalMl)
            assertEquals(0, viewModel.uiState.value.cupCount)
        }

    private companion object {
        const val FIRST_FRAME_MS = 100L
        const val RENDER_SETTLE_MS = 1_500L
        const val WINDOW_GAP_MS = 2_000L
    }
}

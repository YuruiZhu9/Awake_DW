package com.awakedw.feature.stats

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import com.awakedw.core.model.WaterRecord
import com.awakedw.feature.stats.components.TodayTimeline
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class TodayTimelineTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `five records stay fully visible without a more button`() {
        disableMotion()
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Box(Modifier.fillMaxSize()) {
                    TodayTimeline(records = records(5))
                }
            }
        }

        composeRule.onNodeWithText("101ml").assertIsDisplayed()
        composeRule.onNodeWithText("105ml").assertIsDisplayed()
        composeRule.onNodeWithText("显示更多").assertDoesNotExist()
    }

    @Test
    fun `more button keeps the latest five records collapsed and expands the full timeline`() {
        disableMotion()
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Box(Modifier.fillMaxSize()) {
                    TodayTimeline(records = records(6))
                }
            }
        }

        composeRule.onAllNodesWithText("101ml").assertCountEquals(0)
        composeRule.onNodeWithText("102ml").assertIsDisplayed()
        composeRule.onNodeWithText("106ml").assertIsDisplayed()
        composeRule.onNodeWithText("显示更多（还有 1 次）").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("101ml").assertIsDisplayed()
        composeRule.onNodeWithText("收起").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText("101ml").assertCountEquals(0)
    }

    @Test
    fun `删除一条记录时该行走离场收束而非瞬间消失`() {
        composeRule.mainClock.autoAdvance = false
        val state = mutableStateOf(records(3))
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Box(Modifier.fillMaxSize()) {
                    TodayTimeline(records = state.value, onRequestDelete = {})
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("103ml").assertExists()

        composeRule.runOnIdle { state.value = records(3).filter { it.id != 3L } }
        // 离场宽限拍内：被删行仍在树上走收场动画。
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("103ml").assertExists()
        // 宽限与收场动画走完：行离场，其余行不受影响。
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("103ml").assertDoesNotExist()
        composeRule.onNodeWithText("101ml").assertExists()
        composeRule.onNodeWithText("102ml").assertExists()
    }

    @Test
    fun `收起时多出的行走离场收束而非瞬间消失`() {
        composeRule.mainClock.autoAdvance = false
        val state = mutableStateOf(records(6))
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Box(Modifier.fillMaxSize()) {
                    TodayTimeline(records = state.value)
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("显示更多（还有 1 次）").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("101ml").assertExists()

        composeRule.onNodeWithText("收起").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("101ml").assertExists()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("101ml").assertDoesNotExist()
        composeRule.onNodeWithText("102ml").assertExists()
    }

    @Test
    fun `减少动态下删除即时消失不走宽限`() {
        disableMotion()
        val state = mutableStateOf(records(2))
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Box(Modifier.fillMaxSize()) {
                    TodayTimeline(records = state.value, onRequestDelete = {})
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("102ml").assertExists()

        composeRule.runOnIdle { state.value = records(2).take(1) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("102ml").assertDoesNotExist()
        composeRule.onNodeWithText("101ml").assertExists()
    }

    private fun disableMotion() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            0f,
        )
    }

    private fun records(count: Int): List<WaterRecord> =
        (1..count).map { index ->
            WaterRecord(
                id = index.toLong(),
                amountMl = 100 + index,
                drankAtEpochMs = BASE_TIME + index * 60_000L,
                dayKeyLocal = BASE_DAY_KEY,
            )
        }
}

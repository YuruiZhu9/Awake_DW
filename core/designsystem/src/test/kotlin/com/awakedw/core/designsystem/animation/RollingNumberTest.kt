package com.awakedw.core.designsystem.animation

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.awakedw.core.designsystem.MotionTokens
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 全页统一数字滚动的行为契约（2.6.0 动效二阶）：
 * 1. 值变化时从旧值滚到新值并在统一时长内收敛；
 * 2. 首组合直接显示目标值——冷启动与切页不做「从 0 滚起」的表演；
 * 3. 减少动效直显，值变化即时生效，零动画。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class RollingNumberTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `值变化时数字滚动且中途未提前到达并在统一时长内收敛`() {
        composeRule.mainClock.autoAdvance = false
        val target = mutableIntStateOf(120)
        composeRule.setContent {
            Text(rememberRollingInt(targetValue = target.intValue, label = "testRoll").toString())
        }
        // 首组合直接落在目标值：120 在任何动画帧推进前就已经在树上。
        composeRule.mainClock.advanceTimeBy(16)
        composeRule.onNodeWithText("120").assertExists()

        composeRule.runOnIdle { target.intValue = 250 }
        // 滚动中途（统一时长走 1/5）：新值尚未到达。
        composeRule.mainClock.advanceTimeBy(MotionTokens.DURATION_NUMBER_ROLL_MS / 5L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("250").assertDoesNotExist()
        // 统一时长走满并留足帧量化余量：tween 结束帧的精确落点（16ms 步进）晚于名义时长，
        // 余量给够后 Animatable 收尾落定目标值。
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("250").assertExists()
    }

    @Test
    fun `减少动效时直显目标值且变化即时生效`() {
        val target = mutableIntStateOf(120)
        composeRule.setContent {
            Text(rememberRollingInt(targetValue = target.intValue, label = "testRoll", reduceMotion = true).toString())
        }
        composeRule.onNodeWithText("120").assertExists()
        composeRule.runOnIdle { target.intValue = 300 }
        composeRule.onNodeWithText("300").assertExists()
    }
}

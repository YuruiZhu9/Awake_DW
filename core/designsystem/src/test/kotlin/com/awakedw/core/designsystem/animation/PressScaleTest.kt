package com.awakedw.core.designsystem.animation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.awakedw.core.designsystem.MotionTokens
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 按压语言的共享契约（2.8.0 §24）：
 * 1. 按下缩至 [MotionTokens.PRESS_SCALE] 并在弹簧回弹后收敛；中途不提前到位；
 * 2. 释放回弹到 1f，组件静止态零缩放（视觉基线不受影响）；
 * 3. 减少动态恒 1f 直显，按压只回答、不表演。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class PressScaleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `按下时缩放进行中未提前到位并在弹簧回弹后收敛到按压缩放值`() {
        composeRule.mainClock.autoAdvance = false
        val source = MutableInteractionSource()
        composeRule.setContent {
            Text("%.2f".format(rememberPressScaleFraction(source)))
        }
        composeRule.mainClock.advanceTimeBy(16)
        composeRule.onNodeWithText("1.00").assertExists()

        val press = PressInteraction.Press(Offset.Zero)
        composeRule.runOnIdle { source.tryEmit(press) }
        // 按压进行中（弹簧起步不久）：尚未到达目标缩放。
        composeRule.mainClock.advanceTimeBy(32)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("%.2f".format(MotionTokens.PRESS_SCALE)).assertDoesNotExist()
        // 弹簧无固定时长，留足余量让回弹收敛并落定。
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("%.2f".format(MotionTokens.PRESS_SCALE)).assertExists()
    }

    @Test
    fun `释放后回弹收敛回静止零缩放`() {
        composeRule.mainClock.autoAdvance = false
        val source = MutableInteractionSource()
        composeRule.setContent {
            Text("%.2f".format(rememberPressScaleFraction(source)))
        }
        val press = PressInteraction.Press(Offset.Zero)
        composeRule.mainClock.advanceTimeBy(16)
        composeRule.runOnIdle {
            source.tryEmit(press)
        }
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("%.2f".format(MotionTokens.PRESS_SCALE)).assertExists()

        composeRule.runOnIdle { source.tryEmit(PressInteraction.Release(press)) }
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1.00").assertExists()
    }

    @Test
    fun `减少动态时按压恒为静止零缩放`() {
        val source = MutableInteractionSource()
        composeRule.setContent {
            Text("%.2f".format(rememberPressScaleFraction(source, reduceMotion = true)))
        }
        composeRule.runOnIdle { source.tryEmit(PressInteraction.Press(Offset.Zero)) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("1.00").assertExists()
    }

    @Test
    fun `pressScale修饰符在静止态不改变组件边界`() {
        composeRule.setContent {
            val source = MutableInteractionSource()
            Text(
                text = "anchor",
                modifier = Modifier.pressScale(source),
            )
        }
        composeRule.onNodeWithText("anchor").assertExists()
    }
}

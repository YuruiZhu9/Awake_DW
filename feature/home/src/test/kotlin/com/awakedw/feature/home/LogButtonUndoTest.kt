package com.awakedw.feature.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.home.components.LogButton
import com.awakedw.feature.home.components.UNDO_ACTION_LABEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 主按钮尾端撤回动作（2.11.0）：
 * 窗口内「+{cupMl}ml」换成「撤回」，独立可点、≥48dp、读屏可读；
 * 2× 字体下动作仍完整落在 360dp 屏内。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp")
class LogButtonUndoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setButton(
        undoVisible: Boolean = false,
        onTap: () -> Unit = {},
        onUndo: () -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = current.density, fontScale = fontScale),
            ) {
                AwakeTheme(themeId = ThemeId.EMERALD) {
                    LogButton(
                        onTap = onTap,
                        cupMl = 250,
                        undoVisible = undoVisible,
                        undoAmountMl = if (undoVisible) 250 else null,
                        onUndo = onUndo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    @Test
    fun `无撤回窗口时尾端显示杯量且不出现撤回动作`() {
        setButton()

        composeRule.onNodeWithText("+250ml").assertIsDisplayed()
        composeRule.onNodeWithText(UNDO_ACTION_LABEL).assertDoesNotExist()
    }

    @Test
    fun `撤回窗口内尾端换成撤回动作并回调`() {
        var undone = 0
        setButton(undoVisible = true, onUndo = { undone += 1 })

        composeRule.onNodeWithText("+250ml").assertDoesNotExist()
        composeRule.onNodeWithText(UNDO_ACTION_LABEL).assertIsDisplayed().performClick()

        assertEquals(1, undone)
    }

    @Test
    fun `真实触摸撤回区只撤回不会同时记一杯`() {
        var taps = 0
        var undos = 0
        setButton(undoVisible = true, onTap = { taps += 1 }, onUndo = { undos += 1 })

        // 点击走真实指针命中：撤回区必须自己消费，不能穿透到外层「记一杯」。
        composeRule.onNodeWithText(UNDO_ACTION_LABEL).performTouchInput { click() }

        assertEquals(0, taps)
        assertEquals(1, undos)
    }

    @Test
    fun `撤回动作可点高度不低于48dp且读屏补充对象`() {
        setButton(undoVisible = true)

        val node =
            composeRule
                .onNodeWithContentDescription("撤回上一杯，250ml")
                .assertExists()
                .fetchSemanticsNode()
        val minPx = with(composeRule.density) { 48.dp.toPx() }
        assertTrue(
            "撤回动作可点高度应 ≥ 48dp：${node.boundsInRoot}",
            node.boundsInRoot.height >= minPx,
        )
    }

    @Test
    fun `两倍字体下撤回动作仍完整落在屏内`() {
        setButton(undoVisible = true, fontScale = 2f)

        val rootWidth = composeRule.onRoot().fetchSemanticsNode().size.width.toFloat()
        val undo = composeRule.onNodeWithText(UNDO_ACTION_LABEL).fetchSemanticsNode()
        assertTrue(
            "撤回动作不应越出 360dp 屏宽：${undo.boundsInRoot}",
            undo.boundsInRoot.right <= rootWidth,
        )
    }
}

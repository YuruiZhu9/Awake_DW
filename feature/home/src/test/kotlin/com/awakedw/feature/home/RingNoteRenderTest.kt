package com.awakedw.feature.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 环心引文的渲染形态（视觉基线 §12.1）：正文与落款都要出现在环心，
 * 而**没有落款的原创句不能补出一行「佚名」**——那是为原创句伪造出处。
 *
 * 0.9.1 补大字档回归：落款语料上限 14 字（语料纪律 §14），fontScale 2.0 时
 * 长落款与两行正文都应完整可读——环心文案区允许撑高，但绝不截断。
 */
@RunWith(RobolectricTestRunner::class)
class RingNoteRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        note: RingNote?,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                val current = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(current.density, fontScale)) {
                    RingCenterContent(totalMl = 250, reduceMotion = true, centerNote = note)
                }
            }
        }
    }

    @Test
    fun `带落款的引文同时显示正文与落款`() {
        setContent(RingNote(text = "月光如流水一般", attribution = "朱自清《荷塘月色》"))

        composeRule.onNodeWithText("月光如流水一般").assertIsDisplayed()
        composeRule.onNodeWithText("—— 朱自清《荷塘月色》").assertIsDisplayed()
    }

    @Test
    fun `大字档下两行正文与最长落款完整展示`() {
        // 落款恰为语料上限 14 字（含书名号）；正文在环心窄宽度内自然折行。
        setContent(
            RingNote(
                text = "月光落在窗台上，像一封没有拆开的信",
                attribution = "菲茨杰拉德《了不起的盖茨比》",
            ),
            fontScale = 2f,
        )

        composeRule.onNodeWithText("月光落在窗台上，像一封没有拆开的信").assertIsDisplayed()
        composeRule.onNodeWithText("—— 菲茨杰拉德《了不起的盖茨比》").assertIsDisplayed()
    }

    @Test
    fun `没有落款的原创句只显示正文且不补佚名`() {
        setContent(RingNote(text = "窗子刚亮，水已经倒好了"))

        composeRule.onNodeWithText("窗子刚亮，水已经倒好了").assertIsDisplayed()
        composeRule.onAllNodesWithText("佚名", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("——", substring = true).assertCountEquals(0)
    }

    @Test
    fun `没有确认语时环心回到今日已喝`() {
        setContent(note = null)

        composeRule.onNodeWithText("今日已喝").assertIsDisplayed()
    }
}

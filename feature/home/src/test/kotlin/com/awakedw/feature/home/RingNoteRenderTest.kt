package com.awakedw.feature.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 环心引文的渲染形态（视觉基线 §12.1）：正文与落款都要出现在环心，
 * 而**没有落款的原创句不能补出一行「佚名」**——那是为原创句伪造出处。
 */
@RunWith(RobolectricTestRunner::class)
class RingNoteRenderTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(note: RingNote?) {
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                RingCenterContent(totalMl = 250, reduceMotion = true, centerNote = note)
            }
        }
    }

    @Test
    fun `带落款的引文同时显示正文与落款`() {
        setContent(RingNote(text = "晚来天欲雪，能饮一杯无", attribution = "白居易"))

        composeRule.onNodeWithText("晚来天欲雪，能饮一杯无").assertIsDisplayed()
        composeRule.onNodeWithText("—— 白居易").assertIsDisplayed()
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

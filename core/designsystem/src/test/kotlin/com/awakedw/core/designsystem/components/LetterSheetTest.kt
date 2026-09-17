package com.awakedw.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 信纸载体（1.1.0 视觉 refresh 方向 A）：冒烟覆盖——浅色/深色主题组合不崩溃、内容完整可见。
 * 信纸只是版面骨骼，不承载语义；视觉观感由 feature:home 的 visual review 截图把关。
 */
@RunWith(RobolectricTestRunner::class)
class LetterSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `浅色主题信纸组合不崩溃且内容可见`() {
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                LetterSheet(Modifier.fillMaxWidth()) {
                    Text("信纸正文", Modifier.fillMaxWidth().heightIn(min = 48.dp))
                }
            }
        }
        composeRule.onNodeWithText("信纸正文").assertIsDisplayed()
    }

    @Test
    fun `深色主题信纸组合不崩溃`() {
        composeRule.setContent {
            AwakeTheme(ThemeId.NIGHT) {
                LetterSheet(Modifier.fillMaxWidth()) {
                    Text("深夜信纸", Modifier.fillMaxWidth().heightIn(min = 48.dp))
                }
            }
        }
        composeRule.onNodeWithText("深夜信纸").assertIsDisplayed()
    }
}

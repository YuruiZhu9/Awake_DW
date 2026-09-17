package com.awakedw.core.designsystem.animation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 墨水浸润（1.3.0 全局墨水语言）：冒烟覆盖——挂载组合不崩溃、真实触摸路径
 * （按下 → 抬起）的交互流走一遍不异常。墨的观感（晕开节奏、峰值透明度）由真机走查把关。
 */
@RunWith(RobolectricTestRunner::class)
class InkWashTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `墨水浸润组合与真实触摸路径不崩溃`() {
        val interactionSource = MutableInteractionSource()
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Box(
                    Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFFEFF5F1))
                        .inkWash(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) {}
                        .testTag("inkTarget"),
                )
            }
        }
        composeRule.onNodeWithTag("inkTarget").performTouchInput {
            down(center)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("inkTarget").performTouchInput {
            up()
        }
        composeRule.waitForIdle()
    }
}

package com.awakedw.core.designsystem.lolita

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.EmeraldThemeSpec
import com.awakedw.core.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class LolitaBackdropLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `backdrop root consumes the page size instead of collapsing inside crossfade`() {
        // 场景层（中景漂移 / 近景光斑）含无限帧循环：测试必须关闭时钟自动推进，
        // 否则 Espresso 永不空闲（visual-baseline §11 测试 gotcha 的既定解法）。
        composeRule.mainClock.autoAdvance = false
        var measured = IntSize.Zero
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Box(Modifier.size(width = 180.dp, height = 320.dp)) {
                    LolitaBackdrop(
                        spec = EmeraldThemeSpec,
                        modifier = Modifier.matchParentSize().onSizeChanged { measured = it },
                    )
                }
            }
        }

        composeRule.waitForIdle()
        assertEquals(IntSize(180, 320), measured)
    }
}

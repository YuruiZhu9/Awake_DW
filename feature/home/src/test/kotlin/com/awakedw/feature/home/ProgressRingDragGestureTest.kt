package com.awakedw.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.designsystem.ring.ProgressRing
import com.awakedw.core.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 环即把手的手势仲裁（2.2.0，Robolectric 触摸注入）：
 * 环带内切向拖拽认领为拖环、消费位移并取消点按；纵向拖拽让位滚动、两者都不触发；轻点仍走点按。
 * 进度环没有语义文本，定位走 testTag；mdpi 下 200dp 盒即 200px，弧半径约 94px。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class ProgressRingDragGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `沿环切向拖拽认领为拖环回传正扫过角并取消点按`() {
        var taps = 0
        var endSwept: Float? = null
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                ProgressRing(
                    progress = 0.2f,
                    modifier = Modifier.size(200.dp).testTag(RING_TAG),
                    onRingTap = { taps++ },
                    onRingDrag = {},
                    onRingDragEnd = { endSwept = it },
                ) {}
            }
        }
        // 自 12 点沿顺时针拖到 3 点方向（≈90°）：环带内、切向主导。
        composeRule.onNodeWithTag(RING_TAG).performTouchInput {
            down(Offset(center.x, center.y - 94f))
            moveTo(Offset(center.x + 66f, center.y - 66f))
            moveTo(Offset(center.x + 94f, center.y))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(0, taps)
        assertNotNull("切向拖拽松手应回传扫过角", endSwept)
        assertTrue("顺时针拖拽应记为正角度：$endSwept", endSwept!! > 60f)
    }

    @Test
    fun `纵向拖拽让位滚动不触发拖环与点按`() {
        var taps = 0
        var dragCalled = false
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(60.dp))
                    ProgressRing(
                        progress = 0.2f,
                        modifier = Modifier.size(200.dp).testTag(RING_TAG),
                        onRingTap = { taps++ },
                        onRingDrag = { dragCalled = true },
                        onRingDragEnd = {},
                    ) {}
                    Spacer(Modifier.height(400.dp))
                }
            }
        }
        composeRule.onNodeWithTag(RING_TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, 120f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals(0, taps)
        assertFalse("纵向拖拽应让位滚动，不得认领为拖环", dragCalled)
    }

    @Test
    fun `轻点环区仍触发点按不触发拖环收场`() {
        var taps = 0
        var endSwept: Float? = null
        composeRule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                ProgressRing(
                    progress = 0.2f,
                    modifier = Modifier.size(200.dp).testTag(RING_TAG),
                    onRingTap = { taps++ },
                    onRingDrag = {},
                    onRingDragEnd = { endSwept = it },
                ) {}
            }
        }
        composeRule.onNodeWithTag(RING_TAG).performTouchInput { click() }
        composeRule.waitForIdle()

        assertEquals(1, taps)
        assertNull("点按不应走拖环收场", endSwept)
    }

    private companion object {
        const val RING_TAG = "gesture-ring"
    }
}

package com.awakedw.feature.settings

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.ThemeId
import com.awakedw.feature.settings.copyeditor.CopyGroup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 文案分组展开/收起的过渡契约（2.9.0 过渡补完）：
 * 1. 动效开启时，收起不再硬切——离场过渡拍内内容仍在树上，拍毕才消失；
 * 2. 展开即达：内容进树后「＋ 新增一句」与句子行立即可见；
 * 3. 减少动态直切直显，与全局纪律一致。
 *
 * 直接组 [CopyGroup]（不挂全量设置页）：设置页的常驻粒子层在动效开启下
 * 会让 `waitForIdle` 永不返回（`CopyEditorTouchTargetTest` 顶部注释同源问题）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp-mdpi")
class CopyGroupTransitionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `收起分组时内容走离场过渡而非瞬间消失`() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Box(Modifier.fillMaxSize()) {
                    CopyGroup(
                        label = "早上",
                        items = listOf("早安，喝水啦", "新的一天从一杯水开始"),
                        onEdit = {},
                        onAdd = {},
                        onDelete = {},
                    )
                }
            }
        }
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()

        rule.onNodeWithText("早上").performClick()
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        rule.onNodeWithText("早安，喝水啦").assertExists()

        rule.onNodeWithText("早上").performClick()
        // 离场过渡拍内：内容仍在树上走收场。
        rule.mainClock.advanceTimeBy(60)
        rule.waitForIdle()
        rule.onNodeWithText("早安，喝水啦").assertExists()
        // 过渡完成：内容离场。
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        rule.onNodeWithText("早安，喝水啦").assertDoesNotExist()
    }

    @Test
    fun `减少动态下展开收起直切直显`() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )
        rule.setContent {
            AwakeTheme(ThemeId.EMERALD) {
                Box(Modifier.fillMaxSize()) {
                    CopyGroup(
                        label = "早上",
                        items = listOf("早安，喝水啦"),
                        onEdit = {},
                        onAdd = {},
                        onDelete = {},
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithText("早上").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("早安，喝水啦").assertExists()

        rule.onNodeWithText("早上").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("早安，喝水啦").assertDoesNotExist()
    }
}

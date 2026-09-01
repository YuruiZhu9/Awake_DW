package com.awakedw.core.designsystem.art

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import com.awakedw.core.model.CatAccessory
import com.awakedw.core.model.CatMood
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 胆大王立绘组件（Robolectric compose）：
 *
 * - 三 mood 组合均不崩溃且语义节点存在（cat/ 资产未就位 → 走矢量兜底路径）；
 * - 点击立绘触发 onPet（注入真实触摸事件走 detectTapGestures，不经语义 click 动作）；
 * - 简报逐字动效/锚点参数用纯函数直断言（呼吸峰值、配饰锚点比例、三态资产路径）。
 *
 * 不加 @GraphicsMode(NATIVE)：本组件测试不做位图解码与像素取样，
 * 矢量路径的 draw 指令在 LEGACY 阴影下即可覆盖「组合不崩溃」断言。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class CatFigureTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---------- 三 mood 组合：不崩溃 + 语义节点存在 ----------

    @Test
    fun `IDLE_全配饰_组合不崩溃且语义节点存在`() {
        composeRule.setContent {
            CatFigure(mood = CatMood.IDLE, accessories = CatAccessory.entries.toList())
        }
        composeRule.onNodeWithContentDescription(CAT_SEMANTICS).assertExists()
    }

    @Test
    fun `HAPPY_空配饰_组合不崩溃且语义节点存在`() {
        composeRule.setContent {
            CatFigure(mood = CatMood.HAPPY, accessories = emptyList())
        }
        composeRule.onNodeWithContentDescription(CAT_SEMANTICS).assertExists()
    }

    @Test
    fun `SLEEPY_空配饰_组合不崩溃且语义节点存在`() {
        composeRule.setContent {
            CatFigure(mood = CatMood.SLEEPY, accessories = emptyList())
        }
        composeRule.onNodeWithContentDescription(CAT_SEMANTICS).assertExists()
    }

    // ---------- onPet：点击立绘触发 ----------

    @Test
    fun `点击立绘触发onPet`() {
        var pets = 0
        composeRule.setContent {
            CatFigure(
                mood = CatMood.IDLE,
                accessories = emptyList(),
                onPet = { pets++ },
            )
        }
        composeRule
            .onNodeWithContentDescription(CAT_SEMANTICS)
            .performTouchInput { click() }
        composeRule.waitForIdle()
        assertEquals(1, pets)
    }

    // ---------- 简报逐字参数：纯函数直断言 ----------

    @Test
    fun `呼吸缩放峰值_SLEEPY幅度减半`() {
        assertEquals(1.02f, breathTargetOf(CatMood.IDLE))
        assertEquals(1.02f, breathTargetOf(CatMood.HAPPY))
        assertEquals(1.01f, breathTargetOf(CatMood.SLEEPY))
    }

    @Test
    fun `配饰锚点比例与简报一致`() {
        assertEquals(0.18f, accessoryAnchorY(CatAccessory.BOW))
        assertEquals(0.52f, accessoryAnchorY(CatAccessory.PEARL))
        assertEquals(0.72f, accessoryAnchorY(CatAccessory.OUTFIT))
    }

    @Test
    fun `三态立绘资产路径映射`() {
        assertEquals("cat/idle.webp", catAssetFileOf(CatMood.IDLE))
        assertEquals("cat/happy.webp", catAssetFileOf(CatMood.HAPPY))
        assertEquals("cat/sleepy.webp", catAssetFileOf(CatMood.SLEEPY))
    }
}

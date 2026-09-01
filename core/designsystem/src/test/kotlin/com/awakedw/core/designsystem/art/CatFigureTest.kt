package com.awakedw.core.designsystem.art

import androidx.compose.ui.graphics.Color
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
    fun `呼吸周期对齐3秒完整周期`() {
        // 1.5s 单程 ×2（Reverse 回程）= 3s 完整呼吸周期（审查裁定）。
        assertEquals(1500, BREATH_LEG_MS)
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

    // ---------- 布偶猫矢量兜底：固定色板纯函数 ----------

    @Test
    fun `布偶猫色板_浅色关键色与规格一致`() {
        val palette = catPaletteOf(isDark = false)
        // 身体/脸颊/胸口：奶油白。
        assertEquals(Color(0xFFF7EFE4), palette.body)
        // 重点色（双耳/面具/尾巴）：暖灰褐。
        assertEquals(Color(0xFF9C8474), palette.point)
        // 胸前围脖：略深米色。
        assertEquals(Color(0xFFEFE2D0), palette.ruff)
        // 蓝宝石眼。
        assertEquals(Color(0xFF5B84B1), palette.iris)
        // 耳内浅粉 / 小粉鼻。
        assertEquals(Color(0xFFF2D8D5), palette.innerEar)
        assertEquals(Color(0xFFE8B4B8), palette.nose)
        // 胡须固定浅色（深夜也可见，不再随主题 chipText 变深）。
        assertEquals(Color(0xFFF7EFE4), palette.whisker)
        // 尾尖略浅于重点色。
        assertEquals(Color(0xFFC2AB99), palette.tailTip)
    }

    @Test
    fun `布偶猫色板_深夜压暗语义_毛色不换只罩暗`() {
        val light = catPaletteOf(isDark = false)
        val dark = catPaletteOf(isDark = true)
        // 同一套固定毛色：深夜不换色，整体套压暗罩。
        assertEquals(light.body, dark.body)
        assertEquals(light.point, dark.point)
        assertEquals(light.iris, dark.iris)
        assertEquals(light.whisker, dark.whisker)
        // 浅色主题罩层全透明（无压暗），深夜为黑 12% 压暗罩。
        assertEquals(Color.Transparent, light.veil)
        assertEquals(Color.Black.copy(alpha = 0.12f), dark.veil)
    }
}

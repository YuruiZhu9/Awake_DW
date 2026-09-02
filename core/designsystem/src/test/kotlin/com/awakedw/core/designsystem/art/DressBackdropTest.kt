package com.awakedw.core.designsystem.art

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.awakedw.core.designsystem.AwakeTheme
import com.awakedw.core.model.Outfit
import com.awakedw.core.model.OutfitCategory
import com.awakedw.core.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * 全屏画卷底层（Robolectric，NATIVE 图形模式）：
 *
 * - alpha 语义与 Crop 裁切抽成纯函数直断言（浅 0.30f Multiply / 深 0.22f SrcOver、
 *   override 优先、src 居中裁切）；
 * - 组合与绘制路径用纯红底上的像素取样验证：测试资产 arttest/dot.png 为 1×1 半透明绿、
 *   dot_night.png 为 1×1 全透明，Multiply 叠上纯红底后红通道按 alpha 比例压暗——
 *   夜变体全透明则纹丝不动，一粒像素即可分辨「画了主图/画了夜变体/什么都没画」。
 *
 * 像素取样走 `decorView.draw(Canvas)` 手动栅格化：compose ui-test 的 captureToImage
 * 在 Robolectric 上依赖的 forceRedraw 轮询无法满足（choreographer 不回调），手动绘制
 * 直接落到 NATIVE Skia 画布，稳定且同样真实。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DressBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val redBackground = Color.Red.toArgb()

    /** 手动栅格化整窗并取中心像素（画布铺满红底，中心必落在画卷层内）。 */
    private fun renderedCenterPixel(): Int {
        composeRule.waitForIdle()
        val activity = (composeRule as AndroidComposeTestRule<*, *>).activity
        val decor = activity.window.decorView
        val bitmap = android.graphics.Bitmap.createBitmap(decor.width, decor.height, android.graphics.Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        return bitmap.getPixel(decor.width / 2, decor.height / 2)
    }

    private fun centerRedChannel(): Float = android.graphics.Color.red(renderedCenterPixel()) / 255f

    // ---------- backdropAlpha / backdropBlendMode / resolveBackdropAlpha：纯函数 ----------

    @Test
    fun `默认alpha_浅色0点30_深色0点22`() {
        assertEquals(0.30f, backdropAlpha(isDark = false))
        assertEquals(0.22f, backdropAlpha(isDark = true))
    }

    @Test
    fun `混合模式_浅色Multiply_深夜SrcOver`() {
        assertEquals(BlendMode.Multiply, backdropBlendMode(isDark = false))
        assertEquals(BlendMode.SrcOver, backdropBlendMode(isDark = true))
    }

    @Test
    fun `alphaOverride优先于主题档位`() {
        assertEquals(0.5f, resolveBackdropAlpha(isDark = false, alphaOverride = 0.5f))
        assertEquals(0.5f, resolveBackdropAlpha(isDark = true, alphaOverride = 0.5f))
        assertEquals(0.30f, resolveBackdropAlpha(isDark = false, alphaOverride = null))
        assertEquals(0.22f, resolveBackdropAlpha(isDark = true, alphaOverride = null))
    }

    // ---------- backdropCropSrc：ContentScale.Crop 语义（src 区居中裁切） ----------

    @Test
    fun `同宽高比源图铺满不裁切`() {
        val src = backdropCropSrc(imageWidth = 100, imageHeight = 100, dstWidth = 50, dstHeight = 50)
        assertEquals(IntRect(IntOffset(0, 0), IntSize(100, 100)), src)
    }

    @Test
    fun `横长图水平居中裁切`() {
        // 200×100 铺进 100×100：按高对齐缩放，左右各裁 50。
        val src = backdropCropSrc(imageWidth = 200, imageHeight = 100, dstWidth = 100, dstHeight = 100)
        assertEquals(IntRect(IntOffset(50, 0), IntSize(100, 100)), src)
    }

    @Test
    fun `竖长图垂直居中裁切`() {
        // 100×200 铺进 100×100：按宽对齐缩放，上下各裁 50。
        val src = backdropCropSrc(imageWidth = 100, imageHeight = 200, dstWidth = 100, dstHeight = 100)
        assertEquals(IntRect(IntOffset(0, 50), IntSize(100, 100)), src)
    }

    // ---------- DressBackdrop：组合与绘制路径 ----------

    @Test
    fun `outfit为null组合不崩溃且不绘制`() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                DressBackdrop(outfit = null, modifier = Modifier.fillMaxSize())
            }
        }
        assertEquals(redBackground, renderedCenterPixel())
    }

    @Test
    fun `资产缺失组合不崩溃且不绘制`() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                DressBackdrop(
                    outfit = outfitOf("outfit/__nope__.webp"),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // 给 Dispatchers.IO 的失败装载留出落定时间，再确认画布纹丝未动。
        Thread.sleep(500)
        assertEquals(redBackground, renderedCenterPixel())
    }

    @Test
    fun `浅色主题按默认alpha乘压绘制`() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                DressBackdrop(outfit = outfitOf("arttest/dot.png"), modifier = Modifier.fillMaxSize())
            }
        }
        // 半透明绿（alpha 127）Multiply 叠纯红：红通道压到 1 - (127/255)*0.30。
        val expected = 1f - (127 / 255f) * backdropAlpha(isDark = false)
        awaitBackdropSettled(expected)
        assertTrue("红通道应为 $expected，实际 ${centerRedChannel()}", abs(centerRedChannel() - expected) <= 3f / 255f)
    }

    @Test
    fun `alphaOverride影响实际绘制`() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red)) {
                DressBackdrop(
                    outfit = outfitOf("arttest/dot.png"),
                    modifier = Modifier.fillMaxSize(),
                    alphaOverride = 0.5f,
                )
            }
        }
        val expected = 1f - (127 / 255f) * 0.5f
        awaitBackdropSettled(expected)
        assertTrue("红通道应为 $expected，实际 ${centerRedChannel()}", abs(centerRedChannel() - expected) <= 3f / 255f)
    }

    @Test
    fun `深色主题优先夜变体`() {
        composeRule.setContent {
            AwakeTheme(themeId = ThemeId.NIGHT) {
                Box(Modifier.fillMaxSize().background(Color.Red)) {
                    DressBackdrop(outfit = outfitOf("arttest/dot.png"), modifier = Modifier.fillMaxSize())
                }
            }
        }
        // dot_night.png 全透明：Multiply 不改底色。若误用主图 dot.png，红通道会被压暗。
        Thread.sleep(500)
        assertEquals("夜变体应全透明不动底色", redBackground, renderedCenterPixel())
    }

    // ---------- 辅助 ----------

    private fun outfitOf(assetFile: String) =
        Outfit(
            id = "test",
            title = "测试之裙",
            note = "像素级测试专用",
            category = OutfitCategory.DRESS,
            assetFile = assetFile,
            unlockDay = 0,
        )

    /** 等待位图装载上屏并走完 600ms 交叉淡入：先等到画面开始变化，再等到稳态值。 */
    private fun awaitBackdropSettled(expected: Float) {
        composeRule.waitUntil(timeoutMillis = 5_000) { centerRedChannel() < 1f }
        composeRule.waitUntil(timeoutMillis = 5_000) { abs(centerRedChannel() - expected) <= 3f / 255f }
    }
}

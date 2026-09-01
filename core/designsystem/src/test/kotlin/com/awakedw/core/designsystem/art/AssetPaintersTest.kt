package com.awakedw.core.designsystem.art

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * assets 位图装载与夜变体解析（Robolectric）：
 * 缺失/解码失败一律回退为 null，绝不抛异常（规格：图像产线的工程半边）。
 *
 * - 正向路径用 src/test/assets/arttest/ 下的 1×1 PNG（dot.png / dot_night.png）验证（夹具不进生产包）；
 * - 回退路径用不存在的 `__nope__` 路径与损坏字节（broken.png）验证；
 * - 组合层 rememberAssetImageOrN 用 createComposeRule 走真实 composition（produceState + Dispatchers.IO）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class AssetPaintersTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = RuntimeEnvironment.getApplication()

    // ---------- nightVariantOf：纯路径映射 ----------

    @Test
    fun `夜变体映射_在最后一个扩展名点前插入_night`() {
        assertEquals("outfit/dress_01_night.webp", nightVariantOf("outfit/dress_01.webp"))
    }

    @Test
    fun `夜变体映射_无扩展名时直接追加_night`() {
        assertEquals("outfit/dress_01_night", nightVariantOf("outfit/dress_01"))
    }

    // ---------- nightVariantOf(context)：存在则用之，否则原文件 ----------

    @Test
    fun `夜变体解析_夜间图存在则用之`() {
        // arttest/dot_night.png 由测试资产提供。
        assertEquals("arttest/dot_night.png", nightVariantOf(context, "arttest/dot.png"))
    }

    @Test
    fun `夜变体解析_夜间图缺失则回退原文件`() {
        assertEquals("arttest/broken.png", nightVariantOf(context, "arttest/broken.png"))
    }

    // ---------- hasAsset：assets.list() 探测 ----------

    @Test
    fun `资产探测_存在为真_缺失为假`() {
        assertTrue(hasAsset(context, "arttest/dot.png"))
        assertFalse(hasAsset(context, "outfit/__nope__.webp"))
    }

    // ---------- loadAssetBitmap：缺失/解码失败回退 null，绝不抛 ----------

    @Test
    fun `装载缺失资产返回null且不抛`() =
        runBlocking {
            assertNull(loadAssetBitmap(context, "outfit/__nope__.webp"))
        }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `装载损坏字节解码失败返回null且不抛`() =
        runBlocking {
            // LEGACY 阴影对任意字节都造出占位 Bitmap（永不返回 null），
            // 故切到 NATIVE（真实 Skia 解码）才能复现真机上的「解码失败返回 null」。
            assertNull(loadAssetBitmap(context, "arttest/broken.png"))
        }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `装载真实资产得到1x1位图`() =
        runBlocking {
            val bitmap = loadAssetBitmap(context, "arttest/dot.png")
            assertNotNull(bitmap)
            assertEquals(1, bitmap!!.width)
            assertEquals(1, bitmap.height)
        }

    // ---------- rememberAssetImageOrN：组合后值 ----------

    @Test
    fun `组合缺失资产后值为null且不抛`() {
        var captured: ImageBitmap? = null
        composeRule.setContent {
            captured = rememberAssetImageOrN("outfit/__nope__.webp")
        }
        composeRule.waitForIdle()
        assertNull(captured)
    }

    @Test
    fun `组合真实资产后异步产出位图`() {
        var captured: ImageBitmap? = null
        composeRule.setContent {
            captured = rememberAssetImageOrN("arttest/dot.png")
        }
        // IO 装载不受帧钟调度，轮询等待 produceState 落值。
        composeRule.waitUntil(timeoutMillis = 5_000) { captured != null }
        assertNotNull(captured)
    }
}

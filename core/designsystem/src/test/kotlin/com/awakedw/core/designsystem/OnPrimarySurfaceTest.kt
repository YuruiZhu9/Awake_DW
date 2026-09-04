package com.awakedw.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * 主色底字色与深夜蕾丝的对比度防漂移（P2-4 / P2-5，纯 JVM）。
 *
 * 用 WCAG 2.x 相对亮度公式逐对计算对比度：
 * - [onPrimarySurface] 清晨/午后返回深暖褐；棕褐焦糖与深夜按钮返回白字；
 *   各自按钮渐变两端与 primary 保持清晰对比；
 *   （白字压浅暖主色仅 1.8–2.5:1 的病灶在此固化断言，防止色板再漂移回去）；
 * - 深夜主题返回白字（其按钮色板按白字校准）；
 * - 深夜蕾丝 #525C72 对徽章底 #182630 ≥1.8:1（旧锚 #343A4C 仅 1.3:1 不可见）。
 */
class OnPrimarySurfaceTest {
    // ---------- WCAG 2.x 相对亮度与对比度 ----------

    /** sRGB 通道线性化：c ≤ 0.03928 走 /12.92，否则 ((c+0.055)/1.055)^2.4。 */
    private fun channel(value: Int): Float {
        val c = value / 255f
        return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    private fun luminance(argb: Long): Float {
        val r = ((argb ushr 16) and 0xFF).toInt()
        val g = ((argb ushr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        return 0.2126f * channel(r) + 0.7152f * channel(g) + 0.0722f * channel(b)
    }

    /** 对比度 = (亮 + 0.05) / (暗 + 0.05)，恒 ≥1。 */
    private fun contrast(
        a: Long,
        b: Long,
    ): Float {
        val la = luminance(a) + 0.05f
        val lb = luminance(b) + 0.05f
        return if (la > lb) la / lb else lb / la
    }

    // ---------- onPrimarySurface：返回值语义 ----------

    @Test
    fun `深夜主题返回白字`() {
        assertEquals(Color.White, onPrimarySurface(NightThemeSpec))
    }

    @Test
    fun `焦糖棕褐主题使用白字其余浅色主题使用深暖褐`() {
        assertEquals(Color.White, onPrimarySurface(CaramelThemeSpec))
        listOf(EmeraldThemeSpec, StrawberryThemeSpec).forEach { spec ->
            assertEquals(Color(ThemePalette.ON_PRIMARY_SURFACE), onPrimarySurface(spec))
        }
    }

    // ---------- P2-4：按钮前景色对各主题主色与渐变两端保持清晰 ----------

    @Test
    fun `深暖褐对清晨主题按钮渐变两端与primary对比度均不低于3`() {
        val text = ThemePalette.ON_PRIMARY_SURFACE
        assertTrue(
            "buttonTop #279061 实际 " + contrast(text, ThemePalette.QINGCHEN_BUTTON_TOP),
            contrast(text, ThemePalette.QINGCHEN_BUTTON_TOP) >= 3f,
        )
        assertTrue(
            "buttonBottom #43B988 实际 " + contrast(text, ThemePalette.QINGCHEN_BUTTON_BOTTOM),
            contrast(text, ThemePalette.QINGCHEN_BUTTON_BOTTOM) >= 3f,
        )
        assertTrue(
            "primary #2A9A6A 实际 " + contrast(text, ThemePalette.QINGCHEN_PRIMARY),
            contrast(text, ThemePalette.QINGCHEN_PRIMARY) >= 3f,
        )
    }

    @Test
    fun `深暖褐对午后主题按钮渐变两端与primary对比度均不低于3`() {
        val text = ThemePalette.ON_PRIMARY_SURFACE
        assertTrue(
            "buttonTop #EE9E60 实际 " + contrast(text, ThemePalette.WUHOU_BUTTON_TOP),
            contrast(text, ThemePalette.WUHOU_BUTTON_TOP) >= 3f,
        )
        assertTrue(
            "buttonBottom #FBB98A 实际 " + contrast(text, ThemePalette.WUHOU_BUTTON_BOTTOM),
            contrast(text, ThemePalette.WUHOU_BUTTON_BOTTOM) >= 3f,
        )
        assertTrue("primary #F8B37F 实际 " + contrast(text, ThemePalette.WUHOU_PRIMARY), contrast(text, ThemePalette.WUHOU_PRIMARY) >= 3f)
    }

    @Test
    fun `白字对焦糖棕褐主题按钮渐变两端与primary对比度均不低于3`() {
        val text = 0xFFFFFFFFL
        assertTrue(
            "buttonTop #785646 实际 " + contrast(text, ThemePalette.HUANGHUN_BUTTON_TOP),
            contrast(text, ThemePalette.HUANGHUN_BUTTON_TOP) >= 3f,
        )
        assertTrue(
            "buttonBottom #9D7562 实际 " + contrast(text, ThemePalette.HUANGHUN_BUTTON_BOTTOM),
            contrast(text, ThemePalette.HUANGHUN_BUTTON_BOTTOM) >= 3f,
        )
        assertTrue(
            "primary #8B6654 实际 " + contrast(text, ThemePalette.HUANGHUN_PRIMARY),
            contrast(text, ThemePalette.HUANGHUN_PRIMARY) >= 3f,
        )
    }

    // ---------- P2-5：深夜蕾丝提亮后对徽章底 ≥1.8:1 ----------

    @Test
    fun `深夜蕾丝对徽章底对比度不低于1点8`() {
        val ratio = contrast(ThemePalette.SHENYE_LACE, ThemePalette.SHENYE_CHIP_BG)
        assertTrue("lace #525C72 vs chipBg #182630 实际 $ratio", ratio >= 1.8f)
    }
}

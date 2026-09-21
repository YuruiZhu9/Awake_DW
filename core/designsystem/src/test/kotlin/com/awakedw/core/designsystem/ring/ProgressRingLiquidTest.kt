package com.awakedw.core.designsystem.ring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 环体液化（1.8.0）的纯几何：液面高度映射与波面相位数学。 */
class ProgressRingLiquidTest {
    private companion object {
        const val CENTER_Y = 100f
        const val INNER_RADIUS = 50f
    }

    @Test
    fun `零进度液面贴内圆底缘`() {
        assertEquals(CENTER_Y + INNER_RADIUS, liquidSurfaceY(0f, CENTER_Y, INNER_RADIUS), 0.001f)
    }

    @Test
    fun `满杯液面留杯沿空气`() {
        val surface = liquidSurfaceY(1f, CENTER_Y, INNER_RADIUS)
        val top = CENTER_Y - INNER_RADIUS
        assertTrue("满杯液面 $surface 应仍低于内圆顶缘 $top（留空气）", surface > top)
    }

    @Test
    fun `进度越界钳制到0和1`() {
        assertEquals(
            liquidSurfaceY(-0.5f, CENTER_Y, INNER_RADIUS),
            liquidSurfaceY(0f, CENTER_Y, INNER_RADIUS),
            0.001f,
        )
        assertEquals(
            liquidSurfaceY(1.5f, CENTER_Y, INNER_RADIUS),
            liquidSurfaceY(1f, CENTER_Y, INNER_RADIUS),
            0.001f,
        )
    }

    @Test
    fun `波面相位整周无缝回绕`() {
        for (fraction in listOf(0f, 0.25f, 0.5f, 0.73f, 1f)) {
            assertEquals(
                "横向 $fraction 处相位 0 与相位 1 的波面应重合",
                waveSurfaceOffset(fraction, 0f, 4f),
                waveSurfaceOffset(fraction, 1f, 4f),
                0.001f,
            )
        }
    }

    @Test
    fun `波面偏移不越过振幅包络`() {
        for (i in 0..24) {
            val offset = waveSurfaceOffset(xFraction = i / 24f, phase = 0.3f, amplitudePx = 5f)
            assertTrue("偏移 $offset 越过振幅包络 5f", offset <= 5f + 0.001f && offset >= -5f - 0.001f)
        }
    }

    @Test
    fun `零振幅波面即基准面`() {
        assertEquals(0f, waveSurfaceOffset(xFraction = 0.42f, phase = 0.7f, amplitudePx = 0f), 0.0001f)
    }
}

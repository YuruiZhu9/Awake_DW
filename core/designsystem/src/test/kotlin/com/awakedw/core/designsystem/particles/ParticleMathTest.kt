package com.awakedw.core.designsystem.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot

/**
 * ParticleMath 纯函数行为契约（TDD 先行）。
 *
 * 覆盖四类意图：
 * 1. floating 沿 progress01 单调上浮，越过顶部渐隐为零后回卷、自底部重现；
 * 2. 上浮轨迹按整周期闭合，p 与 p+1 结果一致（无缝循环的数学保证）；
 * 3. 半径/基础不透明度按「大-中-小」三层带内取值，辉光标志与层归属一一对应；
 * 4. burst 沿角度线性推进、不透明度随 travel01 严格单调降至零。
 */
class ParticleMathTest {
    private val area = Size(width = 400f, height = 800f)

    /** 密度锚点：测试固定为 40 像素，等价 density=1 时 40.dp.toPx()。 */
    private val anchorPx = 40f

    // ------------------------------------------------------------------
    // floating：单调上浮 / 顶部渐隐 / 回卷重现
    // ------------------------------------------------------------------

    @Test
    fun `首粒子随进度严格上浮渐隐归零后回卷并从底部重现`() {
        val n = 201
        val ys = FloatArray(n)
        val alphas = FloatArray(n)
        for (i in 0 until n) {
            val frame = ParticleMath.floating(index = 0, seed = 7L, sizePx = anchorPx, progress01 = i / (n - 1).toFloat(), area = area)
            ys[i] = frame.center.y
            alphas[i] = frame.alpha
        }
        // 回卷事件：y 突然大幅增加；其前一帧必须已在顶部外完全透明，
        // 回卷落点必须位于屏幕底部区域且恢复到该层可见不透明度。
        var wraps = 0
        for (i in 0 until n - 1) {
            val dy = ys[i + 1] - ys[i]
            if (dy > 1f) {
                wraps++
                assertTrue("回卷前应已完全渐隐", alphas[i] <= 0.001f)
                assertTrue("回卷前不应有向下漂移", ys[i] <= 1f)
                assertTrue("回卷后应落在底部区域", ys[i + 1] >= area.height - anchorPx)
                assertTrue(
                    "回卷后应恢复可见",
                    alphas[i + 1] >= ParticleMath.BIG_ALPHA_RANGE.start * 0.9f,
                )
            } else {
                // 非回卷段只允许向上移动（y 单调减小）。
                assertTrue("上浮过程中 y 不应增大", dy <= 1e-3f)
            }
        }
        // 速度超过一屏高 ⇒ 整周期内至少回卷一次。
        assertTrue("整周期至少发生一次回卷", wraps >= 1)
        // 首个非回卷段必须严格上浮。
        var first = 0
        while (first < n - 1 && ys[first + 1] - ys[first] < 1f) first++
        assertTrue(first > 0)
        for (i in 0 until first) {
            assertTrue("首段应严格上浮", ys[i + 1] < ys[i])
        }
    }

    @Test
    fun `上浮轨迹按整周期闭合可无缝循环拼接`() {
        for (p in listOf(0.13f, 0.5f, 0.87f)) {
            val a = ParticleMath.floating(index = 2, seed = 7L, sizePx = anchorPx, progress01 = p, area = area)
            val b = ParticleMath.floating(index = 2, seed = 7L, sizePx = anchorPx, progress01 = p + 1f, area = area)
            assertTrue(abs(a.center.x - b.center.x) < 0.5f)
            assertTrue(abs(a.center.y - b.center.y) < 0.5f)
            assertTrue(abs(a.alpha - b.alpha) < 1e-3f)
        }
        // 同参重复调用完全确定。
        val x = ParticleMath.floating(index = 5, seed = 99L, sizePx = anchorPx, progress01 = 0.42f, area = area)
        val y = ParticleMath.floating(index = 5, seed = 99L, sizePx = anchorPx, progress01 = 0.42f, area = area)
        assertEquals(x, y)
    }

    // ------------------------------------------------------------------
    // floating：三层半径 / 不透明度带 / 辉光标志
    // ------------------------------------------------------------------

    @Test
    fun `半径与基础不透明度分层落带且辉光标志与大中小一致`() {
        assertEquals(24, ParticleMath.DOT_COUNT)
        val samples = 33
        val baseAlpha = FloatArray(ParticleMath.DOT_COUNT)
        val radius = FloatArray(ParticleMath.DOT_COUNT)
        for (index in 0 until ParticleMath.DOT_COUNT) {
            var best = -1f
            radius[index] = ParticleMath.floating(index, 7L, anchorPx, 0f, area).radiusPx
            for (s in 0 until samples) {
                val frame = ParticleMath.floating(index, 7L, anchorPx, s / (samples - 1).toFloat(), area)
                if (frame.alpha > best) best = frame.alpha
                assertTrue(radius[index] > 0f)
                assertEquals(frame.radiusPx, radius[index])
            }
            baseAlpha[index] = best
        }
        val bigIndices = ParticleMath.BIG_INDEX_RANGE.toList()
        val mediumIndices = ParticleMath.MEDIUM_INDEX_RANGE.toList()
        val smallIndices = ParticleMath.SMALL_INDEX_RANGE.toList()

        fun band(values: List<Float>) = values.min() to values.max()
        val (bigRMin, bigRMax) = band(bigIndices.map { radius[it] })
        val (medRMin, medRMax) = band(mediumIndices.map { radius[it] })
        val (smallRMin, _) = band(smallIndices.map { radius[it] })
        // 半径严格分层，带间无重叠。
        assertTrue(bigRMin > medRMax)
        assertTrue(medRMin > smallRMin)
        // 各层半径落在各自因子带 × 锚点。
        for ((indices, range) in listOf(
            bigIndices to ParticleMath.BIG_RADIUS_FACTOR_RANGE,
            mediumIndices to ParticleMath.MEDIUM_RADIUS_FACTOR_RANGE,
            smallIndices to ParticleMath.SMALL_RADIUS_FACTOR_RANGE,
        )) {
            for (i in indices) {
                assertTrue("index=$i 半径越界", radius[i] in range.start * anchorPx..range.endInclusive * anchorPx)
            }
        }
        // 基础不透明度落在各自层带（取全程最大值即无衰减时的基准值）。
        for ((indices, range) in listOf(
            bigIndices to ParticleMath.BIG_ALPHA_RANGE,
            mediumIndices to ParticleMath.MEDIUM_ALPHA_RANGE,
            smallIndices to ParticleMath.SMALL_ALPHA_RANGE,
        )) {
            for (i in indices) {
                assertTrue("index=$i alpha=$baseAlpha", baseAlpha[i] in range.start..range.endInclusive)
            }
        }
        // 辉光标志：大/中层带辉光，小层不带。
        for (i in 0 until ParticleMath.DOT_COUNT) {
            val frame = ParticleMath.floating(i, 7L, anchorPx, 0.37f, area)
            assertEquals("index=$i glow 标志与层不符", i <= ParticleMath.MEDIUM_INDEX_RANGE.last, frame.glow)
        }
    }

    // ------------------------------------------------------------------
    // burst：角度线性推进 / 不透明度严格单调
    // ------------------------------------------------------------------

    @Test
    fun `迸发位置沿角度线性推进且距离随进度精确缩放`() {
        val origin = Offset(50f, 80f)
        val angle = (PI / 3.0).toFloat()
        val distance = 120f
        val direction = Offset(kotlin.math.cos(angle), kotlin.math.sin(angle))
        for ((travel, expectLen) in listOf(0f to 0f, 0.5f to 60f, 1f to 120f)) {
            val frame = ParticleMath.burst(travel01 = travel, origin = origin, angleRad = angle, distancePx = distance)
            val dx = frame.center.x - origin.x
            val dy = frame.center.y - origin.y
            val len = hypot(dx, dy)
            assertEquals(expectLen, len, 1e-3f)
            if (len > 0f) {
                assertEquals(direction.x, dx / len, 1e-4f)
                assertEquals(direction.y, dy / len, 1e-4f)
            }
            assertTrue(frame.glow)
        }
    }

    @Test
    fun `迸发不透明度随进度严格单调递减至完全透明`() {
        val origin = Offset(0f, 0f)
        val steps = 40
        var prevAlpha = 1.0001f
        val radiusFirst = ParticleMath.burst(0f, origin, 1.1f, 300f).radiusPx
        val radiusLast = ParticleMath.burst(1f, origin, 1.1f, 300f).radiusPx
        for (s in 0..steps) {
            val t = s / steps.toFloat()
            val frame = ParticleMath.burst(t, origin, 1.1f, 300f)
            assertTrue(frame.radiusPx > 0f)
            assertTrue("alpha 应严格递减", frame.alpha < prevAlpha)
            prevAlpha = frame.alpha
        }
        assertEquals(1f, ParticleMath.burst(0f, origin, 1.1f, 300f).alpha, 1e-6f)
        assertTrue("终点应完全透明", ParticleMath.burst(1f, origin, 1.1f, 300f).alpha <= 1e-3f)
        assertTrue("迸发过程应有收缩", radiusLast < radiusFirst)
    }
}

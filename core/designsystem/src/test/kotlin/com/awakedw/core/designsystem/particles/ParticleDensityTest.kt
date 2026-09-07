package com.awakedw.core.designsystem.particles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 粒子密度只是视觉层级参数，不改变粒子运动学和业务状态。 */
class ParticleDensityTest {
    @Test
    fun `安静密度比标准密度少且保留稳定的十四枚圆点`() {
        assertEquals(14, ParticleDensity.QUIET.dotCount)
        assertEquals(ParticleMath.DOT_COUNT, ParticleDensity.STANDARD.dotCount)
        assertTrue(ParticleDensity.QUIET.dotCount < ParticleDensity.STANDARD.dotCount)
    }

    @Test
    fun `安静粒子最大半径不超过六dp且亮度低于标准层`() {
        val quiet = ParticleDensity.QUIET
        assertTrue(40f * ParticleMath.BIG_RADIUS_FACTOR_RANGE.endInclusive * quiet.radiusScale <= 6f)
        assertTrue(quiet.accentAlphaScale < ParticleDensity.STANDARD.accentAlphaScale)
    }
}

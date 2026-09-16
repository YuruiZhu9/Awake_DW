package com.awakedw.core.designsystem.particles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 粒子密度只是视觉层级参数，不改变粒子运动学和业务状态。 */
class ParticleDensityTest {
    @Test
    fun `安静密度比标准密度少且保留稳定的二十四枚圆点`() {
        assertEquals(24, ParticleDensity.QUIET.dotCount)
        assertEquals(ParticleMath.DOT_COUNT, ParticleDensity.STANDARD.dotCount)
        assertTrue(ParticleDensity.QUIET.dotCount < ParticleDensity.STANDARD.dotCount)
    }

    @Test
    fun `安静密度层级与扩容前同构_以中粒收尾小粒少量`() {
        // QUIET 取前 24 颗 = 大 4 + 中 16 + 小 4：与扩容前「大中为主 + 少量小粒」同构。
        assertEquals(ParticleMath.SMALL_INDEX_RANGE.first + 4, ParticleDensity.QUIET.dotCount)
    }

    @Test
    fun `安静粒子最大半径不超过六dp且亮度低于标准层`() {
        val quiet = ParticleDensity.QUIET
        assertTrue(40f * ParticleMath.BIG_RADIUS_FACTOR_RANGE.endInclusive * quiet.radiusScale <= 6f)
        assertTrue(quiet.accentAlphaScale < ParticleDensity.STANDARD.accentAlphaScale)
    }
}

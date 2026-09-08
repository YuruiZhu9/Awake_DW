package com.awakedw.core.designsystem.particles

import com.awakedw.core.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeParticlesTest {
    @Test
    fun `all themes have intentional quiet particle motifs`() {
        assertEquals(ParticleStyle.PEARL, particleStyleOf(ThemeId.THIN_MINT))
        assertEquals(ParticleStyle.SILVER, particleStyleOf(ThemeId.GOTHIC))
        assertEquals(ParticleStyle.PETAL, particleStyleOf(ThemeId.CLERIC))
        assertEquals(ParticleStyle.PEARL, particleStyleOf(ThemeId.CARAMEL))
        assertEquals(ParticleStyle.PEARL, particleStyleOf(ThemeId.EMERALD))
        assertEquals(ParticleStyle.SILVER, particleStyleOf(ThemeId.NIGHT))
        assertEquals(ParticleStyle.PETAL, particleStyleOf(ThemeId.STRAWBERRY))
        assertEquals(ParticleStyle.PETAL, particleStyleOf(ThemeId.LAVENDER))
    }

    @Test
    fun `reading column is quieter than the edges with a symmetric bounded fade`() {
        assertTrue(readingColumnAlpha(0.5f) < readingColumnAlpha(0.1f))
        for (step in 0..100) {
            val x = step / 100f
            assertTrue(readingColumnAlpha(x) in 0.2f..1f)
            assertEquals(readingColumnAlpha(x), readingColumnAlpha(1f - x), 0.00001f)
        }
    }
}

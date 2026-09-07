package com.awakedw.core.designsystem

import com.awakedw.core.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Material components must inherit the same palette as bespoke Awake surfaces. */
class AwakeThemeTest {
    @Test
    fun `material scheme follows light theme anchors`() {
        val spec = ThemeById.getValue(ThemeId.EMERALD)
        val colors = materialColorSchemeOf(spec)

        assertEquals(spec.primary, colors.primary)
        assertEquals(spec.chipBg, colors.surface)
        assertEquals(spec.greetingColor, colors.onBackground)
    }

    @Test
    fun `gothic theme remains monochrome and dark`() {
        val spec = ThemeById.getValue(ThemeId.GOTHIC)
        val colors = materialColorSchemeOf(spec)

        assertEquals(spec.primary, colors.primary)
        assertEquals(spec.backgroundGradient.first(), colors.background)
        assertEquals(true, spec.isDark)
    }

    @Test
    fun `dark theme remains distinct and uses dark background`() {
        val spec = ThemeById.getValue(ThemeId.NIGHT)
        val colors = materialColorSchemeOf(spec)

        assertEquals(spec.primary, colors.primary)
        assertEquals(spec.backgroundGradient.first(), colors.background)
        assertNotEquals(ThemeById.getValue(ThemeId.EMERALD).backgroundGradient.first(), colors.background)
    }
}

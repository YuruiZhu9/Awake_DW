package com.awakedw.core.designsystem

import androidx.compose.ui.graphics.Color
import com.awakedw.core.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingFieldTest {
    @Test
    fun `reading field keeps a small deterministic opacity budget for every theme`() {
        ThemeId.entries.map(ThemeById::getValue).forEach { spec ->
            val alpha = readingFieldAlpha(spec)
            assertTrue("${spec.id} alpha must be positive", alpha > 0f)
            assertTrue("${spec.id} alpha must stay below a visible card", alpha <= 0.16f)
            assertEquals(if (spec.isDark) 0.16f else 0.12f, alpha, 0f)
        }
    }

    @Test
    fun `reading field mirrors the theme brightness instead of using artwork colors`() {
        assertEquals(Color.Black, readingFieldColor(ThemeById.getValue(ThemeId.GOTHIC)))
        assertEquals(Color.Black, readingFieldColor(ThemeById.getValue(ThemeId.NIGHT)))
        assertEquals(Color.White, readingFieldColor(ThemeById.getValue(ThemeId.EMERALD)))
        assertEquals(Color.White, readingFieldColor(ThemeById.getValue(ThemeId.CLERIC)))
    }
}

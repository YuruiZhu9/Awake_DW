package com.awakedw.core.designsystem.lolita

import com.awakedw.core.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Test

/** 用户提供的氛围素材按主题稳定映射，避免重组时出现随机跳图。 */
class LolitaBackdropTest {
    @Test
    fun `五个主题都有对应的氛围素材`() {
        assertEquals("lolita/green.jpg", lolitaAssetFileOf(ThemeId.EMERALD))
        assertEquals("lolita/rose.jpg", lolitaAssetFileOf(ThemeId.STRAWBERRY))
        assertEquals("lolita/warm.jpg", lolitaAssetFileOf(ThemeId.CARAMEL))
        assertEquals("lolita/gothic.jpg", lolitaAssetFileOf(ThemeId.NIGHT))
        assertEquals("lolita/blue.jpg", lolitaAssetFileOf(ThemeId.LAVENDER))
    }
}

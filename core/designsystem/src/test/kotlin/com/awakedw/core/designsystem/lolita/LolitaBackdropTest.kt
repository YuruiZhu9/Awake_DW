package com.awakedw.core.designsystem.lolita

import com.awakedw.core.model.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Test

/** 用户提供的氛围素材按主题稳定映射，避免重组时出现随机跳图。 */
class LolitaBackdropTest {
    @Test
    fun `所有主题都有对应的氛围素材`() {
        assertEquals("lolita/morning_blue_porcelain.jpg", lolitaAssetFileOf(ThemeId.EMERALD))
        assertEquals("lolita/afternoon_lotus.jpg", lolitaAssetFileOf(ThemeId.STRAWBERRY))
        assertEquals("lolita/twilight_milk_tea.jpg", lolitaAssetFileOf(ThemeId.CARAMEL))
        assertEquals("lolita/gothic.jpg", lolitaAssetFileOf(ThemeId.NIGHT))
        assertEquals("lolita/mist_lavender_rose.jpg", lolitaAssetFileOf(ThemeId.LAVENDER))
        assertEquals("lolita/gothic_frame.jpg", lolitaAssetFileOf(ThemeId.GOTHIC))
        assertEquals("lolita/thin_mint.jpg", lolitaAssetFileOf(ThemeId.THIN_MINT))
        assertEquals("lolita/cleric.jpg", lolitaAssetFileOf(ThemeId.CLERIC))
    }
}

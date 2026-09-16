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
        assertEquals("lolita/midnight_indigo.jpg", lolitaAssetFileOf(ThemeId.NIGHT))
        assertEquals("lolita/mist_lavender_rose.jpg", lolitaAssetFileOf(ThemeId.LAVENDER))
        assertEquals("lolita/gothic_frame.jpg", lolitaAssetFileOf(ThemeId.GOTHIC))
        assertEquals("lolita/thin_mint.jpg", lolitaAssetFileOf(ThemeId.THIN_MINT))
        assertEquals("lolita/cleric.jpg", lolitaAssetFileOf(ThemeId.CLERIC))
    }

    @Test
    fun `中景装饰层仅试点主题配置其余静默`() {
        // alpha13 §13 分层试点：只有深夜青黛配了中景素材，其余主题必须为 null（整层静默）。
        assertEquals("lolita/night_midground.png", themeArtworkOf(ThemeId.NIGHT).midgroundAsset)
        ThemeId.entries
            .filter { it != ThemeId.NIGHT }
            .forEach { id ->
                assertEquals("主题 $id 不应有中景素材", null, themeArtworkOf(id).midgroundAsset)
            }
    }
}

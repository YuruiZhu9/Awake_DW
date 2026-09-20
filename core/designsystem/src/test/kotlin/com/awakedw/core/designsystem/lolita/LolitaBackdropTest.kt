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
    fun `中景装饰层八主题全员配置画框主题降档`() {
        // 1.4.0 中景铺开：七主题素材到位，试点隔离退役；WebP 化后深夜也换 .webp。
        // 画框主题（哥特/圣职/薄巧）中景降档 0.45——与画框细节错开重量，非画框走默认 0.55。
        assertEquals("lolita/night_midground.webp", themeArtworkOf(ThemeId.NIGHT).midgroundAsset)
        ThemeId.entries.forEach { id ->
            val art = themeArtworkOf(id)
            assertEquals("主题 $id 应有中景素材", true, art.midgroundAsset != null)
            val expected = if (art.framed) 0.45f else 0.55f
            assertEquals("主题 $id 中景不透明度", expected, art.midgroundOpacity)
        }
    }
}

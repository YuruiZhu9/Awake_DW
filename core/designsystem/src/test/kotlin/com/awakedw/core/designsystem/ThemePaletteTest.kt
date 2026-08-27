package com.awakedw.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 设计规格 §2.1「三主题色板」十六进制锚点快照测试。
 *
 * 锚点以 ARGB Long（0xFFxxxxxx，不透明）形式保存在 [ThemePalette]，
 * 组件层的 androidx Color 由这些常量转换而来——本测试只依赖纯 JVM 常量表，
 * 防止任何色值无意漂移。
 */
class ThemePaletteTest {
    @Test
    fun `翡翠绿全部十六进制锚点`() {
        // 背景渐变：#F3FBF7 → #E2F3EC → #D8EEE4
        assertEquals(0xFFF3FBF7, ThemePalette.EMERALD_BG_1)
        assertEquals(0xFFE2F3EC, ThemePalette.EMERALD_BG_2)
        assertEquals(0xFFD8EEE4, ThemePalette.EMERALD_BG_3)
        // 主色（进度环/强调）：#0FA37A
        assertEquals(0xFF0FA37A, ThemePalette.EMERALD_PRIMARY)
        // 进度环轨道：#BFEBDC
        assertEquals(0xFFBFEBDC, ThemePalette.EMERALD_RING_TRACK)
        // 环心数值文字：#0B7D5C
        assertEquals(0xFF0B7D5C, ThemePalette.EMERALD_RING_VALUE_TEXT)
        // 问候语文字：#14684E（副 #4F8A75）
        assertEquals(0xFF14684E, ThemePalette.EMERALD_GREETING)
        assertEquals(0xFF4F8A75, ThemePalette.EMERALD_GREETING_SUB)
        // 按钮（线性渐变）：#0E9F74 → #26BE8D
        assertEquals(0xFF0E9F74, ThemePalette.EMERALD_BUTTON_TOP)
        assertEquals(0xFF26BE8D, ThemePalette.EMERALD_BUTTON_BOTTOM)
        // 徽章底/字：#DCF1E8 / #177A5C
        assertEquals(0xFFDCF1E8, ThemePalette.EMERALD_CHIP_BG)
        assertEquals(0xFF177A5C, ThemePalette.EMERALD_CHIP_TEXT)
        // 粒子色族：#10A87C #2BC493 #57D3AC #6ADBBC
        assertEquals(0xFF10A87C, ThemePalette.EMERALD_PARTICLE_1)
        assertEquals(0xFF2BC493, ThemePalette.EMERALD_PARTICLE_2)
        assertEquals(0xFF57D3AC, ThemePalette.EMERALD_PARTICLE_3)
        assertEquals(0xFF6ADBBC, ThemePalette.EMERALD_PARTICLE_4)
    }

    @Test
    fun `草莓雾光全部十六进制锚点`() {
        // 背景渐变：#FFFFFF → #FFF4F6 → #FFEAEE/#FFE5EB
        assertEquals(0xFFFFFFFF, ThemePalette.STRAWBERRY_BG_1)
        assertEquals(0xFFFFF4F6, ThemePalette.STRAWBERRY_BG_2)
        assertEquals(0xFFFFEAEE, ThemePalette.STRAWBERRY_BG_3)
        assertEquals(0xFFFFE5EB, ThemePalette.STRAWBERRY_BG_4)
        // 主色（进度环/强调）：#F9709A
        assertEquals(0xFFF9709A, ThemePalette.STRAWBERRY_PRIMARY)
        // 进度环轨道：#FDE4EB
        assertEquals(0xFFFDE4EB, ThemePalette.STRAWBERRY_RING_TRACK)
        // 环心数值文字：#EE6390
        assertEquals(0xFFEE6390, ThemePalette.STRAWBERRY_RING_VALUE_TEXT)
        // 问候语文字：#D0688A（副 #DB96AC）
        assertEquals(0xFFD0688A, ThemePalette.STRAWBERRY_GREETING)
        assertEquals(0xFFDB96AC, ThemePalette.STRAWBERRY_GREETING_SUB)
        // 按钮（线性渐变）：#F986A6 → #FFB3C8
        assertEquals(0xFFF986A6, ThemePalette.STRAWBERRY_BUTTON_TOP)
        assertEquals(0xFFFFB3C8, ThemePalette.STRAWBERRY_BUTTON_BOTTOM)
        // 徽章底：rgba(255,233,240,.92)，锚定其 RGB 等价值与独立透明度
        assertEquals(0xFFFFE9F0, ThemePalette.STRAWBERRY_CHIP_BG)
        assertEquals(0.92f, ThemePalette.CHIP_BG_ALPHA)
        // 徽章字：#C9688A
        assertEquals(0xFFC9688A, ThemePalette.STRAWBERRY_CHIP_TEXT)
        // 粒子色族：#F9709A #F986A6 #FA9AB6 #FF9FBC #FFC2D4
        assertEquals(0xFFF9709A, ThemePalette.STRAWBERRY_PARTICLE_1)
        assertEquals(0xFFF986A6, ThemePalette.STRAWBERRY_PARTICLE_2)
        assertEquals(0xFFFA9AB6, ThemePalette.STRAWBERRY_PARTICLE_3)
        assertEquals(0xFFFF9FBC, ThemePalette.STRAWBERRY_PARTICLE_4)
        assertEquals(0xFFFFC2D4, ThemePalette.STRAWBERRY_PARTICLE_5)
    }

    @Test
    fun `焦糖奶茶全部十六进制锚点`() {
        // 背景渐变：#FFF8EA → #FFEED4 → #FFE2C0
        assertEquals(0xFFFFF8EA, ThemePalette.CARAMEL_BG_1)
        assertEquals(0xFFFFEED4, ThemePalette.CARAMEL_BG_2)
        assertEquals(0xFFFFE2C0, ThemePalette.CARAMEL_BG_3)
        // 主色（进度环/强调）：#E8853F
        assertEquals(0xFFE8853F, ThemePalette.CARAMEL_PRIMARY)
        // 进度环轨道：#F8DCBA
        assertEquals(0xFFF8DCBA, ThemePalette.CARAMEL_RING_TRACK)
        // 环心数值文字：#BF671F
        assertEquals(0xFFBF671F, ThemePalette.CARAMEL_RING_VALUE_TEXT)
        // 问候语文字：#8F5626（副 #B89066）
        assertEquals(0xFF8F5626, ThemePalette.CARAMEL_GREETING)
        assertEquals(0xFFB89066, ThemePalette.CARAMEL_GREETING_SUB)
        // 按钮（线性渐变）：#E07E36 → #FFAB57
        assertEquals(0xFFE07E36, ThemePalette.CARAMEL_BUTTON_TOP)
        assertEquals(0xFFFFAB57, ThemePalette.CARAMEL_BUTTON_BOTTOM)
        // 徽章底/字：#FFE9CC / #8F5626
        assertEquals(0xFFFFE9CC, ThemePalette.CARAMEL_CHIP_BG)
        assertEquals(0xFF8F5626, ThemePalette.CARAMEL_CHIP_TEXT)
        // 粒子色族：#E8853F #FFB761 #FFCF8E #FFE0AE
        assertEquals(0xFFE8853F, ThemePalette.CARAMEL_PARTICLE_1)
        assertEquals(0xFFFFB761, ThemePalette.CARAMEL_PARTICLE_2)
        assertEquals(0xFFFFCF8E, ThemePalette.CARAMEL_PARTICLE_3)
        assertEquals(0xFFFFE0AE, ThemePalette.CARAMEL_PARTICLE_4)
    }
}

package com.awakedw.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v0.2 中国色再映射（moodboard §4）四主题十六进制锚点快照测试。
 *
 * 锚点以 ARGB Long（0xFFxxxxxx，不透明）形式保存在 [ThemePalette]，
 * 组件层的 androidx Color 由这些常量转换而来——本测试只依赖纯 JVM 常量表，
 * 防止任何色值无意漂移。四主题锚点方向：清晨天水碧系 / 午后藕荷系 /
 * 黄昏缃叶系 / 深夜青黛系。
 */
class ThemePaletteTest {
    @Test
    fun `清晨天水碧系全部十六进制锚点`() {
        // 背景渐变：#F1FBF7 → #E2F4EB → #D4F2E7（天水碧底锚）
        assertEquals(0xFFF1FBF7, ThemePalette.QINGCHEN_BG_1)
        assertEquals(0xFFE2F4EB, ThemePalette.QINGCHEN_BG_2)
        assertEquals(0xFFD4F2E7, ThemePalette.QINGCHEN_BG_3)
        // 主色（进度环/强调）：#2A9A6A 柏枝绿（指示值）
        assertEquals(0xFF2A9A6A, ThemePalette.QINGCHEN_PRIMARY)
        // 进度环轨道：#BFE7DC
        assertEquals(0xFFBFE7DC, ThemePalette.QINGCHEN_RING_TRACK)
        // 环心数值文字：#157A50
        assertEquals(0xFF157A50, ThemePalette.QINGCHEN_RING_VALUE_TEXT)
        // 问候语文字：#1A684D（副 #55907A）
        assertEquals(0xFF1A684D, ThemePalette.QINGCHEN_GREETING)
        assertEquals(0xFF55907A, ThemePalette.QINGCHEN_GREETING_SUB)
        // 按钮（线性渐变）：#279061 → #43B988
        assertEquals(0xFF279061, ThemePalette.QINGCHEN_BUTTON_TOP)
        assertEquals(0xFF43B988, ThemePalette.QINGCHEN_BUTTON_BOTTOM)
        // 徽章底/字：#D6ECF0（月白面锚）/ #16704A
        assertEquals(0xFFD6ECF0, ThemePalette.QINGCHEN_CHIP_BG)
        assertEquals(0xFF16704A, ThemePalette.QINGCHEN_CHIP_TEXT)
        // 粒子色族：#2A9A6A #3FB389 #62C9A6 #79D4B5
        assertEquals(0xFF2A9A6A, ThemePalette.QINGCHEN_PARTICLE_1)
        assertEquals(0xFF3FB389, ThemePalette.QINGCHEN_PARTICLE_2)
        assertEquals(0xFF62C9A6, ThemePalette.QINGCHEN_PARTICLE_3)
        assertEquals(0xFF79D4B5, ThemePalette.QINGCHEN_PARTICLE_4)
    }

    @Test
    fun `午后藕荷系全部十六进制锚点`() {
        // 背景渐变：#FEF9FA → #F9E8EC → #F1D6DD → #E4C6D0（藕荷底锚）
        assertEquals(0xFFFEF9FA, ThemePalette.WUHOU_BG_1)
        assertEquals(0xFFF9E8EC, ThemePalette.WUHOU_BG_2)
        assertEquals(0xFFF1D6DD, ThemePalette.WUHOU_BG_3)
        assertEquals(0xFFE4C6D0, ThemePalette.WUHOU_BG_4)
        // 主色（进度环/强调）：#F8B37F 十样锦
        assertEquals(0xFFF8B37F, ThemePalette.WUHOU_PRIMARY)
        // 进度环轨道：#F5D3DC
        assertEquals(0xFFF5D3DC, ThemePalette.WUHOU_RING_TRACK)
        // 环心数值文字：#DB955F
        assertEquals(0xFFDB955F, ThemePalette.WUHOU_RING_VALUE_TEXT)
        // 问候语文字：#A66F7E（副 #C79AA6）
        assertEquals(0xFFA66F7E, ThemePalette.WUHOU_GREETING)
        assertEquals(0xFFC79AA6, ThemePalette.WUHOU_GREETING_SUB)
        // 按钮（线性渐变）：#EE9E60 → #FBB98A
        assertEquals(0xFFEE9E60, ThemePalette.WUHOU_BUTTON_TOP)
        assertEquals(0xFFFBB98A, ThemePalette.WUHOU_BUTTON_BOTTOM)
        // 徽章底：rgba(254,223,225,.92)，锚定其 RGB 等价值与独立透明度
        assertEquals(0xFFFEDFE1, ThemePalette.WUHOU_CHIP_BG)
        assertEquals(0.92f, ThemePalette.CHIP_BG_ALPHA)
        // 徽章字：#AC6879
        assertEquals(0xFFAC6879, ThemePalette.WUHOU_CHIP_TEXT)
        // 粒子色族：#F8B37F #FABE92 #FBC9A7 #FCD4BB #FEE3D3
        assertEquals(0xFFF8B37F, ThemePalette.WUHOU_PARTICLE_1)
        assertEquals(0xFFFABE92, ThemePalette.WUHOU_PARTICLE_2)
        assertEquals(0xFFFBC9A7, ThemePalette.WUHOU_PARTICLE_3)
        assertEquals(0xFFFCD4BB, ThemePalette.WUHOU_PARTICLE_4)
        assertEquals(0xFFFEE3D3, ThemePalette.WUHOU_PARTICLE_5)
    }

    @Test
    fun `黄昏缃叶系全部十六进制锚点`() {
        // 背景渐变：#FDF9EC → #FAF0D0 → #F6E7B4（缃叶浅底锚·指示值）
        assertEquals(0xFFFDF9EC, ThemePalette.HUANGHUN_BG_1)
        assertEquals(0xFFFAF0D0, ThemePalette.HUANGHUN_BG_2)
        assertEquals(0xFFF6E7B4, ThemePalette.HUANGHUN_BG_3)
        // 主色（进度环/强调）：#D9B611 秋香
        assertEquals(0xFFD9B611, ThemePalette.HUANGHUN_PRIMARY)
        // 进度环轨道：#F3E3B0
        assertEquals(0xFFF3E3B0, ThemePalette.HUANGHUN_RING_TRACK)
        // 环心数值文字：#9A7B0A
        assertEquals(0xFF9A7B0A, ThemePalette.HUANGHUN_RING_VALUE_TEXT)
        // 问候语文字：#6F5A12（副 #A88462 驼面锚）
        assertEquals(0xFF6F5A12, ThemePalette.HUANGHUN_GREETING)
        assertEquals(0xFFA88462, ThemePalette.HUANGHUN_GREETING_SUB)
        // 按钮（线性渐变）：#C49E10 → #E2BC30
        assertEquals(0xFFC49E10, ThemePalette.HUANGHUN_BUTTON_TOP)
        assertEquals(0xFFE2BC30, ThemePalette.HUANGHUN_BUTTON_BOTTOM)
        // 徽章底/字：#F0DCC8（驼淡化）/ #6F5A12
        assertEquals(0xFFF0DCC8, ThemePalette.HUANGHUN_CHIP_BG)
        assertEquals(0xFF6F5A12, ThemePalette.HUANGHUN_CHIP_TEXT)
        // 粒子色族：#D9B611 #DFBF33 #E9CB61 #F2DFA4
        assertEquals(0xFFD9B611, ThemePalette.HUANGHUN_PARTICLE_1)
        assertEquals(0xFFDFBF33, ThemePalette.HUANGHUN_PARTICLE_2)
        assertEquals(0xFFE9CB61, ThemePalette.HUANGHUN_PARTICLE_3)
        assertEquals(0xFFF2DFA4, ThemePalette.HUANGHUN_PARTICLE_4)
    }

    @Test
    fun `深夜青黛系全部十六进制锚点`() {
        // 背景渐变：#0C0E19 → #0F1221 → #111430（青黛底压暗）
        assertEquals(0xFF0C0E19, ThemePalette.SHENYE_BG_1)
        assertEquals(0xFF0F1221, ThemePalette.SHENYE_BG_2)
        assertEquals(0xFF111430, ThemePalette.SHENYE_BG_3)
        // 主色（进度环/强调）：#B9D8E0 月白降饱和变体
        assertEquals(0xFFB9D8E0, ThemePalette.SHENYE_PRIMARY)
        // 进度环轨道：#1F2639
        assertEquals(0xFF1F2639, ThemePalette.SHENYE_RING_TRACK)
        // 环心数值文字：#D6EAEE
        assertEquals(0xFFD6EAEE, ThemePalette.SHENYE_RING_VALUE_TEXT)
        // 问候语文字：#DDEDF2（副 #8AA9B4）
        assertEquals(0xFFDDEDF2, ThemePalette.SHENYE_GREETING)
        assertEquals(0xFF8AA9B4, ThemePalette.SHENYE_GREETING_SUB)
        // 按钮（线性渐变）：#5C8FA0 → #86B7C4
        assertEquals(0xFF5C8FA0, ThemePalette.SHENYE_BUTTON_TOP)
        assertEquals(0xFF86B7C4, ThemePalette.SHENYE_BUTTON_BOTTOM)
        // 徽章底/字：#182630（鸦青压暗）/ #B4D0D8
        assertEquals(0xFF182630, ThemePalette.SHENYE_CHIP_BG)
        assertEquals(0xFFB4D0D8, ThemePalette.SHENYE_CHIP_TEXT)
        // 粒子色族：#B9D8E0 #C9E2E7 #D8EAEE #31424C（末位降亮度适配暗底）
        assertEquals(0xFFB9D8E0, ThemePalette.SHENYE_PARTICLE_1)
        assertEquals(0xFFC9E2E7, ThemePalette.SHENYE_PARTICLE_2)
        assertEquals(0xFFD8EAEE, ThemePalette.SHENYE_PARTICLE_3)
        assertEquals(0xFF31424C, ThemePalette.SHENYE_PARTICLE_4)
    }

    @Test
    fun `四主题蕾丝线与描金锚点`() {
        // 洛丽塔配饰层（§12）：蕾丝线按主题派生，描金共享软香槟金；
        // 深夜蕾丝改锚「暗银」，与描金成暗银描金。
        assertEquals(0xFFFADCE2, ThemePalette.WUHOU_LACE)
        assertEquals(0xFFD2EDE7, ThemePalette.QINGCHEN_LACE)
        assertEquals(0xFFFAECC9, ThemePalette.HUANGHUN_LACE)
        assertEquals(0xFF343A4C, ThemePalette.SHENYE_LACE)
        assertEquals(0xFFD9B98A, ThemePalette.GOLD_TRIM)
    }
}

package com.awakedw.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v0.3 主题色板（moodboard §4）五主题十六进制锚点快照测试。
 *
 * 锚点以 ARGB Long（0xFFxxxxxx，不透明）形式保存在 [ThemePalette]，
 * 组件层的 androidx Color 由这些常量转换而来——本测试只依赖纯 JVM 常量表，
 * 防止任何色值无意漂移。四主题锚点方向：清晨天水碧系 / 午后藕荷系 /
 * 黄昏奶茶棕褐系 / 深夜青黛系 / 雾紫玫瑰系。
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
    fun `黄昏奶茶棕褐系全部十六进制锚点`() {
        // 背景渐变：#FBF8F4 → #F1EAE4 → #E5D9D2（奶茶棕褐·清透纸面）
        assertEquals(0xFFFBF8F4, ThemePalette.HUANGHUN_BG_1)
        assertEquals(0xFFF1EAE4, ThemePalette.HUANGHUN_BG_2)
        assertEquals(0xFFE5D9D2, ThemePalette.HUANGHUN_BG_3)
        // 主色（进度环/强调）：#8B6654 可可棕
        assertEquals(0xFF8B6654, ThemePalette.HUANGHUN_PRIMARY)
        // 进度环轨道：#D7C6BC
        assertEquals(0xFFD7C6BC, ThemePalette.HUANGHUN_RING_TRACK)
        // 环心数值文字：#704E42
        assertEquals(0xFF704E42, ThemePalette.HUANGHUN_RING_VALUE_TEXT)
        // 问候语文字：#5C4840（副 #92766B）
        assertEquals(0xFF5C4840, ThemePalette.HUANGHUN_GREETING)
        assertEquals(0xFF92766B, ThemePalette.HUANGHUN_GREETING_SUB)
        // 按钮（线性渐变）：#785646 → #9D7562
        assertEquals(0xFF785646, ThemePalette.HUANGHUN_BUTTON_TOP)
        assertEquals(0xFF9D7562, ThemePalette.HUANGHUN_BUTTON_BOTTOM)
        // 徽章底/字：#E9DED8（奶茶纸面）/ #6A5047
        assertEquals(0xFFE9DED8, ThemePalette.HUANGHUN_CHIP_BG)
        assertEquals(0xFF6A5047, ThemePalette.HUANGHUN_CHIP_TEXT)
        // 粒子色族：#8B6654 #AA826E #C19B88 #D5BBAE
        assertEquals(0xFF8B6654, ThemePalette.HUANGHUN_PARTICLE_1)
        assertEquals(0xFFAA826E, ThemePalette.HUANGHUN_PARTICLE_2)
        assertEquals(0xFFC19B88, ThemePalette.HUANGHUN_PARTICLE_3)
        assertEquals(0xFFD5BBAE, ThemePalette.HUANGHUN_PARTICLE_4)
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
    fun `五主题蕾丝线与描金锚点`() {
        // 洛丽塔配饰层（§12）：蕾丝线按主题派生，描金共享软香槟金；
        // 深夜蕾丝提亮锚（P2-5）：#525C72 = 月白·灰按 35% 预混鸦青暗底的等效暗银，
        // 旧锚 #343A4C 对徽章底仅 1.3:1 不可见，提亮后 ≥1.8（对比度断言见 OnPrimarySurfaceTest）。
        assertEquals(0xFFFADCE2, ThemePalette.WUHOU_LACE)
        assertEquals(0xFFD2EDE7, ThemePalette.QINGCHEN_LACE)
        assertEquals(0xFFDECAC0, ThemePalette.HUANGHUN_LACE)
        assertEquals(0xFF525C72, ThemePalette.SHENYE_LACE)
        assertEquals(0xFFE2D3EF, ThemePalette.LAVENDER_LACE)
        assertEquals(0xFFD9B98A, ThemePalette.GOLD_TRIM)
    }

    @Test
    fun `雾紫玫瑰系全部十六进制锚点`() {
        assertEquals(0xFFFBF9FE, ThemePalette.LAVENDER_BG_1)
        assertEquals(0xFFF1ECFA, ThemePalette.LAVENDER_BG_2)
        assertEquals(0xFFE4D9F2, ThemePalette.LAVENDER_BG_3)
        assertEquals(0xFF8C6BB1, ThemePalette.LAVENDER_PRIMARY)
        assertEquals(0xFFD9CDEB, ThemePalette.LAVENDER_RING_TRACK)
        assertEquals(0xFF6F548E, ThemePalette.LAVENDER_RING_VALUE_TEXT)
        assertEquals(0xFF5C476F, ThemePalette.LAVENDER_GREETING)
        assertEquals(0xFF9A87AB, ThemePalette.LAVENDER_GREETING_SUB)
        assertEquals(0xFF8061A8, ThemePalette.LAVENDER_BUTTON_TOP)
        assertEquals(0xFFA98CC5, ThemePalette.LAVENDER_BUTTON_BOTTOM)
        assertEquals(0xFFE9E0F4, ThemePalette.LAVENDER_CHIP_BG)
        assertEquals(0xFF6F548E, ThemePalette.LAVENDER_CHIP_TEXT)
        assertEquals(0xFFE2D3EF, ThemePalette.LAVENDER_LACE)
    }
}

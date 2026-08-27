package com.awakedw.core.designsystem

/**
 * 三主题十六进制色值锚点表（ARGB Long，0xFFxxxxxx 表示不透明）。
 *
 * 数值逐一对照设计规格 §2.1「三主题色板」，供 [ThemePaletteTest] 做防漂移快照断言；
 * 组件层的 androidx Color 均由这些常量转换而来。纯 JVM 常量，不依赖任何 Android 类。
 */
object ThemePalette {
    // ------------------------------------------------------------------
    // A · 翡翠绿 Emerald（默认/白天）
    // ------------------------------------------------------------------

    /** 背景渐变：#F3FBF7 → #E2F3EC → #D8EEE4 */
    const val EMERALD_BG_1 = 0xFFF3FBF7
    const val EMERALD_BG_2 = 0xFFE2F3EC
    const val EMERALD_BG_3 = 0xFFD8EEE4

    /** 主色（进度环/强调）：#0FA37A */
    const val EMERALD_PRIMARY = 0xFF0FA37A

    /** 进度环轨道：#BFEBDC */
    const val EMERALD_RING_TRACK = 0xFFBFEBDC

    /** 环心数值文字：#0B7D5C */
    const val EMERALD_RING_VALUE_TEXT = 0xFF0B7D5C

    /** 问候语文字：#14684E（副 #4F8A75） */
    const val EMERALD_GREETING = 0xFF14684E
    const val EMERALD_GREETING_SUB = 0xFF4F8A75

    /** 按钮（线性渐变）：#0E9F74 → #26BE8D */
    const val EMERALD_BUTTON_TOP = 0xFF0E9F74
    const val EMERALD_BUTTON_BOTTOM = 0xFF26BE8D

    /** 徽章底/字：#DCF1E8 / #177A5C */
    const val EMERALD_CHIP_BG = 0xFFDCF1E8
    const val EMERALD_CHIP_TEXT = 0xFF177A5C

    /** 粒子色族：#10A87C #2BC493 #57D3AC #6ADBBC */
    const val EMERALD_PARTICLE_1 = 0xFF10A87C
    const val EMERALD_PARTICLE_2 = 0xFF2BC493
    const val EMERALD_PARTICLE_3 = 0xFF57D3AC
    const val EMERALD_PARTICLE_4 = 0xFF6ADBBC

    // ------------------------------------------------------------------
    // B · 草莓雾光 Strawberry（早晨）
    // ------------------------------------------------------------------

    /** 背景渐变：#FFFFFF → #FFF4F6 → #FFEAEE/#FFE5EB（末两段为相邻色标） */
    const val STRAWBERRY_BG_1 = 0xFFFFFFFF
    const val STRAWBERRY_BG_2 = 0xFFFFF4F6
    const val STRAWBERRY_BG_3 = 0xFFFFEAEE
    const val STRAWBERRY_BG_4 = 0xFFFFE5EB

    /** 主色（进度环/强调）：#F9709A */
    const val STRAWBERRY_PRIMARY = 0xFFF9709A

    /** 进度环轨道：#FDE4EB */
    const val STRAWBERRY_RING_TRACK = 0xFFFDE4EB

    /** 环心数值文字：#EE6390 */
    const val STRAWBERRY_RING_VALUE_TEXT = 0xFFEE6390

    /** 问候语文字：#D0688A（副 #DB96AC） */
    const val STRAWBERRY_GREETING = 0xFFD0688A
    const val STRAWBERRY_GREETING_SUB = 0xFFDB96AC

    /** 按钮（线性渐变）：#F986A6 → #FFB3C8 */
    const val STRAWBERRY_BUTTON_TOP = 0xFFF986A6
    const val STRAWBERRY_BUTTON_BOTTOM = 0xFFFFB3C8

    /** 徽章底 rgba(255,233,240,.92)：锚定 RGB 等价值，透明度见 [CHIP_BG_ALPHA] */
    const val STRAWBERRY_CHIP_BG = 0xFFFFE9F0

    /** 徽章字：#C9688A */
    const val STRAWBERRY_CHIP_TEXT = 0xFFC9688A

    /** 粒子色族：#F9709A #F986A6 #FA9AB6 #FF9FBC #FFC2D4 */
    const val STRAWBERRY_PARTICLE_1 = 0xFFF9709A
    const val STRAWBERRY_PARTICLE_2 = 0xFFF986A6
    const val STRAWBERRY_PARTICLE_3 = 0xFFFA9AB6
    const val STRAWBERRY_PARTICLE_4 = 0xFFFF9FBC
    const val STRAWBERRY_PARTICLE_5 = 0xFFFFC2D4

    // ------------------------------------------------------------------
    // C · 焦糖奶茶 Caramel（夜晚）
    // ------------------------------------------------------------------

    /** 背景渐变：#FFF8EA → #FFEED4 → #FFE2C0 */
    const val CARAMEL_BG_1 = 0xFFFFF8EA
    const val CARAMEL_BG_2 = 0xFFFFEED4
    const val CARAMEL_BG_3 = 0xFFFFE2C0

    /** 主色（进度环/强调）：#E8853F */
    const val CARAMEL_PRIMARY = 0xFFE8853F

    /** 进度环轨道：#F8DCBA */
    const val CARAMEL_RING_TRACK = 0xFFF8DCBA

    /** 环心数值文字：#BF671F */
    const val CARAMEL_RING_VALUE_TEXT = 0xFFBF671F

    /** 问候语文字：#8F5626（副 #B89066） */
    const val CARAMEL_GREETING = 0xFF8F5626
    const val CARAMEL_GREETING_SUB = 0xFFB89066

    /** 按钮（线性渐变）：#E07E36 → #FFAB57 */
    const val CARAMEL_BUTTON_TOP = 0xFFE07E36
    const val CARAMEL_BUTTON_BOTTOM = 0xFFFFAB57

    /** 徽章底/字：#FFE9CC / #8F5626 */
    const val CARAMEL_CHIP_BG = 0xFFFFE9CC
    const val CARAMEL_CHIP_TEXT = 0xFF8F5626

    /** 粒子色族：#E8853F #FFB761 #FFCF8E #FFE0AE */
    const val CARAMEL_PARTICLE_1 = 0xFFE8853F
    const val CARAMEL_PARTICLE_2 = 0xFFFFB761
    const val CARAMEL_PARTICLE_3 = 0xFFFFCF8E
    const val CARAMEL_PARTICLE_4 = 0xFFFFE0AE

    // ------------------------------------------------------------------
    // 统一质感参数（§2.2）
    // ------------------------------------------------------------------

    /** 徽章底透明度：草莓主题 rgba(255,233,240,.92) */
    const val CHIP_BG_ALPHA = 0.92f
}

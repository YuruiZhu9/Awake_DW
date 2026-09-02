package com.awakedw.core.designsystem

/**
 * 四主题中国色十六进制锚点表（ARGB Long，0xFFxxxxxx 表示不透明）。
 *
 * v0.2 中国色再映射（moodboard §4）：清晨天水碧系 / 午后藕荷系 / 黄昏缃叶系 /
 * 深夜青黛系，常量名为主题拼音 + 角色后缀，注释标注中国传统色名。
 * 供 [ThemePaletteTest] 做防漂移快照断言；组件层的 androidx Color 均由这些
 * 常量转换而来。纯 JVM 常量，不依赖任何 Android 类。
 *
 * 注意：简报锚点为指示值，未经 zhongguose.com 站值校准（该站为 JS 渲染，
 * 抓取无效已放弃校准），用户可按站值一行常量微调。
 */
object ThemePalette {
    // ------------------------------------------------------------------
    // A · 清晨 · 天水碧系（ThemeId.EMERALD，默认/白天）
    // ------------------------------------------------------------------

    /** 背景渐变：#F1FBF7 → #E2F4EB → #D4F2E7（天水碧底锚） */
    const val QINGCHEN_BG_1 = 0xFFF1FBF7 // 天水碧·纸白
    const val QINGCHEN_BG_2 = 0xFFE2F4EB // 天水碧·浅
    const val QINGCHEN_BG_3 = 0xFFD4F2E7 // 天水碧（底锚）

    /** 主色（进度环/强调）：#2A9A6A 柏枝绿（指示值，未经站值校准） */
    const val QINGCHEN_PRIMARY = 0xFF2A9A6A // 柏枝绿

    /** 进度环轨道：#BFE7DC */
    const val QINGCHEN_RING_TRACK = 0xFFBFE7DC // 天水碧·深

    /** 环心数值文字：#157A50 */
    const val QINGCHEN_RING_VALUE_TEXT = 0xFF157A50 // 柏枝绿·深

    /** 问候语文字：#1A684D（副 #55907A） */
    const val QINGCHEN_GREETING = 0xFF1A684D // 官绿·深
    const val QINGCHEN_GREETING_SUB = 0xFF55907A // 铜绿·灰

    /** 按钮（线性渐变）：#279061 → #43B988 */
    const val QINGCHEN_BUTTON_TOP = 0xFF279061 // 柏枝绿·深
    const val QINGCHEN_BUTTON_BOTTOM = 0xFF43B988 // 柏枝绿·亮

    /** 徽章底/字：#D6ECF0（月白面锚）/ #16704A */
    const val QINGCHEN_CHIP_BG = 0xFFD6ECF0 // 月白（面锚）
    const val QINGCHEN_CHIP_TEXT = 0xFF16704A // 官绿

    /** 粒子色族：#2A9A6A #3FB389 #62C9A6 #79D4B5 */
    const val QINGCHEN_PARTICLE_1 = 0xFF2A9A6A // 柏枝绿
    const val QINGCHEN_PARTICLE_2 = 0xFF3FB389 // 松花绿·亮
    const val QINGCHEN_PARTICLE_3 = 0xFF62C9A6 // 天水碧·饱和
    const val QINGCHEN_PARTICLE_4 = 0xFF79D4B5 // 天水碧·亮

    // ------------------------------------------------------------------
    // B · 午后 · 藕荷系（ThemeId.STRAWBERRY，早晨）
    // ------------------------------------------------------------------

    /** 背景渐变：#FEF9FA → #F9E8EC → #F1D6DD → #E4C6D0（藕荷底锚，末两段为相邻色标） */
    const val WUHOU_BG_1 = 0xFFFEF9FA // 藕荷·纸白
    const val WUHOU_BG_2 = 0xFFF9E8EC // 藕荷·浅一
    const val WUHOU_BG_3 = 0xFFF1D6DD // 藕荷·浅二
    const val WUHOU_BG_4 = 0xFFE4C6D0 // 藕荷（底锚）

    /** 主色（进度环/强调）：#F8B37F 十样锦 */
    const val WUHOU_PRIMARY = 0xFFF8B37F // 十样锦（强调锚）

    /** 进度环轨道：#F5D3DC */
    const val WUHOU_RING_TRACK = 0xFFF5D3DC // 藕荷·深

    /** 环心数值文字：#DB955F */
    const val WUHOU_RING_VALUE_TEXT = 0xFFDB955F // 十样锦·深

    /** 问候语文字：#A66F7E（副 #C79AA6） */
    const val WUHOU_GREETING = 0xFFA66F7E // 藕荷·深
    const val WUHOU_GREETING_SUB = 0xFFC79AA6 // 藕荷·灰

    /** 按钮（线性渐变）：#EE9E60 → #FBB98A */
    const val WUHOU_BUTTON_TOP = 0xFFEE9E60 // 十样锦·深
    const val WUHOU_BUTTON_BOTTOM = 0xFFFBB98A // 十样锦·亮

    /** 徽章底 rgba(254,223,225,.92)：锚定 RGB 等价值，透明度见 [CHIP_BG_ALPHA] */
    const val WUHOU_CHIP_BG = 0xFFFEDFE1 // 樱花（面锚·指示值）

    /** 徽章字：#AC6879 */
    const val WUHOU_CHIP_TEXT = 0xFFAC6879 // 藕荷·深

    /** 粒子色族：#F8B37F #FABE92 #FBC9A7 #FCD4BB #FEE3D3 */
    const val WUHOU_PARTICLE_1 = 0xFFF8B37F // 十样锦
    const val WUHOU_PARTICLE_2 = 0xFFFABE92 // 十样锦·亮一
    const val WUHOU_PARTICLE_3 = 0xFFFBC9A7 // 十样锦·亮二
    const val WUHOU_PARTICLE_4 = 0xFFFCD4BB // 十样锦·亮三
    const val WUHOU_PARTICLE_5 = 0xFFFEE3D3 // 十样锦·纸白

    // ------------------------------------------------------------------
    // C · 黄昏 · 缃叶系（ThemeId.CARAMEL，夜晚）
    // ------------------------------------------------------------------

    /** 背景渐变：#FDF9EC → #FAF0D0 → #F6E7B4（缃叶浅底锚·指示值） */
    const val HUANGHUN_BG_1 = 0xFFFDF9EC // 缃叶·纸白
    const val HUANGHUN_BG_2 = 0xFFFAF0D0 // 缃叶·浅
    const val HUANGHUN_BG_3 = 0xFFF6E7B4 // 缃叶浅（底锚·指示值）

    /** 主色（进度环/强调）：#D9B611 秋香 */
    const val HUANGHUN_PRIMARY = 0xFFD9B611 // 秋香（强调锚）

    /** 进度环轨道：#F3E3B0 */
    const val HUANGHUN_RING_TRACK = 0xFFF3E3B0 // 缃叶·深

    /** 环心数值文字：#9A7B0A */
    const val HUANGHUN_RING_VALUE_TEXT = 0xFF9A7B0A // 秋香·深

    /** 问候语文字：#6F5A12（副 #A88462 驼面锚原值） */
    const val HUANGHUN_GREETING = 0xFF6F5A12 // 秋香·褐
    const val HUANGHUN_GREETING_SUB = 0xFFA88462 // 驼（面锚原值）

    /** 按钮（线性渐变）：#C49E10 → #E2BC30 */
    const val HUANGHUN_BUTTON_TOP = 0xFFC49E10 // 秋香·深
    const val HUANGHUN_BUTTON_BOTTOM = 0xFFE2BC30 // 秋香·亮

    /** 徽章底/字：#F0DCC8（驼淡化，明度校正见报告）/ #6F5A12 */
    const val HUANGHUN_CHIP_BG = 0xFFF0DCC8 // 驼·淡化
    const val HUANGHUN_CHIP_TEXT = 0xFF6F5A12 // 秋香·褐

    /** 粒子色族：#D9B611 #DFBF33 #E9CB61 #F2DFA4 */
    const val HUANGHUN_PARTICLE_1 = 0xFFD9B611 // 秋香
    const val HUANGHUN_PARTICLE_2 = 0xFFDFBF33 // 秋香·亮一
    const val HUANGHUN_PARTICLE_3 = 0xFFE9CB61 // 秋香·亮二
    const val HUANGHUN_PARTICLE_4 = 0xFFF2DFA4 // 秋香·纸白

    // ------------------------------------------------------------------
    // D · 深夜 · 青黛系（ThemeId.NIGHT，深夜，治愈铁律：保持暗底）
    // ------------------------------------------------------------------

    /** 背景渐变：#0C0E19 → #0F1221 → #111430（青黛压暗，维持旧板 L6–13 暗底带） */
    const val SHENYE_BG_1 = 0xFF0C0E19 // 青黛·墨
    const val SHENYE_BG_2 = 0xFF0F1221 // 青黛·深
    const val SHENYE_BG_3 = 0xFF111430 // 青黛·暗

    /** 主色（进度环/强调）：#B9D8E0 月白降饱和变体（S60→39，月光感） */
    const val SHENYE_PRIMARY = 0xFFB9D8E0 // 月白·降饱和

    /** 进度环轨道：#1F2639 */
    const val SHENYE_RING_TRACK = 0xFF1F2639 // 青黛·灰

    /** 环心数值文字：#D6EAEE */
    const val SHENYE_RING_VALUE_TEXT = 0xFFD6EAEE // 月白·亮

    /** 问候语文字：#DDEDF2（副 #8AA9B4） */
    const val SHENYE_GREETING = 0xFFDDEDF2 // 月白·纸
    const val SHENYE_GREETING_SUB = 0xFF8AA9B4 // 鸦青·灰

    /** 按钮（线性渐变）：#5C8FA0 → #86B7C4（中明度带，白字对比度对齐旧板） */
    const val SHENYE_BUTTON_TOP = 0xFF5C8FA0 // 鸦青·中
    const val SHENYE_BUTTON_BOTTOM = 0xFF86B7C4 // 鸦青·亮

    /** 徽章底/字：#182630（鸦青压暗，明度校正见报告）/ #B4D0D8 */
    const val SHENYE_CHIP_BG = 0xFF182630 // 鸦青·暗
    const val SHENYE_CHIP_TEXT = 0xFFB4D0D8 // 月白·灰

    /** 粒子色族：#B9D8E0 #C9E2E7 #D8EAEE #31424C（末位降亮度适配暗底） */
    const val SHENYE_PARTICLE_1 = 0xFFB9D8E0 // 月白·降饱和
    const val SHENYE_PARTICLE_2 = 0xFFC9E2E7 // 月白·亮一
    const val SHENYE_PARTICLE_3 = 0xFFD8EAEE // 月白·亮二
    const val SHENYE_PARTICLE_4 = 0xFF31424C // 鸦青·深

    // ------------------------------------------------------------------
    // 洛丽塔配饰层（§12）：蕾丝线按主题派生，描金为共享软香槟金；
    // 深夜蕾丝改锚「暗银」，与描金成暗银描金（moodboard §4）
    // ------------------------------------------------------------------

    /** 蕾丝线：午后（樱花浅粉）#FADCE2 */
    const val WUHOU_LACE = 0xFFFADCE2 // 樱花·浅

    /** 蕾丝线：清晨（月白薄荷）#D2EDE7 */
    const val QINGCHEN_LACE = 0xFFD2EDE7 // 月白·薄荷

    /** 蕾丝线：黄昏（缃叶奶油）#FAECC9 */
    const val HUANGHUN_LACE = 0xFFFAECC9 // 缃叶·奶油

    /** 蕾丝线：深夜（暗银提亮，配描金）#525C72——旧锚 #343A4C 对徽章底仅 1.3:1 不可见（P2-5），
     * 提亮为月白·灰 #B4D0D8 按 35% 预混于鸦青暗底 #182630 的等效色，对徽章底对比 ≈2.3:1（≥1.8 目标）。
     */
    const val SHENYE_LACE = 0xFF525C72 // 暗银·提亮

    /** 描金：软香槟金（蝴蝶结中结），四主题共用 #D9B98A */
    const val GOLD_TRIM = 0xFFD9B98A

    /** 主色底字色（P2-4）：深暖褐 #3E322B——三浅色主题（清晨/午后/黄昏）的按钮渐变与
     * 选中 chip 上原用白字仅 1.8–2.5:1，统一改此色后对各自主色渐变两端与 primary 均 ≥3:1
     * （逐项计算见 OnPrimarySurfaceTest）；深夜主题按钮色板按白字校准，不走此锚。
     */
    const val ON_PRIMARY_SURFACE = 0xFF3E322B // 深暖褐

    // ------------------------------------------------------------------
    // 统一质感参数（§2.2）
    // ------------------------------------------------------------------

    /** 徽章底透明度：午后主题 rgba(254,223,225,.92) */
    const val CHIP_BG_ALPHA = 0.92f
}

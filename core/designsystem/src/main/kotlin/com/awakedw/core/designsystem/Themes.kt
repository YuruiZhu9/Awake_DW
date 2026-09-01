package com.awakedw.core.designsystem

import androidx.compose.ui.graphics.Color
import com.awakedw.core.model.ThemeId

/** A · 清晨天水碧系（ThemeId.EMERALD，默认/白天），色值对照 moodboard §4。 */
val EmeraldThemeSpec: ThemeSpec =
    ThemeSpec(
        id = ThemeId.EMERALD,
        backgroundGradient =
            listOf(
                Color(ThemePalette.QINGCHEN_BG_1),
                Color(ThemePalette.QINGCHEN_BG_2),
                Color(ThemePalette.QINGCHEN_BG_3),
            ),
        primary = Color(ThemePalette.QINGCHEN_PRIMARY),
        ringTrack = Color(ThemePalette.QINGCHEN_RING_TRACK),
        ringValueText = Color(ThemePalette.QINGCHEN_RING_VALUE_TEXT),
        greetingColor = Color(ThemePalette.QINGCHEN_GREETING),
        greetingSubColor = Color(ThemePalette.QINGCHEN_GREETING_SUB),
        buttonTop = Color(ThemePalette.QINGCHEN_BUTTON_TOP),
        buttonBottom = Color(ThemePalette.QINGCHEN_BUTTON_BOTTOM),
        chipBg = Color(ThemePalette.QINGCHEN_CHIP_BG),
        chipText = Color(ThemePalette.QINGCHEN_CHIP_TEXT),
        particleColors =
            listOf(
                Color(ThemePalette.QINGCHEN_PARTICLE_1),
                Color(ThemePalette.QINGCHEN_PARTICLE_2),
                Color(ThemePalette.QINGCHEN_PARTICLE_3),
                Color(ThemePalette.QINGCHEN_PARTICLE_4),
            ),
        // 柔光晕取主题主色低透明度（§2.2），绘制时由 GradientBackdrop 施加透明度
        haloColor = Color(ThemePalette.QINGCHEN_PRIMARY),
        laceColor = Color(ThemePalette.QINGCHEN_LACE),
    )

/** B · 午后藕荷系（ThemeId.STRAWBERRY，早晨），色值对照 moodboard §4。 */
val StrawberryThemeSpec: ThemeSpec =
    ThemeSpec(
        id = ThemeId.STRAWBERRY,
        backgroundGradient =
            listOf(
                Color(ThemePalette.WUHOU_BG_1),
                Color(ThemePalette.WUHOU_BG_2),
                Color(ThemePalette.WUHOU_BG_3),
                Color(ThemePalette.WUHOU_BG_4),
            ),
        primary = Color(ThemePalette.WUHOU_PRIMARY),
        ringTrack = Color(ThemePalette.WUHOU_RING_TRACK),
        ringValueText = Color(ThemePalette.WUHOU_RING_VALUE_TEXT),
        greetingColor = Color(ThemePalette.WUHOU_GREETING),
        greetingSubColor = Color(ThemePalette.WUHOU_GREETING_SUB),
        buttonTop = Color(ThemePalette.WUHOU_BUTTON_TOP),
        buttonBottom = Color(ThemePalette.WUHOU_BUTTON_BOTTOM),
        chipBg = Color(ThemePalette.WUHOU_CHIP_BG).copy(alpha = ThemePalette.CHIP_BG_ALPHA),
        chipText = Color(ThemePalette.WUHOU_CHIP_TEXT),
        particleColors =
            listOf(
                Color(ThemePalette.WUHOU_PARTICLE_1),
                Color(ThemePalette.WUHOU_PARTICLE_2),
                Color(ThemePalette.WUHOU_PARTICLE_3),
                Color(ThemePalette.WUHOU_PARTICLE_4),
                Color(ThemePalette.WUHOU_PARTICLE_5),
            ),
        haloColor = Color(ThemePalette.WUHOU_PRIMARY),
        laceColor = Color(ThemePalette.WUHOU_LACE),
    )

/** C · 黄昏缃叶系（ThemeId.CARAMEL，夜晚），色值对照 moodboard §4。 */
val CaramelThemeSpec: ThemeSpec =
    ThemeSpec(
        id = ThemeId.CARAMEL,
        backgroundGradient =
            listOf(
                Color(ThemePalette.HUANGHUN_BG_1),
                Color(ThemePalette.HUANGHUN_BG_2),
                Color(ThemePalette.HUANGHUN_BG_3),
            ),
        primary = Color(ThemePalette.HUANGHUN_PRIMARY),
        ringTrack = Color(ThemePalette.HUANGHUN_RING_TRACK),
        ringValueText = Color(ThemePalette.HUANGHUN_RING_VALUE_TEXT),
        greetingColor = Color(ThemePalette.HUANGHUN_GREETING),
        greetingSubColor = Color(ThemePalette.HUANGHUN_GREETING_SUB),
        buttonTop = Color(ThemePalette.HUANGHUN_BUTTON_TOP),
        buttonBottom = Color(ThemePalette.HUANGHUN_BUTTON_BOTTOM),
        chipBg = Color(ThemePalette.HUANGHUN_CHIP_BG),
        chipText = Color(ThemePalette.HUANGHUN_CHIP_TEXT),
        particleColors =
            listOf(
                Color(ThemePalette.HUANGHUN_PARTICLE_1),
                Color(ThemePalette.HUANGHUN_PARTICLE_2),
                Color(ThemePalette.HUANGHUN_PARTICLE_3),
                Color(ThemePalette.HUANGHUN_PARTICLE_4),
            ),
        haloColor = Color(ThemePalette.HUANGHUN_PRIMARY),
        laceColor = Color(ThemePalette.HUANGHUN_LACE),
    )

/** D · 深夜青黛系（ThemeId.NIGHT，深夜），色值对照 moodboard §4；青黛暗底 + 月白强调，柔光晕与粒子在暗底上成为主角。 */
val NightThemeSpec: ThemeSpec =
    ThemeSpec(
        id = ThemeId.NIGHT,
        backgroundGradient =
            listOf(
                Color(ThemePalette.SHENYE_BG_1),
                Color(ThemePalette.SHENYE_BG_2),
                Color(ThemePalette.SHENYE_BG_3),
            ),
        primary = Color(ThemePalette.SHENYE_PRIMARY),
        ringTrack = Color(ThemePalette.SHENYE_RING_TRACK),
        ringValueText = Color(ThemePalette.SHENYE_RING_VALUE_TEXT),
        greetingColor = Color(ThemePalette.SHENYE_GREETING),
        greetingSubColor = Color(ThemePalette.SHENYE_GREETING_SUB),
        buttonTop = Color(ThemePalette.SHENYE_BUTTON_TOP),
        buttonBottom = Color(ThemePalette.SHENYE_BUTTON_BOTTOM),
        chipBg = Color(ThemePalette.SHENYE_CHIP_BG),
        chipText = Color(ThemePalette.SHENYE_CHIP_TEXT),
        particleColors =
            listOf(
                Color(ThemePalette.SHENYE_PARTICLE_1),
                Color(ThemePalette.SHENYE_PARTICLE_2),
                Color(ThemePalette.SHENYE_PARTICLE_3),
                Color(ThemePalette.SHENYE_PARTICLE_4),
            ),
        haloColor = Color(ThemePalette.SHENYE_PRIMARY),
        isDark = true,
        laceColor = Color(ThemePalette.SHENYE_LACE),
    )

/** ThemeId → 主题规格 的全覆盖查找表。 */
val ThemeById: Map<ThemeId, ThemeSpec> =
    mapOf(
        EmeraldThemeSpec.id to EmeraldThemeSpec,
        StrawberryThemeSpec.id to StrawberryThemeSpec,
        CaramelThemeSpec.id to CaramelThemeSpec,
        NightThemeSpec.id to NightThemeSpec,
    )

package com.awakedw.core.designsystem

import androidx.compose.ui.graphics.Color
import com.awakedw.core.model.ThemeId

/** A · 翡翠绿（默认/白天），色值对照规格 §2.1。 */
val EmeraldThemeSpec: ThemeSpec =
    ThemeSpec(
        id = ThemeId.EMERALD,
        backgroundGradient =
            listOf(
                Color(ThemePalette.EMERALD_BG_1),
                Color(ThemePalette.EMERALD_BG_2),
                Color(ThemePalette.EMERALD_BG_3),
            ),
        primary = Color(ThemePalette.EMERALD_PRIMARY),
        ringTrack = Color(ThemePalette.EMERALD_RING_TRACK),
        ringValueText = Color(ThemePalette.EMERALD_RING_VALUE_TEXT),
        greetingColor = Color(ThemePalette.EMERALD_GREETING),
        greetingSubColor = Color(ThemePalette.EMERALD_GREETING_SUB),
        buttonTop = Color(ThemePalette.EMERALD_BUTTON_TOP),
        buttonBottom = Color(ThemePalette.EMERALD_BUTTON_BOTTOM),
        chipBg = Color(ThemePalette.EMERALD_CHIP_BG),
        chipText = Color(ThemePalette.EMERALD_CHIP_TEXT),
        particleColors =
            listOf(
                Color(ThemePalette.EMERALD_PARTICLE_1),
                Color(ThemePalette.EMERALD_PARTICLE_2),
                Color(ThemePalette.EMERALD_PARTICLE_3),
                Color(ThemePalette.EMERALD_PARTICLE_4),
            ),
        // 柔光晕取主题主色低透明度（§2.2），绘制时由 GradientBackdrop 施加透明度
        haloColor = Color(ThemePalette.EMERALD_PRIMARY),
    )

/** B · 草莓雾光（早晨），色值对照规格 §2.1。 */
val StrawberryThemeSpec: ThemeSpec =
    ThemeSpec(
        id = ThemeId.STRAWBERRY,
        backgroundGradient =
            listOf(
                Color(ThemePalette.STRAWBERRY_BG_1),
                Color(ThemePalette.STRAWBERRY_BG_2),
                Color(ThemePalette.STRAWBERRY_BG_3),
                Color(ThemePalette.STRAWBERRY_BG_4),
            ),
        primary = Color(ThemePalette.STRAWBERRY_PRIMARY),
        ringTrack = Color(ThemePalette.STRAWBERRY_RING_TRACK),
        ringValueText = Color(ThemePalette.STRAWBERRY_RING_VALUE_TEXT),
        greetingColor = Color(ThemePalette.STRAWBERRY_GREETING),
        greetingSubColor = Color(ThemePalette.STRAWBERRY_GREETING_SUB),
        buttonTop = Color(ThemePalette.STRAWBERRY_BUTTON_TOP),
        buttonBottom = Color(ThemePalette.STRAWBERRY_BUTTON_BOTTOM),
        chipBg = Color(ThemePalette.STRAWBERRY_CHIP_BG).copy(alpha = ThemePalette.CHIP_BG_ALPHA),
        chipText = Color(ThemePalette.STRAWBERRY_CHIP_TEXT),
        particleColors =
            listOf(
                Color(ThemePalette.STRAWBERRY_PARTICLE_1),
                Color(ThemePalette.STRAWBERRY_PARTICLE_2),
                Color(ThemePalette.STRAWBERRY_PARTICLE_3),
                Color(ThemePalette.STRAWBERRY_PARTICLE_4),
                Color(ThemePalette.STRAWBERRY_PARTICLE_5),
            ),
        haloColor = Color(ThemePalette.STRAWBERRY_PRIMARY),
    )

/** C · 焦糖奶茶（夜晚），色值对照规格 §2.1。 */
val CaramelThemeSpec: ThemeSpec =
    ThemeSpec(
        id = ThemeId.CARAMEL,
        backgroundGradient =
            listOf(
                Color(ThemePalette.CARAMEL_BG_1),
                Color(ThemePalette.CARAMEL_BG_2),
                Color(ThemePalette.CARAMEL_BG_3),
            ),
        primary = Color(ThemePalette.CARAMEL_PRIMARY),
        ringTrack = Color(ThemePalette.CARAMEL_RING_TRACK),
        ringValueText = Color(ThemePalette.CARAMEL_RING_VALUE_TEXT),
        greetingColor = Color(ThemePalette.CARAMEL_GREETING),
        greetingSubColor = Color(ThemePalette.CARAMEL_GREETING_SUB),
        buttonTop = Color(ThemePalette.CARAMEL_BUTTON_TOP),
        buttonBottom = Color(ThemePalette.CARAMEL_BUTTON_BOTTOM),
        chipBg = Color(ThemePalette.CARAMEL_CHIP_BG),
        chipText = Color(ThemePalette.CARAMEL_CHIP_TEXT),
        particleColors =
            listOf(
                Color(ThemePalette.CARAMEL_PARTICLE_1),
                Color(ThemePalette.CARAMEL_PARTICLE_2),
                Color(ThemePalette.CARAMEL_PARTICLE_3),
                Color(ThemePalette.CARAMEL_PARTICLE_4),
            ),
        haloColor = Color(ThemePalette.CARAMEL_PRIMARY),
    )

/** D · 深夜墨青（深夜），色值对照规格 §2.1；墨青底 + 薄荷强调，柔光晕与粒子在暗底上成为主角。 */
val NightThemeSpec: ThemeSpec =
    ThemeSpec(
        id = ThemeId.NIGHT,
        backgroundGradient =
            listOf(
                Color(ThemePalette.NIGHT_BG_1),
                Color(ThemePalette.NIGHT_BG_2),
                Color(ThemePalette.NIGHT_BG_3),
            ),
        primary = Color(ThemePalette.NIGHT_PRIMARY),
        ringTrack = Color(ThemePalette.NIGHT_RING_TRACK),
        ringValueText = Color(ThemePalette.NIGHT_RING_VALUE_TEXT),
        greetingColor = Color(ThemePalette.NIGHT_GREETING),
        greetingSubColor = Color(ThemePalette.NIGHT_GREETING_SUB),
        buttonTop = Color(ThemePalette.NIGHT_BUTTON_TOP),
        buttonBottom = Color(ThemePalette.NIGHT_BUTTON_BOTTOM),
        chipBg = Color(ThemePalette.NIGHT_CHIP_BG),
        chipText = Color(ThemePalette.NIGHT_CHIP_TEXT),
        particleColors =
            listOf(
                Color(ThemePalette.NIGHT_PARTICLE_1),
                Color(ThemePalette.NIGHT_PARTICLE_2),
                Color(ThemePalette.NIGHT_PARTICLE_3),
                Color(ThemePalette.NIGHT_PARTICLE_4),
            ),
        haloColor = Color(ThemePalette.NIGHT_PRIMARY),
        isDark = true,
    )

/** ThemeId → 主题规格 的全覆盖查找表。 */
val ThemeById: Map<ThemeId, ThemeSpec> =
    mapOf(
        EmeraldThemeSpec.id to EmeraldThemeSpec,
        StrawberryThemeSpec.id to StrawberryThemeSpec,
        CaramelThemeSpec.id to CaramelThemeSpec,
        NightThemeSpec.id to NightThemeSpec,
    )

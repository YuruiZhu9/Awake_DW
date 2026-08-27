package com.awakedw.core.designsystem

import androidx.compose.ui.graphics.Color
import com.awakedw.core.model.ThemeId

/**
 * 单一主题的全部视觉规格（设计规格 §2.1「三主题色板」）。
 *
 * [backgroundGradient] 为背景渐变的逐段色标列表，顺序与 §2.1 表格一致；
 * 其余字段一一对应表格行。所有色值来源于 [ThemePalette] 的十六进制锚点常量。
 */
data class ThemeSpec(
    val id: ThemeId,
    val backgroundGradient: List<Color>,
    val primary: Color,
    val ringTrack: Color,
    val ringValueText: Color,
    val greetingColor: Color,
    val greetingSubColor: Color,
    val buttonTop: Color,
    val buttonBottom: Color,
    val chipBg: Color,
    val chipText: Color,
    val particleColors: List<Color>,
    val haloColor: Color,
)

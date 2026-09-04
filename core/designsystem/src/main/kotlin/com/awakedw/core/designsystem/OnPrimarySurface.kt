package com.awakedw.core.designsystem

import androidx.compose.ui.graphics.Color
import com.awakedw.core.model.ThemeId

/**
 * 主色底（按钮渐变 / 选中 chip / 步进钮）上的字色助手（P2-4 对比度修复）。
 *
 * - 深夜墨青（isDark）：按钮色板（#5C8FA0→#86B7C4）按白字校准，维持 [Color.White]；
 * - 焦糖奶茶改为棕褐色深按钮，使用白字保持清晰且避免黄色大色块；
 * - 其余浅色主题：原用白字压浅暖主色仅 1.8–2.5:1，统一改深暖褐
 *   [ThemePalette.ON_PRIMARY_SURFACE]（#3E322B），对各自主色渐变两端与 primary
 *   均 ≥3:1——逐项对比度计算由 OnPrimarySurfaceTest 用 WCAG 公式断言防漂移。
 *
 * 纯函数（不依赖组合状态），组合内可直接调用，JVM 测试无需 Robolectric。
 */
fun onPrimarySurface(spec: ThemeSpec): Color =
    if (spec.isDark || spec.id == ThemeId.CARAMEL) Color.White else Color(ThemePalette.ON_PRIMARY_SURFACE)

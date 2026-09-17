package com.awakedw.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.artworkPanelOpacity

/** 左边距线距纸左缘的距离：信纸版面的装订侧留白。 */
private val SHEET_MARGIN_LINE_START = 26.dp

/** 内容列距边距线的呼吸距离。 */
private val SHEET_CONTENT_START = 40.dp

/**
 * 信纸载体（1.1.0 视觉 refresh 方向 A，见 `docs/design/v1.1-visual-refresh-proposal.md`）：
 * 把页面内容收进一张「纸」——纸色与背景拉开一档明度，背景主图在纸外呼吸更多；
 * 左侧一条贯穿的边距线 + 顶端珍珠点是信纸的版面骨骼，替代旧的横向蕾丝分隔线
 * （同一种语言收成一个元素，不叠加重复图框）。
 *
 * 本组件只承载版面，不承载任何业务语义；长内容自然换行、自然撑高（基线 §3）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun LetterSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spec = currentThemeSpec()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = spec.chipBg.copy(alpha = artworkPanelOpacity(spec.id, 0.78f)),
        border = BorderStroke(width = 1.dp, color = spec.laceColor.copy(alpha = 0.45f)),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = SHEET_MARGIN_LINE_START, top = 14.dp, bottom = 14.dp)
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(spec.laceColor.copy(alpha = 0.55f)),
                )
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = SHEET_MARGIN_LINE_START - 2.5.dp, top = 8.dp)
                        .size(6.dp)
                        .background(spec.primary.copy(alpha = 0.72f), CircleShape),
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = SHEET_CONTENT_START, end = 16.dp, top = 18.dp, bottom = 18.dp),
                content = content,
            )
        }
    }
}

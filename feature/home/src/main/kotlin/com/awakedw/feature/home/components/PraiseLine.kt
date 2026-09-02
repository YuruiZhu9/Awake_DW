package com.awakedw.feature.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec

/** 夸夸语淡入淡出时长。 */
private const val PRAISE_FADE_MS = 400

/** 猫语气泡宽度上限（布局审计 P1-2）：容纳一句胆大王短语，超出即换行、行数不限。 */
internal val CAT_LINE_BUBBLE_MAX_WIDTH = 200.dp

/** 猫语气泡的内边距：胶囊底与文字的呼吸边。 */
private val CAT_LINE_BUBBLE_PADDING = PaddingValues(horizontal = 14.dp, vertical = 8.dp)

/**
 * 随机夸夸语（规格 §4.2 第 5 步）：打卡瞬间淡入，~1.4s 后由 ViewModel 收起，
 * Crossfade 负责淡入淡出。高度自适应（布局审计 P1-2）：
 * - 默认（环下夸夸语）路径：最多两行（≈44dp）后省略号，不再固定 26dp 腰斩长句；
 * - [multiLine]（猫语气泡）路径：宽度随内容（上限 [CAT_LINE_BUBBLE_MAX_WIDTH]）、行数不限、
 *   胶囊圆角随内容长出来；
 * - [text] 为 null 时零占位——不渲染任何盒子，调用方以叠层挂载即无常驻空档。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun PraiseLine(
    text: String?,
    modifier: Modifier = Modifier,
    multiLine: Boolean = false,
) {
    val spec = currentThemeSpec()
    Crossfade(
        targetState = text,
        animationSpec = tween(durationMillis = PRAISE_FADE_MS),
        label = "praiseLine",
        modifier = modifier,
    ) { current ->
        if (current != null) {
            if (multiLine) {
                Box(
                    modifier =
                        Modifier
                            .widthIn(max = CAT_LINE_BUBBLE_MAX_WIDTH)
                            .background(color = spec.chipBg, shape = RoundedCornerShape(percent = 50))
                            .padding(CAT_LINE_BUBBLE_PADDING),
                ) {
                    Text(
                        text = current,
                        color = spec.chipText,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    text = current,
                    color = spec.greetingColor,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

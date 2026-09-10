package com.awakedw.feature.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberReduceMotion

/** 猫语气泡淡入淡出时长。 */
private const val CAT_BUBBLE_FADE_MS = 400

/** 猫语气泡宽度上限：短句自然内缩成小胶囊，超出即换行、行数不限。 */
internal val CAT_LINE_BUBBLE_MAX_WIDTH = 200.dp

/** 猫语气泡的内边距：胶囊底与文字的呼吸边。 */
private val CAT_LINE_BUBBLE_PADDING = PaddingValues(horizontal = 14.dp, vertical = 8.dp)

/** 气泡圆角：左下角收小，像朝向下方猫的一声轻语。 */
private val CAT_LINE_BUBBLE_SHAPE = RoundedCornerShape(16.dp, 16.dp, 16.dp, 5.dp)

/**
 * 猫咪回应气泡：只承载**猫语**（[com.awakedw.core.data.copy.ShortCopies] 的短句池）。
 *
 * 与打卡确认分开是有意的：打卡确认走环心（[com.awakedw.feature.home.HomeScreen] 的 centerNote），
 * 两者同屏时一上一下、各说各的事，不再抢同一格而互相覆盖。
 *
 * 宽度随内容（上限 [CAT_LINE_BUBBLE_MAX_WIDTH]），短句内缩成小胶囊、长句自然换行，
 * 文字始终左对齐（视觉基线：猫咪长回应保持左对齐）；[text] 为 null 时零占位。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun CatBubble(
    text: String?,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val reduceMotion = rememberReduceMotion()
    Crossfade(
        targetState = text,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else CAT_BUBBLE_FADE_MS),
        label = "catBubble",
        modifier = modifier,
    ) { current ->
        if (current != null) {
            Box(
                modifier =
                    Modifier
                        .widthIn(max = CAT_LINE_BUBBLE_MAX_WIDTH)
                        .background(color = spec.chipBg.copy(alpha = 0.92f), shape = CAT_LINE_BUBBLE_SHAPE)
                        .border(1.dp, spec.laceColor.copy(alpha = 0.48f), CAT_LINE_BUBBLE_SHAPE)
                        .padding(CAT_LINE_BUBBLE_PADDING)
                        .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text(
                    text = current,
                    color = spec.chipText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

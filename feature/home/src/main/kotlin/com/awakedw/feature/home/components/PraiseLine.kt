package com.awakedw.feature.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec

/** 夸夸语淡入淡出时长。 */
private const val PRAISE_FADE_MS = 400

/** 夸夸语行的保留高度：文案进出时布局不跳。 */
private val PRAISE_LINE_HEIGHT = 26.dp

/**
 * 随机夸夸语（规格 §4.2 第 5 步）：打卡瞬间淡入，~1.4s 后由 ViewModel 收起，
 * Crossfade 负责淡入淡出；无文案时保留等高占位，按钮不跳位。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun PraiseLine(
    text: String?,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(PRAISE_LINE_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = text,
            animationSpec = tween(durationMillis = PRAISE_FADE_MS),
            label = "praiseLine",
        ) { current ->
            if (current != null) {
                Text(
                    text = current,
                    color = spec.greetingColor,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

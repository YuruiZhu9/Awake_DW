package com.awakedw.core.designsystem.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 单次入场编排（§10.3，克制基调）：组合挂载即播一次「淡入 + 轻上移」，
 * 之后不再重放（[MutableTransitionState] 单向翻转，重组不触发二次动画）。
 * [delayMillis] 供列表逐条错峰；默认 200ms / 8dp，与全局动效基调一致。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun FadeUpOnce(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    durationMillis: Int = 200,
    rise: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    val risePx = with(LocalDensity.current) { rise.roundToPx() }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter =
            fadeIn(animationSpec = tween(durationMillis, delayMillis = delayMillis)) +
                slideInVertically(animationSpec = tween(durationMillis, delayMillis = delayMillis)) { risePx },
    ) {
        content()
    }
}

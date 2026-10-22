package com.awakedw.core.designsystem.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.awakedw.core.designsystem.MotionTokens
import com.awakedw.core.designsystem.rememberReduceMotion

/**
 * 按压语言全 app 只有一种（§24）：按下缩至 [MotionTokens.PRESS_SCALE] 并以弹簧回弹，
 * 回答「界面听到了」；不出水波纹。弹簧参数（damping 0.5 + StiffnessMedium）即 2.2.0
 * 主按钮/环经真机确认的既有手感，本实现原样收敛三处重复代码并供全部可点击组件共用。
 * 系统减少动态时恒 1f 直显——按压反馈是装饰，不承载任何业务语义。
 */
@Composable
fun rememberPressScaleFraction(
    interactionSource: InteractionSource,
    reduceMotion: Boolean = rememberReduceMotion(),
): Float {
    if (reduceMotion) {
        return 1f
    }
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) MotionTokens.PRESS_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    ).value
}

/**
 * [rememberPressScaleFraction] 的 modifier 形态：放在 `clip`/`background` 之前，
 * 让整个组件（含底色与描边）一起缩放，与 LogButton 的既有排布一致。
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val scale = rememberPressScaleFraction(interactionSource)
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

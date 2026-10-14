package com.awakedw.core.designsystem.animation

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import com.awakedw.core.designsystem.MotionTokens
import com.awakedw.core.designsystem.rememberReduceMotion

/**
 * 全页统一的数字滚动（2.6.0 动效二阶）：hero 与环心的数值变化共用同一拍。
 *
 * 首组合直接落在目标值——冷启动与切页不做「从 0 滚起」的表演，只有打卡、删除等
 * 真实值变化时从旧值滚到新值；系统减少动效时直显。被动显示值用同拍 tween；
 * 进度环弧线作为交互操纵件走弹簧（§19），这一分工见 [MotionTokens] 注释。
 */
@Composable
fun rememberRollingInt(
    targetValue: Int,
    label: String,
    reduceMotion: Boolean = rememberReduceMotion(),
): Int =
    if (reduceMotion) {
        targetValue
    } else {
        animateIntAsState(
            targetValue = targetValue,
            animationSpec = tween(durationMillis = MotionTokens.DURATION_NUMBER_ROLL_MS, easing = MotionTokens.EasingStandard),
            label = label,
        ).value
    }

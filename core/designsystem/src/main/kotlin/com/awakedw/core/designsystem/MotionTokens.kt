package com.awakedw.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * 场景动效 token（alpha13 基线 §13）：把场景系动效的时长与缓动收敛为具名常量。
 * 本轮只沉淀新增场景代码引用的值；存量组件仍用各自局部常量，触碰时逐步迁移。
 */
object MotionTokens {
    /** 时段切换等全局氛围插值的时长：与换肤逐锚点插值（500ms）同拍。 */
    const val DURATION_SCENE_INTERP_MS = 500

    /** 中景层淡入 reveal：与主题主图 reveal（700ms）同拍。 */
    const val DURATION_SCENE_REVEAL_MS = 700

    /** 中景层慢漂移周期：慢到只可感知、不可追踪。 */
    const val SCENE_DRIFT_PERIOD_MS = 90_000L

    /** AGSL 雾光流动周期（0.8.0）：40s 一整循环，无缝闭合。 */
    const val MIST_LOOP_PERIOD_MS = 40_000L

    /** 标准缓动：两端柔和，用于一切氛围插值。 */
    val EasingStandard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

package com.awakedw.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * 场景与氛围动效 token（alpha13 基线 §13）：把场景系动效的时长与缓动收敛为具名常量。
 * 命名分两族：`DURATION_*_MS`（Int，tween 单段时长）与 `*_PERIOD_MS`（Long，无缝循环周期，
 * 换算纳米走 `* 1_000_000L`）。交互反馈类短时长（环心横切、横幅进出、打卡光环等）是各自
 * 规格里逐字裁定的一次性节奏，留在所属组件并随注释维护，不进本表。
 */
object MotionTokens {
    /** 时段切换等全局氛围插值的时长：与换肤逐锚点插值（500ms）同拍。 */
    const val DURATION_SCENE_INTERP_MS = 500

    /** 主题主图与中景层的淡入 reveal（700ms）：新图浮现的一次性节奏。 */
    const val DURATION_SCENE_REVEAL_MS = 700

    /** 主题主图横切时长（0.9.1 收敛）：同主题多张候选图轮换时的交接。 */
    const val DURATION_ARTWORK_CROSSFADE_MS = 900

    /** 呼吸单程时长（0.9.1 收敛）：猫立绘与光袋共用 1.5s 单程，×2 = 3s 完整呼吸周期。 */
    const val DURATION_BREATH_LEG_MS = 1500

    /** 主题主图同主题候选轮换周期：12s 一换，慢到只可感知。 */
    const val ARTWORK_ROTATION_PERIOD_MS = 12_000L

    /** 背景柔光晕呼吸周期：约 8s 一个完整循环，慢到近乎察觉不到。 */
    const val HALO_BREATH_PERIOD_MS = 8_000L

    /** 漂浮粒子上浮循环周期（0.9.1 收敛）：48s 一整循环，无缝闭合。 */
    const val PARTICLE_LOOP_PERIOD_MS = 48_000L

    /** 近景光斑漂移相位循环：120s，与中景层（90s）、粒子（48s）错开节奏。 */
    const val BOKEH_LOOP_PERIOD_MS = 120_000L

    /** 中景层慢漂移周期：慢到只可感知、不可追踪。 */
    const val SCENE_DRIFT_PERIOD_MS = 90_000L

    /** AGSL 雾光流动周期（0.8.0）：40s 一整循环，无缝闭合。 */
    const val MIST_LOOP_PERIOD_MS = 40_000L

    /** 液面波动循环周期（1.8.0 环体液化）：相位整周回绕无缝，慢到只可感知。 */
    const val LIQUID_WAVE_PERIOD_MS = 7_000L

    /** 标准缓动：两端柔和，用于一切氛围插值。 */
    val EasingStandard: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
}

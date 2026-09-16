package com.awakedw.core.designsystem.scene

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.awakedw.core.common.TimeSlots
import com.awakedw.core.designsystem.MotionTokens
import com.awakedw.core.model.TimeSlot
import java.time.LocalTime

/**
 * 场景氛围规格（alpha13 基线 §13）：在 [com.awakedw.core.designsystem.ThemeSpec]
 * 之上按「主题 × 时段」叠加的一层连续演化的装饰强度参数。
 *
 * 只描述装饰氛围，不携带业务语义；减少动态门控的是动画而非取值本身。
 */
data class SceneSpec(
    /** 中景装饰层的可见强度：清晨明亮、正午收敛、夜晚最盛（夜色里装饰即氛围）。 */
    val midgroundAlpha: Float,
    /** 粒子亮度乘子：晨光里粒子更亮，夜里收暗让位给中景。 */
    val particleBoost: Float,
    /**
     * 渐变地平线偏移（-1..1）：清晨光晕偏上（0.34 向上）、正午居中、夜晚沉底——
     * 消费点为 [com.awakedw.core.designsystem.GradientBackdrop] 的光晕与洗染中心。
     */
    val horizonShift: Float,
) {
    init {
        require(midgroundAlpha in 0f..1f) { "midgroundAlpha 应在 0..1：$midgroundAlpha" }
        require(particleBoost in 0.5f..1.5f) { "particleBoost 应在 0.5..1.5：$particleBoost" }
        require(horizonShift in -1f..1f) { "horizonShift 应在 -1..1：$horizonShift" }
    }

    companion object {
        /** 默认昼间值：无时段上下文（预览、测试）时的中性场景。 */
        val DAY = SceneSpec(midgroundAlpha = 0.65f, particleBoost = 1.0f, horizonShift = 0f)
    }
}

/** 按业务时段（与提醒通知同一套 [TimeSlots] 划分）取静态场景锚点。 */
fun sceneSpecOf(slot: TimeSlot): SceneSpec =
    when (slot) {
        TimeSlot.MORNING -> SceneSpec(midgroundAlpha = 0.85f, particleBoost = 1.15f, horizonShift = -0.5f)
        TimeSlot.DAY -> SceneSpec.DAY
        TimeSlot.EVENING -> SceneSpec(midgroundAlpha = 1.0f, particleBoost = 0.85f, horizonShift = 0.5f)
    }

/**
 * 读取当前时段的场景规格并做逐锚点插值：时段切换（极低频）时氛围平滑过渡，
 * 与换肤的 500ms 同拍。装饰组件内部直接调用即可，无需经过 CompositionLocal。
 */
@Composable
fun rememberSceneSpec(): SceneSpec {
    val target = sceneSpecOf(TimeSlots.slotOfHour(LocalTime.now().hour))
    val midground =
        animateFloatAsState(
            targetValue = target.midgroundAlpha,
            animationSpec = tween(durationMillis = MotionTokens.DURATION_SCENE_INTERP_MS, easing = MotionTokens.EasingStandard),
            label = "sceneMidgroundAlpha",
        ).value
    val boost =
        animateFloatAsState(
            targetValue = target.particleBoost,
            animationSpec = tween(durationMillis = MotionTokens.DURATION_SCENE_INTERP_MS, easing = MotionTokens.EasingStandard),
            label = "sceneParticleBoost",
        ).value
    val horizon =
        animateFloatAsState(
            targetValue = target.horizonShift,
            animationSpec = tween(durationMillis = MotionTokens.DURATION_SCENE_INTERP_MS, easing = MotionTokens.EasingStandard),
            label = "sceneHorizonShift",
        ).value
    return SceneSpec(midgroundAlpha = midground, particleBoost = boost, horizonShift = horizon)
}

package com.awakedw.core.designsystem.lolita

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.awakedw.core.designsystem.MotionTokens
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.designsystem.scene.rememberSceneSpec
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 中景层漂移相位循环：progress01 走满一圈恰好回到起点（整数周期闭合）。 */
private const val DRIFT_LOOP_NANOS = MotionTokens.SCENE_DRIFT_PERIOD_MS * 1_000_000L

/**
 * 中景装饰层（alpha13 基线 §13 场景第二层）：透明 PNG 贴图，cover 铺满后做
 * 确定性慢漂移——运动学纯函数（正弦相位），只重绘不重组；减少动态下完全静止。
 *
 * 亮度由三个因子相乘：素材基础不透明度（[baseOpacity]）× 时段氛围
 * （[rememberSceneSpec] 的 midgroundAlpha）× 淡入 reveal。层不进语义树、
 * 不持有任何业务语义；素材缺失时整层静默不绘制（调用方无需回退）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun SceneBackdropLayer(
    asset: String?,
    modifier: Modifier = Modifier,
    baseOpacity: Float = 0.55f,
    seed: Long = 11L,
) {
    if (asset == null) return
    val image = rememberAssetImageOrN(asset)
    val reduceMotion = rememberReduceMotion()
    val scene = rememberSceneSpec()
    val progress = remember { mutableFloatStateOf(0f) }
    if (!reduceMotion) {
        LaunchedEffect(seed) {
            var last = withFrameNanos { it }
            var accumulated = 0L
            while (true) {
                val now = withFrameNanos { it }
                accumulated += now - last
                last = now
                progress.floatValue = (accumulated % DRIFT_LOOP_NANOS).toFloat() / DRIFT_LOOP_NANOS
            }
        }
    }
    val reveal =
        animateFloatAsState(
            targetValue = if (image == null) 0f else 1f,
            animationSpec =
                tween(
                    durationMillis = if (reduceMotion) 0 else MotionTokens.DURATION_SCENE_REVEAL_MS,
                    easing = MotionTokens.EasingStandard,
                ),
            label = "sceneLayerReveal",
        ).value

    Box(
        modifier =
            modifier.drawWithCache {
                val bitmap = image ?: return@drawWithCache onDrawBehind { }
                val scale = maxOf(size.width / bitmap.width, size.height / bitmap.height)
                val dstW = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
                val dstH = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
                onDrawBehind {
                    val p = progress.floatValue
                    val twoPi = (2.0 * kotlin.math.PI).toFloat()
                    val driftX = size.width * 0.012f * sin(twoPi * p + seed * 0.7f)
                    val driftY = size.height * 0.008f * cos(twoPi * p)
                    drawImage(
                        image = bitmap,
                        dstOffset =
                            IntOffset(
                                ((size.width - dstW) / 2f + driftX).roundToInt(),
                                ((size.height - dstH) / 2f + driftY).roundToInt(),
                            ),
                        dstSize = IntSize(dstW, dstH),
                        alpha = reveal * baseOpacity * scene.midgroundAlpha,
                    )
                }
            },
    )
}

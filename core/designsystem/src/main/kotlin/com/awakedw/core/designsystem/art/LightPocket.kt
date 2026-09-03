package com.awakedw.core.designsystem.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberReduceMotion

/** 呼吸单程时长：1.5s（升/降各一段，×2 = 3s 完整呼吸周期——与 [BREATH_LEG_MS] 同裁定「循环=完整周期」）。 */
internal const val POCKET_LEG_MS = 1500

/** 呼吸 alpha 下限（简报逐字 0.06f）：光袋最低存在感，弱到不打扰内容。 */
internal const val POCKET_ALPHA_MIN = 0.06f

/** 呼吸 alpha 上限（简报逐字 0.14f）：亮起也只有一层薄薄的光，不压内容。 */
internal const val POCKET_ALPHA_MAX = 0.14f

/**
 * 光袋呼吸的 alpha 纯函数（简报逐字三点：0f→0.06f、0.5f→0.14f、1f→0.06f）：
 * [phase] ∈ [0,1] 前半程线性升满、后半程线性回落——一次往返即一个完整呼吸周期。
 * 越界相位先收敛到 [0,1]（呼吸到头即折返，不越幅）。
 */
internal fun pocketAlpha(phase: Float): Float {
    val t = phase.coerceIn(0f, 1f)
    // 方向系数 d：前半程 0→1（升）后半程 1→0（降），中点恰为 1f——一次往返即一个完整呼吸周期。
    val d = if (t <= 0.5f) t * 2f else (1f - t) * 2f
    // 凸组合写法保证简报三点逐字精确（d=0 恰 0.06f、d=1 恰 0.14f；MIN+rise 的加减式会差一个 ulp）。
    return POCKET_ALPHA_MIN * (1f - d) + POCKET_ALPHA_MAX * d
}

/**
 * 光袋（moodboard §2 光·遇手法）：可交互元素旁的呼吸光晕。
 *
 * - radial gradient 光晕自中心铺开、边缘透明（绘制走 [drawBehind]，永远在后续内容之下，
 *   光晕不压内容；尺寸语义：光晕铺满 [modifier] 尺寸，由调用方控制大小（96–160dp））；
 * - 呼吸走 [rememberInfiniteTransition]：phase 0→1 线性走完 1.5s 升 + 1.5s 降 = 3s 完整周期
 *   （与 Task 9 CatFigure 的「循环=完整周期」读法一致），alpha 经 [pocketAlpha] 0.06→0.14 往返；
 *   LinearEasing 保证线性往返不被缓动曲线扭曲——是呼吸不是闪烁（治愈铁律：频率克制、无高频脉动）；
 * - 默认色取当前主题 haloColor（深色主题色板已按暗底适配，无需特判）；
 *   光袋是 halo 既有语义的延伸，不新增装饰种类（装饰纪律）。
 *
 * 用法：置于目标元素的 Box 底层（先 [LightPocket] 后内容），如「记一杯」按钮与胆大王立绘后方。
 *
 * @param modifier 光晕的尺寸与布局（铺满该尺寸画 radial gradient）。
 * @param color 光晕颜色，默认当前主题 [com.awakedw.core.designsystem.ThemeSpec.haloColor]。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun LightPocket(
    modifier: Modifier,
    color: Color = currentThemeSpec().haloColor,
) {
    val reduceMotion = rememberReduceMotion()
    val phase =
        if (reduceMotion) {
            0f
        } else {
            val transition = rememberInfiniteTransition(label = "lightPocket")
            val animatedPhase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = POCKET_LEG_MS * 2, easing = LinearEasing),
                    ),
                label = "lightPocketPhase",
            )
            animatedPhase
        }
    Box(
        modifier =
            modifier
                .drawBehind {
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(color.copy(alpha = pocketAlpha(phase)), Color.Transparent),
                                center = center,
                                radius = size.minDimension / 2f,
                            ),
                    )
                },
    )
}

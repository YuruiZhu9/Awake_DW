package com.awakedw.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.particles.FloatingParticles

/** 庆祝态横幅淡入时长。 */
private const val BANNER_FADE_IN_MS = 320

/** 庆祝态收敛淡出时长。 */
private const val BANNER_FADE_OUT_MS = 420

/** 粒子雨三层种子（密度 ×3）：FloatingParticles 组合内零随机，种子错开即三层不同分布。 */
private val RAIN_SEEDS = listOf(11L, 23L, 47L)

/**
 * 达成日目标的庆祝态（规格 §4.2 第 6 步）：
 * 满屏粒子雨（三层 FloatingParticles 纵向翻转为下落，密度 ×3）+
 * 「今日份水灵达成 ✨」横幅；收敛时机由 ViewModel 的 2500ms 时序驱动。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun CelebrationOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = BANNER_FADE_IN_MS)),
        exit = fadeOut(tween(durationMillis = BANNER_FADE_OUT_MS)),
        modifier = modifier,
    ) {
        val spec = currentThemeSpec()
        Box(modifier = Modifier.fillMaxSize()) {
            RAIN_SEEDS.forEach { seed ->
                FloatingParticles(
                    colors = spec.particleColors,
                    // 纵向翻转：原点上浮的粒子场即成下落的粒子雨。
                    modifier =
                        Modifier
                            .matchParentSize()
                            .graphicsLayer { scaleY = -1f },
                    seed = seed,
                )
            }
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = spec.chipBg,
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = "今日份水灵达成 ✨",
                    color = spec.chipText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 26.dp, vertical = 14.dp),
                )
            }
        }
    }
}

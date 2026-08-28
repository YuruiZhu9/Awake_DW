package com.awakedw.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.ring.ProgressRing
import com.awakedw.feature.home.components.BadgesRow
import com.awakedw.feature.home.components.CelebrationOverlay
import com.awakedw.feature.home.components.Greeting
import com.awakedw.feature.home.components.HealthTipLine
import com.awakedw.feature.home.components.LogButton
import com.awakedw.feature.home.components.PraiseLine

/** 首页进度环直径：开屏形序段（SplashMorph）以它为涟漪终态半径，改值需与开屏同步观感。 */
val HOME_RING_DIAMETER = 220.dp

/** 环心数字滚动时长（规格 §4.2 第 3 步：~500ms）。 */
private const val NUMBER_ROLL_MS = 500

/** 达标微光呼吸的 alpha 区间与单程时长。 */
private const val GLOW_ALPHA_MIN = 0.10f
private const val GLOW_ALPHA_MAX = 0.26f
private const val GLOW_BREATH_MS = 1600

/**
 * 治愈打卡首页（规格 §3.2 自上而下：问候 → 进度环 → 统计徽章 → 健康贴士 → 「记一杯」按钮）：
 * 可点按进度环居中承重，夸夸语在环下方浮现（§4.2 第 5 步），达标后满环微光呼吸；
 * 底座为渐变背景 + 漂浮粒子（GradientBackdrop 自带柔光晕与噪点颗粒）。
 * 打卡反馈 6 步时序由 [HomeViewModel] 与本层协同完成（规格 §4.2）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val spec = currentThemeSpec()

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        FloatingParticles(colors = spec.particleColors, modifier = Modifier.matchParentSize())

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(44.dp))
            Greeting(customGreeting = state.greeting, totalMl = state.totalMl, goalMl = state.goalMl)
            Spacer(Modifier.height(20.dp))
            RingBlock(
                progress = state.progress,
                totalMl = state.totalMl,
                onRingTap = viewModel::tapRing,
            )
            Spacer(Modifier.height(12.dp))
            PraiseLine(text = state.praiseLine)
            Spacer(Modifier.height(20.dp))
            BadgesRow(cupCount = state.cupCount, avgIntervalLabel = state.avgIntervalLabel)
            Spacer(Modifier.height(12.dp))
            HealthTipLine()
            Spacer(Modifier.weight(1f))
            LogButton(themeId = state.themeId, onTap = viewModel::tapLogButton)
            Spacer(Modifier.height(36.dp))
        }

        CelebrationOverlay(visible = state.celebrating, modifier = Modifier.matchParentSize())
    }
}

/** 进度环区块：达标后满环微光呼吸 + 可点按环体 + 环心数字滚动。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun RingBlock(
    progress: Float,
    totalMl: Int,
    onRingTap: (Offset?) -> Unit,
) {
    var ringCenter by remember { mutableStateOf<Offset?>(null) }

    Box(contentAlignment = Alignment.Center) {
        if (progress >= 1f) {
            BreathingGlow()
        }
        ProgressRing(
            progress = progress,
            modifier =
                Modifier
                    .size(HOME_RING_DIAMETER)
                    .onGloballyPositioned { coordinates ->
                        ringCenter = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                    },
            onRingTap = { onRingTap(ringCenter) },
        ) {
            RingCenterContent(totalMl = totalMl)
        }
    }
}

/** 环心：滚动到新值的总量 + 「今日已喝」小字（规格 §3.2 第 2 条）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun RingCenterContent(totalMl: Int) {
    val spec = currentThemeSpec()
    val rolledTotal by animateIntAsState(
        targetValue = totalMl,
        animationSpec = tween(durationMillis = NUMBER_ROLL_MS, easing = FastOutSlowInEasing),
        label = "ringTotalMl",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${rolledTotal}ml",
            color = spec.ringValueText,
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "今日已喝",
            color = spec.ringValueText.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** 满环微光呼吸（规格 §4.2 第 6 步「满环微光呼吸」）：柔光晕在环后缓缓起伏。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun BreathingGlow() {
    val spec = currentThemeSpec()
    val transition = rememberInfiniteTransition(label = "goalGlow")
    val glowAlpha by transition.animateFloat(
        initialValue = GLOW_ALPHA_MIN,
        targetValue = GLOW_ALPHA_MAX,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = GLOW_BREATH_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "goalGlowAlpha",
    )
    Box(
        modifier =
            Modifier
                .size(HOME_RING_DIAMETER)
                .drawBehind {
                    drawCircle(color = spec.haloColor.copy(alpha = glowAlpha), radius = size.minDimension / 2f)
                },
    )
}

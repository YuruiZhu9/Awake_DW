package com.awakedw.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awakedw.core.designsystem.LocalAwakeTheme
import com.awakedw.core.designsystem.ThemeById
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.model.ThemeId

/** 换肤过渡时长（规格 §2.3：0.5s 平滑换肤）。 */
private const val THEME_TRANSITION_MS = 500

/**
 * 0.5s 平滑换肤包装（规格 §2.3）：[ThemeSpec] 的每个颜色锚点经
 * `animateColorAsState(tween(500))` 插值后再写入 [LocalAwakeTheme]，
 * 背景渐变逐停靠点、粒子色族随行插值，换肤全程无硬切。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun AnimatedAwakeTheme(
    themeId: ThemeId,
    content: @Composable () -> Unit,
) {
    val target = ThemeById.getValue(themeId)
    val spec =
        ThemeSpec(
            id = target.id,
            backgroundGradient = target.backgroundGradient.animateEach("awakeBgStop"),
            particleColors = target.particleColors.animateEach("awakeParticle"),
            primary = target.primary.animateAnchor("awakePrimary"),
            ringTrack = target.ringTrack.animateAnchor("awakeRingTrack"),
            ringValueText = target.ringValueText.animateAnchor("awakeRingValueText"),
            greetingColor = target.greetingColor.animateAnchor("awakeGreeting"),
            greetingSubColor = target.greetingSubColor.animateAnchor("awakeGreetingSub"),
            buttonTop = target.buttonTop.animateAnchor("awakeButtonTop"),
            buttonBottom = target.buttonBottom.animateAnchor("awakeButtonBottom"),
            chipBg = target.chipBg.animateAnchor("awakeChipBg"),
            chipText = target.chipText.animateAnchor("awakeChipText"),
            haloColor = target.haloColor.animateAnchor("awakeHalo"),
        )
    CompositionLocalProvider(LocalAwakeTheme provides spec) {
        content()
    }
}

/** 色族逐位插值：停靠点数以目标主题为准，新增位从目标色起步（无历史可插）。 */
@Composable
private fun List<Color>.animateEach(label: String): List<Color> = List(size) { index -> get(index).animateAnchor("$label$index") }

@Composable
private fun Color.animateAnchor(label: String): Color =
    animateColorAsState(
        targetValue = this,
        animationSpec = tween(durationMillis = THEME_TRANSITION_MS),
        label = label,
    ).value

/**
 * 应用根组合：主题解析流 → 平滑换肤 → 导航壳。
 *
 * [onEntryReady] 在组合挂载后回调一次，用于释放系统闪屏驻留
 * （此后由 Compose 内的 [SplashMorph] 续场接管）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun AwakeApp(onEntryReady: () -> Unit = {}) {
    val viewModel: MainViewModel = viewModel()
    val themeId by viewModel.themeId.collectAsState()

    SideEffect(onEntryReady)

    AnimatedAwakeTheme(themeId = themeId) {
        AwakeNavHost()
    }
}

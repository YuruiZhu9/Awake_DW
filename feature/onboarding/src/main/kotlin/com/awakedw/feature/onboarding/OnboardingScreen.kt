package com.awakedw.feature.onboarding

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.GOLD_TRIM
import com.awakedw.core.designsystem.lolita.drawBow
import com.awakedw.core.designsystem.onPrimarySurface
import com.awakedw.core.designsystem.particles.FloatingParticles

/** 大水滴插画尺寸：宽为底部圆的直径，高约 1.35 倍留出顶部尖端。 */
private val DROPLET_SIZE = DpSize(170.dp, 230.dp)

/**
 * 白名单引导页（首次启动，无底栏——由导航壳/集成任务接线）：
 * 当刻主题的渐变底 + 漂浮粒子 + 大水滴插画，温柔地建议开启省电白名单。
 *
 * 主按钮「去设置 ♡」权限时序化（T13 minor 收口，杜绝对话框与跳转 Activity 竞态）：
 * Android 13+ 且未授予通知权限时先弹系统权限对话框，**拿到 result 后（无论授权与否）
 * 再**进入 [BatteryIntentLauncher.bestEffortIntents] 逐个跳转序列，首个成功即置位
 * onboarding_done 并经 [onComplete] 接缝离页；已授予或低版本则直接进入跳转序列。
 * 次按钮「以后再说」：直接置位完成。跳转全部失败时留在本页，可重试可跳过。
 * 授权被拒不阻拦：通知权限是锦上添花，白名单引导照常走完。
 *
 * [onComplete] 为导航接缝（默认空实现），由集成任务接上「返回首页」；本层不感知 NavHost。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val spec = currentThemeSpec()
    val context = LocalContext.current

    // 通知权限 result 落点：授权与否都放行进入白名单跳转序列。
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            startWhitelistJump(context, viewModel)
        }

    // 完成接缝：VM 置位 completed 后离开本页（导航由集成任务接线）。
    LaunchedEffect(state.completed) {
        if (state.completed) onComplete()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        FloatingParticles(colors = spec.particleColors, modifier = Modifier.matchParentSize())

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.8f))
            DropletIllustration()
            Spacer(Modifier.height(28.dp))
            Text(
                text = "为了让每一次温柔准时抵达",
                color = spec.greetingColor,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "建议把 Awake_DW 加入电池优化白名单，提醒会更可靠。也可以先跳过。",
                color = spec.greetingSubColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = "去设置 ♡",
                onTap = {
                    if (needsNotificationPermission(context)) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        startWhitelistJump(context, viewModel)
                    }
                },
            )
            Spacer(Modifier.height(14.dp))
            SkipButton(onTap = viewModel::complete)
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** 33+ 且未授予通知权限时需先弹系统对话框；其余版本/已授予直接进入跳转序列。 */
private fun needsNotificationPermission(context: Context): Boolean =
    BatteryIntentLauncher.neededNotificationPermissions(context).isNotEmpty()

/** 白名单跳转序列：逐个尝试入口 → 首个成功交 VM 收口完成引导；失败留在本页可重试。 */
private fun startWhitelistJump(
    context: Context,
    viewModel: OnboardingViewModel,
) {
    val success =
        BatteryIntentLauncher.tryStartInOrder(
            context,
            BatteryIntentLauncher.bestEffortIntents(context),
        )
    viewModel.onWhitelistJumpResult(success)
}

/** 大水滴插画：柔光晕 + 主题渐变水滴（尖端向上）+ 一点高光，整滴极缓呼吸。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun DropletIllustration(modifier: Modifier = Modifier) {
    val spec = currentThemeSpec()
    val breath by
        rememberInfiniteTransition(label = "dropletBreath").animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "dropletBreathScale",
        )

    Canvas(
        modifier =
            modifier
                .size(DROPLET_SIZE)
                .graphicsLayer {
                    scaleX = breath
                    scaleY = breath
                },
    ) {
        drawDroplet(spec)
        // 水滴系蝴蝶结（§12 L2）：系在滴肩一侧，温柔但不抢主标识。
        drawBow(
            center = Offset(size.width * 0.68f, size.height * 0.22f),
            width = size.width * 0.34f,
            color = spec.primary,
            knotColor = GOLD_TRIM,
        )
    }
}

/** 水滴绘制：光晕垫底 → 渐变滴身 → 高光椭圆（比例锚定，任意尺寸不变形）。 */
private fun DrawScope.drawDroplet(spec: ThemeSpec) {
    val glowRadius = size.minDimension * 0.85f
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(spec.haloColor.copy(alpha = 0.32f), Color.Transparent), radius = glowRadius),
        radius = glowRadius,
        center = Offset(size.width / 2f, size.height / 2f),
    )
    drawPath(path = dropletPath(size), brush = Brush.verticalGradient(listOf(spec.buttonTop, spec.buttonBottom)))
    drawOval(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(size.width * 0.30f, size.height * 0.60f),
        size = Size(size.width * 0.16f, size.height * 0.09f),
    )
}

/** 大水滴轮廓：顶部尖端 → 两侧三次曲线 → 底部半圆（宽 [size].width，总高 [size].height）。 */
private fun dropletPath(size: Size): Path {
    val w = size.width
    val h = size.height
    val r = w / 2f
    val circleCenterY = h - r
    return Path().apply {
        moveTo(w / 2f, 0f)
        cubicTo(w * 0.70f, h * 0.20f, w, circleCenterY - r, w, circleCenterY)
        arcTo(
            rect = Rect(left = 0f, top = circleCenterY - r, right = w, bottom = circleCenterY + r),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false,
        )
        cubicTo(0f, circleCenterY - r, w * 0.30f, h * 0.20f, w / 2f, 0f)
        close()
    }
}

/** 主按钮：主题渐变胶囊，与首页「记一杯」同一观感语言（含按压缩放回弹）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun PrimaryButton(
    text: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "onboardingPressScale",
    )
    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(RoundedCornerShape(percent = 50))
                .background(Brush.verticalGradient(listOf(spec.buttonTop, spec.buttonBottom)))
                .clickable(interactionSource = interactionSource, indication = null, onClick = onTap)
                .padding(horizontal = 46.dp, vertical = 17.dp),
    ) {
        // 主按钮文字对比（布局审计 P2-4 同构病灶）：浅色主题主色渐变偏亮，白字改走 onPrimarySurface 深暖字色。
        Text(text = text, color = onPrimarySurface(spec), style = MaterialTheme.typography.titleMedium)
    }
}

/** 次按钮：低扰动的文字按钮——「以后再说」同样置位完成，不阻拦任何人。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun SkipButton(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Text(
        text = "以后再说",
        color = spec.greetingSubColor,
        style = MaterialTheme.typography.labelLarge,
        modifier =
            modifier
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = onTap)
                .padding(horizontal = 24.dp, vertical = 10.dp),
    )
}

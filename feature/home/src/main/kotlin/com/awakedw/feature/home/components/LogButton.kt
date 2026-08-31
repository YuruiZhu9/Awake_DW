package com.awakedw.feature.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.burst.BurstParticles
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.model.ThemeId

/** 按压缩放比例：与进度环一致，轻按即有回弹（规格 §4.2 第 1 步）。 */
private const val PRESS_SCALE = 0.97f

/** 主按钮文案随主题变化（规格 §3.2 第 5 条）。 */
internal fun buttonLabel(themeId: ThemeId): String =
    when (themeId) {
        ThemeId.EMERALD -> "干杯一下 💧"
        ThemeId.STRAWBERRY -> "喝一杯啦 ♡"
        ThemeId.CARAMEL -> "来一口温暖"
        ThemeId.NIGHT -> "轻轻抿一口 🌙"
    }

/**
 * 「记一杯」渐变胶囊大按钮（规格 §3.2 第 5 条 + §4.2 反馈编排）：
 * 主题渐变底、按压回弹，点击即打卡并从按钮中心迸出 10 颗主题色粒子（~900ms）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun LogButton(
    themeId: ThemeId,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) PRESS_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "logButtonPressScale",
    )

    var size by remember { mutableStateOf(IntSize.Zero) }
    var burstTrigger by remember { mutableIntStateOf(0) }
    val view = LocalView.current

    // 外层 Box 既是迸发粒子的坐标空间（origin=按钮中心），也保证粒子可飞出按钮边界。
    Box(
        modifier =
            modifier
                .onSizeChanged { size = it }
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Brush.verticalGradient(listOf(spec.buttonTop, spec.buttonBottom)))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        burstTrigger += 1
                        onTap()
                    }
                    .padding(horizontal = 46.dp, vertical = 17.dp),
        ) {
            Text(
                text = buttonLabel(themeId),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        BurstParticles(
            origin = Offset(size.width / 2f, size.height / 2f),
            colors = spec.particleColors,
            trigger = burstTrigger,
            onFinish = { },
        )
    }
}

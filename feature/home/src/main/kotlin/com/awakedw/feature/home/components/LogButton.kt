package com.awakedw.feature.home.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.burst.BurstParticles
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.onPrimarySurface
import com.awakedw.core.designsystem.rememberReduceMotion

/** Button label is deliberately stable: the primary action always means one thing. */
internal const val LOG_BUTTON_LABEL = "记一杯"

/** Press scale for the primary action. */
private const val PRESS_SCALE = 0.97f

/** 印章外形（1.1.0 信纸化）：比方胶囊更方，像盖在纸上的一枚墨印。 */
private val STAMP_SHAPE = RoundedCornerShape(10.dp)

/**
 * Primary water logging action（1.1.0 印章化）: a deep ink stamp on the letter sheet —
 * gradient block, inner hairline frame, letterset label, pressed feedback, and a
 * restrained burst that confirms the tap without adding a dialog.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun LogButton(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    cupMl: Int? = null,
) {
    val spec = currentThemeSpec()
    val reduceMotion = rememberReduceMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale =
        if (reduceMotion) {
            1f
        } else {
            animateFloatAsState(
                targetValue = if (pressed) PRESS_SCALE else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "logButtonPressScale",
            ).value
        }

    var size by remember { mutableStateOf(IntSize.Zero) }
    var burstTrigger by remember { mutableIntStateOf(0) }
    val view = LocalView.current

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
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .clip(STAMP_SHAPE)
                    .background(Brush.verticalGradient(listOf(spec.buttonTop, spec.buttonBottom)))
                    .border(1.dp, Color.White.copy(alpha = if (spec.isDark) 0.24f else 0.34f), STAMP_SHAPE)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        burstTrigger += 1
                        onTap()
                    },
        ) {
            // 内嵌细框（信纸化 1.1.0）：印章的阳文边——内容与外缘之间的第二道线。
            Box(
                Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .border(0.8.dp, onPrimarySurface(spec).copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .background(onPrimarySurface(spec).copy(alpha = 0.14f), RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = onPrimarySurface(spec),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = LOG_BUTTON_LABEL,
                    color = onPrimarySurface(spec),
                    // 印章铭文（1.1.0）：加字距、半粗——「记一杯」读起来像印在纸上的三个字。
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 9.dp),
                )
                if (cupMl != null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "+${cupMl}ml",
                        color = onPrimarySurface(spec),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
        BurstParticles(
            origin = Offset(size.width / 2f, size.height / 2f),
            colors = spec.particleColors,
            trigger = burstTrigger,
            onFinish = { },
        )
    }
}

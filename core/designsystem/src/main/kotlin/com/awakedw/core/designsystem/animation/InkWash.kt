package com.awakedw.core.designsystem.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import com.awakedw.core.designsystem.currentThemeSpec
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

/** 墨水浸润的峰值透明度：一层薄墨，压得住又读得清。 */
private const val INK_PEAK_ALPHA = 0.12f

/** 按住时墨晕开的时长：比涟漪慢半拍，像墨在纸上洇。 */
private const val INK_SPREAD_MS = 180

/** 松手后收干的时长。 */
private const val INK_DRY_MS = 260

/**
 * 墨水浸润按压反馈（1.3.0 全局墨水语言）：按住时一团主题色墨在组件内晕开、
 * 松手缓缓收干——替代 M3 默认涟漪，与信纸/印章的纸墨语言同族。
 *
 * 只用于**自有胶囊/卡片级**可点组件；行级组件与 M3 组件（TextButton、底栏）保留默认反馈。
 * 用法与 `clickable(interactionSource = …, indication = null)` 配对，先 `clip(shape)` 再挂本修饰符
 * （墨被裁进形状内）：
 * ```
 * Box(Modifier.clip(shape).inkWash(interactionSource).clickable(interactionSource, indication = null) { … })
 * ```
 */
@Composable
fun Modifier.inkWash(
    interactionSource: InteractionSource,
    color: Color = currentThemeSpec().primary,
): Modifier = this.then(InkWashElement(interactionSource, color))

/** [inkWash] 的元素载体：主题色变化经 [update] 热替换，不重建节点。 */
private class InkWashElement(
    private val interactionSource: InteractionSource,
    private val color: Color,
) : ModifierNodeElement<InkWashNode>() {
    override fun create(): InkWashNode = InkWashNode(interactionSource, color)

    override fun update(node: InkWashNode) {
        node.color = color
        node.bind(interactionSource)
    }

    override fun equals(other: Any?): Boolean =
        other is InkWashElement && other.interactionSource == interactionSource && other.color == color

    override fun hashCode(): Int = interactionSource.hashCode() * 31 + color.hashCode()
}

/** 墨水节点：交互流驱动一枚浮点进度，draw 相只读进度（重绘不重组）。 */
private class InkWashNode(
    private var interactionSource: InteractionSource,
    internal var color: Color,
) : Modifier.Node(), DrawModifierNode {
    private var boundSource: InteractionSource? = null
    private var pressed by mutableStateOf(false)

    /** 墨量 0..1：0 干纸，1 墨满。只进 draw 相。 */
    private var ink by mutableFloatStateOf(0f)

    /** 交互源在 update 里可能热替换：只绑一次当前源。 */
    fun bind(source: InteractionSource) {
        if (boundSource == source) return
        boundSource = source
        if (isAttached) {
            observePresses()
        }
    }

    override fun onAttach() {
        observePresses()
        coroutineScope.launch {
            snapshotFlow { pressed }
                .collectLatest { isDown ->
                    if (isDown) {
                        animate(ink, 1f, animationSpec = tween(INK_SPREAD_MS, easing = LinearEasing)) { value, _ ->
                            ink = value
                        }
                    } else {
                        animate(ink, 0f, animationSpec = tween(INK_DRY_MS)) { value, _ -> ink = value }
                    }
                }
        }
    }

    private fun observePresses() {
        coroutineScope.launch {
            boundSource?.interactions?.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressed = true
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = INK_PEAK_ALPHA * ink
        if (alpha > 0.005f) {
            // 墨从中心晕开：按得越久铺得越满。
            val spread = 0.62f + 0.38f * ink
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = max(size.width, size.height) / 2f * spread,
                center = center,
            )
        }
    }
}

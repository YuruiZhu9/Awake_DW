package com.awakedw.core.designsystem.art

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.drawBow
import com.awakedw.core.model.CatAccessory
import com.awakedw.core.model.CatMood
import kotlin.math.min
import kotlin.math.roundToInt

/** 立绘语义描述：测试与无障碍定位胆大王的锚点。 */
internal const val CAT_SEMANTICS = "胆大王"

/** 立绘固定边长（96dp 见方，尺寸语义：常驻首页一角）。 */
private const val CAT_FIGURE_SIZE_DP = 96

/** 呼吸单程时长：3s（1.00→峰值 3s + 峰值→1.00 3s，Reverse 循环）。 */
private const val BREATH_LEG_MS = 3000

/** SLEEPY 轻微低饱和保留系数（0.85f：安睡的柔和感，不降透明度不灰暗——治愈铁律）。 */
private const val SLEEPY_SATURATION = 0.85f

/**
 * 呼吸缩放峰值：常规 1.02f；SLEEPY 幅度减半 → 1.01f（简报逐字参数）。
 */
internal fun breathTargetOf(mood: CatMood): Float = if (mood == CatMood.SLEEPY) 1.01f else 1.02f

/**
 * 配饰叠绘锚点比例（简报逐字参数，相对 96dp 方框高度）：
 * bow=头顶 0.18h、pearl=颈 0.52h、dress=身 0.72h。资产叠绘与矢量配饰共用，保证两路径同位。
 */
internal fun accessoryAnchorY(accessory: CatAccessory): Float =
    when (accessory) {
        CatAccessory.BOW -> 0.18f
        CatAccessory.PEARL -> 0.52f
        CatAccessory.OUTFIT -> 0.72f
    }

/** 三态立绘资产路径（:app assets 相对路径；缺失由 [rememberAssetImageOrN] 回退 null）。 */
internal fun catAssetFileOf(mood: CatMood): String =
    when (mood) {
        CatMood.IDLE -> "cat/idle.webp"
        CatMood.HAPPY -> "cat/happy.webp"
        CatMood.SLEEPY -> "cat/sleepy.webp"
    }

/** 配饰叠绘宽度档（相对方框宽）：按各配饰图原比例等比缩放到该宽度。 */
private val OVERLAY_WIDTH_FRACTIONS =
    mapOf(
        CatAccessory.BOW to 0.40f,
        CatAccessory.PEARL to 0.42f,
        CatAccessory.OUTFIT to 0.58f,
    )

/**
 * 胆大王（moodboard §6）：96dp 见方常驻首页一角。
 * 有资产用图（idle/happy/sleepy 三态 + 配饰 overlay 叠绘），
 * 无资产画内置矢量简笔猫（Canvas：圆头+三角耳+卷尾曲线，主题色调用）——体验先行，资产后补即生效。
 * 微动效常驻：呼吸缩放 1.00→1.02（3s 循环，SLEEPY 减半）；HAPPY 一次 spring 弹跳。
 *
 * 双路径规则：
 * - 立绘资产（[catAssetFileOf]）存在 → 整幅用图；已解锁配饰的资产也存在时按 [accessoryAnchorY]
 *   锚点叠绘（图缺失不画，资产后补即生效，不做矢量与位图混拼）；
 * - 立绘资产缺失 → Canvas 矢量简笔猫 + 全矢量配饰（BOW 复用 [drawBow]，PEARL 珍珠串，OUTFIT 钟形裙）。
 *
 * 微动效：
 * - 呼吸缩放 [breathTargetOf]（3s 单程 Reverse 循环；mood 变化时 [key] 重启换档）；
 * - HAPPY 一次 spring 弹跳（[Animatable] 蹲 0.92 → MediumBouncy 弹回 1.00，mood 变 HAPPY 触发一次，
 *   离开 HAPPY 复位，重组不重放）；
 * - SLEEPY 绘制层 0.85f 低饱和（[SLEEPY_SATURATION]，仅柔和降饱和、不降透明度不灰暗）。
 *
 * 点击任意处触发 [onPet]（[detectTapGestures]，纯手势层，不进语义 click）。
 * 治愈铁律：视觉零惩罚——SLEEPY 是安睡不是消极。
 *
 * @param mood 心情三态，由调用方传入（本组件不感知时间）。
 * @param accessories 已解锁配饰（通常为 [com.awakedw.core.model.unlockedCatAccessories] 的结果）。
 * @param modifier 外部布局修饰（对齐/边距等）；96dp 见方由本组件内部固定。
 * @param onPet 摸猫回调（点击立绘触发）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun CatFigure(
    mood: CatMood,
    accessories: List<CatAccessory>,
    modifier: Modifier = Modifier,
    onPet: () -> Unit = {},
) {
    val theme = currentThemeSpec()
    val bodyImage = rememberAssetImageOrN(catAssetFileOf(mood))
    val accessoryImages: List<Pair<CatAccessory, ImageBitmap?>> =
        accessories.map { accessory -> accessory to rememberAssetImageOrN(accessory.assetFile) }

    // 呼吸缩放：3s 单程 Reverse 循环；SLEEPY 幅度减半。key(mood) 换档即重启（从 1.00 起步，跳变 ≤2% 不可感）。
    val breathState: State<Float> =
        key(mood) {
            rememberInfiniteTransition(label = "CatBreath").animateFloat(
                initialValue = 1f,
                targetValue = breathTargetOf(mood),
                animationSpec = infiniteRepeatable(tween(durationMillis = BREATH_LEG_MS), RepeatMode.Reverse),
                label = "CatBreathScale",
            )
        }
    val breathScale by breathState

    // HAPPY 一次 spring 弹跳：蹲 0.92 蓄力后以 MediumBouncy 弹回 1.00（自然过冲回摆）；离开 HAPPY 复位。
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(mood) {
        if (mood == CatMood.HAPPY) {
            bounce.snapTo(0.92f)
            bounce.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            )
        } else {
            bounce.snapTo(1f)
        }
    }

    Box(
        modifier =
            modifier
                .size(CAT_FIGURE_SIZE_DP.dp)
                .graphicsLayer {
                    scaleX = breathScale * bounce.value
                    scaleY = breathScale * bounce.value
                }
                .drawBehind {
                    // SLEEPY 轻微低饱和（0.85f）：安睡的柔和感，不降透明度不灰暗——治愈铁律。
                    val sleepyFilter =
                        if (mood == CatMood.SLEEPY) {
                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(SLEEPY_SATURATION) })
                        } else {
                            null
                        }
                    val body = bodyImage
                    if (body != null) {
                        drawFitted(body, sleepyFilter)
                        for ((accessory, image) in accessoryImages) {
                            if (image != null) drawAccessoryOverlay(accessory, image, sleepyFilter)
                        }
                    } else {
                        drawVectorCat(mood, theme, sleepyFilter)
                        for ((accessory, _) in accessoryImages) drawVectorAccessory(accessory, theme)
                    }
                }
                .semantics { contentDescription = CAT_SEMANTICS }
                .pointerInput(Unit) { detectTapGestures { onPet() } },
    )
}

/** 等比适配铺绘（ContentScale.Fit 语义）：96dp 方框内居中、不变形、不裁切。 */
private fun DrawScope.drawFitted(
    image: ImageBitmap,
    colorFilter: ColorFilter?,
) {
    val scale = min(size.width / image.width, size.height / image.height)
    val dstWidth = (image.width * scale).roundToInt().coerceAtLeast(1)
    val dstHeight = (image.height * scale).roundToInt().coerceAtLeast(1)
    drawImage(
        image = image,
        dstOffset =
            IntOffset(
                ((size.width - dstWidth) / 2f).roundToInt(),
                ((size.height - dstHeight) / 2f).roundToInt(),
            ),
        dstSize = IntSize(dstWidth, dstHeight),
        colorFilter = colorFilter,
    )
}

/** 配饰资产叠绘：中心锚定在 [accessoryAnchorY] 比例位，按原比例缩放到 [OVERLAY_WIDTH_FRACTIONS] 宽度档。 */
private fun DrawScope.drawAccessoryOverlay(
    accessory: CatAccessory,
    image: ImageBitmap,
    colorFilter: ColorFilter?,
) {
    val targetWidth = size.width * OVERLAY_WIDTH_FRACTIONS.getValue(accessory)
    val dstWidth = targetWidth.roundToInt().coerceAtLeast(1)
    val dstHeight = (image.height * targetWidth / image.width).roundToInt().coerceAtLeast(1)
    val centerY = size.height * accessoryAnchorY(accessory)
    drawImage(
        image = image,
        dstOffset =
            IntOffset(
                ((size.width - dstWidth) / 2f).roundToInt(),
                (centerY - dstHeight / 2f).roundToInt(),
            ),
        dstSize = IntSize(dstWidth, dstHeight),
        colorFilter = colorFilter,
    )
}

/**
 * 矢量简笔猫（资产缺失兜底，主题色调用）：卷尾曲线 → 身体椭圆 → 双耳三角 → 圆头 → 表情 → 胡须。
 * 眼睛区分三态：IDLE 圆点 / HAPPY 弯月笑眼（拱向上）/ SLEEPY 安睡闭眼（拱向下）——安睡不是消极。
 */
private fun DrawScope.drawVectorCat(
    mood: CatMood,
    theme: ThemeSpec,
    colorFilter: ColorFilter?,
) {
    val w = size.width
    val h = size.height
    val fur = theme.primary
    val ink = theme.chipText
    val eye = theme.ringTrack

    // 卷尾：身体右侧甩出向上卷回（圆头笔触）。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.68f, h * 0.70f)
                cubicTo(w * 0.94f, h * 0.62f, w * 0.96f, h * 0.36f, w * 0.80f, h * 0.40f)
                cubicTo(w * 0.68f, h * 0.42f, w * 0.70f, h * 0.54f, w * 0.78f, h * 0.54f)
            },
        color = fur,
        style = Stroke(width = w * 0.055f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // 身体椭圆（趴姿）。
    drawOval(color = fur, topLeft = Offset(w * 0.24f, h * 0.56f), size = Size(w * 0.52f, h * 0.30f), colorFilter = colorFilter)

    // 双耳三角。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.32f, h * 0.30f)
                lineTo(w * 0.27f, h * 0.11f)
                lineTo(w * 0.47f, h * 0.21f)
                close()
            },
        color = fur,
        colorFilter = colorFilter,
    )
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.68f, h * 0.30f)
                lineTo(w * 0.73f, h * 0.11f)
                lineTo(w * 0.53f, h * 0.21f)
                close()
            },
        color = fur,
        colorFilter = colorFilter,
    )

    // 圆头（盖住耳根与身体上缘）。
    drawCircle(color = fur, radius = w * 0.21f, center = Offset(w * 0.50f, h * 0.38f), colorFilter = colorFilter)

    // 表情：IDLE 圆点 / HAPPY 笑眼（上拱）/ SLEEPY 闭眼（下拱）。
    val eyeRadius = w * 0.045f
    val eyeHalfWidth = w * 0.08f
    when (mood) {
        CatMood.IDLE -> {
            drawCircle(color = eye, radius = w * 0.018f, center = Offset(w * 0.42f, h * 0.40f), colorFilter = colorFilter)
            drawCircle(color = eye, radius = w * 0.018f, center = Offset(w * 0.58f, h * 0.40f), colorFilter = colorFilter)
        }
        CatMood.HAPPY -> {
            drawArc(
                color = eye,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.42f - eyeHalfWidth, h * 0.40f - eyeRadius),
                size = Size(eyeHalfWidth * 2f, eyeRadius * 2f),
                style = Stroke(width = w * 0.012f, cap = StrokeCap.Round),
                colorFilter = colorFilter,
            )
            drawArc(
                color = eye,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.58f - eyeHalfWidth, h * 0.40f - eyeRadius),
                size = Size(eyeHalfWidth * 2f, eyeRadius * 2f),
                style = Stroke(width = w * 0.012f, cap = StrokeCap.Round),
                colorFilter = colorFilter,
            )
        }
        CatMood.SLEEPY -> {
            drawArc(
                color = eye,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.42f - eyeHalfWidth, h * 0.40f - eyeRadius),
                size = Size(eyeHalfWidth * 2f, eyeRadius * 2f),
                style = Stroke(width = w * 0.012f, cap = StrokeCap.Round),
                colorFilter = colorFilter,
            )
            drawArc(
                color = eye,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.58f - eyeHalfWidth, h * 0.40f - eyeRadius),
                size = Size(eyeHalfWidth * 2f, eyeRadius * 2f),
                style = Stroke(width = w * 0.012f, cap = StrokeCap.Round),
                colorFilter = colorFilter,
            )
        }
    }

    // 胡须：左右各二（墨色，随主题明暗自适应）。
    drawLine(
        color = ink,
        start = Offset(w * 0.26f, h * 0.40f),
        end = Offset(w * 0.12f, h * 0.38f),
        strokeWidth = w * 0.008f,
        cap = StrokeCap.Round,
        colorFilter = colorFilter,
    )
    drawLine(
        color = ink,
        start = Offset(w * 0.26f, h * 0.44f),
        end = Offset(w * 0.12f, h * 0.46f),
        strokeWidth = w * 0.008f,
        cap = StrokeCap.Round,
        colorFilter = colorFilter,
    )
    drawLine(
        color = ink,
        start = Offset(w * 0.74f, h * 0.40f),
        end = Offset(w * 0.88f, h * 0.38f),
        strokeWidth = w * 0.008f,
        cap = StrokeCap.Round,
        colorFilter = colorFilter,
    )
    drawLine(
        color = ink,
        start = Offset(w * 0.74f, h * 0.44f),
        end = Offset(w * 0.88f, h * 0.46f),
        strokeWidth = w * 0.008f,
        cap = StrokeCap.Round,
        colorFilter = colorFilter,
    )
}

/** 矢量配饰（资产缺失兜底）：BOW 复用 [drawBow]（描金中结）；PEARL 珍珠串；OUTFIT 钟形小裙。锚点与资产叠绘同位。 */
private fun DrawScope.drawVectorAccessory(
    accessory: CatAccessory,
    theme: ThemeSpec,
) {
    val w = size.width
    val h = size.height
    val anchorY = h * accessoryAnchorY(accessory)
    when (accessory) {
        CatAccessory.BOW ->
            drawBow(
                center = Offset(w * 0.50f, anchorY),
                width = w * 0.36f,
                color = theme.buttonTop,
            )
        CatAccessory.PEARL -> {
            // 颈间珍珠串：5 颗沿浅垂弧排布（t∈[-1,1]，中点最低）。
            for (i in -2..2) {
                val t = i / 2f
                drawCircle(
                    color = theme.laceColor,
                    radius = w * 0.022f,
                    center = Offset(w * 0.50f + t * w * 0.17f, anchorY + (1f - t * t) * h * 0.035f),
                )
            }
        }
        CatAccessory.OUTFIT -> {
            // 钟形小裙：肩线收窄、裙摆外扩、底缘中点微收出小波浪。
            drawPath(
                path =
                    Path().apply {
                        moveTo(w * 0.42f, anchorY - h * 0.06f)
                        lineTo(w * 0.58f, anchorY - h * 0.06f)
                        lineTo(w * 0.68f, anchorY + h * 0.10f)
                        lineTo(w * 0.50f, anchorY + h * 0.07f)
                        lineTo(w * 0.32f, anchorY + h * 0.10f)
                        close()
                    },
                color = theme.buttonTop,
            )
        }
    }
}

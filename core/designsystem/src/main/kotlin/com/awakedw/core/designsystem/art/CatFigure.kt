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
import androidx.compose.ui.graphics.Color
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
import com.awakedw.core.designsystem.lolita.GOLD_TRIM
import com.awakedw.core.model.CatMood
import kotlin.math.min
import kotlin.math.roundToInt

/** 立绘语义描述：测试与无障碍定位胆大王的锚点。 */
internal const val CAT_SEMANTICS = "胆大王"

/** 立绘固定边长（96dp 见方，尺寸语义：常驻首页一角）。 */
private const val CAT_FIGURE_SIZE_DP = 96

/** 呼吸单程时长：1.5s（1.5s 单程 ×2 = 3s 完整呼吸周期，Reverse 循环；审查裁定「3s 循环」= 完整周期）。 */
internal const val BREATH_LEG_MS = 1500

/** SLEEPY 轻微低饱和保留系数（0.85f：安睡的柔和感，不降透明度不灰暗——治愈铁律）。 */
private const val SLEEPY_SATURATION = 0.85f

/**
 * 呼吸缩放峰值：常规 1.02f；SLEEPY 幅度减半 → 1.01f（简报逐字参数）。
 */
internal fun breathTargetOf(mood: CatMood): Float = if (mood == CatMood.SLEEPY) 1.01f else 1.02f

/** 三态立绘资产路径（:app assets 相对路径；缺失由 [rememberAssetImageOrN] 回退 null）。 */
internal fun catAssetFileOf(mood: CatMood): String =
    when (mood) {
        CatMood.IDLE -> "cat/idle.webp"
        CatMood.HAPPY -> "cat/happy.webp"
        CatMood.SLEEPY -> "cat/sleepy.webp"
    }

/**
 * 布偶猫矢量兜底固定色板（Q 版 2.5 头身，可爱优先）：
 * 重点色（colorpoint）配色——奶油白身体 + 暖灰褐重点（耳/面具/尾），
 * 不再全盘用主题色（真机反馈「现在的猫非常丑，要一只布偶猫」的返修规格）。
 * 深夜由 [catPaletteOf] 构造时对各色预混 12% 黑（[darken]）——压暗内化进色值，
 * 只随猫本体形状轮廓生效，不再有整幅方形罩层。
 *
 * @property body 奶油白：身体/脸颊/胸口。
 * @property point 重点色暖灰褐：双耳/面部小面具/羽状尾。
 * @property ruff 胸前围脖：略深于身体的米色（短弧簇表现长毛感）。
 * @property iris 蓝宝石眼（布偶身份标志）。
 * @property innerEar 耳内浅粉。
 * @property nose 小粉鼻。
 * @property whisker 胡须固定浅色：深夜下也可见（修复终审遗留「深夜胡须隐形」，不再用主题 chipText）。
 * @property tailTip 尾尖略浅于重点色（羽状尾贵气收笔）。
 */
internal data class CatVectorPalette(
    val body: Color,
    val point: Color,
    val ruff: Color,
    val iris: Color,
    val innerEar: Color,
    val nose: Color,
    val whisker: Color,
    val tailTip: Color,
)

/** 深夜预混压暗比例：12% 黑（每通道 ×(1-0.12)），与旧「整幅罩黑 12%」视觉等效。 */
private const val DARK_VEIL_FRACTION = 0.12f

/**
 * 深夜压暗预混：把 12% 黑直接混入色值（RGB 每通道 ×(1-0.12)，alpha 不动）。
 * 内化自旧「drawRect 整幅罩黑 12%」（审查修复）：压暗只作用于猫本体形状，
 * 深夜首页不再出现叠在光袋光晕上的肉眼可见黑色方形。
 */
private fun darken(color: Color): Color =
    Color(
        red = color.red * (1f - DARK_VEIL_FRACTION),
        green = color.green * (1f - DARK_VEIL_FRACTION),
        blue = color.blue * (1f - DARK_VEIL_FRACTION),
        alpha = color.alpha,
    )

/**
 * 布偶猫矢量色板纯函数：浅/深夜共用同一套固定毛色，深夜将各色预混 12% 黑
 * （[darken]，等效旧整幅罩黑——压暗只随猫本体形状生效）。
 * 纯函数化供 [CatFigureTest] 断言关键色值与预混压暗语义。
 */
internal fun catPaletteOf(isDark: Boolean): CatVectorPalette {
    val mix: (Color) -> Color = if (isDark) ::darken else { color -> color }
    return CatVectorPalette(
        body = mix(Color(0xFFF7EFE4)),
        point = mix(Color(0xFF9C8474)),
        ruff = mix(Color(0xFFEFE2D0)),
        iris = mix(Color(0xFF5B84B1)),
        innerEar = mix(Color(0xFFF2D8D5)),
        nose = mix(Color(0xFFE8B4B8)),
        whisker = mix(Color(0xFFF7EFE4)),
        tailTip = mix(Color(0xFFC2AB99)),
    )
}

/**
 * Optional mascot used as a visual accent and lightweight tap response.
 * It never exposes progression, collection, or reward state.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun CatFigure(
    mood: CatMood,
    modifier: Modifier = Modifier,
    onPet: () -> Unit = {},
) {
    val theme = currentThemeSpec()
    val bodyImage = rememberAssetImageOrN(catAssetFileOf(mood))

    // 呼吸缩放：1.5s 单程 ×2 = 3s 完整呼吸周期（Reverse 循环）；SLEEPY 幅度减半。key(mood) 换档即重启（从 1.00 起步，跳变 ≤2% 不可感）。
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
                    } else {
                        drawVectorCat(mood, theme, sleepyFilter)
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

/**
 * 矢量布偶猫（资产缺失兜底，Q 版 2.5 头身，可爱优先）：
 * 羽状尾 → 坐姿圆身 → 胸前围脖 → 脚爪 → 圆角双耳 → 圆头 → 重点色小面具 → 蓝宝石眼 →
 * 粉鼻倒 Y 嘴 → 胡须。
 *
 * 配色固定取 [catPaletteOf]（奶油白身体 + 暖灰褐重点色，不再全盘用主题色）；
 * 深夜由色板构造时预混 12% 黑压暗（见 [darken]，压暗只作用于猫本体形状，无整幅罩层）。
 * 轮廓全部贝塞尔柔和曲线，无生硬直线拼接；
 * 头 0.135h~0.525h（耳尖至 0.068h）/ 颈 0.52h / 身 0.53h~0.96h，与配饰锚点（bow 0.18h / pearl 0.52h / dress 0.72h）自洽。
 * 眼睛区分三态（蓝宝石眼是布偶身份标志，三态均保蓝）：
 * IDLE 圆睁蓝瞳白高光 / HAPPY 上弯月牙（拱向上）/ SLEEPY 安睡闭眼（拱向下）——安睡不是消极。
 */

private fun DrawScope.drawVectorCat(
    mood: CatMood,
    theme: ThemeSpec,
    colorFilter: ColorFilter?,
) {
    val w = size.width
    val h = size.height
    val palette = catPaletteOf(theme.isDark)
    val outline = palette.point.copy(alpha = 0.62f)
    val softOutline = palette.point.copy(alpha = 0.22f)
    val cheek = palette.nose.copy(alpha = 0.16f)

    // A small grounded shadow keeps the figure from floating above the page.
    drawOval(
        color = softOutline,
        topLeft = Offset(w * 0.24f, h * 0.875f),
        size = Size(w * 0.52f, h * 0.09f),
        colorFilter = colorFilter,
    )

    // Feathered tail, drawn first so the body naturally sits in front of it.
    val tail =
        Path().apply {
            moveTo(w * 0.66f, h * 0.80f)
            cubicTo(w * 0.82f, h * 0.88f, w * 0.95f, h * 0.78f, w * 0.91f, h * 0.61f)
            cubicTo(w * 0.89f, h * 0.51f, w * 0.79f, h * 0.46f, w * 0.73f, h * 0.52f)
        }
    drawPath(
        path = tail,
        color = palette.point,
        style = Stroke(width = w * 0.14f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )
    val tailLight =
        Path().apply {
            moveTo(w * 0.70f, h * 0.80f)
            cubicTo(w * 0.83f, h * 0.84f, w * 0.90f, h * 0.75f, w * 0.87f, h * 0.62f)
            cubicTo(w * 0.85f, h * 0.56f, w * 0.80f, h * 0.54f, w * 0.77f, h * 0.57f)
        }
    drawPath(
        path = tailLight,
        color = palette.tailTip.copy(alpha = 0.66f),
        style = Stroke(width = w * 0.055f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // Compact sitting body: a calmer silhouette than the former segmented figure.
    val body =
        Path().apply {
            moveTo(w * 0.30f, h * 0.56f)
            cubicTo(w * 0.27f, h * 0.68f, w * 0.27f, h * 0.83f, w * 0.37f, h * 0.90f)
            cubicTo(w * 0.44f, h * 0.95f, w * 0.56f, h * 0.95f, w * 0.63f, h * 0.90f)
            cubicTo(w * 0.73f, h * 0.83f, w * 0.73f, h * 0.67f, w * 0.70f, h * 0.56f)
            cubicTo(w * 0.60f, h * 0.49f, w * 0.40f, h * 0.49f, w * 0.30f, h * 0.56f)
            close()
        }
    drawPath(path = body, color = palette.body, colorFilter = colorFilter)
    drawPath(
        path = body,
        color = outline,
        style = Stroke(width = w * 0.012f),
        colorFilter = colorFilter,
    )

    // Chest ruff and its scalloped hem: a fixed identity detail, not an unlockable accessory.
    val ruff =
        Path().apply {
            moveTo(w * 0.35f, h * 0.54f)
            cubicTo(w * 0.40f, h * 0.60f, w * 0.60f, h * 0.60f, w * 0.65f, h * 0.54f)
            cubicTo(w * 0.65f, h * 0.63f, w * 0.61f, h * 0.70f, w * 0.50f, h * 0.72f)
            cubicTo(w * 0.39f, h * 0.70f, w * 0.35f, h * 0.63f, w * 0.35f, h * 0.54f)
            close()
        }
    drawPath(path = ruff, color = palette.ruff, colorFilter = colorFilter)
    drawPath(
        path = ruff,
        color = softOutline,
        style = Stroke(width = w * 0.009f),
        colorFilter = colorFilter,
    )
    for (index in 0..4) {
        drawCircle(
            color = palette.ruff,
            radius = w * 0.045f,
            center = Offset(w * (0.39f + index * 0.055f), h * 0.675f),
            colorFilter = colorFilter,
        )
    }

    // Two neat paws and a few restrained toe marks.
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.35f, h * 0.835f),
        size = Size(w * 0.15f, h * 0.10f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.50f, h * 0.835f),
        size = Size(w * 0.15f, h * 0.10f),
        colorFilter = colorFilter,
    )
    val toeStroke = Stroke(width = w * 0.008f, cap = StrokeCap.Round)
    drawArc(
        color = softOutline,
        startAngle = 200f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(w * 0.375f, h * 0.855f),
        size = Size(w * 0.10f, h * 0.06f),
        style = toeStroke,
        colorFilter = colorFilter,
    )
    drawArc(
        color = softOutline,
        startAngle = 200f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(w * 0.525f, h * 0.855f),
        size = Size(w * 0.10f, h * 0.06f),
        style = toeStroke,
        colorFilter = colorFilter,
    )

    // Head with soft triangular ears and a broad, recognisable Ragdoll silhouette.
    val head =
        Path().apply {
            moveTo(w * 0.24f, h * 0.24f)
            cubicTo(w * 0.22f, h * 0.15f, w * 0.25f, h * 0.08f, w * 0.32f, h * 0.10f)
            cubicTo(w * 0.37f, h * 0.11f, w * 0.40f, h * 0.18f, w * 0.42f, h * 0.21f)
            cubicTo(w * 0.47f, h * 0.19f, w * 0.53f, h * 0.19f, w * 0.58f, h * 0.21f)
            cubicTo(w * 0.60f, h * 0.18f, w * 0.63f, h * 0.11f, w * 0.68f, h * 0.10f)
            cubicTo(w * 0.75f, h * 0.08f, w * 0.78f, h * 0.15f, w * 0.76f, h * 0.24f)
            cubicTo(w * 0.84f, h * 0.32f, w * 0.81f, h * 0.46f, w * 0.72f, h * 0.53f)
            cubicTo(w * 0.62f, h * 0.61f, w * 0.38f, h * 0.61f, w * 0.28f, h * 0.53f)
            cubicTo(w * 0.19f, h * 0.46f, w * 0.16f, h * 0.32f, w * 0.24f, h * 0.24f)
            close()
        }
    drawPath(path = head, color = palette.body, colorFilter = colorFilter)
    drawPath(
        path = head,
        color = outline,
        style = Stroke(width = w * 0.012f),
        colorFilter = colorFilter,
    )

    // Warm colourpoint ear tips sit underneath the pink inner-ear wash.
    val leftEarPoint =
        Path().apply {
            moveTo(w * 0.24f, h * 0.24f)
            cubicTo(w * 0.23f, h * 0.16f, w * 0.25f, h * 0.09f, w * 0.32f, h * 0.10f)
            cubicTo(w * 0.35f, h * 0.12f, w * 0.37f, h * 0.17f, w * 0.38f, h * 0.21f)
            cubicTo(w * 0.33f, h * 0.19f, w * 0.28f, h * 0.21f, w * 0.24f, h * 0.24f)
            close()
        }
    drawPath(path = leftEarPoint, color = palette.point.copy(alpha = 0.86f), colorFilter = colorFilter)
    val rightEarPoint =
        Path().apply {
            moveTo(w * 0.76f, h * 0.24f)
            cubicTo(w * 0.77f, h * 0.16f, w * 0.75f, h * 0.09f, w * 0.68f, h * 0.10f)
            cubicTo(w * 0.65f, h * 0.12f, w * 0.63f, h * 0.17f, w * 0.62f, h * 0.21f)
            cubicTo(w * 0.67f, h * 0.19f, w * 0.72f, h * 0.21f, w * 0.76f, h * 0.24f)
            close()
        }
    drawPath(path = rightEarPoint, color = palette.point.copy(alpha = 0.86f), colorFilter = colorFilter)

    // Colourpoint ear inserts.
    val leftEar =
        Path().apply {
            moveTo(w * 0.27f, h * 0.20f)
            cubicTo(w * 0.27f, h * 0.15f, w * 0.29f, h * 0.13f, w * 0.32f, h * 0.17f)
            cubicTo(w * 0.34f, h * 0.19f, w * 0.35f, h * 0.23f, w * 0.34f, h * 0.27f)
            cubicTo(w * 0.31f, h * 0.25f, w * 0.29f, h * 0.23f, w * 0.27f, h * 0.20f)
            close()
        }
    drawPath(path = leftEar, color = palette.innerEar, colorFilter = colorFilter)
    val rightEar =
        Path().apply {
            moveTo(w * 0.73f, h * 0.20f)
            cubicTo(w * 0.73f, h * 0.15f, w * 0.71f, h * 0.13f, w * 0.68f, h * 0.17f)
            cubicTo(w * 0.66f, h * 0.19f, w * 0.65f, h * 0.23f, w * 0.66f, h * 0.27f)
            cubicTo(w * 0.69f, h * 0.25f, w * 0.71f, h * 0.23f, w * 0.73f, h * 0.20f)
            close()
        }
    drawPath(path = rightEar, color = palette.innerEar, colorFilter = colorFilter)

    // Soft mask: the broad shape and clean muzzle are what make the cat read as a Ragdoll.
    val mask =
        Path().apply {
            moveTo(w * 0.25f, h * 0.28f)
            cubicTo(w * 0.31f, h * 0.23f, w * 0.39f, h * 0.24f, w * 0.45f, h * 0.29f)
            cubicTo(w * 0.50f, h * 0.32f, w * 0.55f, h * 0.29f, w * 0.61f, h * 0.25f)
            cubicTo(w * 0.68f, h * 0.22f, w * 0.75f, h * 0.25f, w * 0.77f, h * 0.30f)
            cubicTo(w * 0.78f, h * 0.40f, w * 0.72f, h * 0.50f, w * 0.64f, h * 0.54f)
            cubicTo(w * 0.57f, h * 0.57f, w * 0.54f, h * 0.51f, w * 0.50f, h * 0.48f)
            cubicTo(w * 0.46f, h * 0.51f, w * 0.43f, h * 0.57f, w * 0.36f, h * 0.54f)
            cubicTo(w * 0.28f, h * 0.50f, w * 0.22f, h * 0.40f, w * 0.25f, h * 0.28f)
            close()
        }
    drawPath(path = mask, color = palette.point.copy(alpha = 0.90f), colorFilter = colorFilter)
    drawOval(
        color = Color.White.copy(alpha = 0.20f),
        topLeft = Offset(w * 0.34f, h * 0.235f),
        size = Size(w * 0.10f, h * 0.045f),
        colorFilter = colorFilter,
    )

    // Warm cheek tint is intentionally almost imperceptible at normal size.
    drawOval(
        color = cheek,
        topLeft = Offset(w * 0.27f, h * 0.43f),
        size = Size(w * 0.13f, h * 0.07f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = cheek,
        topLeft = Offset(w * 0.60f, h * 0.43f),
        size = Size(w * 0.13f, h * 0.07f),
        colorFilter = colorFilter,
    )

    drawCatEyes(mood = mood, palette = palette, colorFilter = colorFilter)

    // Two small ivory muzzle pads soften the mask and keep the expression gentle.
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.37f, h * 0.405f),
        size = Size(w * 0.14f, h * 0.12f),
        colorFilter = colorFilter,
    )
    drawOval(
        color = palette.body,
        topLeft = Offset(w * 0.49f, h * 0.405f),
        size = Size(w * 0.14f, h * 0.12f),
        colorFilter = colorFilter,
    )

    // Tiny pink nose and a fine mouth line.
    val nose =
        Path().apply {
            moveTo(w * 0.47f, h * 0.435f)
            cubicTo(w * 0.48f, h * 0.42f, w * 0.52f, h * 0.42f, w * 0.53f, h * 0.435f)
            cubicTo(w * 0.525f, h * 0.455f, w * 0.51f, h * 0.465f, w * 0.50f, h * 0.465f)
            cubicTo(w * 0.49f, h * 0.465f, w * 0.475f, h * 0.455f, w * 0.47f, h * 0.435f)
            close()
        }
    drawPath(path = nose, color = palette.nose, colorFilter = colorFilter)
    val mouth =
        Path().apply {
            moveTo(w * 0.50f, h * 0.462f)
            cubicTo(w * 0.50f, h * 0.475f, w * 0.49f, h * 0.482f, w * 0.475f, h * 0.485f)
            moveTo(w * 0.50f, h * 0.462f)
            cubicTo(w * 0.50f, h * 0.475f, w * 0.51f, h * 0.482f, w * 0.525f, h * 0.485f)
        }
    drawPath(
        path = mouth,
        color = outline,
        style = Stroke(width = w * 0.008f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // Fine whiskers, kept short so the icon remains elegant rather than spiky.
    val whiskers =
        Path().apply {
            moveTo(w * 0.36f, h * 0.445f)
            cubicTo(w * 0.28f, h * 0.435f, w * 0.22f, h * 0.425f, w * 0.17f, h * 0.41f)
            moveTo(w * 0.35f, h * 0.465f)
            cubicTo(w * 0.27f, h * 0.468f, w * 0.22f, h * 0.475f, w * 0.17f, h * 0.49f)
            moveTo(w * 0.64f, h * 0.445f)
            cubicTo(w * 0.72f, h * 0.435f, w * 0.78f, h * 0.425f, w * 0.83f, h * 0.41f)
            moveTo(w * 0.65f, h * 0.465f)
            cubicTo(w * 0.73f, h * 0.468f, w * 0.78f, h * 0.475f, w * 0.83f, h * 0.49f)
        }
    drawPath(
        path = whiskers,
        color = palette.whisker.copy(alpha = 0.78f),
        style = Stroke(width = w * 0.007f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // A permanent micro bow at the collar integrates the Lolita language into the mascot itself.
    drawMiniBow(
        center = Offset(w * 0.50f, h * 0.585f),
        width = w * 0.18f,
        color = theme.primary.copy(alpha = 0.82f),
        knotColor = GOLD_TRIM.copy(alpha = 0.92f),
        colorFilter = colorFilter,
    )
}

/** Draw the three eye states without changing the surrounding face geometry. */
private fun DrawScope.drawCatEyes(
    mood: CatMood,
    palette: CatVectorPalette,
    colorFilter: ColorFilter?,
) {
    val w = size.width
    val h = size.height
    val eyeColor = palette.iris
    val eyeOutline = palette.point.copy(alpha = 0.82f)
    val centers = listOf(w * 0.405f, w * 0.595f)
    when (mood) {
        CatMood.IDLE -> {
            centers.forEach { centerX ->
                drawOval(
                    color = eyeOutline,
                    topLeft = Offset(centerX - w * 0.055f, h * 0.325f),
                    size = Size(w * 0.11f, h * 0.11f),
                    colorFilter = colorFilter,
                )
                drawOval(
                    color = eyeColor,
                    topLeft = Offset(centerX - w * 0.040f, h * 0.337f),
                    size = Size(w * 0.080f, h * 0.085f),
                    colorFilter = colorFilter,
                )
                drawOval(
                    color = eyeOutline,
                    topLeft = Offset(centerX - w * 0.016f, h * 0.345f),
                    size = Size(w * 0.032f, h * 0.070f),
                    colorFilter = colorFilter,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.94f),
                    radius = w * 0.012f,
                    center = Offset(centerX - w * 0.018f, h * 0.355f),
                    colorFilter = colorFilter,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.62f),
                    radius = w * 0.006f,
                    center = Offset(centerX + w * 0.020f, h * 0.389f),
                    colorFilter = colorFilter,
                )
            }
        }

        CatMood.HAPPY -> {
            centers.forEach { centerX ->
                drawArc(
                    color = eyeColor,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - w * 0.050f, h * 0.335f),
                    size = Size(w * 0.10f, h * 0.075f),
                    style = Stroke(width = w * 0.015f, cap = StrokeCap.Round),
                    colorFilter = colorFilter,
                )
            }
        }

        CatMood.SLEEPY -> {
            centers.forEach { centerX ->
                drawArc(
                    color = eyeColor.copy(alpha = 0.88f),
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - w * 0.050f, h * 0.335f),
                    size = Size(w * 0.10f, h * 0.075f),
                    style = Stroke(width = w * 0.014f, cap = StrokeCap.Round),
                    colorFilter = colorFilter,
                )
            }
        }
    }
}

/** Small fixed bow used as part of the mascot silhouette, not as mutable state. */
private fun DrawScope.drawMiniBow(
    center: Offset,
    width: Float,
    color: Color,
    knotColor: Color,
    colorFilter: ColorFilter?,
) {
    val left =
        Path().apply {
            moveTo(center.x, center.y)
            cubicTo(center.x - width * 0.22f, center.y - width * 0.30f, center.x - width * 0.56f, center.y - width * 0.22f, center.x - width * 0.48f, center.y)
            cubicTo(center.x - width * 0.44f, center.y + width * 0.20f, center.x - width * 0.16f, center.y + width * 0.24f, center.x, center.y + width * 0.08f)
            close()
        }
    val right =
        Path().apply {
            moveTo(center.x, center.y)
            cubicTo(center.x + width * 0.22f, center.y - width * 0.30f, center.x + width * 0.56f, center.y - width * 0.22f, center.x + width * 0.48f, center.y)
            cubicTo(center.x + width * 0.44f, center.y + width * 0.20f, center.x + width * 0.16f, center.y + width * 0.24f, center.x, center.y + width * 0.08f)
            close()
        }
    drawPath(path = left, color = color, colorFilter = colorFilter)
    drawPath(path = right, color = color, colorFilter = colorFilter)
    drawCircle(color = knotColor, radius = width * 0.12f, center = center, colorFilter = colorFilter)
}

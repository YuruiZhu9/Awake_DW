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

    // 羽状尾：体侧甩出、沿右侧上扬收卷的宽笔触曲线（重点色，根部藏于身后）。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.66f, h * 0.85f)
                cubicTo(w * 0.86f, h * 0.87f, w * 0.95f, h * 0.72f, w * 0.93f, h * 0.58f)
                cubicTo(w * 0.915f, h * 0.47f, w * 0.85f, h * 0.42f, w * 0.80f, h * 0.44f)
            },
        color = palette.point,
        style = Stroke(width = w * 0.095f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )
    // 尾尖略浅叠段：末段羽尖提亮，蓬松贵气。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.905f, h * 0.60f)
                cubicTo(w * 0.89f, h * 0.49f, w * 0.85f, h * 0.43f, w * 0.80f, h * 0.44f)
            },
        color = palette.tailTip,
        style = Stroke(width = w * 0.095f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // 坐姿身体：颈口收窄、臀部外扩、底缘圆润的梨形（头略大身圆的 2.5 头身）。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.395f, h * 0.53f)
                cubicTo(w * 0.30f, h * 0.58f, w * 0.245f, h * 0.70f, w * 0.245f, h * 0.82f)
                cubicTo(w * 0.245f, h * 0.915f, w * 0.335f, h * 0.955f, w * 0.50f, h * 0.955f)
                cubicTo(w * 0.665f, h * 0.955f, w * 0.755f, h * 0.915f, w * 0.755f, h * 0.82f)
                cubicTo(w * 0.755f, h * 0.70f, w * 0.70f, h * 0.58f, w * 0.605f, h * 0.53f)
                quadraticBezierTo(w * 0.50f, h * 0.49f, w * 0.395f, h * 0.53f)
                close()
            },
        color = palette.body,
        colorFilter = colorFilter,
    )

    // 胸前围脖（ruff）：略深米色胸襟，上缘藏于头下。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.402f, h * 0.548f)
                cubicTo(w * 0.388f, h * 0.632f, w * 0.435f, h * 0.682f, w * 0.50f, h * 0.682f)
                cubicTo(w * 0.565f, h * 0.682f, w * 0.612f, h * 0.632f, w * 0.598f, h * 0.548f)
                close()
            },
        color = palette.ruff,
        colorFilter = colorFilter,
    )
    // 长毛感：几簇短弧自围脖缘甩出到奶油身上（同色于襟内隐形、出襟可见，成绒毛层次）。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.392f, h * 0.575f)
                quadraticBezierTo(w * 0.355f, h * 0.59f, w * 0.345f, h * 0.62f)
                moveTo(w * 0.398f, h * 0.625f)
                quadraticBezierTo(w * 0.365f, h * 0.645f, w * 0.362f, h * 0.665f)
                moveTo(w * 0.45f, h * 0.665f)
                quadraticBezierTo(w * 0.44f, h * 0.69f, w * 0.452f, h * 0.705f)
                moveTo(w * 0.608f, h * 0.575f)
                quadraticBezierTo(w * 0.645f, h * 0.59f, w * 0.655f, h * 0.62f)
                moveTo(w * 0.602f, h * 0.625f)
                quadraticBezierTo(w * 0.635f, h * 0.645f, w * 0.638f, h * 0.665f)
                moveTo(w * 0.55f, h * 0.665f)
                quadraticBezierTo(w * 0.56f, h * 0.69f, w * 0.548f, h * 0.705f)
            },
        color = palette.ruff,
        style = Stroke(width = w * 0.016f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // 脚爪：身前两只小圆弧（奶油白），围脖色趾缝细线勾饱满。
    drawOval(color = palette.body, topLeft = Offset(w * 0.355f, h * 0.885f), size = Size(w * 0.115f, h * 0.078f), colorFilter = colorFilter)
    drawOval(color = palette.body, topLeft = Offset(w * 0.530f, h * 0.885f), size = Size(w * 0.115f, h * 0.078f), colorFilter = colorFilter)
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.395f, h * 0.90f)
                quadraticBezierTo(w * 0.391f, h * 0.925f, w * 0.396f, h * 0.948f)
                moveTo(w * 0.430f, h * 0.90f)
                quadraticBezierTo(w * 0.429f, h * 0.925f, w * 0.433f, h * 0.948f)
                moveTo(w * 0.570f, h * 0.90f)
                quadraticBezierTo(w * 0.569f, h * 0.925f, w * 0.573f, h * 0.948f)
                moveTo(w * 0.605f, h * 0.90f)
                quadraticBezierTo(w * 0.604f, h * 0.925f, w * 0.609f, h * 0.948f)
            },
        color = palette.ruff,
        style = Stroke(width = w * 0.007f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // 双耳：圆角三角（布偶耳圆），耳外重点色、耳内浅粉；耳根由圆头盖住自然衔接。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.325f, h * 0.275f)
                cubicTo(w * 0.272f, h * 0.21f, w * 0.262f, h * 0.115f, w * 0.302f, h * 0.068f)
                cubicTo(w * 0.352f, h * 0.078f, w * 0.428f, h * 0.128f, w * 0.462f, h * 0.185f)
                close()
            },
        color = palette.point,
        colorFilter = colorFilter,
    )
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.675f, h * 0.275f)
                cubicTo(w * 0.728f, h * 0.21f, w * 0.738f, h * 0.115f, w * 0.698f, h * 0.068f)
                cubicTo(w * 0.648f, h * 0.078f, w * 0.572f, h * 0.128f, w * 0.538f, h * 0.185f)
                close()
            },
        color = palette.point,
        colorFilter = colorFilter,
    )
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.345f, h * 0.245f)
                cubicTo(w * 0.312f, h * 0.196f, w * 0.305f, h * 0.128f, w * 0.328f, h * 0.098f)
                cubicTo(w * 0.365f, h * 0.118f, w * 0.418f, h * 0.152f, w * 0.443f, h * 0.195f)
                close()
            },
        color = palette.innerEar,
        colorFilter = colorFilter,
    )
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.655f, h * 0.245f)
                cubicTo(w * 0.688f, h * 0.196f, w * 0.695f, h * 0.128f, w * 0.672f, h * 0.098f)
                cubicTo(w * 0.635f, h * 0.118f, w * 0.582f, h * 0.152f, w * 0.557f, h * 0.195f)
                close()
            },
        color = palette.innerEar,
        colorFilter = colorFilter,
    )

    // 圆头：略宽的圆润椭圆（盖住耳根、颈口与围脖上缘）。
    drawOval(color = palette.body, topLeft = Offset(w * 0.285f, h * 0.135f), size = Size(w * 0.43f, h * 0.39f), colorFilter = colorFilter)

    // 重点色小面具：眼周到鼻梁的柔和 V 形（不整脸涂满，奶油脸颊大半保留，下巴奶油）。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.50f, h * 0.24f)
                cubicTo(w * 0.60f, h * 0.245f, w * 0.635f, h * 0.30f, w * 0.635f, h * 0.36f)
                cubicTo(w * 0.635f, h * 0.42f, w * 0.585f, h * 0.455f, w * 0.50f, h * 0.462f)
                cubicTo(w * 0.415f, h * 0.455f, w * 0.365f, h * 0.42f, w * 0.365f, h * 0.36f)
                cubicTo(w * 0.365f, h * 0.30f, w * 0.40f, h * 0.245f, w * 0.50f, h * 0.24f)
                close()
            },
        color = palette.point,
        colorFilter = colorFilter,
    )

    // 蓝宝石眼：IDLE 圆睁蓝瞳白高光 / HAPPY 上弯月牙 / SLEEPY 安睡闭眼下弧。
    val irisRadius = w * 0.037f
    val crescentHalfWidth = w * 0.045f
    val crescentRadius = w * 0.035f
    when (mood) {
        CatMood.IDLE -> {
            drawCircle(color = palette.iris, radius = irisRadius, center = Offset(w * 0.425f, h * 0.355f), colorFilter = colorFilter)
            drawCircle(color = palette.iris, radius = irisRadius, center = Offset(w * 0.575f, h * 0.355f), colorFilter = colorFilter)
            drawCircle(color = Color.White, radius = w * 0.012f, center = Offset(w * 0.438f, h * 0.342f), colorFilter = colorFilter)
            drawCircle(color = Color.White, radius = w * 0.012f, center = Offset(w * 0.588f, h * 0.342f), colorFilter = colorFilter)
        }
        CatMood.HAPPY -> {
            drawArc(
                color = palette.iris,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.425f - crescentHalfWidth, h * 0.355f - crescentRadius),
                size = Size(crescentHalfWidth * 2f, crescentRadius * 2f),
                style = Stroke(width = w * 0.014f, cap = StrokeCap.Round),
                colorFilter = colorFilter,
            )
            drawArc(
                color = palette.iris,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.575f - crescentHalfWidth, h * 0.355f - crescentRadius),
                size = Size(crescentHalfWidth * 2f, crescentRadius * 2f),
                style = Stroke(width = w * 0.014f, cap = StrokeCap.Round),
                colorFilter = colorFilter,
            )
        }
        CatMood.SLEEPY -> {
            drawArc(
                color = palette.iris,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.425f - crescentHalfWidth, h * 0.355f - crescentRadius),
                size = Size(crescentHalfWidth * 2f, crescentRadius * 2f),
                style = Stroke(width = w * 0.014f, cap = StrokeCap.Round),
                colorFilter = colorFilter,
            )
            drawArc(
                color = palette.iris,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.575f - crescentHalfWidth, h * 0.355f - crescentRadius),
                size = Size(crescentHalfWidth * 2f, crescentRadius * 2f),
                style = Stroke(width = w * 0.014f, cap = StrokeCap.Round),
                colorFilter = colorFilter,
            )
        }
    }

    // 小粉鼻：柔和圆角小三角（重点色面具内）。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.471f, h * 0.427f)
                cubicTo(w * 0.478f, h * 0.408f, w * 0.522f, h * 0.408f, w * 0.529f, h * 0.427f)
                cubicTo(w * 0.526f, h * 0.443f, w * 0.509f, h * 0.452f, w * 0.50f, h * 0.452f)
                cubicTo(w * 0.491f, h * 0.452f, w * 0.474f, h * 0.443f, w * 0.471f, h * 0.427f)
                close()
            },
        color = palette.nose,
        colorFilter = colorFilter,
    )

    // 倒 Y 小嘴：鼻下短茎 + 左右分叉弧（重点色细线，落在奶油下巴上清晰）。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.50f, h * 0.452f)
                quadraticBezierTo(w * 0.502f, h * 0.462f, w * 0.50f, h * 0.470f)
                moveTo(w * 0.50f, h * 0.470f)
                quadraticBezierTo(w * 0.488f, h * 0.480f, w * 0.474f, h * 0.479f)
                moveTo(w * 0.50f, h * 0.470f)
                quadraticBezierTo(w * 0.512f, h * 0.480f, w * 0.526f, h * 0.479f)
            },
        color = palette.point,
        style = Stroke(width = w * 0.008f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )

    // 胡须：左右各三根微曲浅色须（固定奶油白，深夜下也可见——不再随主题 chipText 变深）。
    drawPath(
        path =
            Path().apply {
                moveTo(w * 0.345f, h * 0.375f)
                quadraticBezierTo(w * 0.24f, h * 0.362f, w * 0.155f, h * 0.352f)
                moveTo(w * 0.348f, h * 0.400f)
                quadraticBezierTo(w * 0.24f, h * 0.402f, w * 0.150f, h * 0.412f)
                moveTo(w * 0.352f, h * 0.425f)
                quadraticBezierTo(w * 0.25f, h * 0.440f, w * 0.165f, h * 0.462f)
                moveTo(w * 0.655f, h * 0.375f)
                quadraticBezierTo(w * 0.76f, h * 0.362f, w * 0.845f, h * 0.352f)
                moveTo(w * 0.652f, h * 0.400f)
                quadraticBezierTo(w * 0.76f, h * 0.402f, w * 0.850f, h * 0.412f)
                moveTo(w * 0.648f, h * 0.425f)
                quadraticBezierTo(w * 0.75f, h * 0.440f, w * 0.835f, h * 0.462f)
            },
        color = palette.whisker,
        style = Stroke(width = w * 0.009f, cap = StrokeCap.Round),
        colorFilter = colorFilter,
    )
}

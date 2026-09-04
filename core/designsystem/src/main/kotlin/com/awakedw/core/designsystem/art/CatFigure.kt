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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.model.CatMood
import kotlin.math.min
import kotlin.math.roundToInt

/** 立绘语义描述：测试与无障碍定位胆大王的锚点。 */
internal const val CAT_SEMANTICS = "胆大王"

/** 立绘固定边长（108dp 见方，尺寸语义：常驻首页一角）。 */
private const val CAT_FIGURE_SIZE_DP = 108

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
        CatMood.IDLE -> "cat/beforedrink.png"
        CatMood.HAPPY -> "cat/afterdrink.png"
        CatMood.SLEEPY -> "cat/beforedrink.png"
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
    val reduceMotion = rememberReduceMotion()

    // 呼吸缩放：系统减少动态时保持静止；正常状态为 1.5s 单程 ×2 = 3s 完整周期。
    val breathState: State<Float> =
        if (reduceMotion) {
            remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
        } else {
            key(mood) {
                rememberInfiniteTransition(label = "CatBreath").animateFloat(
                    initialValue = 1f,
                    targetValue = breathTargetOf(mood),
                    animationSpec = infiniteRepeatable(tween(durationMillis = BREATH_LEG_MS), RepeatMode.Reverse),
                    label = "CatBreathScale",
                )
            }
        }
    val breathScale by breathState

    // HAPPY 一次 spring 弹跳；系统减少动态时不蓄力、不回弹，避免装饰动作干扰主任务。
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(mood, reduceMotion) {
        if (reduceMotion) {
            bounce.snapTo(1f)
        } else if (mood == CatMood.HAPPY) {
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
                .semantics {
                    contentDescription = CAT_SEMANTICS
                    role = Role.Button
                    onClick(label = "摸摸胆大王") {
                        onPet()
                        true
                    }
                }
                .pointerInput(Unit) { detectTapGestures { onPet() } },
    )
}

/** 等比适配铺绘（ContentScale.Fit 语义）：108dp 方框内居中、不变形、不裁切。 */
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
 * 头部占比约 0.08h~0.63h、身体约 0.47h~0.98h；蝴蝶结固定在围脖下方，只作为轮廓细节。
 * 眼睛区分三态（蓝宝石眼是布偶身份标志，三态均保蓝）：
 * IDLE 圆睁蓝瞳白高光 / HAPPY 上弯月牙（拱向上）/ SLEEPY 安睡闭眼（拱向下）——安睡不是消极。
 */

package com.awakedw.core.designsystem.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/**
 * 单颗粒子的帧不变属性：全部由 `(index, seed, sizePx, area)` 确定性推导，与时间无关。
 * 由 [ParticleMath.specOf] 产出，供绘制层在 `drawWithCache` 缓存段一次算好——
 * 帧间只做 [ParticleMath.evaluate]，不再逐帧重掷随机与重算锚点（2.6.0 绘制审计）。
 */
data class ParticleSpec(
    val baseAlpha: Float,
    val radiusPx: Float,
    val startX: Float,
    val startY: Float,
    val travel: Float,
    val swayAmplitude: Float,
    val swayPhase: Float,
    val glow: Boolean,
)

/**
 * 单颗粒子在某帧的绘制结论：位置、半径、不透明度与是否带辉光。
 *
 * 纯数据类，由 [ParticleMath] 纯函数产出，供 Canvas 绘制层直接消费。
 */
data class ParticleFrame(
    val center: Offset,
    val radiusPx: Float,
    val alpha: Float,
    val glow: Boolean,
)

/**
 * 粒子运动学纯函数集（设计规格 §2.2「漂浮粒子」）。
 *
 * 可测性核心：所有属性（初始相位 / 半径 / 速度 / 摆幅）均按 `(index, seed)`
 * 经 [kotlin.random.Random] 确定性推导，同一输入永远得到同一输出——
 * 因此 Composable 层无需持有任何随机对象，天然规避重组重采样。
 */
object ParticleMath {
    /** 大粒索引（alpha13 §13 粒子扩容：4 颗，带辉光）。 */
    val BIG_INDEX_RANGE = 0..3

    /** 中粒索引（16 颗，微辉）。 */
    val MEDIUM_INDEX_RANGE = 4..19

    /** 小粒索引（60 颗，无辉光）。 */
    val SMALL_INDEX_RANGE = 20..79

    /** 圆点粒子总数：大 4 + 中 16 + 小 60。 */
    const val DOT_COUNT = 80

    /** 固定相位星芒粒子数（由 Composable 层独立绘制，不经本引擎）。 */
    const val STAR_COUNT = 3

    /** 大粒不透明度带（规格 §2.2：0.25–0.55）。 */
    val BIG_ALPHA_RANGE = 0.25f..0.55f

    /** 中粒不透明度带（0.45–0.65）。 */
    val MEDIUM_ALPHA_RANGE = 0.45f..0.65f

    /** 小粒不透明度带（0.60–0.75）。 */
    val SMALL_ALPHA_RANGE = 0.60f..0.75f

    /** 大粒半径因子带 × [floating] 的 sizePx 锚点。 */
    val BIG_RADIUS_FACTOR_RANGE = 0.34f..0.46f

    /** 中粒半径因子带。 */
    val MEDIUM_RADIUS_FACTOR_RANGE = 0.20f..0.27f

    /** 小粒半径因子带。 */
    val SMALL_RADIUS_FACTOR_RANGE = 0.11f..0.16f

    /** 单周期（progress01 走满 1）内上浮总路程相对屏幕高度的比例带；恒大于一屏保证无缝回卷。 */
    private const val TRAVEL_MIN = 1.10f
    private const val TRAVEL_MAX = 1.45f

    /** 水平锚点在宽度上的比例带。 */
    private const val X_ANCHOR_MIN = 0.08f
    private const val X_ANCHOR_MAX = 0.92f

    /** 迸发粒子收缩终点占初径的比例。 */
    private const val BURST_END_SHRINK = 0.35f

    /**
     * 漂浮粒子第 [index] 颗的帧不变属性（2.6.0 绘制审计拆分）：把 [floating] 里
     * 与时间无关的全部随机推导一次算好，绘制层在缓存段调用；帧间只走 [evaluate]。
     *
     * @param seed   随机种子；同一种子下每颗粒子的初始相位/半径/速度完全确定
     * @param sizePx 半径锚点像素（调用方按屏幕密度换算，如 `40.dp.toPx()`）
     */
    fun specOf(
        index: Int,
        seed: Long,
        sizePx: Float,
        area: Size,
    ): ParticleSpec {
        val (alphaBand, radiusFactor) = layerSpecFor(index)
        val rng = kotlin.random.Random(seed = seed * 1_000_003L + index + 101L)

        val baseAlpha = lerp(alphaBand.start, alphaBand.endInclusive, rng.nextFloat())
        val radius = sizePx * lerp(radiusFactor.start, radiusFactor.endInclusive, rng.nextFloat())

        val height = area.height
        val width = area.width
        val startXAnchor = lerp(X_ANCHOR_MIN, X_ANCHOR_MAX, rng.nextFloat())
        val startRatio = rng.nextFloat()
        val travel = lerp(TRAVEL_MIN, TRAVEL_MAX, rng.nextFloat()) * height
        val swayAmplitude = sizePx * lerp(0.4f, 0.9f, rng.nextFloat())
        val swayPhase = rng.nextFloat() * 2f * PI.toFloat()

        // 纵向旅程：起点随机分布于屏内，沿负方向匀速上浮。
        val startY = radius + startRatio * (height - 2f * radius)

        return ParticleSpec(
            baseAlpha = baseAlpha,
            radiusPx = radius,
            startX = startXAnchor * width,
            startY = startY,
            travel = travel,
            swayAmplitude = swayAmplitude,
            swayPhase = swayPhase,
            glow = index <= MEDIUM_INDEX_RANGE.last,
        )
    }

    /**
     * 漂浮粒子第 [index] 颗在总时长进度 [progress01] 处的绘制结论。
     *
     * @param seed     随机种子；同一种子下每颗粒子的初始相位/半径/速度完全确定
     * @param sizePx   半径锚点像素（调用方按屏幕密度换算，如 `40.dp.toPx()`），
     *                 使同一套因子带可服务不同密度设备
     * @param progress01 总循环进度，任意实数均可：轨迹对 `p ↦ p+1` 周期闭合
     *                   （单粒行程恰为一圈周长，故每个整周期恰好无缝回卷一次）
     */
    fun floating(
        index: Int,
        seed: Long,
        sizePx: Float,
        progress01: Float,
        area: Size,
    ): ParticleFrame = evaluate(specOf(index, seed, sizePx, area), progress01, area)

    /**
     * 由帧不变 [ParticleSpec] 求某进度下的绘制结论：只做纯浮点运算，无随机对象。
     * 与 [floating] 按同序运算逐位一致（等价性由 `ParticleMathTest` 扫描钉住）。
     */
    fun evaluate(
        spec: ParticleSpec,
        progress01: Float,
        area: Size,
    ): ParticleFrame {
        // 纵向旅程：以「行程长度」为周期做模运算——越过顶部渐隐区后自然从底部重现。
        val exitY = -(spec.radiusPx + topFadeBand(spec.radiusPx))
        val rawY = spec.startY - spec.travel * progress01
        val shift = ceil((exitY - rawY) / spec.travel)
        val y = rawY + spec.travel * shift

        // 横向轻微正弦摆动（整数周期保证整环闭合），并约束在屏内。
        val sway = spec.swayAmplitude * sin(2f * PI.toFloat() * progress01 + spec.swayPhase)
        val maxX = (area.width - spec.radiusPx).coerceAtLeast(spec.radiusPx)
        val x = (spec.startX + sway).coerceIn(spec.radiusPx, maxX)

        // 顶部渐隐包络：接近顶缘时平滑衰减为零，越过顶缘即完全不可见。
        val envelope = smoothstep((y / topFadeBand(spec.radiusPx)).coerceIn(0f, 1f))

        return ParticleFrame(
            center = Offset(x, y),
            radiusPx = spec.radiusPx,
            alpha = spec.baseAlpha * envelope,
            glow = spec.glow,
        )
    }

    /**
     * 迸发粒子在某进度 [travel01] 处的绘制结论：自 [origin] 起、朝 [angleRad]
     * 方向严格线性推进至 [distancePx]，不透明度由 1 线性降至 0，半径同步收缩。
     * 迸发总为「发光体」（glow=true），位置缓动由调用方把时间映射到 [travel01] 完成。
     */
    fun burst(
        travel01: Float,
        origin: Offset,
        angleRad: Float,
        distancePx: Float,
    ): ParticleFrame {
        val travel = travel01.coerceIn(0f, 1f)
        val direction = Offset(cos(angleRad), sin(angleRad))
        return ParticleFrame(
            center = origin + direction * (travel * distancePx),
            radiusPx = (distancePx * 0.12f * (1f - BURST_END_SHRINK * travel)).coerceAtLeast(2f),
            alpha = 1f - travel,
            glow = true,
        )
    }

    /** 顶部渐隐带宽随半径自适应（半径越大越早开始淡出）。 */
    private fun topFadeBand(radiusPx: Float): Float = (radiusPx * 2.5f).coerceIn(36f, 80f)

    private fun layerSpecFor(index: Int): Pair<ClosedFloatingPointRange<Float>, ClosedFloatingPointRange<Float>> =
        when (index) {
            in BIG_INDEX_RANGE -> BIG_ALPHA_RANGE to BIG_RADIUS_FACTOR_RANGE
            in MEDIUM_INDEX_RANGE -> MEDIUM_ALPHA_RANGE to MEDIUM_RADIUS_FACTOR_RANGE
            else -> SMALL_ALPHA_RANGE to SMALL_RADIUS_FACTOR_RANGE
        }

    private fun lerp(
        start: Float,
        end: Float,
        fraction: Float,
    ): Float = start + (end - start) * fraction

    /** 三次平滑插值 smoothstep，两端导数为零，渐隐无折角。 */
    private fun smoothstep(x: Float): Float = x * x * (3f - 2f * x)
}

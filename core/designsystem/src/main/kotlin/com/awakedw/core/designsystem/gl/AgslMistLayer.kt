package com.awakedw.core.designsystem.gl

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.nativeCanvas
import com.awakedw.core.designsystem.MotionTokens
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.designsystem.scene.rememberSceneSpec

/**
 * AGSL 雾光可用性门控（纯函数，供测试）：
 * - Android 13（API 33）起才有 [RuntimeShader]；
 * - 减少动态模式不启用任何氛围动画。
 */
fun agslSupported(
    sdkInt: Int,
    reduceMotion: Boolean,
): Boolean = sdkInt >= 33 && !reduceMotion

/**
 * 雾光着色器（AGSL / SkSL 子集）：两路错相低频 value-noise 缓慢流动，
 * 主题 halo 色低强度着色；横向包络让中央阅读列弱于边缘（基线 §3）。
 * 输出为预乘 alpha 形式（rgb = tint * a），SrcOver 直叠于背景渐变之上。
 */
private const val MIST_SKSL =
    """
    uniform float2 uResolution;
    uniform float uTime;
    uniform float uSeed;
    uniform half3 uTint;

    float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash(i), hash(i + float2(1, 0)), u.x),
                   mix(hash(i + float2(0, 1)), hash(i + float2(1, 1)), u.x), u.y);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / uResolution;
        float t = uTime * 6.2831853;
        float2 flow = float2(sin(t) * 0.9, cos(t * 0.85) * 0.6) * 1.6;
        float m = noise(uv * float2(2.6, 3.8) + flow * 0.7 + uSeed) * 0.62
                + noise(uv * float2(4.6, 3.2) - flow + 11.3 + uSeed) * 0.38;
        float band = smoothstep(0.30, 0.85, m);
        float lateral = 1.0 - smoothstep(0.16, 0.5, abs(uv.x - 0.5));
        float intensity = band * mix(0.35, 1.0, lateral);
        float a = intensity * 0.12;
        return half4(uTint * a, a);
    }
    """

/**
 * AGSL 雾光层（0.8.0，alpha13 §13.4）：全屏动态雾光，替代静态渐变的「平面感」。
 *
 * 三重门控（任一不满足即整层静默，天然回退到既有渐变 + 噪点底座）：
 * 1. API 33+（[RuntimeShader] 的最低版本）；
 * 2. 非减少动态；
 * 3. 着色器构造成功（runCatching 兜底：低概率设备 / Robolectric 环境异常时静默）。
 *
 * 强度随时段氛围（[rememberSceneSpec].midgroundAlpha）插值；颜色取主题 haloColor；
 * 只进 draw 相（40s 无缝循环），不触发重组。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun AgslMistLayer(
    modifier: Modifier = Modifier,
    seed: Float = 3.7f,
) {
    val reduceMotion = rememberReduceMotion()
    // 显式版本守卫（lint 可识别）；与 agslSupported() 语义一致，一致性由 AgslGateTest 守住。
    if (Build.VERSION.SDK_INT < 33 || reduceMotion) return

    val spec = currentThemeSpec()
    val scene = rememberSceneSpec()
    val halo = spec.haloColor
    val progress = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var accumulated = 0L
        while (true) {
            val now = withFrameNanos { it }
            accumulated += now - last
            last = now
            progress.floatValue = (accumulated % (MotionTokens.MIST_LOOP_PERIOD_MS * 1_000_000L)).toFloat() /
                (MotionTokens.MIST_LOOP_PERIOD_MS * 1_000_000L)
        }
    }

    // runCatching 兜底：低概率设备 / Robolectric 环境下着色器异常时整层静默。
    val shader = remember(halo) { runCatching { buildMistShader(halo, seed) }.getOrNull() } ?: return
    val paint = remember { Paint() }

    Box(
        modifier =
            modifier.drawWithCache {
                shader.setFloatUniform("uResolution", size.width, size.height)
                onDrawBehind {
                    shader.setFloatUniform("uTime", progress.floatValue)
                    val dim = scene.midgroundAlpha
                    shader.setFloatUniform(
                        "uTint",
                        halo.red * dim,
                        halo.green * dim,
                        halo.blue * dim,
                    )
                    paint.shader = shader
                    val canvas = drawContext.canvas.nativeCanvas
                    canvas.drawRect(0f, 0f, size.width, size.height, paint)
                }
            },
    )
}

/** 着色器构造（API 33+）：解析 SKSL 并注入静态 uniform；解析失败抛异常由调用方兜底。 */
@RequiresApi(33)
private fun buildMistShader(
    halo: androidx.compose.ui.graphics.Color,
    seed: Float,
): RuntimeShader =
    RuntimeShader(MIST_SKSL).apply {
        setFloatUniform("uSeed", seed)
        setFloatUniform("uTint", halo.red, halo.green, halo.blue)
    }

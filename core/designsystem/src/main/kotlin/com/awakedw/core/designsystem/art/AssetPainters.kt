package com.awakedw.core.designsystem.art

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 资源装载的最长边上限：足够支撑全屏氛围图，也避免原始大图占满内存。 */
private const val MAX_DECODE_DIMENSION_PX = 1024

/**
 * 读 assets 位图；缺失/解码失败返回 null（调用方回退，绝不抛异常）。内部用 [Dispatchers.IO]。
 *
 * 供 [rememberAssetImageOrN] 在组合内调用；也可在挂起环境直接使用。先读尺寸再按需采样，
 * 猫咪小图和全屏 Lolita 氛围图都不会以原始超大尺寸长期驻留内存。
 */
internal suspend fun loadAssetBitmap(
    context: Context,
    assetFile: String,
): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.assets.open(assetFile).use { it.readBytes() }
            decodeSampled(bytes)?.asImageBitmap()
        }.getOrNull()
    }

private fun decodeSampled(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options =
        BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun sampleSizeFor(
    width: Int,
    height: Int,
): Int {
    var sample = 1
    while (width / (sample * 2) >= MAX_DECODE_DIMENSION_PX || height / (sample * 2) >= MAX_DECODE_DIMENSION_PX) {
        sample *= 2
    }
    return sample
}

/**
 * 探测 assets 中是否存在 [assetFile]：按目录拆分后用 `assets.list()` 探测存在与否。
 * 任何 IO 异常一律按「不存在」处理，绝不抛异常。
 */
internal fun hasAsset(
    context: Context,
    assetFile: String,
): Boolean {
    val dir = assetFile.substringBeforeLast('/', missingDelimiterValue = "")
    val name = assetFile.substringAfterLast('/')
    return runCatching {
        context.assets.list(dir)?.contains(name) == true
    }.getOrDefault(false)
}

/**
 * 夜变体路径映射：`art/theme_day.webp` → `art/theme_day_night.webp`。
 * 仅在最后一个 `.` 前插入 `_night` 后缀；无扩展名（或点在目录段里）则直接追加 `_night`。
 *
 * 需要按存在性解析（存在则用之，否则原文件）时用重载 [nightVariantOf]。
 */
fun nightVariantOf(assetFile: String): String {
    val slash = assetFile.lastIndexOf('/')
    val dot = assetFile.lastIndexOf('.')
    if (dot <= slash) return assetFile + "_night"
    return buildString {
        append(assetFile, 0, dot)
        append("_night")
        append(assetFile, dot, assetFile.length)
    }
}

/** 夜变体解析：夜间图（[nightVariantOf] 映射结果）在 assets 中存在则用之，否则原文件。 */
fun nightVariantOf(
    context: Context,
    assetFile: String,
): String {
    val night = nightVariantOf(assetFile)
    return if (hasAsset(context, night)) night else assetFile
}

/**
 * 组合期读取 assets 位图；缺失/解码失败组合值为 null（调用方回退，绝不抛异常）。
 * 内部 [Dispatchers.IO] 异步装载，[assetFile] 变化时自动重载。
 *
 * 实现注记：等价于 `produceState` 的展开写法（unkeyed `remember { mutableStateOf }` +
 * keyed [LaunchedEffect]）——语义逐点一致：state 实例跨键保留（换图期间旧图保持上屏，
 * 不闪空帧）、键变化取消旧装载并重启、IO 调度、缺失回退 null。弃用 `produceState`
 * 是因为当前工具链下 Compose runtime lint 对 lambda 接收者上的 `value =` 赋值会误报，
 * 展开写法无此问题。
 */
@Composable
fun rememberAssetImageOrN(assetFile: String): ImageBitmap? {
    val context = LocalContext.current
    val state = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(assetFile) {
        state.value = withContext(Dispatchers.IO) { loadAssetBitmap(context, assetFile) }
    }
    return state.value
}

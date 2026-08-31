package com.awakedw.core.designsystem.art

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 读 assets 位图；缺失/解码失败返回 null（调用方回退，绝不抛异常）。内部用 [Dispatchers.IO]。
 *
 * 供 [rememberAssetImageOrN] 在组合内调用；也可在挂起环境直接使用。
 */
internal suspend fun loadAssetBitmap(
    context: Context,
    assetFile: String,
): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(assetFile).use { input ->
                val bytes = input.readBytes()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }.getOrNull()
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
 * 夜变体路径映射：`outfit/dress_01.webp` → `outfit/dress_01_night.webp`。
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

/**
 * 夜变体解析：夜间图（[nightVariantOf] 映射结果）在 assets 中存在则用之，否则原文件。
 */
fun nightVariantOf(
    context: Context,
    assetFile: String,
): String {
    val night = nightVariantOf(assetFile)
    return if (hasAsset(context, night)) night else assetFile
}

/**
 * 组合期读取 assets 位图；缺失/解码失败组合值为 null（调用方回退，绝不抛异常）。
 * 内部 [produceState] + [Dispatchers.IO] 异步装载，[assetFile] 变化时自动重载。
 */
@Composable
fun rememberAssetImageOrN(assetFile: String): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, assetFile) {
        value = loadAssetBitmap(context, assetFile)
    }.value
}

package com.awakedw.feature.gallery.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.drawBow
import com.awakedw.feature.gallery.GalleryItemUi

/** 卡片缩略图区宽高比（裙装竖构图 3:4）。 */
private const val CARD_ASPECT_RATIO = 0.75f

/** 锁定件缩略图压暗程度：剪影可辨但「还未属于你」的轻盈隔层感。 */
private const val LOCKED_DIM_ALPHA = 0.55f

/**
 * 画廊卡片（moodboard §5.2 册页式呈现）：
 * 缩略图（assets 位图；缺失/锁定回退主题色渐变 + 裙装剪影）+ 标题行；
 * 锁定件叠加「第 N 天解锁 ♡」小字——是期待感文案不是惩罚文案（治愈铁律）；
 * [item.pinned] 为 true 时标题行挂「今日之裙」小签。整卡可点开详情底部弹层。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun OutfitCard(
    item: GalleryItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    spec: ThemeSpec = currentThemeSpec(),
) {
    Column(modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        val bitmap = rememberAssetImageOrN(item.outfit.assetFile)
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(CARD_ASPECT_RATIO)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(spec.primary.copy(alpha = 0.20f), spec.haloColor.copy(alpha = 0.10f)),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = item.outfit.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (!item.unlocked) {
                    // 理论兜底：锁定件正常不装资产；万一资产先行到位仍压暗呈剪影。
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = LOCKED_DIM_ALPHA)))
                }
            } else {
                DressSilhouette(spec = spec, modifier = Modifier.fillMaxSize())
            }
            if (!item.unlocked) {
                Text(
                    text = UNLOCK_HINT_FORMAT.format(item.outfit.unlockDay),
                    color = spec.chipText.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp),
        ) {
            Text(
                text = item.outfit.title,
                color = spec.chipText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (item.pinned) {
                Text(
                    text = TODAY_SIGN,
                    color = spec.primary,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                )
            }
        }
    }
}

/** 「今日之裙」卡片小签。 */
private const val TODAY_SIGN = "♡ 今日之裙"

/** 锁定件期待感文案（治愈铁律：倒数的是见面，不是惩罚）。 */
private val UNLOCK_HINT_FORMAT = "第 %d 天解锁 ♡"

/**
 * 裙装剪影（占位视觉）：A 字裙身矢量剪影 + 腰间小蝴蝶结，主题色低透明度绘制——
 * 资产未就位/锁定件的「画框留白」，与首页画卷层的克制基调一致。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun DressSilhouette(
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
) {
    val silhouette = spec.primary.copy(alpha = 0.26f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dress =
            Path().apply {
                moveTo(w * 0.40f, h * 0.16f)
                quadraticBezierTo(w * 0.50f, h * 0.24f, w * 0.60f, h * 0.16f)
                lineTo(w * 0.555f, h * 0.40f)
                lineTo(w * 0.80f, h * 0.82f)
                quadraticBezierTo(w * 0.50f, h * 0.90f, w * 0.20f, h * 0.82f)
                lineTo(w * 0.445f, h * 0.40f)
                close()
            }
        drawPath(dress, silhouette)
        drawBow(
            center = Offset(w * 0.5f, h * 0.40f),
            width = w * 0.14f,
            color = spec.primary.copy(alpha = 0.45f),
        )
    }
}

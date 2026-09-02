package com.awakedw.feature.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.PagePadding
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.art.rememberAssetImageOrN
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.onPrimarySurface
import com.awakedw.feature.gallery.components.OutfitCard

/** 分区页签：裙装 / 馆藏（moodboard §5.2 馆藏分区决议③）。 */
private val SECTION_LABELS = listOf("裙装", "馆藏")

/** 网格间距（册页之间的呼吸）。 */
private val GRID_GAP = 14.dp

/** 页签指示器底色透明度：与导航壳底栏选中指示器同档。 */
private const val TAB_INDICATOR_ALPHA = 0.14f

/** 弹层大图高度（竖构图 3:4 的可观尺度）。 */
private val DETAIL_IMAGE_HEIGHT = 300.dp

/**
 * 画廊页「衣橱」（moodboard §5.2 收集循环）：
 * 顶栏（返回 + 标题）+ 裙装/馆藏分区页签 + 2 列册页网格（[OutfitCard]）；
 * 点开卡片浮出详情底部弹层：大图 + 裙装小注 + 「设为今日之裙 / 取消指定」（即 pin 切换）。
 * 返回经 [onBack]（:app 接 navController.popBackStack），系统返回键由导航壳自理。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val spec = currentThemeSpec()
    var section by rememberSaveable { mutableIntStateOf(0) }
    var selectedOutfitId by remember { mutableStateOf<String?>(null) }

    // P3-7：平涂单色底改为与首页同底座的 GradientBackdrop（渐变 + 柔光晕 + 噪点），内容置于其上。
    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        Column(modifier = Modifier.fillMaxSize()) {
            GalleryTopBar(spec = spec, onBack = onBack)
            SectionTabs(
                labels = SECTION_LABELS,
                selected = section,
                spec = spec,
                onSelect = { section = it },
            )
            val items = if (section == 0) state.dresses else state.museum
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = PagePadding, end = PagePadding, top = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(GRID_GAP),
                verticalArrangement = Arrangement.spacedBy(GRID_GAP),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.outfit.id }) { item ->
                    OutfitCard(item = item, onClick = { selectedOutfitId = item.outfit.id })
                }
            }
        }
    }

    // 弹层持 id 而非整件：pin 翻转后从最新 uiState 重取，按钮文案随之实时换面。
    val selectedItem = selectedOutfitId?.let { id -> (state.dresses + state.museum).firstOrNull { it.outfit.id == id } }
    if (selectedItem != null) {
        OutfitDetailSheet(
            item = selectedItem,
            spec = spec,
            onPin = viewModel::pin,
            onDismiss = { selectedOutfitId = null },
        )
    }
}

/** 顶栏：返回箭头 + 「衣橱」标题（画廊非主页签：底栏隐藏，返回键回原页签）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun GalleryTopBar(
    spec: ThemeSpec,
    onBack: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, start = 6.dp, end = 20.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = spec.chipText,
            )
        }
        Text(
            text = "衣橱",
            color = spec.greetingColor,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

/** 分区页签行：选中态主题色胶囊，未选中淡化（克制的册页标签，不做重动画）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun SectionTabs(
    labels: List<String>,
    selected: Int,
    spec: ThemeSpec,
    onSelect: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = 26.dp, top = 8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (isSelected) spec.primary.copy(alpha = TAB_INDICATOR_ALPHA) else Color.Transparent,
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    color = if (isSelected) spec.primary else spec.chipText.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * 详情底部弹层（moodboard §5.2：单件大图 + 50 字以内裙装小注 + 可手动指定今日之裙）：
 * 「设为今日之裙 / 取消指定」按钮文案随钉选态翻转，点击经 [onPin]（VM 内再点同一件即取消）；
 * 锁定件不提供指定入口，只呈「第 N 天解锁 ♡」期待感小签。
 */
@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutfitDetailSheet(
    item: GalleryItemUi,
    spec: ThemeSpec,
    onPin: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = spec.backgroundGradient.first(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 36.dp),
        ) {
            val bitmap = rememberAssetImageOrN(item.outfit.assetFile)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(DETAIL_IMAGE_HEIGHT)
                        .clip(RoundedCornerShape(24.dp))
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
                } else {
                    Text(
                        text = "尚未展出的藏品",
                        color = spec.chipText.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = item.outfit.title,
                color = spec.greetingColor,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.outfit.note,
                color = spec.chipText.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            if (item.unlocked) {
                DetailActionButton(
                    label = if (item.pinned) "取消指定" else "设为今日之裙",
                    spec = spec,
                    onClick = { onPin(item.outfit.id) },
                )
            } else {
                Text(
                    text = "第 ${item.outfit.unlockDay} 天解锁 ♡",
                    color = spec.primary,
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                )
            }
        }
    }
}

/** 弹层主按钮：主题渐变胶囊（与首页「记一杯」同款底），文案随钉选态翻转。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun DetailActionButton(
    label: String,
    spec: ThemeSpec,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(percent = 50))
                .background(Brush.verticalGradient(listOf(spec.buttonTop, spec.buttonBottom)))
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        // P2-4：与首页「记一杯」同款主色渐变底，字色同走 onPrimarySurface（深夜维持白字）。
        Text(text = label, color = onPrimarySurface(spec), style = MaterialTheme.typography.titleMedium)
    }
}

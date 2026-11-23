package com.awakedw.feature.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.HomeHorizontalPadding
import com.awakedw.core.designsystem.MotionTokens
import com.awakedw.core.designsystem.animation.rememberRollingInt
import com.awakedw.core.designsystem.components.AwakeConfirmDialog
import com.awakedw.core.designsystem.components.EditorialPageFooter
import com.awakedw.core.designsystem.components.EditorialPageHeader
import com.awakedw.core.designsystem.components.EditorialSectionLabel
import com.awakedw.core.designsystem.components.editorialFolio
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.LolitaBackdrop
import com.awakedw.core.designsystem.paper.ArtworkPlate
import com.awakedw.core.designsystem.paper.EditorialSheet
import com.awakedw.core.designsystem.paper.editorialCropsOf
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.particles.ParticleDensity
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.designsystem.rememberUiNow
import com.awakedw.core.model.WaterRecord
import com.awakedw.feature.stats.components.MonthHeatmap
import com.awakedw.feature.stats.components.TodayTimeline
import com.awakedw.feature.stats.components.WeekRecordList

/**
 * 统计页（2.5.0 去图表化，回应使用者反馈「统计的意义在于记录，柱状图会增加焦虑」）：
 * 全页只有记录的形态——今日 hero 与细轨、文本事实格、周记录列表（发丝行）、
 * 本月日历（三档：未饮/有记录/达标）与今日记录时间线。柱状图、分布条与年度信纸已撤除，
 * 周统计与月统计足够（使用者确认）。分节标题走微字距小标签。
 *
 * 2.14.0 纸面化（大改版 Phase 2）：接入 `EditorialSheet` 与共享页框，版心 28dp；
 * 页眉 folio、卷首 `ArtworkPlate` 与页脚「AWAKE · 记录本」；英雄数字改衬线。
 * 记录信息层级不变，业务/持久化/导航/权限零改动。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    StatsContent(state, onDeleteRecord = viewModel::deleteRecord)
}

/** Pure display entry point for populated, empty and large-font visual regression. */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun StatsContent(
    state: StatsUiState,
    onDeleteRecord: (Long) -> Unit = {},
) {
    val spec = currentThemeSpec()
    val now = rememberUiNow()
    var pendingDelete by remember { mutableStateOf<WaterRecord?>(null) }
    Box(Modifier.fillMaxSize()) {
        GradientBackdrop(spec, Modifier.matchParentSize())
        LolitaBackdrop(spec, Modifier.matchParentSize())
        FloatingParticles(
            spec.particleColors,
            Modifier.matchParentSize(),
            seed = 11L,
            showFlowers = false,
            showStars = false,
            density = ParticleDensity.QUIET,
        )
        // 纸面层（大改版 Phase 2）：统计从「壁纸上的列表」变成一张可翻阅的记录页。
        // ReadingField 退役——纸本身承担阅读面对比度。
        EditorialSheet(modifier = Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = HomeHorizontalPadding, end = HomeHorizontalPadding, bottom = STATS_BOTTOM_BREATHING),
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        ) {
            Spacer(Modifier.height(8.dp))
            EditorialPageHeader(title = "统计", subtitle = "今天的饮水与近七日变化", now = now)
            ArtworkPlate(
                crop = editorialCropsOf(spec.id).plate,
                caption = "卷首 · ${state.monthLabel.ifBlank { "记录" }}",
                modifier = Modifier.fillMaxWidth(),
            )
            // —— 今日饮水：hero 段（微标签悬在大数字上，Nike 式层级）——
            Text(
                "今日饮水",
                color = spec.greetingSubColor,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = HERO_LABEL_SPACING.sp),
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // 数字滚动全页统一（2.6.0 动效二阶）：与首页英雄数字同一拍——打卡/删除后从旧值滚到新值。
                Text(
                    rememberRollingInt(targetValue = state.badges.totalMl, label = "statsTotalMl").toString(),
                    color = spec.greetingColor,
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
                )
                Text(
                    "ml",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            TodayProgressLine(
                totalMl = state.badges.totalMl,
                goalMl = state.goalMl,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "每日目标 ${state.goalMl}ml",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.weight(1f))
                // 达标是一个事实，不是奖励：只把「已达成」写清楚并提到右端主色位，不引入徽章或连续语义。
                if (state.badges.totalMl >= state.goalMl) {
                    Text("已达成", color = spec.primary, style = MaterialTheme.typography.labelMedium)
                }
            }
            // —— 指标格（Nike 式：每格只压一道顶部发丝线，零盒子零竖线）——
            Row(Modifier.fillMaxWidth()) {
                StatsFact("记录次数", "${state.badges.cupCount} 次", Modifier.weight(1f))
                StatsFact("平均间隔", state.badges.avgIntervalLabel, Modifier.weight(1f))
            }
            // 0.9.0 近七日摘要：与柱状图同源（含今天），是事实陈述、不引入连续或奖励语义。
            Row(Modifier.fillMaxWidth()) {
                StatsFact("近七日合计", StatsMath.weekTotalLabel(state.weekTotalMl), Modifier.weight(1f))
                StatsFact("近七日达标", StatsMath.weekMetDaysLabel(state.bars.map { it.totalMl }, state.goalMl), Modifier.weight(1f))
            }
            // —— 近七日：周记录列表（2.5.0 去图表化）——每天一行发丝行，今天在最上 ——
            EditorialSectionLabel("近七日")
            WeekRecordList(
                bars = state.bars,
                todayKey = state.todayKey,
                modifier = Modifier.fillMaxWidth(),
            )
            // —— 本月（2.1.0 引入，2.5.0 三档降噪）：日历即记录 ——
            if (state.monthCells.isNotEmpty()) {
                EditorialSectionLabel(state.monthLabel)
                MonthHeatmap(
                    cells = state.monthCells,
                    goalMl = state.goalMl,
                    summary = state.monthSummary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // —— 今日记录：发丝行，无卡片 ——
            EditorialSectionLabel("今日记录 · ${state.timeline.size} 次")
            TodayTimeline(
                records = state.timeline,
                onRequestDelete = { record -> pendingDelete = record },
                modifier = Modifier.fillMaxWidth(),
            )
            EditorialPageFooter(
                folio = editorialFolio(now),
                imprint = "AWAKE · 记录本",
                modifier = Modifier.fillMaxWidth().testTag("stats-footer"),
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    pendingDelete?.let { record ->
        AwakeConfirmDialog(
            title = "删除这条记录",
            body = "${record.amountMl}ml 的这条记录会被删掉，今日总数与近七日同步减少。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                onDeleteRecord(record.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** 分节间距（1.5.0）：去卡片后区块靠更大的纵向留白分节——呼吸感来自间距而非框。 */
private val SECTION_GAP = 22.dp

/** 页底呼吸：滚动到底时页脚不贴系统导航栏。 */
private val STATS_BOTTOM_BREATHING = 32.dp

/** Hero 微标签字距：与分节标签同一档耳语。 */
private const val HERO_LABEL_SPACING = 2.5f

/** 细轨粗细与进度头珠点半径（0.9.2）：线上一颗小珍珠，替代 M3 进度条自带的终点圆点。 */
private val TRACK_HEIGHT = 3.dp
private val PEARL_RADIUS = 3.5.dp
private val PEARL_HALO_RADIUS = 5.5.dp

/** 进度低于此值不画珠点与填充：空态的轨道应该是纯粹的空。 */
private const val PEARL_MIN_PROGRESS = 0.015f

/**
 * 今日细轨进度（0.9.2，MyFitnessPal 同款模式）：3dp 圆角细轨 + 主色圆角填充 + 进度头一颗珍珠——
 * 珍珠外圈垫一圈纸色，让它从轨道上轻轻浮起。M3 `LinearProgressIndicator`
 * 自带终点 stop 圆点，空态下也悬在那里，读起来既像滑块又像「已满」，故弃用。
 * 语义等价：`contentDescription` 走 [StatsMath.todayProgressLabel]。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun TodayProgressLine(
    totalMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val reduceMotion = rememberReduceMotion()
    val target = (totalMl.toFloat() / goalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    val progress =
        if (reduceMotion) {
            target
        } else {
            animateFloatAsState(
                targetValue = target,
                // 与数字滚动同族同拍（2.6.0 动效二阶）：同一份「今日总量」的两种呈现，一种节奏。
                animationSpec = tween(durationMillis = MotionTokens.DURATION_NUMBER_ROLL_MS, easing = MotionTokens.EasingStandard),
                label = "todayProgressFill",
            ).value
        }
    Canvas(
        modifier =
            modifier
                .height(14.dp)
                .semantics { contentDescription = StatsMath.todayProgressLabel(totalMl, goalMl) },
    ) {
        val cy = size.height / 2f
        val stroke = TRACK_HEIGHT.toPx()
        drawLine(
            color = spec.ringTrack,
            start = Offset(0f, cy),
            end = Offset(size.width, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        if (progress > PEARL_MIN_PROGRESS) {
            val headX = size.width * progress
            drawLine(
                color = spec.primary,
                start = Offset(0f, cy),
                end = Offset(headX, cy),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawCircle(spec.chipBg, radius = PEARL_HALO_RADIUS.toPx(), center = Offset(headX, cy))
            drawCircle(spec.primary, radius = PEARL_RADIUS.toPx(), center = Offset(headX, cy))
        }
    }
}

/**
 * 指标小格（1.5.0 Nike 式重排）：每格只在顶部压一道发丝线，无盒子无竖线；
 * 标签与数值在等宽格内居中——与首页摘要的等宽居中（0.4.0 规则）同族。
 * 合并语义「标签 值」保持不变，屏幕阅读器一次读完整格。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun StatsFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp)
            .clearAndSetSemantics { contentDescription = "$label $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(spec.laceColor.copy(alpha = 0.28f)))
        Text(label, color = spec.greetingSubColor, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text(value, color = spec.greetingColor, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

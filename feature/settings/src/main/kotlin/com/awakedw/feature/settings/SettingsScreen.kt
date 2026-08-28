package com.awakedw.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.feature.settings.components.IntervalChipsRow
import com.awakedw.feature.settings.components.StepperRow
import com.awakedw.feature.settings.components.ThemeChoiceChips
import com.awakedw.feature.settings.components.ToggleRow
import com.awakedw.feature.settings.components.WindowRangeSlider
import com.awakedw.feature.settings.copyeditor.CopyLibrarySection

/** 分区卡圆角。 */
private val CARD_SHAPE: Shape = RoundedCornerShape(24.dp)

/** 分区内元素的统一行间距。 */
private val SECTION_SPACING = 14.dp

/**
 * 「我的」页（设计规格 §3.4）：四个分区卡 + 白名单引导入口——
 * 目标（± 步进器）、提醒（总开关 / 清醒时段双滑杆 / 间隔档位 chips）、
 * 外观（主题单选 chips）、心意文案库（早/午/晚折叠编辑器）。
 * 所有变更经 [SettingsViewModel] 即时持久化，无保存键。
 *
 * [onOpenWhitelistGuide] 为「省电白名单引导」整行点击的出口：
 * 路由跳转由导航壳（集成任务）接线，本层不感知 NavHost。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onOpenWhitelistGuide: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "我的",
            color = currentThemeSpec().greetingColor,
            style = MaterialTheme.typography.headlineMedium,
        )

        SettingsCard(title = "目标", subtitle = "喝多少、一杯多大，慢慢调") {
            StepperRow(
                label = "每日目标量",
                valueText = "${settings.goalMl}ml",
                canDecrement = SettingsValidation.isValidMl(settings.goalMl - SettingsValidation.ML_STEP),
                canIncrement = SettingsValidation.isValidMl(settings.goalMl + SettingsValidation.ML_STEP),
                onDecrement = { viewModel.setGoalMl(settings.goalMl - SettingsValidation.ML_STEP) },
                onIncrement = { viewModel.setGoalMl(settings.goalMl + SettingsValidation.ML_STEP) },
            )
            StepperRow(
                label = "一杯容量",
                valueText = "${settings.cupMl}ml",
                canDecrement = SettingsValidation.isValidMl(settings.cupMl - SettingsValidation.ML_STEP),
                canIncrement = SettingsValidation.isValidMl(settings.cupMl + SettingsValidation.ML_STEP),
                onDecrement = { viewModel.setCupMl(settings.cupMl - SettingsValidation.ML_STEP) },
                onIncrement = { viewModel.setCupMl(settings.cupMl + SettingsValidation.ML_STEP) },
            )
        }

        SettingsCard(title = "提醒", subtitle = "在她清醒的时间里轻轻叫一声") {
            ToggleRow(
                label = "温柔提醒",
                checked = settings.remindersEnabled,
                onCheckedChange = viewModel::setRemindersEnabled,
            )
            WindowRangeSlider(
                startMin = settings.windowStartMin,
                endMin = settings.windowEndMin,
                onCommit = viewModel::setWindow,
            )
            IntervalChipsRow(selectedMin = settings.intervalMin, onSelect = viewModel::setIntervalMin)
        }

        SettingsCard(title = "外观", subtitle = "跟随时段流转，或停在最喜欢的颜色") {
            ThemeChoiceChips(
                selected = settings.themeChoice,
                onSelect = viewModel::setThemeChoice,
            )
        }

        SettingsCard(title = null, subtitle = null) {
            CopyLibrarySection(
                library = state.library,
                onUpsert = viewModel::upsertCopy,
                onAdd = viewModel::addCopy,
                onDelete = viewModel::deleteCopy,
                onReset = viewModel::resetCopyLibrary,
            )
        }

        GuideEntryRow(onOpenWhitelistGuide = onOpenWhitelistGuide)

        Spacer(Modifier.height(24.dp))
    }
}

/** 分区卡容器：chipBg 圆角面板 + 标题/副标题（[title] 为 null 时仅作容器，如文案库分区自带头部）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun SettingsCard(
    title: String?,
    subtitle: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spec = currentThemeSpec()
    Surface(shape = CARD_SHAPE, color = spec.chipBg, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(SECTION_SPACING)) {
            if (title != null) {
                Column {
                    Text(text = title, color = spec.greetingColor, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) {
                        Text(text = subtitle, color = spec.greetingSubColor, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            content()
        }
    }
}

/** 白名单引导入口（§3.4）：整行点击项，跳转由导航壳接线。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun GuideEntryRow(
    onOpenWhitelistGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Surface(
        shape = CARD_SHAPE,
        color = spec.chipBg,
        onClick = onOpenWhitelistGuide,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Column {
                Text(text = "省电白名单引导", color = spec.greetingColor, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "打开它，提醒会更可靠一点",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(text = "›", color = spec.greetingSubColor, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

package com.awakedw.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.HomeHorizontalPadding
import com.awakedw.core.designsystem.animation.pressScale
import com.awakedw.core.designsystem.components.EditorialPageFooter
import com.awakedw.core.designsystem.components.EditorialPageHeader
import com.awakedw.core.designsystem.components.EditorialSectionLabel
import com.awakedw.core.designsystem.components.editorialFolio
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.LolitaBackdrop
import com.awakedw.core.designsystem.paper.EditorialSheet
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.particles.ParticleDensity
import com.awakedw.core.designsystem.rememberUiNow
import com.awakedw.feature.settings.components.IntervalChipsRow
import com.awakedw.feature.settings.components.StepperRow
import com.awakedw.feature.settings.components.ThemePickerEntry
import com.awakedw.feature.settings.components.ToggleRow
import com.awakedw.feature.settings.components.WindowRangeSlider
import com.awakedw.feature.settings.copyeditor.CopyLibrarySection

/** 本页漂浮粒子的随机种子：与首页/统计页各不相同，保证各屏粒子排布有别。 */
private const val SETTINGS_PARTICLE_SEED = 13L

/** 分节间距：与统计页同一版面骨架——去卡片后的呼吸来自间距，而不是盒子。 */
private val SETTINGS_SECTION_GAP = 20.dp

/** 分节内行距：发丝线两侧留一指宽，行与行不挤在一起。 */
private val SETTINGS_ROW_GAP = 12.dp

/** 页底呼吸：滚动到底时最后一行不贴系统导航栏。 */
private val SETTINGS_BOTTOM_BREATHING = 32.dp

/**
 * 「我的」页（设计规格 §3.4）：分区卡 + 白名单引导入口——
 * 目标（± 步进器）、提醒（总开关 / 清醒时段双滑杆 / 间隔档位 chips）、
 * 外观（主题单选 chips）、声音（音效开关，任务 12）、心意文案库（早/午/晚折叠编辑器）。
 * 背景为全局渐变底座 + 漂浮粒子（规格 §2.2，与其他各屏同一份主题呼吸）。
 * 所有变更经 [SettingsViewModel] 即时持久化，无保存键。
 *
 * 2.10.0 编辑部化（视觉基线 §26）：页面不再使用 `PaperPanel` 四连卡，改为
 * 「`EditorialSectionLabel` 微标签 + 右延发丝线 + 内容直接落在纸面」——
 * 与统计页自 1.5.0 起确认的版面语言同一套。业务、控件行为与持久化键零改动。
 *
 * 2.14.0 纸面化（大改版 Phase 2）：接入 `EditorialSheet` 与共享页眉/页脚；
 * 步进、开关、时段滑杆、间隔档位换成键帽/铜扣/刻度尺/印章票。行为、默认值、
 * 校验、提醒调度与持久化键不变。
 *
 * [onOpenWhitelistGuide] 为「让提醒更稳定」整行点击的出口：
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
    val spec = currentThemeSpec()
    val context = LocalContext.current

    // 「测试提醒」的通知权限引导（§11.4）：Android 13+ 未授权先弹系统请求，授权后立即试发。
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.testReminder()
        }

    fun tryTestReminder(viewModel: SettingsViewModel) {
        val granted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.testReminder()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        LolitaBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        FloatingParticles(
            colors = spec.particleColors,
            modifier = Modifier.matchParentSize(),
            seed = SETTINGS_PARTICLE_SEED,
            showStars = false,
            showFlowers = false,
            density = ParticleDensity.QUIET,
        )
        // 纸面层（大改版 Phase 2）：设置从「壁纸上的表单」变成一页器物清单。
        EditorialSheet(modifier = Modifier.matchParentSize())

        val now = rememberUiNow()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = HomeHorizontalPadding, end = HomeHorizontalPadding, bottom = SETTINGS_BOTTOM_BREATHING),
            verticalArrangement = Arrangement.spacedBy(SETTINGS_SECTION_GAP),
        ) {
            Spacer(Modifier.height(8.dp))
            EditorialPageHeader(title = "我的", subtitle = "修改后自动保存", now = now)

            SettingsSection(title = "饮水目标") {
                // 两个步进器各自独占整行（并排在窄屏会把标签/数值/按钮挤到换行错位）。
                StepperRow(
                    label = "每日目标量",
                    valueText = "${settings.goalMl}ml",
                    canDecrement = SettingsValidation.isValidMl(settings.goalMl - SettingsValidation.ML_STEP),
                    canIncrement = SettingsValidation.isValidMl(settings.goalMl + SettingsValidation.ML_STEP),
                    onDecrement = { viewModel.stepGoalMl(-SettingsValidation.ML_STEP) },
                    onIncrement = { viewModel.stepGoalMl(+SettingsValidation.ML_STEP) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SettingsDivider()
                StepperRow(
                    label = "一杯容量",
                    valueText = "${settings.cupMl}ml",
                    canDecrement = SettingsValidation.isValidMl(settings.cupMl - SettingsValidation.ML_STEP),
                    canIncrement = SettingsValidation.isValidMl(settings.cupMl + SettingsValidation.ML_STEP),
                    onDecrement = { viewModel.stepCupMl(-SettingsValidation.ML_STEP) },
                    onIncrement = { viewModel.stepCupMl(+SettingsValidation.ML_STEP) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingsSection(title = "提醒") {
                ReminderStatusRow(
                    statusLabel = state.reminderStatusLabel,
                    armed = state.reminderArmed,
                    testSent = state.testReminderSent,
                    onTest = { tryTestReminder(viewModel) },
                )
                SettingsDivider()
                ToggleRow(
                    label = "喝水提醒",
                    checked = settings.remindersEnabled,
                    onCheckedChange = viewModel::setRemindersEnabled,
                )
                SettingsDivider()
                WindowRangeSlider(
                    startMin = settings.windowStartMin,
                    endMin = settings.windowEndMin,
                    onCommit = viewModel::setWindow,
                )
                Text("提醒间隔 · 分钟", color = spec.greetingSubColor, style = MaterialTheme.typography.labelMedium)
                IntervalChipsRow(selectedMin = settings.intervalMin, onSelect = viewModel::setIntervalMin)
            }

            SettingsSection(title = "外观与声音") {
                ThemePickerEntry(selected = settings.themeChoice, onSelect = viewModel::setThemeChoice)
                SettingsDivider()
                ToggleRow(
                    label = "音效",
                    supporting = "系统静音时不播放",
                    checked = state.soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled,
                )
            }

            CopyLibrarySection(
                library = state.library,
                onUpsert = viewModel::upsertCopy,
                onAdd = viewModel::addCopy,
                onDelete = viewModel::deleteCopy,
                onReset = viewModel::resetCopyLibrary,
            )

            SettingsSection(title = "提醒稳定性") {
                GuideEntryRow(onOpenWhitelistGuide = onOpenWhitelistGuide)
            }

            EditorialPageFooter(
                folio = editorialFolio(now),
                imprint = "AWAKE · 设置",
                modifier = Modifier.fillMaxWidth().testTag("settings-footer"),
            )
        }
    }
}

/**
 * 分节骨架：微标签 + 右延发丝线，内容直接落在纸面上。
 * 与统计页共用 [EditorialSectionLabel]（2.10.0 从统计页私有实现提升）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SETTINGS_ROW_GAP)) {
        EditorialSectionLabel(title)
        content()
    }
}

/** 分区内的行间发丝线：代替卡片内边距，让同一分区仍读得成一组。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun SettingsDivider() {
    val spec = currentThemeSpec()
    Box(Modifier.fillMaxWidth().height(1.dp).background(spec.laceColor.copy(alpha = 0.22f)))
}

/** 提醒透明化状态行（§11.3/11.4）：状态圆点 + 文案 + 「测试提醒」即时验证入口（去盒）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun ReminderStatusRow(
    statusLabel: String,
    armed: Boolean,
    testSent: Boolean,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val view = LocalView.current
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = ControlMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(if (armed) spec.primary else spec.greetingSubColor, CircleShape))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(statusLabel, color = spec.greetingColor, style = MaterialTheme.typography.bodySmall)
            if (testSent) {
                Text(
                    "提醒已发出，请查看通知栏",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .heightIn(min = ControlMinHeight)
                    .pressScale(interactionSource)
                    .clip(RoundedCornerShape(3.dp))
                    .background(spec.ringTrack.copy(alpha = 0.18f))
                    .border(1.dp, spec.laceColor.copy(alpha = 0.32f), RoundedCornerShape(3.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onTest()
                        },
                    )
                    .padding(horizontal = 12.dp)
                    .testTag("settings-test-reminder"),
            contentAlignment = Alignment.Center,
        ) {
            Text("测试提醒", color = spec.greetingColor, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * 白名单引导入口（§3.4）：整行点击项，跳转由导航壳接线。
 * 2.10.0 去卡：不再是一张独立 Surface，而是「提醒稳定性」分节下的一行；
 * 按压走共享 `pressScale`（§24），触控高度守住 `ControlMinHeight`。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun GuideEntryRow(
    onOpenWhitelistGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .pressScale(interactionSource)
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onOpenWhitelistGuide,
                )
                .heightIn(min = 56.dp)
                .padding(horizontal = 4.dp, vertical = 12.dp)
                .testTag("settings-guide-entry"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "让提醒更稳定", color = spec.greetingColor, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "打开系统设置，允许后台提醒",
                color = spec.greetingSubColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = spec.greetingSubColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

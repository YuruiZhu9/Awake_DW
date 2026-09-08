package com.awakedw.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.PagePadding
import com.awakedw.core.designsystem.SurfaceCornerRadius
import com.awakedw.core.designsystem.components.EditorialHeader
import com.awakedw.core.designsystem.components.PaperPanel
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.LolitaBackdrop
import com.awakedw.core.designsystem.lolita.artworkPanelOpacity
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.particles.ParticleDensity
import com.awakedw.feature.settings.components.IntervalChipsRow
import com.awakedw.feature.settings.components.StepperRow
import com.awakedw.feature.settings.components.ThemePickerEntry
import com.awakedw.feature.settings.components.ToggleRow
import com.awakedw.feature.settings.components.WindowRangeSlider
import com.awakedw.feature.settings.copyeditor.CopyLibrarySection

private val CARD_SHAPE: Shape = RoundedCornerShape(SurfaceCornerRadius)

/** 本页漂浮粒子的随机种子：与首页/统计页各不相同，保证各屏粒子排布有别。 */
private const val SETTINGS_PARTICLE_SEED = 13L

/**
 * 「我的」页（设计规格 §3.4）：分区卡 + 白名单引导入口——
 * 目标（± 步进器）、提醒（总开关 / 清醒时段双滑杆 / 间隔档位 chips）、
 * 外观（主题单选 chips）、声音（音效开关，任务 12）、心意文案库（早/午/晚折叠编辑器）。
 * 背景为全局渐变底座 + 漂浮粒子（规格 §2.2，与其他各屏同一份主题呼吸）。
 * 所有变更经 [SettingsViewModel] 即时持久化，无保存键。
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

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PagePadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            EditorialHeader("我的", "修改后自动保存", Icons.Rounded.Settings)

            SettingsCard(title = "饮水目标", subtitle = null) {
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

            SettingsCard(title = "喝水提醒", subtitle = null) {
                ReminderStatusRow(
                    statusLabel = state.reminderStatusLabel,
                    armed = state.reminderArmed,
                    testSent = state.testReminderSent,
                    onTest = { tryTestReminder(viewModel) },
                )
                ToggleRow(
                    label = "喝水提醒",
                    checked = settings.remindersEnabled,
                    onCheckedChange = viewModel::setRemindersEnabled,
                )
                WindowRangeSlider(
                    startMin = settings.windowStartMin,
                    endMin = settings.windowEndMin,
                    onCommit = viewModel::setWindow,
                )
                Text("提醒间隔 · 分钟", color = spec.greetingSubColor, style = MaterialTheme.typography.labelMedium)
                IntervalChipsRow(selectedMin = settings.intervalMin, onSelect = viewModel::setIntervalMin)
            }

            SettingsCard(title = "外观与声音", subtitle = null) {
                ThemePickerEntry(selected = settings.themeChoice, onSelect = viewModel::setThemeChoice)
                ToggleRow(
                    label = "音效",
                    supporting = "系统静音时不播放",
                    checked = state.soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled,
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
}

/** 分区卡容器：chipBg 圆角面板 + 标题/副标题（[title] 为 null 时仅作容器，如文案库分区自带头部）。
 * 质感（§10.4）：2dp 柔阴影 + 主题色 8% 细描边，替纯平面色块。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun SettingsCard(
    title: String?,
    subtitle: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PaperPanel(modifier = modifier, title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (subtitle != null) Text(subtitle, color = currentThemeSpec().greetingSubColor, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

/** 提醒透明化状态行（§11.3/11.4）：状态圆点 + 文案 + 「测试提醒」即时验证入口。 */
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
    Surface(shape = RoundedCornerShape(14.dp), color = spec.ringTrack.copy(alpha = 0.16f), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(if (armed) spec.primary else spec.greetingSubColor, CircleShape))
                Text(
                    statusLabel,
                    color = spec.greetingColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                androidx.compose.material3.TextButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onTest()
                }) { Text("测试提醒", color = spec.greetingColor, style = MaterialTheme.typography.labelMedium) }
            }
            if (testSent) {
                Text(
                    "提醒已发出，请查看通知栏",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
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
        color = spec.chipBg.copy(alpha = artworkPanelOpacity(spec.id, 0.64f)),
        onClick = onOpenWhitelistGuide,
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 1.dp,
        border = BorderStroke(width = 1.dp, color = spec.laceColor.copy(alpha = 0.42f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "让提醒更稳定", color = spec.greetingColor, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "打开系统设置，允许后台提醒",
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(text = "›", color = spec.greetingSubColor, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

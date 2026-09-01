package com.awakedw.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.feature.settings.components.IntervalChipsRow
import com.awakedw.feature.settings.components.StepperRow
import com.awakedw.feature.settings.components.ThemeChoiceChips
import com.awakedw.feature.settings.components.ToggleRow
import com.awakedw.feature.settings.components.WindowRangeSlider
import com.awakedw.feature.settings.copyeditor.CopyLibrarySection
import kotlin.math.roundToInt

// 卡形保持圆角矩形：自定义 Shape 的 outline 会干扰触摸注入的命中路径（Robolectric 实测，
// 语义动作正常而位置点击失效）——蕾丝扇贝改为卡顶饰带绘制层实现（见 LaceTrim），观感等价且零交互风险。
private val CARD_SHAPE: Shape = RoundedCornerShape(24.dp)

/** 分区内元素的统一行间距。 */
private val SECTION_SPACING = 14.dp

/** 本页漂浮粒子的随机种子：与首页/统计页各不相同，保证各屏粒子排布有别。 */
private const val SETTINGS_PARTICLE_SEED = 13L

/**
 * 「我的」页（设计规格 §3.4）：分区卡 + 白名单引导入口——
 * 目标（± 步进器）、提醒（总开关 / 清醒时段双滑杆 / 间隔档位 chips）、
 * 外观（主题单选 chips）、声音（音效开关，任务 12）、心意文案库（早/午/晚折叠编辑器）。
 * 背景为全局渐变底座 + 漂浮粒子（规格 §2.2，与其他各屏同一份主题呼吸）。
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
    val spec = currentThemeSpec()
    val context = LocalContext.current

    // 「试一试」的通知权限引导（§11.4）：Android 13+ 未授权先弹系统请求，授权后立即试发。
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
        FloatingParticles(
            colors = spec.particleColors,
            modifier = Modifier.matchParentSize(),
            seed = SETTINGS_PARTICLE_SEED,
        )

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
                color = spec.greetingColor,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            )

            SettingsCard(title = "目标", subtitle = "喝多少、一杯多大，慢慢调") {
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

            SettingsCard(title = "提醒", subtitle = "在我清醒的时间里轻轻叫一声") {
                ReminderStatusRow(
                    statusLabel = state.reminderStatusLabel,
                    armed = state.reminderArmed,
                    testSent = state.testReminderSent,
                    onTest = { tryTestReminder(viewModel) },
                )
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

            SettingsCard(title = "声音", subtitle = null) {
                ToggleRow(
                    label = "音效",
                    supporting = "水滴与八音盒；系统静音时自动安静",
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
    val spec = currentThemeSpec()
    Surface(
        shape = CARD_SHAPE,
        color = spec.chipBg,
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
        border = BorderStroke(width = 1.dp, color = spec.laceColor),
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        ) {
            LaceTrim(spec = spec)
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

/**
 * 蕾丝饰带（§12 L1）：卡顶一排相切的圆弧点带，主题蕾丝线着色。
 * 绘制层实现而非卡片 Shape——自定义 outline 会干扰触摸注入的命中路径（Robolectric 实测：
 * 语义动作正常而位置点击失效），饰带观感等价且零交互风险。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun LaceTrim(
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(8.dp)) {
        val radius = 4.dp.toPx()
        val count = (size.width / (radius * 2f)).roundToInt().coerceAtLeast(2)
        val step = size.width / count
        repeat(count) { i ->
            drawCircle(color = spec.laceColor, radius = radius, center = Offset(step * (i + 0.5f), radius))
        }
    }
}

/** 提醒透明化状态行（§11.3/11.4）：状态圆点 + 文案 + 「试一试」即时验证入口。 */
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
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .background(
                        color = if (armed) spec.primary else spec.chipText.copy(alpha = 0.4f),
                        shape = CircleShape,
                    ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = statusLabel,
            color = if (armed) spec.greetingColor else spec.greetingSubColor,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        if (testSent) {
            Text(
                text = "已发送，看看通知栏 ♪",
                color = spec.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        } else {
            Text(
                text = "试一试",
                color = spec.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier =
                    Modifier.clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onTest()
                    }.padding(start = 12.dp),
            )
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
        shadowElevation = 2.dp,
        border = BorderStroke(width = 1.dp, color = spec.laceColor),
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

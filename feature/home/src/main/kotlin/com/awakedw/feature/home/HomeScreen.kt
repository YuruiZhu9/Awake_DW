package com.awakedw.feature.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.animation.FadeUpOnce
import com.awakedw.core.designsystem.art.CatFigure
import com.awakedw.core.designsystem.art.DressBackdrop
import com.awakedw.core.designsystem.art.LightPocket
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.GOLD_TRIM
import com.awakedw.core.designsystem.lolita.drawBow
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.ring.ProgressRing
import com.awakedw.core.model.CatAccessory
import com.awakedw.core.model.CatMood
import com.awakedw.feature.home.components.BadgesRow
import com.awakedw.feature.home.components.BowEntryButton
import com.awakedw.feature.home.components.CelebrationOverlay
import com.awakedw.feature.home.components.Greeting
import com.awakedw.feature.home.components.HealthTipLine
import com.awakedw.feature.home.components.LogButton
import com.awakedw.feature.home.components.PraiseLine
import com.awakedw.feature.home.components.QuickSipsRow

/** 首页进度环直径：开屏形序段（SplashMorph）以它为涟漪终态半径，改值需与开屏同步观感。 */
val HOME_RING_DIAMETER = 220.dp

/** 环心数字滚动时长（规格 §4.2 第 3 步：~500ms）。 */
private const val NUMBER_ROLL_MS = 500

/** 达标微光呼吸的 alpha 区间与单程时长。 */
private const val GLOW_ALPHA_MIN = 0.10f
private const val GLOW_ALPHA_MAX = 0.26f
private const val GLOW_BREATH_MS = 1600

/** 环顶蝴蝶结尺寸与上移量（§12）：结饰骑在环 stroke 上。 */
private val BOW_WIDTH = 46.dp
private val BOW_HEIGHT = 28.dp
private val BOW_LIFT = 2.dp

/** 猫语气泡宽：容纳一句胆大王短语，悬于猫上方居中（复用 PraiseLine 的浮现样式）。 */
private val CAT_LINE_BUBBLE_WIDTH = 168.dp

/** 「记一杯」按钮光袋尺寸（96–160dp 区间取值）：呼吸光晕衬在按钮后方的浅浅一汪光。 */
private val LOG_BUTTON_POCKET_WIDTH = 160.dp
private val LOG_BUTTON_POCKET_HEIGHT = 96.dp

/** 胆大王光袋直径（96–160dp 区间取值）：给 96dp 立绘留一圈 16dp 的呼吸光晕。 */
private val CAT_POCKET_DIAMETER = 128.dp

/** 胆大王落角的边距：底部 leading 角，「记一杯」按钮（居中）同行对侧。 */
private val CAT_CORNER_PADDING = PaddingValues(start = 12.dp, bottom = 24.dp)

/**
 * 治愈打卡首页（规格 §3.2 自上而下：问候 → 进度环 → 统计徽章 → 健康贴士 → 「记一杯」按钮）：
 * 可点按进度环居中承重，夸夸语在环下方浮现（§4.2 第 5 步），达标后满环微光呼吸；
 * 底座为渐变背景 + 画卷层（moodboard §5.1 今日之裙，[DressBackdrop]）+ 漂浮粒子；
 * 问候语行右端挂 [BowEntryButton] 蝴蝶结衣橱入口（§5.2 重设计：今日穿搭信息回归衣橱页呈现，
 * 首页不再常驻穿搭文字），打卡新解锁时问候语下方浮出「新裙入柜」瞬时轻提示；
 * 胆大王常驻底部 leading 角（moodboard §6.2），猫语气泡悬于其上，摸猫即回应。
 * 打卡反馈 6 步时序由 [HomeViewModel] 与本层协同完成（规格 §4.2）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenGallery: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val spec = currentThemeSpec()
    val view = LocalView.current

    // 达标庆祝瞬间的一次轻震（§10.3）：与横幅浮现同拍，克制不喧哗。
    LaunchedEffect(state.celebrating) {
        if (state.celebrating) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        // 画卷层（moodboard §5.1）：渐变之上、内容之下；今日之裙未就绪或资产缺失时不绘制。
        DressBackdrop(outfit = state.todayOutfit, modifier = Modifier.matchParentSize())
        FloatingParticles(colors = spec.particleColors, modifier = Modifier.matchParentSize())

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(44.dp))
            // 问候语行（§5.2 重设计）：问候块占满左域（文字仍居中），右端与日期副行同行挂蝴蝶结衣橱入口。
            FadeUpOnce {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Greeting(
                        customGreeting = state.greeting,
                        totalMl = state.totalMl,
                        goalMl = state.goalMl,
                        modifier = Modifier.weight(1f),
                    )
                    BowEntryButton(onOpenGallery = onOpenGallery)
                }
            }
            // 打卡新解锁的瞬时轻提示（§5.2，反馈非常驻文案）：问候语下方浮出「新裙入柜 ♡」，
            // 2.5s 后由 ViewModel 收场；无解锁时 [PraiseLine] 留白占位，下方环块不跳位。
            PraiseLine(text = state.newUnlock?.let { "新裙入柜 ♡ ${it.title}" })
            Spacer(Modifier.height(20.dp))
            RingBlock(
                progress = state.progress,
                totalMl = state.totalMl,
                onRingTap = viewModel::tapRing,
            )
            Spacer(Modifier.height(12.dp))
            PraiseLine(text = state.praiseLine)
            Spacer(Modifier.height(20.dp))
            FadeUpOnce(delayMillis = 80) {
                BadgesRow(
                    cupCount = state.cupCount,
                    avgIntervalLabel = state.avgIntervalLabel,
                    lastDrinkLabel = state.lastDrinkLabel,
                    streakDays = state.streakDays,
                )
            }
            Spacer(Modifier.height(12.dp))
            FadeUpOnce(delayMillis = 120) { HealthTipLine() }
            Spacer(Modifier.weight(1f))
            QuickSipsRow(cupMl = state.cupMl, onQuickLog = viewModel::quickLog)
            Spacer(Modifier.height(12.dp))
            // 光袋（moodboard §2 光·遇）：按钮后方的呼吸光晕（96–160dp），绘制在按钮之下、背景之上。
            Box(contentAlignment = Alignment.Center) {
                LightPocket(modifier = Modifier.size(width = LOG_BUTTON_POCKET_WIDTH, height = LOG_BUTTON_POCKET_HEIGHT))
                LogButton(themeId = state.themeId, onTap = viewModel::tapLogButton)
            }
            Spacer(Modifier.height(36.dp))
        }

        CelebrationOverlay(visible = state.celebrating, modifier = Modifier.matchParentSize())

        // 胆大王常驻（moodboard §6.2）：底部 leading 角，「记一杯」按钮同行对侧；
        // 猫语气泡悬于猫上方（独立于环下夸夸语位置），点击立绘即摸猫（viewModel::petCat）。
        CatCorner(
            mood = state.catMood,
            accessories = state.catAccessories,
            line = state.catLine,
            onPet = viewModel::petCat,
            modifier = Modifier.align(Alignment.BottomStart).padding(CAT_CORNER_PADDING),
        )
    }
}

/**
 * 胆大王角落（moodboard §6.2）：猫语气泡 + 立绘 + 立绘后方的呼吸光袋（moodboard §2 光·遇）。
 * 气泡复用 [PraiseLine] 的 Crossfade 浮现样式，悬于猫上方、与猫列对齐（独立于环下夸夸语位置）；
 * 立绘常驻不缺席（治愈铁律：mood 任何状态都渲染），点击任意处触发 [onPet]（摸猫）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun CatCorner(
    mood: CatMood,
    accessories: List<CatAccessory>,
    line: String?,
    onPet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.width(CAT_LINE_BUBBLE_WIDTH)) {
            PraiseLine(text = line)
        }
        // 光袋（moodboard §2 光·遇）：立绘后方的呼吸光晕（96–160dp），光在猫下、不压猫。
        Box(contentAlignment = Alignment.Center) {
            LightPocket(modifier = Modifier.size(CAT_POCKET_DIAMETER))
            CatFigure(mood = mood, accessories = accessories, onPet = onPet)
        }
    }
}

/** 进度环区块：达标后满环微光呼吸 + 可点按环体 + 环心数字滚动 + 12 点方向蝴蝶结（§12）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun RingBlock(
    progress: Float,
    totalMl: Int,
    onRingTap: (Offset?) -> Unit,
) {
    var ringCenter by remember { mutableStateOf<Offset?>(null) }

    Box(contentAlignment = Alignment.Center) {
        if (progress >= 1f) {
            BreathingGlow()
        }
        ProgressRing(
            progress = progress,
            modifier =
                Modifier
                    .size(HOME_RING_DIAMETER)
                    .onGloballyPositioned { coordinates ->
                        ringCenter = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                    },
            onRingTap = { onRingTap(ringCenter) },
        ) {
            RingCenterContent(totalMl = totalMl)
        }
        Box(Modifier.matchParentSize()) {
            RingBow(goalMet = progress >= 1f, modifier = Modifier.align(Alignment.TopCenter).offset(y = -BOW_LIFT))
        }
    }
}

/**
 * 环顶蝴蝶结（§12 L1）：系在 12 点方向值弧起点——「今日从蝴蝶结开始」。
 * 平时静止；达标呼吸期间随微光同步轻摆（±6°，与 BreathingGlow 同拍），并垂下双尾飘带。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun RingBow(
    goalMet: Boolean,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val transition = rememberInfiniteTransition(label = "bowSway")
    val sway by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = GLOW_BREATH_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "bowSwayAngle",
    )
    Canvas(
        modifier =
            modifier
                .size(BOW_WIDTH, BOW_HEIGHT)
                .graphicsLayer { rotationZ = if (goalMet) sway else 0f },
    ) {
        drawBow(
            center = Offset(size.width / 2f, size.height / 2f),
            width = size.width * 0.72f,
            color = spec.primary,
            knotColor = GOLD_TRIM,
            withTails = goalMet,
        )
    }
}

/** 环心：滚动到新值的总量 + 「今日已喝」小字（规格 §3.2 第 2 条）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun RingCenterContent(totalMl: Int) {
    val spec = currentThemeSpec()
    val rolledTotal by animateIntAsState(
        targetValue = totalMl,
        animationSpec = tween(durationMillis = NUMBER_ROLL_MS, easing = FastOutSlowInEasing),
        label = "ringTotalMl",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${rolledTotal}ml",
            color = spec.ringValueText,
            // 环心排版（§10.4）：数值略收紧字距提精气神，与下方拉开字距的小字形成层次。
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        )
        Spacer(Modifier.height(4.dp))
        // 环心珍珠分隔点（§12）：三枚渐次大小的小珍珠，柔化数字与小字的过渡。
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            PearlDot(size = 3.dp, spec = spec)
            PearlDot(size = 5.dp, spec = spec)
            PearlDot(size = 3.dp, spec = spec)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "今日已喝",
            color = spec.ringValueText.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
        )
    }
}

/** 小珍珠点（§12）：主题环值文字色的柔和圆点。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun PearlDot(
    size: Dp,
    spec: ThemeSpec,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .background(color = spec.ringValueText.copy(alpha = 0.45f), shape = CircleShape),
    )
}

/** 满环微光呼吸（规格 §4.2 第 6 步「满环微光呼吸」）：柔光晕在环后缓缓起伏。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun BreathingGlow() {
    val spec = currentThemeSpec()
    val transition = rememberInfiniteTransition(label = "goalGlow")
    val glowAlpha by transition.animateFloat(
        initialValue = GLOW_ALPHA_MIN,
        targetValue = GLOW_ALPHA_MAX,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = GLOW_BREATH_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "goalGlowAlpha",
    )
    Box(
        modifier =
            Modifier
                .size(HOME_RING_DIAMETER)
                .drawBehind {
                    drawCircle(color = spec.haloColor.copy(alpha = glowAlpha), radius = size.minDimension / 2f)
                },
    )
}

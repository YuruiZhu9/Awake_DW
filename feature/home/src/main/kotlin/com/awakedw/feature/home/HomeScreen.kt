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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.awakedw.core.designsystem.HomeHorizontalPadding
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.animation.FadeUpOnce
import com.awakedw.core.designsystem.art.CatFigure
import com.awakedw.core.designsystem.art.LightPocket
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.GOLD_TRIM
import com.awakedw.core.designsystem.lolita.LolitaRule
import com.awakedw.core.designsystem.lolita.drawBow
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.designsystem.ring.ProgressRing
import com.awakedw.core.model.CatMood
import com.awakedw.feature.home.components.BadgesRow
import com.awakedw.feature.home.components.Greeting
import com.awakedw.feature.home.components.HomeActionDeck
import com.awakedw.feature.home.components.PraiseLine

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

/** 「记一杯」按钮光袋尺寸（96–160dp 区间取值）：呼吸光晕衬在按钮后方的浅浅一汪光。 */
private val LOG_BUTTON_POCKET_WIDTH = 160.dp
private val LOG_BUTTON_POCKET_HEIGHT = 96.dp

/** 胆大王光袋直径（96–160dp 区间取值）：给 96dp 立绘留一圈轻薄呼吸光晕。 */
private val CAT_POCKET_DIAMETER = 120.dp

/**
 * 胆大王落角的边距（布局审计 P1-1 + 审查修复几何重定位）：底部 leading 角，落进居中簇下方的空带——
 * 内容列尾呼吸 132dp 使「记一杯」与 48dp 快捷量行整体高于猫盒；
 * bottom 8dp 使猫盒（y≈8–104）与按钮带下缘 132 保持 20dp 互斥余量，且簇距底固定、滚动任何位置都不变。
 * start 4dp，猫钉在列首不随气泡变宽右移。
 */
private val CAT_CORNER_PADDING = PaddingValues(start = 4.dp, bottom = 8.dp)

/** 环下夸夸语的悬浮落差（布局审计 P1-2）：从环底缘垂下 12dp，浮在既有空档带上，不挤压徽章行。 */
private val PRAISE_LINE_DROP = 12.dp

/**
 * 内容列尾呼吸（布局审计 P1-7 + 审查修复）：整列可滚后列尾固定留白，给「记一杯」按钮与猫角收尾
 * （≈快捷胶囊行 30 + 间距 12 + 按钮 58 + 猫带互斥余量 12 的满滚抵底 clearance——
 * 该值同时为滚动到底态的操作组保留 20dp 视觉余量，与猫盒上缘保持互斥。
 */
private val CONTENT_TAIL_BREATHING = 132.dp

/**
 * Water logging home screen: greeting, progress ring, supportive copy, quick amounts,
 * the primary log action, and an optional mascot response.
 *
 * Visual decoration stays subordinate to the water task. The bow on the ring is a
 * Lolita-inspired accent, not a navigation affordance or reward signal.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val spec = currentThemeSpec()
    val view = LocalView.current

    // 达标庆祝瞬间的一次轻震（§10.3）：与横幅浮现同拍，克制不喧哗。
    LaunchedEffect(state.celebrating) {
        if (state.celebrating) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        // 轻量装饰层：渐变之上、内容之下；只提供主题氛围，不表达“今日内容”。
        FloatingParticles(
            colors = spec.particleColors,
            modifier = Modifier.matchParentSize(),
            showFlowers = false,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = HomeHorizontalPadding, end = HomeHorizontalPadding, bottom = CONTENT_TAIL_BREATHING),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(44.dp))
            // 问候语行（§5.2 重设计 + 审查修复）：Box 叠层——问候语真居中（fillMaxWidth，与下方进度环同轴），
            // 装饰锚点不参与导航，也不挤占问候语的可视宽度；
            FadeUpOnce {
                Greeting(
                    customGreeting = state.greeting,
                    totalMl = state.totalMl,
                    goalMl = state.goalMl,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            LolitaRule(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp))
            Spacer(Modifier.height(10.dp))
            RingBlock(
                progress = state.progress,
                totalMl = state.totalMl,
                praiseLine = state.praiseLine,
                onRingTap = viewModel::tapRing,
            )
            Spacer(Modifier.height(18.dp))
            // 主操作组紧跟进度环：立即记录是第一层级，快捷饮量是同组的次级路径。
            FadeUpOnce(delayMillis = 80) {
                HomeActionDeck(
                    cupMl = state.cupMl,
                    onLog = viewModel::tapLogButton,
                    onQuickLog = viewModel::quickLog,
                )
            }
            Spacer(Modifier.height(18.dp))
            FadeUpOnce(delayMillis = 140) {
                BadgesRow(
                    cupCount = state.cupCount,
                    avgIntervalLabel = state.avgIntervalLabel,
                    lastDrinkLabel = state.lastDrinkLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 列尾呼吸由 padding(bottom = CONTENT_TAIL_BREATHING) 提供，确保窄屏滚动后操作组与猫角互不拥挤。
        }

        // 胆大王常驻：底部 leading 角叠层挂载、不随内容滚动；
        // 猫语气泡悬于猫上方（独立于环下夸夸语位置），点击立绘即摸猫（viewModel::petCat）。
        CatCorner(
            mood = state.catMood,
            line = state.catLine,
            onPet = viewModel::petCat,
            modifier = Modifier.align(Alignment.BottomStart).padding(CAT_CORNER_PADDING),
        )
    }
}

/**
 * 胆大王角落：立绘 + 立绘后方的呼吸光袋 + 猫语气泡。
 * 三者同处底部空带（y≈8–104）：光袋与立绘居中起始，气泡从猫盒右缘开始（start 104dp 起，
 * 多行时上下越出的仍是空带——按钮带自 112dp 起，互斥）；气泡叠绘于猫之上（Box 后绘者在上），
 * 复用 [PraiseLine] 的 multiLine 浮现样式（宽度随内容上限 200dp、行数不限、零占位）。
 * 立绘常驻不缺席（治愈铁律：mood 任何状态都渲染），点击任意处触发 [onPet]（摸猫）。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun CatCorner(
    mood: CatMood,
    line: String?,
    onPet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        // 光袋：立绘后方的轻呼吸光晕（96–160dp 区间取 120dp），光在猫下、不压猫。
        Box(contentAlignment = Alignment.Center) {
            LightPocket(modifier = Modifier.size(CAT_POCKET_DIAMETER))
            CatFigure(mood = mood, onPet = onPet)
        }
        PraiseLine(
            text = line,
            multiLine = true,
            // 气泡从猫盒右缘开始，避免压住脸部与尾巴；小屏下仍留出 4dp 右侧余量。
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 104.dp),
        )
    }
}

/** 进度环区块：达标后满环微光呼吸 + 可点按环体 + 环心数字滚动 + 12 点方向蝴蝶结（§12）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun RingBlock(
    progress: Float,
    totalMl: Int,
    praiseLine: String?,
    onRingTap: (Offset?) -> Unit,
) {
    var ringCenter by remember { mutableStateOf<Offset?>(null) }
    val reduceMotion = rememberReduceMotion()

    Box(contentAlignment = Alignment.Center) {
        if (progress >= 1f) {
            BreathingGlow(reduceMotion = reduceMotion)
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
            RingCenterContent(totalMl = totalMl, reduceMotion = reduceMotion)
        }
        Box(Modifier.matchParentSize()) {
            RingBow(
                goalMet = progress >= 1f,
                reduceMotion = reduceMotion,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = -BOW_LIFT),
            )
        }
        // 夸夸语改叠层挂载（布局审计 P1-2）：从环底缘垂下悬浮，不再占列内 26dp 常驻高度——
        // 无文案时零占位，浮现时不挤压下方徽章行。
        PraiseLine(
            text = praiseLine,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = PRAISE_LINE_DROP),
        )
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
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val sway =
        if (reduceMotion) {
            0f
        } else {
            val transition = rememberInfiniteTransition(label = "bowSway")
            val animatedSway by transition.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = GLOW_BREATH_MS, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "bowSwayAngle",
            )
            animatedSway
        }
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
private fun RingCenterContent(
    totalMl: Int,
    reduceMotion: Boolean,
) {
    val spec = currentThemeSpec()
    val rolledTotal =
        if (reduceMotion) {
            totalMl
        } else {
            animateIntAsState(
                targetValue = totalMl,
                animationSpec = tween(durationMillis = NUMBER_ROLL_MS, easing = FastOutSlowInEasing),
                label = "ringTotalMl",
            ).value
        }
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
private fun BreathingGlow(reduceMotion: Boolean) {
    val spec = currentThemeSpec()
    val glowAlpha =
        if (reduceMotion) {
            GLOW_ALPHA_MIN
        } else {
            val transition = rememberInfiniteTransition(label = "goalGlow")
            val animatedGlowAlpha by transition.animateFloat(
                initialValue = GLOW_ALPHA_MIN,
                targetValue = GLOW_ALPHA_MAX,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = GLOW_BREATH_MS, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "goalGlowAlpha",
            )
            animatedGlowAlpha
        }
    Box(
        modifier =
            Modifier
                .size(HOME_RING_DIAMETER)
                .drawBehind {
                    drawCircle(color = spec.haloColor.copy(alpha = glowAlpha), radius = size.minDimension / 2f)
                },
    )
}

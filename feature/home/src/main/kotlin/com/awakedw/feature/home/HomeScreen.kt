package com.awakedw.feature.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.awakedw.core.designsystem.ControlMinHeight
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.HomeHorizontalPadding
import com.awakedw.core.designsystem.animation.FadeUpOnce
import com.awakedw.core.designsystem.art.CatFigure
import com.awakedw.core.designsystem.art.LightPocket
import com.awakedw.core.designsystem.components.AwakeConfirmDialog
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.lolita.LolitaBackdrop
import com.awakedw.core.designsystem.lolita.LolitaRule
import com.awakedw.core.designsystem.lolita.drawThemeOrnament
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.particles.ParticleDensity
import com.awakedw.core.designsystem.rememberReduceMotion
import com.awakedw.core.designsystem.ring.ProgressRing
import com.awakedw.core.model.CatMood
import com.awakedw.feature.home.components.BadgesRow
import com.awakedw.feature.home.components.CatBubble
import com.awakedw.feature.home.components.Greeting
import com.awakedw.feature.home.components.HomeActionDeck

/** 首页进度环直径：开屏形序段（SplashMorph）以它为涟漪终态半径，改值需与开屏同步观感。 */
val HOME_RING_DIAMETER = 196.dp

/** Shared with the splash handover, so the final ring does not jump vertically. */
val HOME_CONTENT_TOP_PADDING = 24.dp

/** 环心数字滚动时长（规格 §4.2 第 3 步：~500ms）。 */
private const val NUMBER_ROLL_MS = 500

/** 环心确认行与默认小字之间的横切时长。 */
private const val RING_NOTE_FADE_MS = 240

/** 达标微光呼吸的 alpha 区间与单程时长。 */
private const val GLOW_ALPHA_MIN = 0.10f
private const val GLOW_ALPHA_MAX = 0.26f
private const val GLOW_BREATH_MS = 1600

/** 环顶蝴蝶结尺寸与上移量（§12）：结饰骑在环 stroke 上。 */
private val BOW_WIDTH = 46.dp
private val BOW_HEIGHT = 28.dp
private val BOW_LIFT = 2.dp

/** Mascot gets its own flow row after the factual summary, so it never covers statistics. */
private val CAT_RAIL_HEIGHT = 92.dp

/** Small end spacing; the mascot row itself provides the required breathing room. */
private val CONTENT_TAIL_BREATHING = 24.dp

/**
 * 达标横幅文案（视觉规格 §4.2 第 6 步）。只表达「今天够了」，
 * 不承载奖励、连续或解锁语义；改文案只需改这一处。
 */
internal const val CELEBRATION_TEXT = "今日份水灵达成 ✨"

/** 达标横幅展开／收回时长：展开略慢于收回，像缎带被人轻轻拉开。 */
private const val BANNER_ENTER_MS = 260
private const val BANNER_EXIT_MS = 190

/**
 * 撤回手势的说明行。长按是隐藏手势，必须在界面上写出来——
 * 只放进无障碍描述，看得见的用户就永远发现不了。
 */
internal const val REVERT_HINT_TEXT = "长按「最近一杯」可以撤回刚记的那一杯"

/**
 * Water logging home screen: greeting, progress ring, supportive copy, quick amounts,
 * the primary log action, and an optional mascot response.
 *
 * 三条文字反馈通道物理分离，互不挤占同一格：
 * 1. 打卡确认 → 环心（[RingCenterContent] 的 `centerNote`），1.4s 后复位；
 * 2. 达标横幅 → 环下缎带（[CelebrationBanner]），2.5s 后收回；
 * 3. 猫咪回应 → 猫那一行的气泡（[CatRail]），2.0s 后收场。
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
    var revertConfirming by remember { mutableStateOf(false) }

    // 达标庆祝瞬间的一次轻震（§10.3）：与横幅浮现同拍，克制不喧哗。
    LaunchedEffect(state.celebrating) {
        if (state.celebrating) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        LolitaBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        // 轻量装饰层：渐变之上、内容之下；只提供主题氛围，不表达“今日内容”。
        FloatingParticles(
            colors = spec.particleColors,
            modifier = Modifier.matchParentSize(),
            showFlowers = false,
            density = ParticleDensity.QUIET,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = HomeHorizontalPadding, end = HomeHorizontalPadding, bottom = CONTENT_TAIL_BREATHING),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(HOME_CONTENT_TOP_PADDING))
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
                centerNote = state.centerNote,
                onRingTap = viewModel::tapRing,
            )
            // 达标横幅只在当日首次达标时浮现一次；其余时间零占位。
            CelebrationBanner(
                visible = state.celebrating,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            FadeUpOnce(delayMillis = 40) {
                CatRail(
                    mood = state.catMood,
                    line = state.catLine,
                    onPet = viewModel::petCat,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
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
                    // 有记录才提供撤回入口；长按「最近一杯」触发确认。
                    onRevertLast = if (state.lastDrinkLabel != null) ({ revertConfirming = true }) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 撤回手势的说明放在内容最末端：只在有记录时出现，
            // 且位于主操作之后，不会挤占首屏的记录按钮。
            if (state.lastDrinkLabel != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = REVERT_HINT_TEXT,
                    color = spec.greetingSubColor,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (revertConfirming) {
        AwakeConfirmDialog(
            title = "撤回这一杯",
            body = "会删掉今天最后记下的那一杯，今日进度同步减少。",
            confirmLabel = "撤回",
            destructive = true,
            onConfirm = {
                viewModel.revertLatestCup()
                revertConfirming = false
            },
            onDismiss = { revertConfirming = false },
        )
    }
}

/**
 * 达标横幅（视觉规格 §4.2 第 6 步）：缎带自环下中轴展开，2.5s 后收回。
 * 展开是有动画的一次性事件，不是无来由的跳动；减少动态时直接出现与消失。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun CelebrationBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val reduceMotion = rememberReduceMotion()
    AnimatedVisibility(
        visible = visible,
        enter =
            if (reduceMotion) {
                EnterTransition.None
            } else {
                fadeIn(tween(BANNER_ENTER_MS)) +
                    expandVertically(expandFrom = Alignment.Top, animationSpec = tween(BANNER_ENTER_MS, easing = FastOutSlowInEasing))
            },
        exit =
            if (reduceMotion) {
                ExitTransition.None
            } else {
                fadeOut(tween(BANNER_EXIT_MS)) +
                    shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(BANNER_EXIT_MS, easing = FastOutSlowInEasing))
            },
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 2.dp)
                    .heightIn(min = 40.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).height(1.dp).background(spec.laceColor.copy(alpha = 0.46f)))
            Box(Modifier.size(3.dp).background(spec.laceColor.copy(alpha = 0.72f), CircleShape))
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = spec.chipBg.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, spec.laceColor.copy(alpha = 0.62f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(Modifier.size(14.dp)) {
                        drawThemeOrnament(
                            center = Offset(size.width / 2f, size.height / 2f),
                            width = size.width,
                            spec = spec,
                            withTails = true,
                        )
                    }
                    Text(
                        text = CELEBRATION_TEXT,
                        color = spec.chipText,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 7.dp),
                    )
                }
            }
            Box(Modifier.size(3.dp).background(spec.laceColor.copy(alpha = 0.72f), CircleShape))
            Box(Modifier.weight(1f).height(1.dp).background(spec.laceColor.copy(alpha = 0.46f)))
        }
    }
}

/**
 * Compact mascot rail: the cat stays in the first viewport near the ring while the
 * factual summary remains untouched below the action deck.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun CatRail(
    mood: CatMood,
    line: String?,
    onPet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val bubbleShape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 3.dp)
    Row(
        modifier = modifier.heightIn(min = CAT_RAIL_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (line == null) {
            IdleCatAccent(modifier = Modifier.weight(1f))
        } else {
            // 气泡只承载猫语，并按内容宽度内缩，短句不再撑成一条空盒子。
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                CatBubble(text = line)
            }
        }
        Column(modifier = Modifier.width(112.dp), horizontalAlignment = Alignment.End) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                Text(
                    text = "点击我试试~",
                    color = spec.chipText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier =
                        Modifier
                            .clip(bubbleShape)
                            .background(spec.chipBg.copy(alpha = 0.96f))
                            .border(0.5.dp, spec.laceColor.copy(alpha = 0.65f), bubbleShape)
                            .clickable(role = Role.Button, onClickLabel = "摸摸猫咪", onClick = onPet)
                            .heightIn(min = ControlMinHeight)
                            .padding(horizontal = 9.dp, vertical = 10.dp),
                )
            }
            Box(modifier = Modifier.size(84.dp).align(Alignment.Start), contentAlignment = Alignment.Center) {
                LightPocket(modifier = Modifier.matchParentSize())
                CatFigure(mood = mood, onPet = onPet, figureSize = 84.dp)
            }
        }
    }
}

/** Quiet paper-seam ornament that fills the empty response side without adding a message or action. */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun IdleCatAccent(modifier: Modifier = Modifier) {
    val spec = currentThemeSpec()
    Canvas(modifier.heightIn(min = CAT_RAIL_HEIGHT)) {
        val centerY = size.height / 2f
        val left = 10.dp.toPx()
        val right = (size.width - 10.dp.toPx()).coerceAtLeast(left)
        val ink = spec.laceColor
        drawLine(
            color = ink.copy(alpha = 0.22f),
            start = Offset(left, centerY),
            end = Offset(right, centerY),
            strokeWidth = 0.7.dp.toPx(),
        )
        drawCircle(ink.copy(alpha = 0.42f), 1.4.dp.toPx(), Offset(left + 8.dp.toPx(), centerY))
        drawCircle(ink.copy(alpha = 0.34f), 1.dp.toPx(), Offset((left + right) / 2f, centerY))
        drawCircle(ink.copy(alpha = 0.42f), 1.4.dp.toPx(), Offset(right - 8.dp.toPx(), centerY))
        drawCircle(spec.primary.copy(alpha = 0.20f), 4.dp.toPx(), Offset((left + right) / 2f, centerY))
    }
}

/** 今日饮水环：数值与环顶丝带；回应文案只在猫咪行展示。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun RingBlock(
    progress: Float,
    totalMl: Int,
    centerNote: String?,
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
            RingCenterContent(totalMl = totalMl, reduceMotion = reduceMotion, centerNote = centerNote)
        }
        Box(Modifier.matchParentSize()) {
            RingBow(
                goalMet = progress >= 1f,
                reduceMotion = reduceMotion,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = -BOW_LIFT),
            )
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
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    val sway =
        if (reduceMotion || !goalMet) {
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
        drawThemeOrnament(
            center = Offset(size.width / 2f, size.height / 2f),
            width = size.width * 0.72f,
            spec = spec,
            withTails = goalMet,
        )
    }
}

/**
 * 环心：滚动到新值的总量 + 一行小字。
 * 小字在默认的「今日已喝」与 [centerNote]（打卡确认／重复提示）之间横切——
 * 确认就出现在使用者刚刚看着的位置，不产生任何布局位移。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun RingCenterContent(
    totalMl: Int,
    reduceMotion: Boolean,
    centerNote: String? = null,
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
            text =
                buildAnnotatedString {
                    append(rolledTotal.toString())
                    withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)) { append("ml") }
                },
            color = spec.ringValueText,
            // 环心排版（§10.4）：数值略收紧字距提精气神，与下方拉开字距的小字形成层次。
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        )
        Spacer(Modifier.height(4.dp))
        Crossfade(
            targetState = centerNote,
            animationSpec = tween(durationMillis = if (reduceMotion) 0 else RING_NOTE_FADE_MS),
            label = "ringCenterNote",
        ) { note ->
            Text(
                text = note ?: "今日已喝",
                color = if (note != null) spec.chipText else spec.greetingSubColor,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = if (note != null) 0.2.sp else 1.5.sp),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
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

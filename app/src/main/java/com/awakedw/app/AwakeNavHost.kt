package com.awakedw.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.awakedw.core.designsystem.GradientBackdrop
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.burst.BurstParticles
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.designsystem.particles.FloatingParticles
import com.awakedw.core.designsystem.ring.ProgressRing

/** 三大主路由（设计规格 §3：首页 / 统计 / 我的）。 */
sealed class AwakeDestination(val route: String) {
    data object Home : AwakeDestination("home")

    data object Stats : AwakeDestination("stats")

    data object Settings : AwakeDestination("settings")
}

/** 占位首页进度环直径；其半径即开屏涟漪的终态半径，形序无缝衔接。 */
internal val HOME_RING_DIAMETER = 220.dp

/** 占位首页进度环的初始观感（35%），开屏形序段与之对齐。 */
internal const val HOME_PLACEHOLDER_PROGRESS = 0.35f

/** 底部栏选中指示器的低透明度。 */
private const val TAB_INDICATOR_ALPHA = 0.14f

/**
 * 导航壳：冷启动先进 [SplashMorph] 续场（点击或 ~1.2s 后放行），
 * 随后挂载 Scaffold + 底部三标签 + [NavHost]。
 *
 * [startOnSplashDone] 供上游声明「开屏已完成，直接进壳」
 * （Task 13 的 onboarding 分支将经此跳过续场）；
 * splashDone 以 rememberSaveable 持有——旋转等配置变更绝不重放开屏。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun AwakeNavHost(startOnSplashDone: Boolean = false) {
    var splashDone by rememberSaveable { mutableStateOf(startOnSplashDone) }

    if (splashDone) {
        AwakeShell()
    } else {
        SplashMorph(onSplashFinished = { splashDone = true })
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun AwakeShell() {
    val spec = currentThemeSpec()
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        containerColor = spec.backgroundGradient.first(),
        bottomBar = {
            AwakeBottomBar(
                spec = spec,
                currentRoute = currentRoute,
                onSelect = { destination -> navigateToTab(navController, destination) },
            )
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AwakeDestination.Home.route,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
        ) {
            composable(AwakeDestination.Home.route) { HomePlaceholder() }
            // 临时占位页：Task 10–12 以真实功能页替换。
            composable(AwakeDestination.Stats.route) { EmptyPage(title = "统计") }
            composable(AwakeDestination.Settings.route) { EmptyPage(title = "我的") }
        }
    }
}

/** 底部三标签：选中色取当前主题 primary，底色取 chipBg——随主题平滑换肤。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun AwakeBottomBar(
    spec: ThemeSpec,
    currentRoute: String?,
    onSelect: (AwakeDestination) -> Unit,
) {
    NavigationBar(containerColor = spec.chipBg, tonalElevation = 0.dp) {
        BOTTOM_TABS.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.destination.route,
                onClick = { onSelect(tab.destination) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(text = tab.label) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = spec.primary,
                        selectedTextColor = spec.primary,
                        unselectedIconColor = spec.chipText,
                        unselectedTextColor = spec.chipText,
                        indicatorColor = spec.primary.copy(alpha = TAB_INDICATOR_ALPHA),
                    ),
            )
        }
    }
}

/** 单顶层页签导航：不堆栈、保存/恢复各页状态。 */
private fun navigateToTab(
    navController: NavController,
    destination: AwakeDestination,
) {
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private data class BottomTab(
    val destination: AwakeDestination,
    val icon: ImageVector,
    val label: String,
)

private val BOTTOM_TABS: List<BottomTab> =
    listOf(
        BottomTab(AwakeDestination.Home, Icons.Rounded.WaterDrop, "首页"),
        BottomTab(AwakeDestination.Stats, Icons.Rounded.BarChart, "统计"),
        BottomTab(AwakeDestination.Settings, Icons.Rounded.Favorite, "我的"),
    )

/**
 * 首页占位（临时，Task 10 替换）——视觉底座首渲义务（终审要求）：
 * GradientBackdrop + ProgressRing + FloatingParticles + BurstParticles 同屏；
 * 点击进度环触发一次庆祝迸发，为真机冒烟（T8a 守卫 / T8d 叠加观感）提供可复现入口。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
internal fun HomePlaceholder() {
    val spec = currentThemeSpec()
    var burstTrigger by remember { mutableIntStateOf(0) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
    ) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        FloatingParticles(colors = spec.particleColors, modifier = Modifier.matchParentSize())
        ProgressRing(
            progress = HOME_PLACEHOLDER_PROGRESS,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(HOME_RING_DIAMETER),
            onRingTap = { burstTrigger++ },
        ) {
            Text(
                text = "${(HOME_PLACEHOLDER_PROGRESS * 100).toInt()}%",
                color = spec.ringValueText,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        BurstParticles(
            origin = Offset(containerSize.width / 2f, containerSize.height / 2f),
            colors = spec.particleColors,
            trigger = burstTrigger,
            onFinish = { },
        )
    }
}

/** 临时占位页（Task 11/12 替换）：主题渐变底 + 居中标题。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun EmptyPage(
    title: String,
    modifier: Modifier = Modifier,
) {
    val spec = currentThemeSpec()
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GradientBackdrop(spec = spec, modifier = Modifier.matchParentSize())
        Text(text = title, color = spec.greetingColor, style = MaterialTheme.typography.headlineMedium)
    }
}

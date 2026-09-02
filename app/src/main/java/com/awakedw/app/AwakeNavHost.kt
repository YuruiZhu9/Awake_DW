package com.awakedw.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.awakedw.core.common.AppClock
import com.awakedw.core.designsystem.ThemeSpec
import com.awakedw.core.designsystem.currentThemeSpec
import com.awakedw.core.domain.contracts.CopyLibraryRepository
import com.awakedw.core.domain.contracts.UserPreferencesRepository
import com.awakedw.core.domain.contracts.WaterRepository
import com.awakedw.core.notification.NotifBuilder
import com.awakedw.core.notification.Reason
import com.awakedw.core.notification.ReminderScheduler
import com.awakedw.feature.home.HomeScreen
import com.awakedw.feature.onboarding.OnboardingScreen
import com.awakedw.feature.settings.SettingsScreen
import com.awakedw.feature.settings.SettingsViewModel
import com.awakedw.feature.stats.StatsScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Four routes: three water-tool tabs plus first-use onboarding. */
sealed class AwakeDestination(val route: String) {
    data object Home : AwakeDestination("home")

    data object Stats : AwakeDestination("stats")

    data object Settings : AwakeDestination("settings")

    data object Onboarding : AwakeDestination("onboarding")
}

/** 启动分支判定（集成决议）：未完成引导先入引导路由，否则正常进首页。 */
internal fun startDestinationFor(onboardingDone: Boolean): String =
    if (onboardingDone) AwakeDestination.Home.route else AwakeDestination.Onboarding.route

/** 底栏显隐判定：仅三个主页签显示底栏，引导路由（含未知/未定路由）一律隐藏。 */
internal fun showsBottomBar(route: String?): Boolean = route in MAIN_TAB_ROUTES

private val MAIN_TAB_ROUTES: List<String> =
    listOf(AwakeDestination.Home.route, AwakeDestination.Stats.route, AwakeDestination.Settings.route)

/** 底部栏选中指示器的低透明度。 */
private const val TAB_INDICATOR_ALPHA = 0.14f

/** 页签转场上移幅度（§10.2：位移 ≤12dp）。 */
private val TAB_TRANSITION_RISE_DP = 8.dp

/** 页签转场时长：淡入/上移 200ms，淡出稍快。 */
private const val TAB_TRANSITION_MS = 200
private const val TAB_TRANSITION_FADE_OUT_MS = 160

/**
 * 导航壳：冷启动先进 [SplashMorph] 续场（点击或 ~1.2s 后放行），随后挂载导航图。
 *
 * 启动分支（集成决议）：经 [MainViewModel] 读一次 prefs.onboardingDone()——
 * false 时以引导路由启动且隐藏底栏，true 时正常三页签壳；
 * 分支只认 Screen 级接缝：引导完成经 [OnboardingScreen] 的 onComplete 参数导航回首页，
 * VM 构造器 onComplete 保持默认 no-op，防双跳。
 *
 * splashDone 以 rememberSaveable 持有——旋转等配置变更绝不重放开屏。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun AwakeNavHost(startOnSplashDone: Boolean = false) {
    var splashDone by rememberSaveable { mutableStateOf(startOnSplashDone) }

    if (!splashDone) {
        SplashMorph(onSplashFinished = { splashDone = true })
        return
    }

    val viewModel: MainViewModel = viewModel()
    val onboardingDone by viewModel.onboardingDone.collectAsState()

    // 局部捕获：delegated 属性无法 smart cast，分支判定走本地只读值。
    val branch = onboardingDone
    when (branch) {
        // DataStore 首读窗口：自然开屏路径下（≥1.2s）早已就绪，此分支仅兜底防白屏。
        null -> Box(modifier = Modifier.fillMaxSize().background(currentThemeSpec().backgroundGradient.first()))
        else -> AwakeShell(onboardingDone = branch)
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun AwakeShell(onboardingDone: Boolean) {
    val spec = currentThemeSpec()
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        containerColor = spec.backgroundGradient.first(),
        bottomBar = {
            if (showsBottomBar(currentRoute)) {
                AwakeBottomBar(
                    spec = spec,
                    currentRoute = currentRoute,
                    onSelect = { destination -> navigateToTab(navController, destination) },
                )
            }
        },
    ) { contentPadding ->
        // 页签转场（§10.2，克制基调）：淡入 + 8dp 轻上移，替代默认生硬淡入。
        val risePx = with(LocalDensity.current) { TAB_TRANSITION_RISE_DP.toPx().toInt() }
        NavHost(
            navController = navController,
            startDestination = startDestinationFor(onboardingDone = onboardingDone),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(TAB_TRANSITION_MS)) +
                    slideInVertically(animationSpec = tween(TAB_TRANSITION_MS)) { risePx }
            },
            exitTransition = { fadeOut(animationSpec = tween(TAB_TRANSITION_FADE_OUT_MS)) },
            popEnterTransition = {
                fadeIn(animationSpec = tween(TAB_TRANSITION_MS)) +
                    slideInVertically(animationSpec = tween(TAB_TRANSITION_MS)) { risePx }
            },
            popExitTransition = { fadeOut(animationSpec = tween(TAB_TRANSITION_FADE_OUT_MS)) },
        ) {
            composable(AwakeDestination.Onboarding.route) {
                OnboardingScreen(onComplete = { navigateHomeAfterOnboarding(navController) })
            }
            composable(AwakeDestination.Home.route) {
                HomeScreen()
            }
            composable(AwakeDestination.Stats.route) { StatsScreen() }
            composable(AwakeDestination.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel(factory = rememberSettingsViewModelFactory()),
                    onOpenWhitelistGuide = {
                        navController.navigate(AwakeDestination.Onboarding.route) { launchSingleTop = true }
                    },
                )
            }
        }
    }
}

/**
 * 引导完成导航（唯一接线点）：回首页并清空引导栈——
 * 从启动分支进入时清掉引导路由本身；从「我的」页入口进入时整栈回正。
 */
private fun navigateHomeAfterOnboarding(navController: NavController) {
    navController.navigate(AwakeDestination.Home.route) {
        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * 「我的」页 VM 工厂（SETTINGS_CHANGED 恰此一处触发重排）：
 * 经 EntryPoint 取真实图依赖，以主构造器装配 [SettingsViewModel]，
 * 把提醒总开关的副作用接缝接到 `scheduler.rescheduleFromNow(SETTINGS_CHANGED)`；
 * 「试一试」接缝（§11.4）发一条真实样子的提醒通知——NotifBuilder 不出 core:notification，
 * 经 lambda 注入保持 feature 模块边界。双完成缝禁令：调度触发只此一处，别处不得重复调用。
 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun rememberSettingsViewModelFactory(): ViewModelProvider.Factory {
    val appCtx = appContext()
    return remember(appCtx) {
        val graph = EntryPointAccessors.fromApplication(appCtx, AwakeNavGraphEntryPoint::class.java)
        viewModelFactory {
            initializer {
                SettingsViewModel(
                    prefs = graph.prefs(),
                    copies = graph.copies(),
                    water = graph.water(),
                    clock = graph.clock(),
                    onRemindersChanged = { graph.scheduler().rescheduleFromNow(Reason.SETTINGS_CHANGED) },
                    onPostTestReminder = {
                        val notif = graph.notifBuilder()
                        notif.post(notif.reminder(notif.currentSlot(), "试一试：到点的温柔提醒长这个样子 ♡"))
                    },
                )
            }
        }
    }
}

/** 组合期读一次应用级上下文（remember 计算块非组合上下文，不能在块内取 LocalContext）。 */
@Suppress("ktlint:standard:function-naming")
@Composable
private fun appContext() = LocalContext.current.applicationContext

/** :app 导航接线 EntryPoint：与 core/notification 的接收器 EntryPoint 同模式。 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AwakeNavGraphEntryPoint {
    fun prefs(): UserPreferencesRepository

    fun copies(): CopyLibraryRepository

    fun water(): WaterRepository

    fun clock(): AppClock

    fun notifBuilder(): NotifBuilder

    fun scheduler(): ReminderScheduler
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

/** 单顶层页签导航：不堆栈、保存/恢复各页状态。
 *
 * popUpTo 必须指向首页路由而非 `graph.findStartDestination()`：
 * 引导完成后图上的 startDestination 仍是已被弹出的 onboarding 条目，
 * 指向它时 popUpTo 是空操作——页签切换会不断往栈里压重复条目，
 * 系统返回键要按很多下才退得出去（表现为「返回键退不出应用」）。
 */
private fun navigateToTab(
    navController: NavController,
    destination: AwakeDestination,
) {
    navController.navigate(destination.route) {
        popUpTo(AwakeDestination.Home.route) { saveState = true }
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

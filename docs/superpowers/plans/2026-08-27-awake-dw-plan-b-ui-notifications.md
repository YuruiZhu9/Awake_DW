# Awake_DW 计划 B · 界面与提醒链路 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在计划 A 的地基上交付全部用户可见能力：开屏动画、底部三标签导航、治愈打卡首页、统计页、设置与文案编辑器、白名单引导、温柔提醒通知链路，并产出真机验收清单。

**Architecture:** 单 Activity + Navigation Compose + 底部三 Tab；feature 模块各自持有 ViewModel(StateFlow)，跨页只经 `:app` 导航表；通知由 AlarmManager 精确闹钟驱动 Receiver 触发，权限缺失自动降级，开机重排兜底。

**Tech Stack:** Compose(M3) · Navigation Compose · Splash API · Hilt · Robolectric(Receiver 测试) · material-icons-extended(仅图标)

**Spec:** `docs/superpowers/specs/2026-08-27-water-reminder-design.md`；上游接口契约见 `docs/superpowers/plans/2026-08-27-awake-dw-plan-a-foundation.md` 各任务 Interfaces 节

## Global Constraints

- 与计划 A 完全一致的工具链/版本/禁网约束；执行前必须已满足计划 A DoD
- 所有交互文案简体中文且遵循 §2.1 每主题按钮主文案：翡翠绿「干杯一下 💧」草莓雾光「喝一杯啦 ♡」焦糖奶茶「来一口温暖」
- 动画曲线：进度 600ms easeOut；按压 spring(dampingRatioMediumBouncy)；主题切换整体 500ms
- 夸夸短语淡入 ~1.4s 后淡出；庆祝态当日仅触发一次（DataStore `celebrated_day_key` 判定）
- 提交信息 `<type>: <中文摘要>`；每步完成勾选对应 `- [ ]`

---

### Task 0: 计划 B 前置 · 地基闭环五连修（终审 Important 清单）

**Files:**
- Create: `app/src/main/java/com/awakedw/app/AwakeApplication.kt`
- Modify: `app/src/main/AndroidManifest.xml`(android:name + android:label)、`core/data/build.gradle.kts`(api→implementation)、`core/data/.../RoomWaterRepository.kt`(todayStats 单查询化)、`core/domain/.../ObserveHomeUseCase.kt`(删默认参)、`core/designsystem/.../BurstParticles.kt`(trigger<=0 清场守卫)
- Test: 注入烟测 `app/src/test/.../HiltGraphSmokeTest.kt`（Robolectric 跑 AwakeApplication 触发全图校验）

**Interfaces:** 产出零新增契约；本任务只收口，所有既有签名不变。唯一行为变更：ObserveHomeUseCase 构造的 theme 参数从带默认值改为必填（调用方尚不存在，无迁移成本）；BurstParticles 增加"trigger<=0 时立即清场(travelState 复位为 idle)且不回调 onFinish"守卫。

**Steps:**
1. `AwakeApplication : Application()` 标注 `@HiltAndroidApp`，manifest 注册并加 `android:label="Awake_DW"`（后续有中文名资源时再替换）；
2. 四个 UseCase 构造器补 `@Inject constructor(...)`（domain 是纯 Kotlin 模块——`javax.inject.@Inject` 来自 JSR-330，在 toml 加 `inject = "javax.inject:javax.inject:1"` 纯 Java 依赖即可，不引入 hilt-android 到 domain）；
3. `:core:data` 的 `api(libs.room.runtime)` 降级 `implementation`（铁律：Room 存在感不出 data 模块）；
4. todayStats 改单查询：一次 `todayRecords()` 后内存推导 total/cupCount/avgInterval（顺带消除双查询竞态，终审 T4a）；
5. ObserveHomeUseCase 删除 `theme = ResolveThemeUseCase(prefs, SystemAppClock())` 默认值；
6. BurstParticles 加 `if (trigger <= 0) { travelState.floatValue = IDLE; return@LaunchedEffect }` 守卫（终审 T8a 定死的契约）;
7. Room schema 导出开启（ksp arg room.schemaLocation=$projectDir/schemas）并提交 v1 基线 JSON——首改表前必须就位；
8. 验证：`./gradlew build` 全绿 且 Hilt 烟测通过（图真正闭环的第一个证明）；commit `fix: 地基闭环——Hilt 入口/依赖收口/契约定死（终审任务0）`

---

### Task 9: `:app` 导航骨架 + 底部栏 + 开屏续场动画

**Files:**
- Create: `app/src/main/java/com/awakedw/app/{AwakeApp,AwakeNavHost,SplashMorph}.kt`
- Modify: `MainActivity.kt`(接 SplashScreen API)、`themes.xml`(+values-night 不需要)、`res/drawable/splash_logo.xml`
- Test: `app/src/test/.../SplashSequencingTest.kt`（Robolectric 判定 skip 行为与最短驻留）

**Interfaces:**
- Consumes: 计划A的 `ResolveThemeUseCase/AwakeTheme/FloatingParticles/BurstParticles/ProgressRing`（占位路由到即将创建的三个 feature 入口 composable；本任务允许以 `TODO 占位页` 形式引用未来符号——用临时 `EmptyPage(title)` 组件顶替，Task 10–12 替换）
- Produces:

```kotlin
sealed class AwakeDestination(val route: String) {
    data object Home : AwakeDestination("home")
    data object Stats : AwakeDestination("stats")
    data object Settings : AwakeDestination("settings")
}
@Composable fun AwakeNavHost(startOnSplashDone: Boolean = false)
```

- [ ] **Step 1: 失败测试**：`SplashSequencingTest` 断言——点击任意处即完成跳转(状态机 `canSkip=true`)；冷启动全程 ≤1400ms 后强制放行（虚拟时钟推帧）。
- [ ] **Step 2:** FAIL
- [ ] **Step 3: 实现**
  - `MainActivity.installSplashScreen().setKeepOnScreenCondition { !assetsReady }`；
  - `SplashMorph` 编排（Compose 内，总 ~1.2s）：水滴自上而下 spring 落点(y: -80dp→0, 450ms) → 两圈涟漪 radius 扩散 alpha 收敛(各 380ms 相位差 120ms) → 涟漪外圈放大为 ProgressRing 初始态半径并淡入首页（跨页 morph 用共享元素省事方案：直接在同一 Screen 内切换状态+Crossfade 250ms）→ 任何点击中断直达首页；
  - `Scaffold(bottomBar = BottomBar)` 三项：`Icons.Rounded.WaterDrop 首页 / Icons.Rounded.BarChart 统计 / Icons.Rounded.Favorite 我的`（material-icons-extended 加入 app 依赖）；选中色取当前 ThemeSpec.primary；
  - **主题过渡**：在 `:app` 层包装 `AnimatedAwakeTheme(themeId)`——内部对 ThemeSpec 的每个颜色锚点用 `animateColorAsState(targetValue, tween(500))` 后再写入 `LocalAwakeTheme`，兑现规格 §2.3 的 0.5s 平滑换肤（含背景渐变逐停靠点插值）；
  - **视觉底座真机义务（终审要求）**：本任务是首个可安装包，验收必须包含「GradientBackdrop+ProgressRing+FloatingParticles+BurstParticles 同屏冒烟」的真机过目步骤，并复核 BurstParticles 新守卫无残影、GrainOverlay 与 Backdrop 叠加观感（T8d）。
- [ ] **Step 4:** PASS 且 `./gradlew :app:assembleDebug` 成功 → **Step 5:** `git commit -m "feat(app): 底部三标签导航与落滴开屏动画"`

---

### Task 10: `:feature:home` 治愈打卡首页

**Files:**
- Create: `feature/home/src/main/kotlin/com/awakedw/feature/home/`下 `HomeViewModel.kt`、`HomeScreen.kt`、`components/{Greeting,BadgesRow,HealthTipLine,LogButton,PraiseLine,CelebrationOverlay}.kt`
- Modify: `:app` NavHost 替换占位
- Test: `HomeViewModelTest.kt`（JVM）、`HomeScreenTest.kt`（compose-ui-test）

**Interfaces:**
- Consumes: PlanA 全部产出 + `AppClock`
- Produces:

```kotlin
data class HomeUiState(
    val themeId: ThemeId, val progress: Float,        // 0..1 已达标可 >1 截断显示满环微光呼吸
    val totalMl: Int, val goalMl: Int,
    val cupCount: Int, val avgIntervalLabel: String,   // <2 杯为 "—"
    val praiseLine: String?,                           // null=隐藏
    val celebrating: Boolean,
)
class HomeViewModel(clock, observeHome, logWater, prefs) : ViewModel() {
    val uiState: StateFlow<HomeUiState>
    fun tapLogButton()          // 按钮=立即记录
    fun tapRing(offsetPx: Offset?)  // 环区点击记录；两入口共用 800ms 防抖
}
```

- [ ] **Step 1: JVM 失败测试** HomeViewModelTest：
  1. 双击合并：连续两次 tapLogButton 间隔 <800ms 仅产生一次 addCup（fake repo 计数）；
  2. 平均间隔徽章：0 杯/1 杯→"—"；3 杯间隔 60/30 分钟→"45 分钟"（<90min 用分钟文案，否则 "1.6h"）;
  3. 达标瞬时 celebrating=true 且当日后续 log 保持 false；
  4. progress=total/goal 超 1 截断 1。
- [ ] **Step 2:** FAIL
- [ ] **Step 3: 实现 UI**（布局次序照 spec §3.2 自上而下）：
  - Greeting: 按 MORNING/DAY/EVENING 文案组首条循环：「早安呀，今天也要甜甜的 ☀ / 今天也请清清爽爽哦 🍃 / 晚上好，今天辛苦啦 🌙」，副行 `{M月d日} · {已完成 X%|距离目标还有 Yml}`;
  - BadgesRow: 「今日 {n} 杯 ☀」「平均间隔 {label} ⏱」chip 样式(spec chipBg/chipText)；
  - HealthTipLine: 「📖 《中国居民膳食指南》建议成年人每日饮水 1500–1700ml」（直接引用 core-model 常量插值，不硬编码）；
  - 中央 ProgressRing(content=环心 `{total}ml`+「今日已喝」ringValueText 色)+外圈 Halo+FloatingParticles 背景+GrainOverlay 全局；
  - LogButton: ThemeById(themeId) 渐变胶囊，点击→tapLogButton()；配 BurstParticles(origin=按钮中心)、PraiseLine 从 CopyLibraryRepository.randomFor(slot) 取句 Crossfade 进出 1.4s；
  - CelebrationOverlay: reached 时粒子雨(FloatingParticles 密度参数 ×3 下落反向)+「今日份水灵达成 ✨」横幅，2500ms 自动收敛；
  - 打卡反馈 6 步时序在 VM+UI 协同：progress 目标值改变驱动环动画、数字滚动用 `animateIntAsState(500ms)`。
- [ ] **Step 4: compose-ui 测试** createComposeRule: set content 后 `onNodeWithText("记一杯按钮文案") performClick()` 断言环心文本含新总数 & 重复快速点击计数只加一杯。
- [ ] **Step 5:** `git commit -m "feat(feature-home): 治愈打卡首页与反馈演出"`

---

### Task 11: `:feature:stats` 统计页

**Files:**
- Create: `feature/stats/.../StatsViewModel.kt`、`StatsScreen.kt`、`components/{WeekBarsChart,TodayTimeline}.kt`
- Modify: NavHost 替换
- Test: `WeekBarsChartLayoutTest.kt`(逻辑抽纯函数测柱高映射+目标线 y)

**Interfaces:**
- Consumes: `WaterRepository.weekBars/todayRecords`、`GetStreakUseCase`、`UserPreferencesRepository.settings`
- Produces: `data class StatsUiState(badges: StatsBadges, bars: List<WeekBar>, goalMl: Int, timeline: List<WaterRecord>)`; `data class StatsBadges(cupCount:Int, avgIntervalLabel:String, streakDays:Int)`
- [ ] **Step 1: 失败测试** 柱高归一化：max 值柱=图表高×0.86 上限、0 值→基线圆点；streak 尾端随今日实时达标翻转。
- [ ] **Step 2:** FAIL →
- [ ] **Step 3: 实现**：
  - 徽章行三枚 chip（新增 streak「连续 {n} 天 🏅」n≥2 才显示，n==1 显示「第 1 天 ✨」）；
  - WeekBarsChart: Canvas 近 7 天圆角顶柱+虚线目标线+尾列"今"字标注，达标柱 primary 色其余 track 色；
  - TodayTimeline: 时间升序列表，每条左侧小水滴圆点+HH:mm 右侧 `{ml}ml`，空态整块居中文案「今天的第一杯还没出现哦 💧」。
- [ ] **Step 4:** PASS → **Step 5:** `git commit -m "feat(feature-stats): 徽章行周柱状图与今日时间线"`

---

### Task 12: `:feature:settings` 我的·设置与心意文案库

**Files:**
- Create: `feature/settings/.../SettingsViewModel.kt`、`SettingsScreen.kt`、`copyeditor/CopyEditorSheet.kt`
- Modify: NavHost 替换
- Test: `SettingsValidationTest.kt`（VM 纯逻辑）

**Interfaces:**
- Consumes: `UserPreferencesRepository/CopyLibraryRepository`
- Produces: `data class SettingsUiState(settings: UserSettings, library: CopyLibrary, onboardingDone: Boolean)`; VM 暴露 setter 直通 + 校验夹紧：goal/cup ∈[200..4000] 步进50、interval∈setOf(30,60,90,120,150,180,240)、window 起 ∈[300..1380] 且 start<end-30，非法输入回落原值不崩溃。
- [ ] **Step 1: 失败测试**：夹紧规则表驱动（如 setInterval(50)→60? 否——不在候选集回落 90 原值；setWindow(1000,1005) 拒绝）。窗口时间选择以 15min 粒度滑杆展示「开始 HH:mm — 结束 HH:mm」。
- [ ] **Step 2:** FAIL →
- [ ] **Step 3: 实现** 四分区列表卡（§3.4）：目标(±步进器一行两枚)/提醒(ToggleSlider chips)/外观(FOLLOW_TIME+三固定色圆点单选，选项文案「跟随时间/固定翡翠绿/固定草莓雾光/固定焦糖奶茶」)/心意文案库(早午晚折叠列表，长按删除，点条目弹编辑对话框 TextField≤40字，顶部「＋ 新增一句」与右上「恢复默认文案」带确认 Dialog)/引导入口(重新打开白名单页)。所有变更即时生效无保存键。
- [ ] **Step 4:** PASS → **Step 5:** `git commit -m "feat(feature-settings): 设置页与心意文案库编辑器"`

---

### Task 13: `:feature:onboarding` 白名单引导

**Files:**
- Create: `feature/onboarding/.../OnboardingViewModel.kt`、`OnboardingScreen.kt`、`BatteryIntentLauncher.kt`
- Modify: `MainActivity` 启动分支(未 onboarding_done 时先入此页且无底栏)
- Test: `BatteryIntentLauncherTest.kt`（Robolectric 断言 Intent action/data 序列与最终 fallback）

**Interfaces:**
- Produces:

```kotlin
object BatteryIntentLauncher {
    /** 返回厂商定向失败后可用的兜底 Intent（应用详情页）；永不抛异常 */
    fun bestEffortIntents(context: Context): List<Intent>
}
```

- [ ] **Step 1: 失败测试**：intent 列表顺序 == `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS(package uri)` → 小米 `miui.intent.action.HITS_PERMISSION` 尽力构造 → 华为/荣耀 `hiaction.PERMISSION_REQUEST` → OPPO/vivo 各自包名组件跳转 → 兜底 `ACTION_APPLICATION_DETAILS_SETTINGS`；任一 resolveActivity 为空则该条剔除。
- [ ] **Step 2:** FAIL →
- [ ] **Step 3: 实现** 页面视觉沿用当刻主题：插画复用 FloatingParticles+大水滴，标题「为了让每一次温柔准时抵达」，正文说明一句「建议把 Awake_DW 加入电池优化白名单，提醒会更可靠。也可以先跳过。」主按钮「去设置 ♡」逐个 tryStartActivity 直到首个成功并标记 onboarding_done 返回首页；次按钮「以后再说」同样置位 done。 Romss 清单以常量表维护便于追加。
- [ ] **Step 4:** PASS → **Step 5:** `git commit -m "feat(feature-onboarding): 分机型省电白名单引导"`

---

### Task 14: `:core:notification` 温柔提醒链路

**Files:**
- Create: `core/notification/src/main/kotlin/com/awakedw/core/notification/`下 `ReminderScheduler.kt(+Impl)`、`ReminderReceiver.kt`、`BootReceiver.kt`、`NotifBuilder.kt`、`di/NotificationModule.kt`
- Modify: Manifest(`RECEIVE_BOOT_COMPLETED`、`POST_NOTIFICATIONS`、`SCHEDULE_EXACT_ALARM` 声明与两个 receiver 注册)、设置页 Toggle 接入 scheduler
- Test: `NextReminderIntegrationTest.kt`、`ReminderReceiverRobolectricTest.kt`

**Interfaces:**
- Consumes: `NextReminderCalculator`、`LogWaterUseCase`、`CopyLibraryRepository.randomFor`、`TimeSlots.slotOfHour(now)`
- Produces:

```kotlin
interface ReminderScheduler {
    fun rescheduleFromNow(reason: Reason)      // APP_START / SETTINGS_CHANGED / BOOT / LOGGED
    fun cancelAll()
}
enum class Reason { APP_START, SETTINGS_CHANGED, BOOT, LOGGED }
```

- [ ] **Step 1: 失败测试（Robolectric）**
  1. Receiver 场景：AlarmManager shadow 中 pending intent 存在；关闭 remindersEnabled 再 reschedule→cancel 发生；
  2. 通知构建：触发时 ShadowNotificationManager 有通知，渠道「温柔提醒」IMPORTANCE_LOW，actions 含「喝啦 💧」，其 PendingIntent 指向 RecordingBroadcast；
  3. RecordingBroadcast(onReceive)：经 Hilt EntryPoint 同步调用 LogWaterUseCase 落库一条 cupMl 记录并把通知更新为「记好啦 ♡」且 `setTimeoutAfter(2000)`；
  4. Exact 降级：shadow `canScheduleExactAlarms=false` 时使用 `setWindow` 而非 `setExactAndAllowWhileIdle`；
  5. Boot 重排：发 ACTION_BOOT_COMPLETED 后 alarm 队列存在一条。
- [ ] **Step 2:** FAIL →
- [ ] **Step 3: 实现**：调度核心
```kotlin
val fireAt = NextReminderCalculator.nextFire(settings, clock, achievedToday) ?: return cancelPending()
val pi = PendingIntent.getBroadcast(ctx, 1001, intent(userId), FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setWindow(RTC_WAKEUP, fireAt, 10 * 60_000L, pi)
else am.setExactAndAllowWhileIdle(RTC_WAKEUP, fireAt, pi)
```
`LOGGED` 事件后重算下一点；达成日后 schedule 直接短路取消。`PostNotifications` 权限在 onboarding 主路径中一并请求。标题映射：MORNING「早安 ☀」DAY「午后啦 ☀」EVENING「晚上好 🌙」。
- [ ] **Step 4:** PASS → **Step 5:** `git commit -m "feat(core-notification): 精确闹钟温柔提醒与开机重排"`

---

### Task 15: 发布准备与真机验收清单

**Files:**
- Create: `docs/superpowers/checklists/2026-09-v0.1-manual-qa.md`
- Modify: `app/build.gradle.kts`(release: minify=true + proguard 默认规则验证、debug/release signingConfig 由环境变量注入不写死)、根 README 构建一节补充 `./gradlew :app:assembleRelease`
- Test: 无新增自动化；DoD=清单文档存在且 release 包可构建

- [ ] **Step 1: 写清单文档**（内容为真机检查脚本，开发者本人执行；示例条目全部列出不得省略）：冷启动开屏完整播一次/点击跳过有效；三时段模拟改系统时钟观察主题 0.5s 过渡；打卡演出帧率主观流畅(开关开发者选项 GPU 条形图)；双击防抖；达标庆祝仅一次+次日复位；通知到达(白名单前后对比)、点「喝啦 💧」记一杯且收到「记好啦 ♡」；重启手机后待提醒恢复；关提醒开关全静默；文案增删改即时生效；卸载重装默认值还原。
- [ ] **Step 2:** `./gradlew :app:assembleRelease build` 期望 BUILD SUCCESSFUL（签名可选 debug 签名先出未发布包）
- [ ] **Step 3:** `git commit -m "chore(release): v0.1.0 发布配置与真机验收清单"` 并打 tag `v0.1.0`

---

## 计划 B 完成定义（DoD）

- `./gradlew build` 全绿 + `:app:assembleDebug/Release` 可产包
- 设计文档 §3/§4 全部功能点可在真机走通，QA 清单输出实际勾选结果
- 依赖铁律抽查通过：`:feature:*` 无互相依赖、domain 零 Android import（脚本化 grep 验证并入 Task 9 Step 4）

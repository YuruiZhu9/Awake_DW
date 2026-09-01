# Awake_DW v0.2 实施计划：画卷·画廊·胆大王

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

- 日期：2026-08-31
- 状态：待开工（风格锚点已经 moodboard 定稿）
- 规格：`docs/superpowers/specs/2026-08-31-v02-art-moodboard.md`（已确认）——本计划从中 argued，执行者须同时读两份

**Goal:** 把 v0.1 的「能用工具」变成「日常艺术品」：洛丽塔裙装水彩图像作全屏半透明底层，画廊收集制（达标解锁 + 每日穿搭 + 可指定），水彩猫「胆大王」常驻首页纯陪伴，声音（水滴+八音盒），四主题中国色再映射，发布 v0.2.0。

**Architecture:** 沿用现有 11 模块 MVVM 单向数据流。新增 `:feature:gallery` 与 `:core:sound` 两个模块；裙装目录为 `:core:model` 静态数据，解锁/钉选/每日选择状态存 DataStore（沿用 `UserPreferencesRepository` 契约扩展）；图像走 `:app` assets + 运行时加载、缺失优雅回退（资产可后补即生效）；猫用「静态立绘 + Compose 微动效」方案规避帧动画产能；音效用 SoundPool + res/raw，缺失 no-op。

**Tech Stack:** Kotlin 2.0.21 / Compose M3 / Hilt 2.52 / Room 2.6.1（本计划不动 Room schema）/ DataStore Preferences / SoundPool / Robolectric+Turbine 测试。

## Global Constraints

- **构建命令必须带 JAVA_HOME**（本机终端未设）：`JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.20.101-hotspot" ./gradlew <task>`。
- 开发直接在 `main` 分支进行（用户决议），每任务一提交，信息格式 `<type>: <中文摘要>`；提交前跑 `./gradlew ktlintFormat`。
- 每阶段（Task 6/7、10、13、15 之后）必须 `assembleDebug` 产出可安装 APK——日更节奏。
- 零网络权限铁律不破：图像/音频全部随包内置，不引入任何网络依赖（不加 Coil，用 BitmapFactory+IO 协程）。
- 治愈铁律：猫零惩罚、无失败原则——任何反馈不得出现红/警告色、惩罚性长震动、否定文案。
- 单屏装饰 ≤3 类（moodboard §2）：洛丽塔配饰层不再新增装饰种类，画卷与猫是 v0.2 仅有的两个新视觉层。
- 深色主题（墨青）下所有新视觉层必须降透明度/压暗，正文对比度不得低于现状。
- 资产文件名即契约：`outfit/dress_XX.webp`、`outfit/museum_XX.webp`、`cat/idle.webp|happy.webp|sleepy.webp`、`cat/acc_bow.webp|acc_pearl.webp|acc_dress.webp`、`res/raw/drop_a.ogg|drop_b.ogg|drop_c.ogg|goal_melody.ogg|purr.ogg`。资产未就位时功能必须完整可用（回退）。
- 测试纪律沿用现状：纯逻辑 JVM 测试，Compose/Robolectric 用 Robolectric；每任务测试先行。

---

### Task 1: Outfit 模型与静态目录

**Files:**
- Create: `core/model/src/main/kotlin/com/awakedw/core/model/Outfit.kt`
- Test: `core/model/src/test/kotlin/com/awakedw/core/model/OutfitCatalogTest.kt`

**Interfaces:**
- Consumes: 无（纯模型，零依赖）。
- Produces（后续所有任务依赖，逐字实现）:

```kotlin
package com.awakedw.core.model

/** 裙装目录类别：AI 产线的洛丽塔裙 / 博物馆公有领域名画（moodboard §5.2 馆藏分区）。 */
enum class OutfitCategory { DRESS, MUSEUM }

/**
 * 一件「今日穿搭」藏品。[assetFile] 为 :app assets 相对路径；
 * [unlockDay] 为连续达标天数门槛，0 = 开局即有。
 * 夜变体约定存在时与主图同目录、文件名去扩展名加 `_night.webp`。
 */
data class Outfit(
    val id: String,
    val title: String,
    val note: String,
    val category: OutfitCategory,
    val assetFile: String,
    val unlockDay: Int,
)

/** 静态目录：v0.2 首发 12 件（8 裙 + 4 馆藏）。改动须过 [OutfitCatalogTest] 的目录约束。 */
object OutfitCatalog {
    val all: List<Outfit>
    fun byId(id: String): Outfit?
    fun unlockedBy(streakDays: Int): List<Outfit>   // unlockDay <= streakDays，按 unlockDay 升序
}
```

- [ ] **Step 1: 写失败测试**——目录约束四条：id 唯一且非空；unlockDay ≥ 0 且 DRESS 内升序不重复；note 长度 ≤ 50；`unlockedBy(0)` 恰含 1 件（开局款）、`unlockedBy(100)` 含全部 12 件；`byId("dress_00")` 存在且 category 为 DRESS。

```kotlin
class OutfitCatalogTest {
    @Test
    fun `目录约束 - id唯一且note不超长`() {
        val ids = OutfitCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        OutfitCatalog.all.forEach { assertTrue("${it.id} note过长", it.note.length <= 50) }
    }
    @Test
    fun `解锁曲线 - 开局一件百天全量`() {
        assertEquals(1, OutfitCatalog.unlockedBy(0).size)
        assertEquals(OutfitCatalog.all.size, OutfitCatalog.unlockedBy(100).size)
        assertTrue(OutfitCatalog.unlockedBy(0).all { it.unlockDay == 0 })
    }
    @Test
    fun `byId 命中与未命中`() {
        assertEquals(OutfitCategory.DRESS, OutfitCatalog.byId("dress_00")?.category)
        assertNull(OutfitCatalog.byId("nope"))
    }
}
```

- [ ] **Step 2: 跑测试确认失败**：`JAVA_HOME=... ./gradlew :core:model:test` → 编译错误（Outfit 不存在）。
- [ ] **Step 3: 实现**——`Outfit`/`OutfitCategory` 如上；`OutfitCatalog.all` 首发内容逐字如下（assetFile 未就位时由 Task 5 的回退兜底）：

```kotlin
val all: List<Outfit> = listOf(
    Outfit("dress_00", "素呢初见", "本衣柜的第一件：未染的素色软纱，像还没讲出口的早晨。", OutfitCategory.DRESS, "outfit/dress_00.webp", 0),
    Outfit("dress_01", "天水碧·茶会", "天水碧染的轻纱裙摆，茶会还没开始，猫已经先坐好了。", OutfitCategory.DRESS, "outfit/dress_01.webp", 3),
    Outfit("dress_02", "十样锦·午后", "十样锦的甜，是从裙撑边缘一点点漫出来的。", OutfitCategory.DRESS, "outfit/dress_02.webp", 7),
    Outfit("museum_01", "读书少女", "弗拉戈纳尔笔下的少女，读书读累了也会先喝口水吧。", OutfitCategory.MUSEUM, "outfit/museum_01.webp", 5),
    Outfit("museum_02", "猫的肖像", "隆纳-克尼普画笔下的猫，据说是本大王的远房姑妈。", OutfitCategory.MUSEUM, "outfit/museum_02.webp", 10),
    Outfit("dress_03", "藕荷·雨歇", "雨刚停，藕荷色的裙褶里还藏着一点云。", OutfitCategory.DRESS, "outfit/dress_03.webp", 14),
    Outfit("dress_04", "缃叶·黄昏", "缃叶色的裙摆扫过黄昏，把落日别在了腰后。", OutfitCategory.DRESS, "outfit/dress_04.webp", 21),
    Outfit("museum_03", "水彩天鹅", "萨金特笔下的天鹅，水面是一整杯温柔的光。", OutfitCategory.MUSEUM, "outfit/museum_03.webp", 25),
    Outfit("dress_05", "月白·夜曲", "月白色的夜裙，缀着猫眼石，走路有月光的声音。", OutfitCategory.DRESS, "outfit/dress_05.webp", 30),
    Outfit("dress_06", "秋香·拾穗", "秋香色的小裙子，口袋里装满了捡来的光。", OutfitCategory.DRESS, "outfit/dress_06.webp", 50),
    Outfit("museum_04", "花与静物", "荷兰黄金时代的一桌花开，比水壶更懂时间怎么慢慢流。", OutfitCategory.MUSEUM, "outfit/museum_04.webp", 60),
    Outfit("dress_07", "百期·绯缎", "第一百天的绯色缎面——能穿到它的人，早已把温柔穿在身上。", OutfitCategory.DRESS, "outfit/dress_07.webp", 100),
)
```

- [ ] **Step 4: 跑测试确认通过**：`./gradlew :core:model:test`。
- [ ] **Step 5: 提交**：`git commit -m "feat(core-model): 裙装目录与解锁曲线——v0.2 画廊制数据基座"`

---

### Task 2: DataStore 扩展——解锁/钉选/每日选择/音效开关

**Files:**
- Modify: `core/domain/src/main/kotlin/com/awakedw/core/domain/contracts/UserPreferencesRepository.kt`
- Modify: `core/data/src/main/kotlin/com/awakedw/core/data/prefs/UserPreferencesRepositoryImpl.kt`（PrefKeys + 实现）
- Test: `core/data/src/test/kotlin/com/awakedw/core/data/prefs/UserPreferencesRepositoryImplTest.kt`（沿用既有测试文件追加）

**Interfaces:**
- Consumes: 既有 `UserPreferencesRepository`（DataStore Preferences 模式，见 Impl 的 PrefKeys/edit 惯例）。
- Produces（追加到接口，逐字实现）:

```kotlin
/** —— v0.2 画廊与音效（键名：unlocked_outfits / pinned_outfit_id / daily_outfit_day / daily_outfit_id / sound_enabled）—— */
val unlockedOutfits: Flow<Set<String>>          // 已解锁 outfit id 集
suspend fun markOutfitsUnlocked(ids: Collection<String>)   // 幂等合并写入
val pinnedOutfitId: Flow<String?>               // 用户手动指定的「今日之裙」；null = 跟随每日随机
suspend fun setPinnedOutfit(id: String?)
suspend fun dailyOutfit(): Pair<String, String>? // (dayKey, outfitId)；无记录返回 null
suspend fun setDailyOutfit(dayKey: String, outfitId: String)
val soundEnabled: Flow<Boolean>                  // 默认 true
suspend fun setSoundEnabled(v: Boolean)
```

- [ ] **Step 1: 写失败测试**——追加五例：初始 `unlockedOutfits` 为空集；`markOutfitsUnlocked(listOf("dress_00"))` 后 flow 命中且重复标记幂等；`setPinnedOutfit("dress_01")` 后 `pinnedOutfitId` 为 `"dress_01"`，`setPinnedOutfit(null)` 回落 null；`setDailyOutfit("2026-08-31","dress_00")` 后 `dailyOutfit()` 返回 `Pair("2026-08-31","dress_00")`；`soundEnabled` 默认 true 且 `setSoundEnabled(false)` 生效。
- [ ] **Step 2: 跑测试确认失败**：`./gradlew :core:data:test` → 编译错误。
- [ ] **Step 3: 实现**——PrefKeys 追加 `UNLOCKED_OUTFITS="unlocked_outfits"`、`PINNED_OUTFIT_ID="pinned_outfit_id"`、`DAILY_OUTFIT_DAY="daily_outfit_day"`、`DAILY_OUTFIT_ID="daily_outfit_id"`、`SOUND_ENABLED="sound_enabled"`；解锁集用 `stringSetPreferencesKey`，读：`dataStore.data.map { it[stringSetPreferencesKey(UNLOCKED_OUTFITS)] ?: emptySet() }`；写：`edit { it[stringSetPreferencesKey(UNLOCKED_OUTFITS)] = (old + ids.toSet()) }`。
- [ ] **Step 4: 跑测试确认通过**（全模块 `./gradlew :core:data:test :core:domain:test`——接口改动会暴露所有实现类/假实现漏补，逐个补齐编译）。
- [ ] **Step 5: 提交**：`git commit -m "feat(core-data): 画廊解锁/钉选/每日穿搭/音效开关持久化（v0.2 契约扩展）"`

---

### Task 3: 领域用例——解锁同步与每日穿搭

**Files:**
- Create: `core/domain/src/main/kotlin/com/awakedw/core/domain/UnlockOutfitsUseCase.kt`
- Create: `core/domain/src/main/kotlin/com/awakedw/core/domain/ResolveDailyOutfitUseCase.kt`
- Test: `core/domain/src/test/kotlin/com/awakedw/core/domain/UnlockOutfitsUseCaseTest.kt`、`ResolveDailyOutfitUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 1 `OutfitCatalog.unlockedBy(streak)`、Task 2 prefs 新契约、既有 `GetStreakUseCase`（`suspend operator fun invoke(): Int`）。
- Produces:

```kotlin
/** 把当前连胜对照目录，落库新解锁；返回本次新解锁的目录件（幂等，重复调用返回空）。 */
class UnlockOutfitsUseCase @Inject constructor(prefs: UserPreferencesRepository) {
    suspend operator fun invoke(currentStreakDays: Int): List<Outfit>
}

/** 今日之裙：钉选优先；否则对已解锁池按 dayKey 稳定随机（同日重启不变），落库。 */
class ResolveDailyOutfitUseCase @Inject constructor(prefs: UserPreferencesRepository, clock: AppClock) {
    suspend operator fun invoke(): Outfit   // 池意外为空时回退 OutfitCatalog.byId("dress_00")!!
}

/** 纯函数（供测试与复用）：dayKey 稳定随机挑选。 */
fun pickForDay(dayKey: String, pool: List<Outfit>): Outfit  // Random(dayKey.hashCode()).let { pool[it.nextInt(pool.size)] }
```

- [ ] **Step 1: 写失败测试**——`UnlockOutfitsUseCaseTest`：streak=7 时返回 `[dress_01, dress_02]`（unlockDay 3、7）且 prefs 已含二者；再次调用返回空；streak 回落后（=1）不回锁。`ResolveDailyOutfitUseCaseTest`（用假 clock 定 dayKey="2026-08-31"）：`pickForDay` 对同池同 key 两次调用结果一致；pinned 存在时无视随机直接返回 pinned；池为空回退 dress_00；同日第二次调用不重挑（读 dailyOutfit 命中）。
- [ ] **Step 2: 跑测试确认失败**：`./gradlew :core:domain:test`。
- [ ] **Step 3: 实现**——Unlock 内部先读 `unlockedOutfits.first()`，`OutfitCatalog.all.filter { it.unlockDay <= streak && it.id !in have }` → `markOutfitsUnlocked(new.map{it.id})` → 返回 new。Resolve：pinned?.let { byId } ?: 先查 `dailyOutfit()` 同日命中 ?: `pickForDay(dayKey, unlockedPool)` 后 `setDailyOutfit` 落库。dayKey 取 `DayKeys.dayKeyOf(clock)` 语义（与打卡的 dayKeyLocal 同源：`LocalDate` ISO 字符串，见 core/common/DayKeys.kt）。
- [ ] **Step 4: 跑测试确认通过**。
- [ ] **Step 5: 提交**：`git commit -m "feat(core-domain): 解锁同步与每日穿搭用例——达标开裙、同日不重挑"`

---

### Task 4: 资产装载器与回退（图像产线的工程半边）

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/awakedw/core/designsystem/art/AssetPainters.kt`
- Test: `core/designsystem/src/test/kotlin/com/awakedw/core/designsystem/art/AssetPaintersTest.kt`（Robolectric）

**Interfaces:**
- Consumes: Task 1 的 `assetFile` 路径约定。
- Produces:

```kotlin
/** 读 assets 位图；缺失/解码失败返回 null（调用方回退，绝不抛异常）。内部用 Dispatchers.IO。 */
@Composable
fun rememberAssetImageOrN(assetFile: String): ImageBitmap?

/** 夜变体解析：outfit/dress_01.webp → outfit/dress_01_night.webp，存在则用之，否则原文件。 */
fun nightVariantOf(assetFile: String): String
```

- [ ] **Step 1: 写失败测试**（Robolectric，context = ApplicationProvider）：`nightVariantOf("outfit/dress_01.webp") == "outfit/dress_01_night.webp"`；`rememberAssetImageOrN("outfit/__nope__.webp")` 组合后值为 null 且不抛（用 `createComposeRule` 或直接测内部挂起函数 `loadAssetBitmap(context, path)` 的同步版）。
- [ ] **Step 2: 确认失败 → Step 3: 实现**——`loadAssetBitmap(context, assetFile): ImageBitmap?`：`runCatching { context.assets.open(assetFile).use { BitmapFactory.decodeByteArray(it.readBytes(), 0, size) } }.getOrNull()?.asImageBitmap()`；`rememberAssetImageOrN` 用 `produceState(null, assetFile)` + `Dispatchers.IO`。夜变体：仅替换最后一个 `.` 前的文件名，`assets.list()` 探测存在与否（实现放同名 `ArtAssets.kt` 的 `hasAsset(context, path)`）。
- [ ] **Step 4: 确认通过 → Step 5: 提交**：`git commit -m "feat(designsystem): assets 位图装载与夜变体解析——缺失即回退不抛"`

---

### Task 5: DressBackdrop——全屏画卷底层

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/awakedw/core/designsystem/art/DressBackdrop.kt`
- Test: `core/designsystem/src/test/kotlin/com/awakedw/core/designsystem/art/DressBackdropTest.kt`（Robolectric compose）

**Interfaces:**
- Consumes: Task 4 的装载器、既有 `currentThemeSpec()`（AwakeTheme.kt 提供）。
- Produces:

```kotlin
/**
 * 全屏画卷底层（moodboard §5.1）：置于背景渐变之上、内容之下。
 * 浅色主题 alpha=0.30f，深色主题 alpha=0.18f；BlendMode.Multiply；
 * 换图 600ms 交叉淡入；夜变体优先；资产缺失不绘制（回退纯渐变）。
 * 注意：自带纸纹依赖全局 GrainOverlay 体系，不在本层重复叠加。
 */
@Composable
fun DressBackdrop(outfit: Outfit?, modifier: Modifier = Modifier, alphaOverride: Float? = null)
```

- [ ] **Step 1: 写失败测试**——Robolectric compose：outfit=null 组合不崩溃且无图像节点；alphaOverride=0.5f 时绘制 alpha 语义正确（对内部常量抽纯函数 `backdropAlpha(isDark: Boolean): Float` 断言 0.18f/0.30f，override 优先）。
- [ ] **Step 2-4: 红→实现→绿**——`Box(modifier.drawWithCache …)` + `Crossfade(targetState = imageBitmap, animationSpec = tween(600))`；BlendMode.Multiply 画位图；位图按 `ContentScale.Crop` 语义铺满（drawImage 带 srcOffset/srcSize 裁切）。深色主题经 `currentThemeSpec().isDark` 与 `nightVariantOf` 双管齐下。
- [ ] **Step 5: 提交**：`git commit -m "feat(designsystem): 全屏画卷底层——主题感知透明度/夜变体/缺失回退（moodboard §5.1）"`

---

### Task 6: 首页接线——画卷上屏与解锁轻提示

**Files:**
- Modify: `feature/home/src/main/kotlin/com/awakedw/feature/home/HomeViewModel.kt`（HomeUiState + init + logAndPraise）
- Modify: `feature/home/src/main/kotlin/com/awakedw/feature/home/HomeScreen.kt`（Backdrop 挂载 + 今日穿搭签 + 猫位预留）
- Test: `feature/home/src/test/kotlin/com/awakedw/feature/home/HomeViewModelOutfitTest.kt`

**Interfaces:**
- Consumes: Task 3 两个用例、Task 5 DressBackdrop。
- Produces:

```kotlin
// HomeUiState 追加字段：
val todayOutfit: Outfit? = null,   // null = 目录/池未就绪，表现层不画卷不显签
val newUnlock: Outfit? = null,     // 本次打卡新解锁的裙（2.5s 轻提示后清空，复用 feedbackEpoch 防串场）
// HomeViewModel 构造器追加：private val unlockOutfits: UnlockOutfitsUseCase, private val resolveDailyOutfit: ResolveDailyOutfitUseCase
// HomeScreen 追加参数：onOpenGallery: () -> Unit = {}
```

- [ ] **Step 1: 写失败测试**（Turbine + 假 repo/clock，沿用既有 HomeViewModel 测试的假件风格）：init 后 `todayOutfit` 非空且等于 resolve 结果；`logAndPraise` 成功后若 sync 返回新解锁则 `newUnlock` 命中并在 2.5s 后被 epoch 收场清空；无新解锁时 `newUnlock` 保持 null。
- [ ] **Step 2-4: 红→实现→绿**——init 里 `resolveDailyOutfit()` 灌入；`logAndPraise` 内打卡成功分支后调 `unlockOutfits(streakDays)`（streak 用 `HomeSnapshot.streakDays` 最新值），新解锁走与 praiseLine 同款的 2.5s 定时清场。UI：`HomeScreen` 根 Box 最底（Backdrop 之下渐变之上——挂载点在 `GradientBackdrop` 之后第一个子级）放 `DressBackdrop(uiState.todayOutfit)`；日期副行下加一枚「今日之裙 · {title}」小签（`BadgeChip` 复用），点击回调 `onOpenGallery`；`newUnlock` 非空时同位置浮出「新裙入柜 ♡ {title}」2.5s。
- [ ] **Step 5: 跑 `:feature:home:test` + Robolectric e2e 既有用例回归 → 提交**：`git commit -m "feat(feature-home): 画卷上屏与今日穿搭签——打卡联动解锁轻提示（§5.2）"`

---

### Task 7: 画廊模块——衣柜与馆藏

**Files:**
- Create: `feature/gallery/build.gradle.kts`（照抄 `feature/stats/build.gradle.kts` 的插件与依赖块，namespace=`com.awakedw.feature.gallery`）
- Modify: `settings.gradle.kts`（`include(":feature:gallery")`）、`app/build.gradle.kts`（implementation project）
- Create: `feature/gallery/src/main/kotlin/com/awakedw/feature/gallery/GalleryViewModel.kt`、`GalleryScreen.kt`、`components/OutfitCard.kt`
- Modify: `app/src/main/java/com/awakedw/app/AwakeNavHost.kt`（新路由 + 首页入口接线）
- Test: `feature/gallery/src/test/kotlin/com/awakedw/feature/gallery/GalleryViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1/2/4/5 全部；既有 `AwakeDestination` 模式。
- Produces:

```kotlin
data class GalleryItemUi(val outfit: Outfit, val unlocked: Boolean, val pinned: Boolean)

data class GalleryUiState(
    val dresses: List<GalleryItemUi>,   // DRESS 按 unlockDay 升序
    val museum: List<GalleryItemUi>,    // MUSEUM 按 unlockDay 升序
)

class GalleryViewModel(prefs: UserPreferencesRepository) : ViewModel() {
    val uiState: StateFlow<GalleryUiState>
    fun pin(outfitId: String?)   // 再点同一件 = 取消钉选（置 null）
}
// AwakeDestination.Gallery = AwakeDestination("gallery")
// HomeScreen 接线：AwakeNavHost 里 HomeScreen(onOpenGallery = { navController.navigate(AwakeDestination.Gallery.route) { launchSingleTop = true } })
```

- [ ] **Step 1: 写失败测试**——VM 测试（假 prefs，Turbine）：未解锁件 `unlocked=false`；`pin("dress_01")` 后该件 `pinned=true`、其余 false；再 `pin("dress_01")` 取消。showsBottomBar 语义测试追加：`showsBottomBar("gallery") == false`。
- [ ] **Step 2-4: 红→实现→绿**——VM 用 `combine(prefs.unlockedOutfits, prefs.pinnedOutfitId)` 派生双列表。UI：顶栏（返回 + 标题「衣橱」）+ 两个分区 tab（「裙装」「馆藏」）+ 2 列网格 `OutfitCard`（缩略图用 `rememberAssetImageOrN`，锁定件显示剪影 + 「第 N 天解锁」小字，注意：这不是惩罚文案，是期待感文案）；点开详情底部弹层：大图 + `note` + 「设为今日之裙 / 取消指定」按钮（即 pin）。导航：gallery 路由不在 `MAIN_TAB_ROUTES` → 底栏自动隐藏，返回键回原页签（`popUpTo(AwakeDestination.Home.route)` 语义沿用 navigateToTab）。
- [ ] **Step 5: assembleDebug 产出 APK（阶段验证点）→ 提交**：`git commit -m "feat(feature-gallery): 衣橱画廊——解锁网格/馆藏分区/设为今日之裙（§5.2）"`

---

### Task 8: 胆大王领域——猫语料与状态纯函数

**Files:**
- Modify: `core/domain/src/main/kotlin/com/awakedw/core/domain/contracts/CopyLibrary.kt`（追加可空字段）、`contracts/CopyLibraryRepository.kt`
- Modify: `core/data/src/main/kotlin/com/awakedw/core/data/copy/DefaultCopies.kt`、`CopyLibraryRepository.kt`（实现）
- Create: `core/model/src/main/kotlin/com/awakedw/core/model/Cat.kt`
- Test: `core/data/src/test/kotlin/com/awakedw/core/data/copy/CatLineTest.kt`、`core/model/src/test/kotlin/com/awakedw/core/model/CatTest.kt`

**Interfaces:**
- Consumes: 既有 CopyLibrary 序列化（kotlinx-serialization 存 `copy_library_json`）。
- Produces:

```kotlin
// CopyLibrary 追加（旧 JSON 缺字段自动回落空列表——序列化兼容必须测试）：
val cat: List<String> = emptyList()
// CopyLibraryRepository 追加：
suspend fun randomCatLine(avoidRecent: Int = 5): String   // 组空回退默认组第一句
// Cat.kt：
enum class CatMood { IDLE, HAPPY, SLEEPY }
enum class CatAccessory(val unlockDay: Int, val assetFile: String) {
    BOW(3, "cat/acc_bow.webp"), PEARL(14, "cat/acc_pearl.webp"), OUTFIT(30, "cat/acc_dress.webp")
}
fun unlockedCatAccessories(streakDays: Int): List<CatAccessory>
fun resolveCatMood(justCelebrated: Boolean, nowHour: Int): CatMood  // celebrated→HAPPY；22–06→SLEEPY；否则 IDLE
```

- [ ] **Step 1: 写失败测试**——反序列化兼容：`Json.decodeFromString<CopyLibrary>("{\"morning\":[\"a\"],\"day\":[],\"evening\":[]}")` 成功且 `cat` 为空列表；`randomCatLine` 从 cat 组抽取且默认组为空时回退首句不抛；`resolveCatMood(true, 14)==HAPPY`、`resolveCatMood(false, 23)==SLEEPY`、`resolveCatMood(false, 14)==IDLE`；`unlockedCatAccessories(30)` 含全部三件、`(2)` 为空。
- [ ] **Step 2-4: 红→实现→绿**——DefaultCopies 追加 `cat` 组（20 句，内容逐字用附录 C），实现 `randomCatLine` 复用既有去重池模式。实现注意：既有 JSON 无 cat 字段 → `CopyLibrary` 必须给默认值（kotlinx 对缺失字段用默认值，需 `@Serializable` 类字段默认值即可）。
- [ ] **Step 5: 提交**：`git commit -m "feat(core-model+data): 胆大王语料组与猫状态纯函数——序列化向后兼容"`

---

### Task 9: 猫立绘落位——资产优先、矢量兜底

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/awakedw/core/designsystem/art/CatFigure.kt`
- Test: `core/designsystem/src/test/kotlin/com/awakedw/core/designsystem/art/CatFigureTest.kt`（Robolectric compose）

**Interfaces:**
- Consumes: Task 4 装载器、Task 8 `CatMood`/`CatAccessory`。
- Produces:

```kotlin
/**
 * 胆大王（moodboard §6）：96dp 见方常驻首页一角。
 * 有资产用图（idle/happy/sleepy 三态 + 配饰 overlay 叠绘），
 * 无资产画内置矢量简笔猫（Canvas：圆头+三角耳+卷尾曲线，主题色调用）——体验先行，资产后补即生效。
 * 微动效常驻：呼吸缩放 1.00→1.02（3s 循环，SLEEPY 减半）；HAPPY 一次 spring 弹跳。
 */
@Composable
fun CatFigure(mood: CatMood, accessories: List<CatAccessory>, modifier: Modifier = Modifier, onPet: () -> Unit = {})
```

- [ ] **Step 1: 写失败测试**——三种 mood 组合均不崩溃（Robolectric `createComposeRule`，断言至少一个语义节点存在）；onPet 点击触发。
- [ ] **Step 2-4: 红→实现→绿**——矢量兜底猫：Canvas 画头部圆 + 双耳三角 + 身体椭圆 + `drawBow`（复用 LolitaDraw）于 BOW 解锁时；配饰资产存在则叠绘在锚点比例位（bow=头顶 0.18h、pearl=颈 0.52h、dress=身 0.72h）。`pointerInput(Unit) { detectTapGestures { onPet() } }`。
- [ ] **Step 5: 提交**：`git commit -m "feat(designsystem): 胆大王立绘组件——三态/配饰叠绘/矢量兜底"`

---

### Task 10: 记一杯回应编排——猫的反馈

**Files:**
- Modify: `feature/home/src/main/kotlin/com/awakedw/feature/home/HomeViewModel.kt`、`HomeScreen.kt`（猫区挂载 + 气泡）
- Test: `feature/home/src/test/kotlin/com/awakedw/feature/home/HomeViewModelCatTest.kt`

**Interfaces:**
- Consumes: Task 8 全部、Task 9 组件、既有 `logAndPraise` 反馈序列。
- Produces:

```kotlin
// HomeUiState 追加：
val catMood: CatMood = CatMood.IDLE,
val catLine: String? = null,      // 气泡，2.0s 收场（独立于 praiseLine 的 1.4s）
val catAccessories: List<CatAccessory> = emptyList(),
// HomeViewModel 追加注入：private val streakOf: GetStreakUseCase（猫配饰用）
```

- [ ] **Step 1: 写失败测试**——打卡成功：`catMood` 短暂为 HAPPY、`catLine` 命中抽句，2.0s 后 line 清空且 mood 回 IDLE（测试缩小时长常量）；同日已达标后再打卡（celebrated=false）mood 仍 HAPPY 一次（回应每次成笔）；init 时 22 点后 `catMood == SLEEPY`。
- [ ] **Step 2-4: 红→实现→绿**——`logAndPraise` 内并行推进猫序列（epoch 同一防串场）；气泡 UI 复用 PraiseLine 的浮现样式置于猫上方。UI 挂载：`HomeScreen` 底部 leading 角（LogButton 同行对侧），`CatFigure(mood, accessories, onPet = viewModel::petCat)`；`petCat()` = 播一声猫语 + 咕噜音（Task 12 接线，先留接口）。
- [ ] **Step 5: assembleDebug（阶段验证点）→ 全量 `./gradlew build` → 提交**：`git commit -m "feat(feature-home): 胆大王回应编排——记一杯即回应，气泡猫语（moodboard §6.2）"`

---

### Task 11: :core:sound 模块——SoundPool 基建

**Files:**
- Create: `core/sound/build.gradle.kts`（android library + hilt + ktlint，依赖 `:core:domain`）
- Modify: `settings.gradle.kts`
- Create: `core/sound/src/main/kotlin/com/awakedw/core/sound/SoundEvent.kt`、`AwakeSoundPlayer.kt`、`SoundPoolPlayer.kt`、`di/SoundModule.kt`
- Test: `core/sound/src/test/kotlin/com/awakedw/core/sound/SoundPolicyTest.kt`（纯 JVM）+ `SoundPoolPlayerTest.kt`（Robolectric）

**Interfaces:**
- Consumes: Task 2 的 `soundEnabled`。
- Produces:

```kotlin
enum class SoundEvent(val rawName: String) {
    DROP_A("drop_a"), DROP_B("drop_b"), DROP_C("drop_c"),
    GOAL_MELODY("goal_melody"), PURR("purr"),
}

interface AwakeSoundPlayer { fun play(event: SoundEvent) }

// SoundPolicy（纯函数，JVM 测试）：
fun shouldPlay(ringerMode: Int, soundEnabled: Boolean): Boolean  // ringerMode==RINGER_MODE_NORMAL && soundEnabled
// SoundPoolPlayer @Singleton：resId 用 resources.getIdentifier(rawName, "raw", packageName)，<=0 → no-op；
// play() 随机微变调 rate ∈ [0.94, 1.06]（DROP_*），GOAL_MELODY 固定 1.0；
// init 内部 CoroutineScope(SupervisorJob() + Dispatchers.Default) 收集 prefs.soundEnabled → @Volatile 缓存。
```

- [ ] **Step 1: 写失败测试**——`shouldPlay(RINGER_MODE_NORMAL, true)` 真；静音/振动模式假；`soundEnabled=false` 假。Robolectric：无 raw 资源时 `play()` 不抛且不 crash（真实设备资产由用户按附录 B 后补）。
- [ ] **Step 2-4: 红→实现→绿**——SoundPool(maxStreams=2, AudioAttributes USAGE_MEDIA/CONTENT_TYPE_SONIFICATION)。Hilt：`SoundModule` 绑定 `AwakeSoundPlayer → SoundPoolPlayer`。
- [ ] **Step 5: 提交**：`git commit -m "feat(core-sound): SoundPool 基建——静音遵从/缺资源 no-op/微变调（moodboard §7）"`

---

### Task 12: 声音接线——设置开关与触发点

**Files:**
- Modify: `feature/settings/src/main/kotlin/com/awakedw/feature/settings/SettingsViewModel.kt`、`SettingsScreen.kt`（新「声音」卡片区，一枚 switch）
- Modify: `feature/home/src/main/kotlin/com/awakedw/feature/home/HomeViewModel.kt`（打卡→DROP_* 随机；celebrated→GOAL_MELODY；petCat→PURR）
- Modify: `app/src/main/java/com/awakedw/app/AwakeNavHost.kt`（SettingsViewModel 工厂与 HomeViewModel Hilt 注入追加 AwakeSoundPlayer——经 EntryPoint）
- Test: `feature/settings/src/test/.../SettingsSoundTest.kt`、`feature/home/src/test/.../HomeSoundTriggerTest.kt`

**Interfaces:**
- Consumes: Task 11 全部。
- Produces: HomeViewModel 构造器追加 `private val sound: AwakeSoundPlayer`（主构造与 @Inject 构造同步——照 JSR-330 缺省参数委托惯例）；SettingsUiState 追加 `soundEnabled: Boolean = true` 与 `fun setSoundEnabled(v: Boolean)`（乐观更新 + prefs 落库）。
- [ ] **Step 1: 写失败测试**——设置：开关切换后 prefs 假件收到 `setSoundEnabled`；首页：打卡成功触发 DROP_A/B/C 之一、celebrated 时追加 GOAL_MELODY、petCat 触发 PURR（假 player 记录调用）。
- [ ] **Step 2-4: 红→实现→绿**——UI：设置页「外观」区之后新增「声音」卡：label「音效」副文案「水滴与八音盒；系统静音时自动安静」+ switch。
- [ ] **Step 5: 提交**：`git commit -m "feat(settings+home): 声音开关与三触发点接线——打卡/达标/撸猫"`

---

### Task 13: 动效打磨——光袋与庆祝同步

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/awakedw/core/designsystem/art/LightPocket.kt`
- Modify: `feature/home/src/main/kotlin/com/awakedw/feature/home/HomeScreen.kt`（猫与按钮区光袋；庆祝序列并入猫 HAPPY+旋律）
- Test: `core/designsystem/src/test/kotlin/com/awakedw/core/designsystem/art/LightPocketTest.kt`

**Interfaces:**
- Consumes: 既有 CelebrationOverlay、Task 9/10/12。
- Produces:

```kotlin
/** 光袋（moodboard §2 光·遇手法）：可交互元素旁的呼吸光晕。radius 96–160dp，呼吸周期 3s，幅度 alpha 0.06→0.14。 */
@Composable fun LightPocket(modifier: Modifier, color: Color = currentThemeSpec().haloColor)
```

- [ ] **Step 1: 写失败测试**——组合不崩溃 + 呼吸参数纯函数 `pocketAlpha(phase: Float): Float`（0f→0.06f、0.5f→0.14f、1f→0.06f）。
- [ ] **Step 2-4: 红→实现→绿**——radial-gradient `drawBehind` + `rememberInfiniteTransition` 呼吸；猫区与 LogButton 后方各一枚；庆祝时刻编排确认：缎带展开（既有）与猫 HAPPY 弹跳、GOAL_MELODY 同帧触发。
- [ ] **Step 5: assembleDebug（阶段验证点）→ 提交**：`git commit -m "feat(designsystem+home): 光袋微引导与庆祝三联同步（§2/§7）"`

---

### Task 14: 中国色再映射——四主题锚点迁移

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/awakedw/core/designsystem/ThemePalette.kt`（锚点常量重命名+换值）、`Themes.kt`（引用同步）、`AwakeTheme.kt` 若有派生 laceColor 校准
- Test: `core/designsystem/src/test/kotlin/com/awakedw/core/designsystem/ThemePaletteSnapshotTest.kt`（更新既有防漂移快照为新锚点）

**Interfaces:** Consumes/Produces：不改 `ThemeSpec` 结构，只换色值与锚点命名（新常量名用拼音+汉字注释，如 `VAL_TIANSHUIBI = 0xFFD4F2E7 // 天水碧`）。

- [ ] **Step 1: 更新快照测试为目标锚点**（先红）。目标锚点（实现时到 zhongguose.com 逐个校准，容差内取站值，锁定后快照防漂移；下列为指示性值）：
  - 清晨（原翡翠绿）：底 天水碧 `#D4F2E7`、面 月白 `#D6ECF0`、强调 柏枝绿（站值校准）
  - 午后（原草莓雾光）：底 藕荷 `#E4C6D0`、面 樱花浅（站值校准）、强调 十样锦 `#F8B37F`
  - 黄昏（原焦糖奶茶）：底 缃叶浅（站值校准）、面 驼 `#A88462`、强调 秋香 `#D9B611`
  - 深夜（原墨青）：底 青黛 `#45465E`、面 鸦青 `#424C50`、强调 月白（降饱和变体）
- [ ] **Step 2-4: 红→迁移→绿**——迁移纪律：仅动色值与常量名，不动任何布局/透明度/渐变结构； laceColor 派生四主题各验一遍（深夜呈暗银描金）；快照测试全绿 + 全量 `./gradlew build`。
- [ ] **Step 5: 提交**：`git commit -m "feat(designsystem): 四主题中国色再映射——天水碧/藕荷/缃叶/青黛（moodboard §4）"`

---

### Task 15: v0.2.0 发版

**Files:**
- Modify: `app/build.gradle.kts`（versionCode 3 / versionName "0.2.0"）、`CHANGELOG.md`（Unreleased → [v0.2.0] - 发布日 + Download 段）
- Create: `docs/superpowers/checklists/v0.2-manual-qa.md`（真机清单：四主题画卷可读性/夜变体/画廊 pin/猫三态/配饰解锁/静音遵从/解锁时机与防重/图标与旧版共存升级安装）
- 归档：`dist/Awake_DW-v0.2.0-debug.apk`、`dist/Awake_DW-v0.2.0-release.apk`；`git tag v0.2.0`；推送。

- [ ] **Step 1: 全量 `./gradlew build` 绿 → Step 2: 双包归档 dist → Step 3: CHANGELOG/QA 清单/版本号提交 → Step 4: `git tag v0.2.0 && git push origin main --tags`**。
- [ ] **Step 5: 提交信息**：`chore(release): v0.2.0——画卷/画廊/胆大王/声音/中国色`。

---

## 附录 A：即梦出图提示词模板（用户操作轨，与工程轨并行）

统一画风后缀（每条提示词末尾都带）：
> 水彩插画，少女绘本风，低饱和奶油色调，纸张纹理，边缘晕染，柔和光线，柔和模糊背景，治愈系，精致细腻

负面提示词（若即梦支持）：
> 写实照片，3D渲染，高饱和荧光色，杂乱背景，文字，水印，人物面部特写

| 产物 | 提示词骨架 | 规格 |
|---|---|---|
| 裙装主图（无人物） | 「一件洛丽塔裙装的静物插画，裙撑层叠，蕾丝与缎带细节，[主题色描述]为主色，浅色纯背景，裙摆微微飘起」+ 画风后缀 | 9:16 竖图，导出后压 WebP（目标 ≤800KB/张），命名 `dress_XX.webp` |
| 主题色描述 | dress_00 素白米 / dress_01 天水碧青绿 / dress_02 十样锦橙粉 / dress_03 藕荷紫粉 / dress_04 缃叶暖黄 / dress_05 月白夜色缀猫眼石 / dress_06 秋香黄绿 / dress_07 绯红缎面 | 逐张对应目录 |
| 夜变体（可选） | 同提示词 + 「夜晚低亮度版本，整体压暗三成，月光冷调」 | `dress_XX_night.webp` |
| 馆藏替代 | 不用 AI——直接下博物馆 CC0 高清（Met/Rijksmuseum/NGA，见 moodboard §8），裁 9:16 + 顶部/底部加柔和同色渐变遮罩后压 WebP | `museum_XX.webp` |
| 猫三态 | 「Q版水彩小猫，2.5头身，圆脸大眼，米白底毛淡橘斑，[坐姿抬尾/举爪扑水滴/蜷缩打盹闭眼]，纯色浅背景」+ 画风后缀 | 1:1 方图，`cat/idle|happy|sleepy.webp` |
| 猫配饰 | 「[缎带蝴蝶结/珍珠项链/迷你洛丽塔小裙]，水彩质感，纯白背景」+ 画风后缀 | 1:1，纯白底抠图成透明，`cat/acc_bow|acc_pearl|acc_dress.webp` |

交付方式：图片放进 `app/src/main/assets/outfit/` 与 `app/src/main/assets/cat/`（目录不存在则新建），文件名严格按上表——命名即接线，放入即生效，无需改代码。

## 附录 B：音频来源与处理规范（用户操作轨）

- 来源：[freesound.org](https://freesound.org)（筛选条件勾 CC0）；检索词：「water drop single」「music box short melody」「cat purr short」。备选：自己用手机录水滴（滴在瓷碗上，三连录三次取三变体）。
- 处理（Audacity）：归一化 -3dB；导出 OGG（质量 6/44.1kHz/单声道）；时长 DROP ≤0.6s、GOAL_MELODY 3–5s、PURR 1–2s。
- 交付：`core/sound/src/main/res/raw/drop_a.ogg`、`drop_b.ogg`、`drop_c.ogg`、`goal_melody.ogg`、`purr.ogg`（目录不存在则新建）。未交付期间 Task 11/12 的 no-op 保证零崩溃。

## 附录 C：胆大王默认语料（DefaultCopies.cat，20 句）

1. 咕噜咕噜——你回来了，本大王闻到你今天也很努力。
2. 喝水要小口小口哦，本大王看着呢。
3. 尾巴尖摇了一下，是替你开心的意思。
4. 今天的太阳晒得本大王想打盹，你别忘了喝水呀。
5. 罐头可以晚点开，水不能晚点喝——本大王的规矩。
6. 你和水，都是本大王的珍宝。
7. 刚舔了舔爪子，替你把今天的运气擦亮了一点。
8. 肚子咕噜噜？不是饿，是在给你念喝水咒语。
9. 本大王巡视了一圈，你的水杯就是全屋最尊贵的宝座。
10. 晚上也要好好喝水，然后好好睡觉，本大王守着。
11. 别催我陪玩，先把那杯水喝了——嗯，真乖。
12. 听说多喝水的人，摸猫的手感会变好哦。
13. 本大王把窗边最暖的位置留给你，喝完水来晒太阳。
14. 今天的你也很值得被温柔对待，水也要好好喝。
15. 胡须抖了抖——这个方向的风里，有水杯的味道。
16. 水是流动的宝石，你喝下去，眼睛里也会有光。
17. 本大王从不催人，只是恰好在你想偷懒时踩了一下键盘。
18. 累积的每一杯，本大王都记在尾巴的环纹里了。
19. 下雨了就听雨，天晴了就喝水，怎么样都好，慢慢来。
20. 今天也谢谢你喜欢我。先干为敬——咕嘟。

## 自查记录（写完计划后的 spec 覆盖核对）

- moodboard §3 风味 γ → Task 14（色板）+ 附录 A（调色后缀）✓；§4 中国色 → Task 14 ✓；§5.1 呈现规范 → Task 5 ✓；§5.2 收集循环（每日随机/解锁/画廊/馆藏/pin）→ Task 1/2/3/7 ✓；§5.3 首发规模 → Task 1 目录 12 件 + 附录 A 规格 ✓；§6 猫（三态/零惩罚/配饰/气泡）→ Task 8/9/10 ✓；§7 声音 → Task 11/12 + 附录 B ✓；§8 素材授权 → 附录 A 馆藏行 ✓；§2 光袋/无失败/装饰克制 → Task 13 + Global Constraints ✓；§11 阶段五中国色与发版 → Task 14/15 ✓。里程碑数值（3/5/7/10/14/21/25/30/50/60/100 天）为首发定值，收集后可依真机体验调表——只动 Task 1 目录数据，不动机制。

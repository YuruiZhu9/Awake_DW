# Awake_DW 计划 A · 地基与核心 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 Gradle 多模块安卓工程骨架，完成 model/common/data/domain 四个核心层与设计系统（三主题色板、噪点、光晕、粒子引擎、进度环），全部带测试且整体构建绿灯。

**Architecture:** 按 `rules.md` 与设计文档 §5 的多模块拓扑落地。纯 Kotlin 模块（model/common/domain）不含任何 Android 依赖并通过 JVM 单测验证；data 层封装 Room/DataStore 并只经 Repository 接口外露；designsystem 承载三套 ThemeSpec 与共享绘制组件，供后续所有 feature 复用。

**Tech Stack:** Kotlin 2.0 · Jetpack Compose · Hilt · Room · DataStore(kotlinx-serialization JSON) · ktlint

**Spec:** `docs/superpowers/specs/2026-08-27-water-reminder-design.md`（配套 `rules.md` 全文约束）

## Global Constraints

- minSdk 26；compileSdk/targetSdk 35；Kotlin 2.0.21；AGP 8.7.3；Java 17 工具链
- 依赖版本统一走 `gradle/libs.versions.toml`：compose-bom 2024.12.01、hilt 2.52、room 2.6.1、datastore 1.1.1、coroutines 1.9.0、junit 4.13.2、robolectric 4.14.1、turbine 1.2.0、kotlinx-serialization-json 1.7.3
- **禁止引入任何网络/统计类依赖**（无 retrofit/okhttp/analytics）
- UI 文案一律简体中文；提交信息格式 `<type>: <中文摘要>`
- 领域与时间相关逻辑必须注入 `AppClock`，禁止直接调用 `System.currentTimeMillis()` 于被测代码内
- 包名根：`com.awakedw.*`；applicationId：`com.awakedw.app`
- 环境为 Windows + Git Bash；gradle 命令写作 `./gradlew`
- 每完成一步将本文件对应 `- [ ]` 勾选并随代码一并提交

---

### Task 1: 多模块 Gradle 骨架（构建绿灯即完成）

**Files:**
- Create: `settings.gradle.kts`、`build.gradle.kts`(根)、`gradle.properties`、`gradle/libs.versions.toml`
- Create: 各模块 `build.gradle.kts`：`:app`、`:core:model`、`:core:common`、`:core:domain`、`:core:data`、`:core:notification`、`:core:designsystem`、`:feature:home`、`:feature:stats`、`:feature:settings`、`:feature:onboarding`
- Create: `app/src/main/AndroidManifest.xml`、`app/src/main/java/com/awakedw/app/MainActivity.kt`、`app/src/main/res/values/themes.xml`
- Modify: `.gitignore`（追加 `build/`、`local.properties`、`.idea/`、`*.iml`、`.gradle/`）

**Interfaces:**
- Produces: 可用的空模块集与 Version Catalog 别名（后续任务的 `libs.*` 引用以此为准）；`:core:model` 等 Pure-Kotlin 模块的 `org.jetbrains.kotlin.jvm` 插件约定；Compose 全部配置于需要它的模块

- [ ] **Step 1: 环境探针**

```bash
java -version        # 需要 >= 17；缺失则：winget install --id Microsoft.OpenJDK.17 -e --silent
echo $ANDROID_HOME   # 缺失则下载 commandlinetools 解压到 %LOCALAPPDATA%/Android/Sdk，
                     # sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools" 并 yes | sdkmanager --licenses
gradle -v || ls "$HOME/.gradle"  # 无 gradle 则取 https://services.gradle.org/distributions/gradle-8.11.1-bin.zip 解压并用其 bin/gradle 生成 wrapper
```

记录实际安装路径；将 SDK 路径写入 `local.properties`（该文件不入库）。

- [ ] **Step 2: 写根构建文件与目录清单**

`settings.gradle.kts`：

```kotlin
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }

rootProject.name = "Awake_DW"
include(":app")
include(":core:model", ":core:common", ":core:domain", ":core:data", ":core:notification", ":core:designsystem")
include(":feature:home", ":feature:stats", ":feature:settings", ":feature:onboarding")
```

根 `build.gradle.kts`：

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.compose.compiler) apply false
}
```

`gradle.properties`：

```properties
org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
org.gradle.configuration-cache=true
```

`gradle/libs.versions.toml`（关键别名，后续所有任务引用此表）：

```toml
[versions]
agp = "8.7.3"; kotlin = "2.0.21"; compose-bom = "2024.12.01"
hilt = "2.52"; room = "2.6.1"; datastore = "1.1.1"
coroutines = "1.9.0"; serialization = "1.7.3"
splashscreen = "1.0.1"; lifecycle = "2.8.7"; navigation = "2.8.5"
junit = "4.13.2"; robolectric = "4.14.1"; turbine = "1.2.0"; ktlint = "12.1.1"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-test = { module = "androidx.compose.ui:ui-test-junit4" }
activity-compose = { module = "androidx.activity:activity-compose", version = "1.9.3" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
splashscreen = { module = "androidx.core:core-splashscreen", version.ref = "splashscreen" }
junit = { module = "junit:junit", version.ref = "junit" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
```

- [ ] **Step 3: 建 11 个模块的 build 文件与源码目录**

规则（每个模块照做）：纯 Kotlin 模块（model/common/domain）用 `alias(libs.plugins.kotlin.jvm)` + `serialization`（仅需要者），`dependencies { implementation(libs.coroutines.core); testImplementation(libs.junit); testImplementation(libs.coroutines.test); testImplementation(libs.turbine) }`。Android 库模块（data/notification/designsystem/各 feature）用 `android-library`（需在同一 toml 增加 `android-library = { id = "com.android.library", version.ref = "agp" }`）+ kotlin-android，compileSdk 35/minSdk 26，Compose 的加 compose-bom 与 `compose.compiler{}(kotlin 2.0 下由插件自动)`。示例——`:core:model/build.gradle.kts`：

```kotlin
plugins { alias(libs.plugins.kotlin.jvm); alias(libs.plugins.kotlin.serialization) }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
dependencies {
    testImplementation(libs.junit); testImplementation(libs.coroutines.test); testImplementation(libs.turbine)
}
```

示例——`:core/designsystem/build.gradle.kts`：

```kotlin
plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.compose.compiler) }
android { namespace = "com.awakedw.core.designsystem"; compileSdk = 35
    defaultConfig { minSdk = 26 }; compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" } }
dependencies {
    api(platform(libs.compose.bom)); api(libs.compose.ui); api(libs.compose.material3)
    implementation(project(":core:model")); implementation(project(":core:common"))
    testImplementation(libs.junit)
}
```

其余模块同理，namespace 依次 `com.awakedw.{core.data|core.notification|core.common|core.domain|feature.home|feature.stats|feature.settings|feature.onboarding}`；跨模块 `implementation(project(...))` 依赖按 §5.2 铁律接好（data→common/model；domain→model/common；notification→common/model/domain 接口；features→designsystem/model/common/domain；app→全部 feature 与 core:data/notification）。

- [ ] **Step 4: 最小可运行的 ：app 壳**

`app/build.gradle.kts`：

```kotlin
plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android); alias(libs.plugins.compose.compiler); alias(libs.plugins.hilt) }
android {
    namespace = "com.awakedw.app"; compileSdk = 35
    defaultConfig { applicationId = "com.awakedw.app"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform(libs.compose.bom)); implementation(libs.compose.ui); implementation(libs.compose.material3)
    implementation(libs.activity.compose); implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)   // KSP 插件见下方更正说明
    implementation(project(":core:designsystem"))
}
```

> 更正说明：Hilt 注入处理器采用 **KSP**。在同一 toml `[plugins]` 增加 `ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }`，根插件块加 `alias(libs.plugins.ksp) apply false`；`:app`、`:core:data`(room-compiler)、`:core:notification` 及各 feature(Hilt VM 注入)均需 `plugins {}` 引入 ksp 并声明对应 `ksp(...)` 依赖。

`MainActivity.kt` 冒烟内容：

```kotlin
package com.awakedw.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Awake_DW") }
    }
}
```

- [ ] **Step 5: 生成 wrapper 并全量构建**

```bash
gradle wrapper --gradle-version 8.11.1
./gradlew ktlintFormat build      # 期望 BUILD SUCCESSFUL，全部模块编译且测试(暂无)通过
git add -A && git commit -m "chore: 初始化多模块 Gradle 骨架与版本目录"
```

Expected: 绿灯。若 configuration-cache 与 ktlint 冲突，去掉 `org.gradle.configuration-cache=true` 即可。

---

### Task 2: `:core:model` 领域模型

**Files:**
- Create: `core/model/src/main/kotlin/com/awakedw/core/model/{WaterRecord,DailyStats,WeekBar,TimeSlot,ThemeChoice,ThemeId,UserSettings}.kt`
- Test: `core/model/src/test/kotlin/com/awakedw/core/model/UserSettingsTest.kt`

**Interfaces:**
- Consumes: 无
- Produces（后续全部任务的基石，签名必须逐字一致）:

```kotlin
package com.awakedw.core.model
data class WaterRecord(val id: Long = 0L, val amountMl: Int, val drankAtEpochMs: Long, val dayKeyLocal: String)
data class DailyStats(val totalMl: Int, val cupCount: Int, val avgIntervalMin: Int?) // 杯数<2 时 avgIntervalMin=null
data class WeekBar(val dayKey: String, val totalMl: Int)
enum class TimeSlot { MORNING, DAY, EVENING }
enum class ThemeChoice { FOLLOW_TIME, FIXED_EMERALD, FIXED_STRAWBERRY, FIXED_CARAMEL }
enum class ThemeId { EMERALD, STRAWBERRY, CARAMEL }
data class UserSettings(
    val goalMl: Int = 1600, val cupMl: Int = 250,
    val windowStartMin: Int = 480, val windowEndMin: Int = 1350, // 08:00–22:30
    val intervalMin: Int = 90,
    val remindersEnabled: Boolean = true,
    val themeChoice: ThemeChoice = ThemeChoice.FOLLOW_TIME,
)
```

- [ ] **Step 1: 先写失败测试** `UserSettingsTest.kt`：

```kotlin
package com.awakedw.core.model
import org.junit.Assert.assertEquals
import org.junit.Test

class UserSettingsTest {
    @Test fun `默认值为设计与文档常量`() {
        val s = UserSettings()
        assertEquals(1600, s.goalMl); assertEquals(250, s.cupMl)
        assertEquals(480, s.windowStartMin); assertEquals(1350, s.windowEndMin)
        assertEquals(90, s.intervalMin); assertEquals(true, s.remindersEnabled)
        assertEquals(ThemeChoice.FOLLOW_TIME, s.themeChoice)
    }
    @Test fun `建议饮水量常量与膳食指南一致`() {
        assertEquals(1500, RECOMMENDED_MIN_ML); assertEquals(1700, RECOMMENDED_MAX_ML)
    }
}
```

同文件顶部模型文件里增加常量 `const val RECOMMENDED_MIN_ML = 1500; const val RECOMMENDED_MAX_ML = 1700`（《中国居民膳食指南》推荐量，用于健康贴士行展示）。
- [ ] **Step 2:** `./gradlew :core:model:test` Expected: FAIL（常量未定义）
- [ ] **Step 3: 实现** 上述全部类型与两个常量（放 `UserSettings.kt` 顶部）
- [ ] **Step 4:** `./gradlew :core:model:test` Expected: PASS
- [ ] **Step 5:** `git add -A && git commit -m "feat(core-model): 领域模型与健康饮水量常量"`

---

### Task 3: `:core:common` 时钟与时段判定（TDD 边界驱动）

**Files:**
- Create: `core/common/src/main/kotlin/com/awakedw/core/common/{AppClock,SystemAppClock,TimeSlots,DayKeys}.kt`
- Test: `core/common/src/test/kotlin/com/awakedw/core/common/TimeSlotsTest.kt`

**Interfaces:**
- Consumes: `core.model.TimeSlot`
- Produces:

```kotlin
interface AppClock { fun nowEpochMs(): Long; fun zone(): ZoneId }
class SystemAppClock @Inject constructor() : AppClock          // @Inject 仅为将来 Hilt 预留，此处不引 hilt 依赖则去掉注解
object TimeSlots { fun slotOfHour(hour24: Int): TimeSlot }     // 6–10→MORNING, 11–17→DAY, 其余→EVENING
fun Long.toDayKey(zone: ZoneId): String                        // "yyyy-MM-dd" 本地日期
```

- [ ] **Step 1: 失败测试**（节选断言，须全覆盖边界小时）：

```kotlin
@Test fun `时段边界`() {
    assertEquals(TimeSlot.EVENING, TimeSlots.slotOfHour(5))
    assertEquals(TimeSlot.MORNING, TimeSlots.slotOfHour(6))
    assertEquals(TimeSlot.MORNING, TimeSlots.slotOfHour(10))
    assertEquals(TimeSlot.DAY, TimeSlots.slotOfHour(11))
    assertEquals(TimeSlot.DAY, TimeSlots.slotOfHour(17))
    assertEquals(TimeSlot.EVENING, TimeSlots.slotOfHour(18))
    assertEquals(TimeSlot.EVENING, TimeSlots.slotOfHour(3))
}
@Test fun `dayKey 按本地时区切日`() {
    // 2026-08-27 22:00 UTC+8 == 同日本地日
    val ms = ZonedDateTime.of(2026, 8, 27, 22, 0, 0, 0, ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
    assertEquals("2026-08-27", ms.toDayKey(ZoneId.of("Asia/Shanghai")))
}
```

- [ ] **Step 2:** `./gradlew :core:common:test` Expected: FAIL
- [ ] **Step 3: 实现** 四个成员；`toDayKey` 用 `DateTimeFormatter.ofPattern("yyyy-MM-dd")`
- [ ] **Step 4:** 测试转绿
- [ ] **Step 5:** `git commit -m "feat(core-common): 时钟抽象与时段边界判定"`

---

### Task 4: `:core:data` Room 仓储（内存库 TDD）

**Files:**
- Create: `core/data/src/main/kotlin/com/awakedw/core/data/`下 `db/{WaterRecordEntity,WaterRecordDao,AwakeDb}.kt`、`repo/{WaterRepository,RoomWaterRepository}.kt`、`di/DataModule.kt`
- Test: `core/data/src/test/kotlin/com/awakedw/core/data/RoomWaterRepositoryTest.kt`

**Interfaces:**
- Consumes: `AppClock`、`WaterRecord/DailyStats/WeekBar`
- Produces:

```kotlin
interface WaterRepository {
    suspend fun addCup(amountMl: Int): WaterRecord
    suspend fun todayStats(): DailyStats
    suspend fun weekBars(daysBack: Int = 7): List<WeekBar>   // 含今天，共 daysBack 天，缺数天补 0
    suspend fun todayRecords(): List<WaterRecord>            // 时间升序
}
```

Entity 即设计文档列 `water_record(id PK autogen, amount_ml, drank_at_epoch_ms INDEX, day_key_local INDEX)`。

- [ ] **Step 1: 失败测试**（用 Room.inMemoryDatabaseBuilder + 运行块 runBlocking；clock 用固定假钟 `FakeClock(var ms)`，构造当天 10:00 连续三杯间隔 60/30 分钟的记录后断言：todayStats==DailyStats(total=750,cupCount=3,avgIntervalMin=45)；weekBars 尺寸 7 且末位=750 其余 0；空库时 todayStats==DailyStats(0,0,null)、平均间隔 null 语义成立）：
```kotlin
class FakeClock(var ms: Long) : AppClock { override fun nowEpochMs() = ms; override fun zone() = ZoneId.of("Asia/Shanghai") }
```
- [ ] **Step 2:** `./gradlew :core:data:test` FAIL
- [ ] **Step 3: 实现** Entity/DAO（SQL 聚合）：

```kotlin
@Query("SELECT COALESCE(SUM(amount_ml),0) FROM water_record WHERE day_key_local = :day") suspend fun sumFor(day: String): Int
@Query("SELECT * FROM water_record WHERE day_key_local = :day ORDER BY drank_at_epoch_ms ASC") suspend fun recordsFor(day: String): List<WaterRecordEntity>
@Query("SELECT day_key_local AS d, COALESCE(SUM(amount_ml),0) AS s FROM water_record WHERE day_key_local BETWEEN :from AND :to GROUP BY day_key_local")
suspend fun sumsBetween(from: String, to: String): List<DaySum>
```

`RoomWaterRepository.todayStats()` 内由当前日升序记录计算 `avgIntervalMin = ((last-first)/(n-1)/60000).roundToInt()`；`weekBars()` 以本地日历回推 N 天 dayKey 区间查区间和、补零成序。`di/DataModule.kt` 用 Hilt `@Module @InstallIn(SingletonComponent::class)` 绑定接口与单例 DB（`Room.databaseBuilder`）。模块 build 加 `ksp(libs.room.compiler)`、androidTest 不启用。
- [ ] **Step 4:** PASS
- [ ] **Step 5:** `git commit -m "feat(core-data): Room 水记录仓储与聚合统计"`

---

### Task 5: `:core:data` 设置与文案库存储

**Files:**
- Create: `core/data/src/main/kotlin/com/awakedw/core/data/`下 `prefs/UserPreferencesRepository.kt(+Impl)`、`copy/{CopyLibrary,CopyLibraryRepository,DefaultCopies}.kt`
- Test: `core/data/src/test/.../{UserPreferencesImplTest,CopyLibraryRepositoryTest}.kt`

**Interfaces:**
- Consumes: `UserSettings/TimeSlot/AppClock`
- Produces:

```kotlin
interface UserPreferencesRepository {
    val settings: Flow<UserSettings>
    suspend fun setGoalMl(v: Int); suspend fun setCupMl(v: Int)
    suspend fun setWindow(startMin: Int, endMin: Int); suspend fun setIntervalMin(v: Int)
    suspend fun setRemindersEnabled(v: Boolean); suspend fun setThemeChoice(v: ThemeChoice)
    suspend fun markCelebrated(dayKey: String); suspend fun celebratedDayKey(): String?
    suspend fun markOnboardingDone(); suspend fun onboardingDone(): Boolean
}
data class CopyLibrary(val morning: List<String>, val day: List<String>, val evening: List<String>)
interface CopyLibraryRepository {
    val library: Flow<CopyLibrary>
    suspend fun randomFor(slot: TimeSlot, avoidRecent: Int = 5): String
    suspend fun upsert(slot: TimeSlot, index: Int, text: String); suspend fun delete(slot: TimeSlot, index: Int)
    suspend fun resetToDefaults()
}
```

DataStore 键名严格按设计 §5.3（goal_ml/cup_ml/window_start_min/window_end_min/interval_min/reminders_enabled/theme_mode/copy_library_json/recent_copy_ids/onboarding_done/celebrated_day_key）。文案库序列化为 `copy_library_json`。

- [ ] **Step 1: 失败测试** —— Preferences 用 `PreferenceDataStoreFactory.create(testDataStoreFile)`；断言默认值流首帧=UserSettings() 且 set 后回流新值。CopyLibrary 断言三点：默认 30 条且早/午/晚各 10；`randomFor(MORNING)` 返回早组句子且连抽 20 次 never 出现最近 5 条窗口内的重复；删除到只剩 1 条时不抛异常且回退默认组兜底（当组清空后 randomFor 返回任一默认组句子）。
- [ ] **Step 2:** FAIL
- [ ] **Step 3: 实现**；`DefaultCopies.morning/day/evening` 共 30 条中文短句（语气参考示例：「早安，先喝一口水，把今天叫醒 ☀」「午后啦，来一杯继续元气满满」「今晚的水要小口慢慢喝哦🌙」等，每组 10 条，作者交付前可在设置页整库替换）；抽取消重实现：组内洗牌跳过 `recent_copy_ids` 最近 5 条，池空则清空去重池重来。
- [ ] **Step 4:** PASS
- [ ] **Step 5:** `git commit -m "feat(core-data): 用户设置与关心文案库（含默认 30 句与去重抽取）"`

---

### Task 6: `:core:domain` 用例层（纯 JVM TDD）

**Files:**
- Create: `core/domain/src/main/kotlin/com/awakedw/core/domain/`下 `{ObserveHomeUseCase,LogWaterUseCase,GetStreakUseCase,ResolveThemeUseCase,next}Reminder/NextReminderCalculator.kt`
- Test: `core/domain/src/test/.../*.kt`

**Interfaces:**
- Consumes: Task 4/5 全部接口、`AppClock`、`TimeSlots`
- Produces:

```kotlin
sealed interface LogResult { data class Logged(val record: WaterRecord, val celebrated: Boolean): LogResult }
class LogWaterUseCase(water: WaterRepository, prefs: UserPreferencesRepository, clock: AppClock) {
    suspend operator fun invoke(): LogResult                    // 若当日首次达标：写 celebrated_day_key 并 celebrated=true
}
class GetStreakUseCase(water: WaterRepository, prefs: UserPreferencesRepository) { suspend operator fun invoke(): Int /* 连续达标天数 */ }
class ResolveThemeUseCase(prefs: UserPreferencesRepository, clock: AppClock) { operator fun invoke(): Flow<ThemeId> }
class ObserveHomeUseCase(water, prefs) { operator fun invoke(): Flow<HomeSnapshot> }
data class HomeSnapshot(val stats: DailyStats, val goalMl: Int, val cupMl: Int, val themeId: ThemeId, val streakDays: Int)

object NextReminderCalculator {
    /** null 表示今日不再排程 */
    fun nextFire(s: UserSettings, clock: AppClock, achievedToday: Boolean): Long?
    // 规则：disabled/achieved→null；now<今日窗口起点→起点；窗口内→max(now, 上次基准)+interval，
    // 越过窗口终点(endMin 当日时刻)→null
}
```

- [ ] **Step 1: 失败测试**（fake 内存 Repo 各 10~30 行；重点用例）：
  1. 未达标庆祝只发生一次：连续两次 LogWaterUseCase 首次 celebrated=true 第二次 false；
  2. streak：昨天与今天均达标→2；昨缺→1；goal 改变即时生效；
  3. ResolveTheme：FOLLOW_TIME 在时钟拨过 11 点后流出新值 EMERALD；FIXED_* 直接映射；
  4. NextReminderCalculator 表驱动 6 例：禁用→null｜已达成→null｜07:00(<480)→窗口起点 08:00 的 epoch｜08:50→08:50+90min=10:20｜21:40→越界→null｜windows 改参数后重算正确。
- [ ] **Step 2:** FAIL　- [ ] **Step 3:** 实现（`Invoke` 返回 flow 组合 `combine(settings, 水库变化触发器)`；水库触发器可用 WaterRepository 增加 `val changes: Flow<Unit>`——在 Task 4 接口上补充此字段并由 DAO Flow 查询供给，本任务实现之）
- [ ] **Step 4:** PASS　- [ ] **Step 5:** `git commit -m "feat(core-domain): 打卡/连胜/主题解析/下一提醒计算四大用例"`

---

### Task 7: `:core:designsystem` 三主题与质感底座

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/awakedw/core/designsystem/`下 `ThemeSpec.kt`、`Themes.kt`、`AwakeTheme.kt`、`Backdrop.kt`(背景渐变+GrainOverlay+Halo)
- Test: 设计常量快照测 `ThemePaletteTest.kt`（断言十六进制锚点值不漂移）

**Interfaces:**
- Consumes: `ThemeId`
- Produces:

```kotlin
data class ThemeSpec(
    val id: ThemeId, val backgroundGradient: List<Color>,     // 逐段对应 §2.1 十六进制
    val primary: Color, val ringTrack: Color, val ringValueText: Color,
    val greetingColor: Color, val greetingSubColor: Color,
    val buttonTop: Color, val buttonBottom: Color,
    val chipBg: Color, val chipText: Color,
    val particleColors: List<Color>, val haloColor: Color,
)
val EmeraldThemeSpec: ThemeSpec; val StrawberryThemeSpec: ThemeSpec; val CaramelThemeSpec: ThemeSpec
val ThemeById: Map<ThemeId, ThemeSpec>
@Composable fun AwakeTheme(themeId: ThemeId, content: @Composable () -> Unit)   // 提供 LocalAwakeTheme = CompositionLocal<ThemeSpec>
object LocalAwakeTheme; @Composable fun currentThemeSpec(): ThemeSpec
@Composable fun GradientBackdrop(spec: ThemeSpec, modifier: Modifier)            // 渐变底 + GrainOverlay + Halo
```

- [ ] **Step 1: 失败测试**（JVM 层无法构造 Color——把 hex 锚点声明为 Long 常量表供测试，组件层再转 Color；断言如 `Emerald.primaryHex == 0xFF0FA37A`、Strawberry.buttonTop/bottom、Caramel.greetingColor 等 ≥12 个锚点全中，数值抄自规格 §2.1 表格）
- [ ] **Step 2:** FAIL → **Step 3:** 实现（渐变逐字对照：Emerald `#F3FBF7/#E2F3EC/#D8EEE4`；Strawberry `#FFFFFF/#FFF4F6/#FFEAEE/#FFE5EB`；Caramel `#FFF8EA/#FFEED4/#FFE2C0`；粒子色族照表录入）→ **Step 4:** PASS → **Step 5:** `git commit -m "feat(designsystem): 三主题规格与背景质感底座"`

---

### Task 8: 粒子引擎与进度环组件

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/awakedw/core/designsystem/particles/`下 `GrainOverlay.kt`、`FloatingParticles.kt`、`ParticleMath.kt`；`ring/ProgressRing.kt`、`burst/BurstParticles.kt`
- Test: `ParticleMathTest.kt`（纯函数 JVM 单测）

**Interfaces:**
- Consumes: `ThemeSpec`
- Produces:

```kotlin
/** 纯函数：给定总时长进度 p∈[0,1]，输出第 i 个粒子的位置/透明度——可测性核心 */
object ParticleMath {
    fun floating(index: Int, seed: Long, sizePx: Float, progress01: Float, area: Size): ParticleFrame
    fun burst(travel01: Float, origin: Offset, angleRad: Float, distancePx: Float): ParticleFrame
}
data class ParticleFrame(val center: Offset, val radiusPx: Float, val alpha: Float, val glow: Boolean)

@Composable fun FloatingParticles(colors: List<Color>, modifier: Modifier, seed: Long = 7L)      // 16 粒三层：大(2颗,α.25-.55,辉光6px)/中(6颗,α.45-.65,微辉)/小(8颗,α.6-.75)，极缓上浮顶部渐隐循环；另含 2 枚固定相位「✦」星芒文字粒子（对应规格 §2.2）
@Composable fun GrainOverlay(modifier: Modifier, alpha: Float = 0.05f)                           // 4px 网格点阵 multiply
@Composable fun ProgressRing(progress: Float, modifier: Modifier, onRingTap: (() -> Unit)?, content: @Composable BoxScope.() -> Unit)  // conic 弧线 stroke 圆头；progress 变化用 animateFloatAsState(600ms easeOut)；点击按压 0.97 spring 回弹
@Composable fun BurstParticles(origin: Offset, colors: List<Color>, trigger: Int, onFinish: () -> Unit)  // trigger 自增驱动一次 8-12 粒迸发 900ms
```

- [ ] **Step 1: 失败测试** `ParticleMathTest`：floating 第 0 粒 seed=7 时 y 随 progress01 单调减、超过 0 半径区后 alpha 渐隐至 0 再回卷底部；burst travel01∈{0,0.5,1} 输出位置沿角度线性推进且 alpha 由 1→0 单调；glow 标志与大中小三层一致。
- [ ] **Step 2:** FAIL → **Step 3:** 实现（Random(seed) 固定序初始化三层粒子的初始位置/半径/速度相位；Canvas drawWithCache 内按帧 `withFrameNanos` 推进 progress01；Glow 用中心实心圆+放大低透明度同心圆双层绘制避免 BlurFilter 性能坑）→ **Step 4:** PASS → **Step 5:** `./gradlew build && git commit -m "feat(designsystem): 分层粒子引擎与治愈进度环"`

---

## 计划 A 完成定义（DoD）

- `./gradlew build` 全绿；ktlint 通过
- Task 2–6、8 全部单测随库沉淀（预计 ≥25 个用例）
- `:app` 可安装启动显示 "Awake_DW" 冒烟文本（动画与页面属计划 B）
- 类型契约与本文档 Interfaces 一致，计划 B 将逐字消费

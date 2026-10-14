# 2.6.0 · 统一优化版（动效二阶收尾 + 装饰绘制审计 + 仓库治理）

> 2026-10-13 立项。触发：使用者指示「做一个统一优化」。范围取恢复开发后不需要新方向决策的既定收尾项：1.9.0 动效系统预排的「后续阶：数字滚动全页统一、横幅与引文进出节奏对齐」、roadmap P1「审核背景绘制与装饰缓存开销」、rules §八.4 的 dist 定期清理。不开新方向、不新增装饰、不改观感。

## 一、动效二阶 · 数字滚动全页统一

### 现状
- 首页英雄数字（`EditorialHeroValue`）内联 `animateIntAsState(tween(500, FastOutSlowInEasing))`；
- 首页环心总量（`RingCenterContent`）用本地常量 `NUMBER_ROLL_MS = 500` 重复同一份实现；
- 统计页 hero 数字（`StatsScreen`）**完全静止**——同一份「今日总量」在两页动	static 静，语言不一致；
- 统计细轨 `PROGRESS_FILL_MS = 600`（注释还停留在「与首页进度环 600ms 同拍」，首页弧线 2.2.0 已改弹簧，注释失真）。

### 决策
- `MotionTokens` 新增 `DURATION_NUMBER_ROLL_MS = 500`（取现值，既有两处零观感变化）与缓动统一走 `EasingStandard`（与 `FastOutSlowInEasing` 为同一条曲线，仅归一出处）。
- `core:designsystem/animation` 新增 `rememberRollingInt(targetValue, label, reduceMotion = rememberReduceMotion())`：数字滚动的唯一实现，减少动效直显；四处调用方删除各自内联。
- 接入四处：首页英雄数字、首页环心总量、**统计页 hero 数字（本轮唯一的行为变化：补上滚动）**、统计细轨（600→500，注释改指 token）。
- 首进语义不变：`animate*AsState` 首组合直接落在目标值——冷启动、切页无数字表演，只有打卡/删除等值变化时从旧值滚到新值。这与克制基调一致，不做「每次进页从 0 滚起」。
- 分工写进 token 注释：进度环弧线是交互操纵件（2.2.0 §19 弹簧，damping 0.9），数字与细轨是被动显示值（同拍 tween）——弹簧与 tween 的并存是有意的，不是未统一。

## 二、动效二阶 · 打卡反馈进出节奏对齐

### 现状（同一打卡瞬间出现的三条通道，三套节奏）
- 环心引文横切 `RING_NOTE_FADE_MS = 240`（进出同值）；
- 达标缎带 `BANNER_ENTER_MS = 260` / `BANNER_EXIT_MS = 190`；
- 猫语气泡 `CAT_BUBBLE_FADE_MS = 400`。

### 决策
- `MotionTokens` 新增 `DURATION_FEEDBACK_MS = 240`（取环心引文现值）：环心引文横切、达标缎带展开/收起、猫语气泡淡变共用一档，进出同速。
- 依据 §12.2 同屏同拍：引文与猫语「同时出现、同时收场」此前只落在停留时长（3.0s）上，淡变时长各不相同——同刻出现却不同速淡入、同刻收场却不同速淡出。统一到 240ms 后，三通道一体呼吸，规则从「停留同拍」完备为「进出同拍」。
- 观感变化如实披露：缎带 260/190→240/240（更快淡入、略慢收起，均 <20ms 差）、猫气泡 400→240（收得利落一点，为与引文严格同拍）。
- 氛围循环（光袋 1.5s、辉光呼吸、粒子/光斑/雾光周期）不入此族——那是环境，不是反馈。

## 三、装饰绘制审计：逐帧分配清零（观感逐位一致）

逐帧驱动（`withFrameNanos` → `mutableFloatStateOf` → 只重绘不重组）的常驻层里，以下堆分配发生在每一帧：

| 层 | 现状（每帧） | 修复（渲染逐位一致） |
|---|---|---|
| `ParticleMath.floating`（80 颗） | 80× `Random(seed)` + 80× `Pair` + 80× `ParticleFrame` | 拆为 `specOf(index, seed, sizePx, area)`（帧不变量，`drawWithCache` 缓存段一次算好）+ `evaluate(spec, progress01, area)`；`floating()` 保留为 `specOf().evaluate()` 的等价门面，既有测试原样通过 |
| `drawThemeMote` SILVER（哥特/深夜 80 颗） | 80× `Path`（四段贝塞尔菱形） | 模块级复用一枚 scratch Path，每次 `reset()` 重建（UI 线程同步绘制，无共享竞争） |
| `BokehLayer`（10 颗） | 10× 色表 `List` + 10× `Brush` | per-anchor Brush 在 `drawWithCache` 缓存段构建；漂移改经 `translate(dx, dy)` 承载——shader 坐标随画布变换同步移动，与逐帧重建渐变逐位一致；alpha 经 `drawCircle(alpha = …)` 承载（各 stop ×α 与逐帧重配 alpha 逐位一致，透明端点乘 α 仍透明） |
| `LightPocket`（首页 2 袋） | 1× `List` + 1× `Brush` | 同上：`drawWithCache` 缓存 Brush，相位只动 alpha，走 `drawCircle(alpha = …)` |
| `ProgressRing` 值弧 | 1× `List` + 1× `Brush.sweepGradient` | `drawBehind` → `drawWithCache`，渐变按尺寸/主题缓存，逐帧只重铺弧 |

- 不动：达标光环（1.2s 单次事件，不构成持续压力）；猫立绘（呼吸走 `graphicsLayer` 缩放，drawBehind 不逐帧重跑，本就健康）；AGSL 雾光（shader 层，uniform 更新无 JVM 分配）。
- 等价性由数学保证 + 测试钉住：粒子路径 `ParticleMathTest` 全量既有断言（含「同参重复调用完全确定」的 `assertEquals(x, y)`）原样通过，另加 specOf+evaluate 与 floating 的全等扫描测试；其余各层由既有渲染回归（LolitaBackdrop / HomeVisualReview / StatsVisualReview 等）兜底执行路径，观感零变化留真机复查。

## 四、dist 仓库治理（rules §八.4）

- 现状 18 组 APK+SHA256SUMS 共 102MB，自 0.6.0 治理（514MB→52MB）后持续回涨。
- 保留：`1.0.0`（收口里程碑）、`2.0.0`（首页结构突破）、`2.5.0`（最新发布）；删除其余 15 组（git 历史可恢复）。

## 非目标

- 业务写入、持久化键、导航、权限、主题素材、三条文字反馈通道分工、文案语料、弹簧物理参数——零改动。
- 不新增任何装饰、不改任何观感（审计项全部逐位等效；动效统一取现值，唯二观感差异已如实列出）。
- 桌面小部件（使用者已否决）、年统计（使用者已否决）不复活；不做新方向（等真机验收输入）。

## 自动化覆盖

- `ParticleMathTest` 新增：specOf 确定性 + evaluate 与 floating 全等扫描（80 颗 × 多进度采样）。
- 新增 `RollingNumberTest`（designsystem，Robolectric compose，`mainClock` 控帧）：值变化滚动收敛到新值；`reduceMotion = true` 直显目标值。
- 新增 `MotionTokensTest`：钉住共享节奏（`DURATION_NUMBER_ROLL_MS = 500`、`DURATION_FEEDBACK_MS = 240`），防止后续无意识漂移。
- 既有全部布局/渲染/视觉回归原样重跑兜底；全仓库 `ktlintCheck build` 全绿。

## 真机待验收（QA 披露，不宣称完成）

- 打卡瞬间三通道（缎带/引文/猫语）进出同拍的观感；240ms 是否「安静而清楚」。
- 统计页 hero 数字补滚动后的观感（本版唯一行为变化）。
- 哥特/深夜主题首页粒子场帧率对比（Path/Random 复用后应无回退或更优）。
- 八主题截图复查（绘制审计项应与 2.5.0 逐位一致）；减少动态全路径直显直隐。

## 发布

- `versionCode 50 / versionName 2.6.0`；Release APK + SHA-256 入 `dist/`（签名回退 debug 证书，与 0.4.1 以来一致，如实披露）；CHANGELOG、roadmap、版本 QA 同步。

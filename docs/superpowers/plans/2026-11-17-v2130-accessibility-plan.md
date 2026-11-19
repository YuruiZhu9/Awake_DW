# v2.13.0 · 无障碍与大字回归（保养版）实施计划（2026-11-17）

> 状态：**代码侧完成，待真机验收**；versionCode/versionName 未提升（仍为 53 / 2.9.0）。
> 上游：`docs/design/continuous-iteration-plan.md` §6 的 2.13.0（主线被真机验收阻塞时前移的保养项）。
> 一句话：**不新增观感，只把「点得到、看得清、读得出、动得停」四条底线逐页复核，并补上缺的自动化。**

## 1. 范围

四类审计，覆盖 `feature:home` / `feature:stats` / `feature:settings` / `feature:onboarding` 与 `core:designsystem`：

1. **触控**：所有 `.clickable` / `.combinedClickable` / `pointerInput` 的可点目标是否 ≥ 48dp，语义角色与动作说明是否齐全。
2. **大字**：360dp 宽、fontScale 1.3× / 2× 下整页可滚动、不截断、不横向越屏、可点高度不被换行压扁。
3. **减少动态**：所有无限/一次性动效是否有 `rememberReduceMotion` 路径；装饰层是否整层静默。
4. **TalkBack**：环、猫、自绘手势、图标按钮、长按删除是否有可读语义与单指替代路径。

## 2. 审计结论

### 2.1 触控（无缺陷）

逐处核对 28 个触点，全部 ≥ 48dp：

| 位置 | 触控高度/尺寸 |
| --- | --- |
| 主按钮 / 撤回动作 | 56dp / 48dp |
| 快捷量胶囊、主题 chips、间隔 chips | 48dp |
| 设置步进器 ± | 48×48dp |
| 「测试提醒」「让提醒更稳定」 | 48dp / 56dp |
| 主题卡 / 主题入口 | 86dp / 72dp |
| 统计时间线行、文案库句子行、新增一句、恢复默认 | 48dp |
| 对话框动作 | 64×48dp |
| 猫立绘（自绘点击盒） | 64dp |
| 环（点按/拖拽） | 132dp |
| 引导主/次按钮 | ≥56dp / 48dp |

未发现需要放大的目标；`ControlMinHeight` 继续作为新触点的默认下限。

### 2.2 大字（补三页自动化）

- 首页：`HomeScreenOverflowTest` 已覆盖 1.3× 滚动可达与几何不相交；`RingNoteRenderTest` 覆盖环心 2×。
- 统计：`StatsWeekSummaryLayoutTest` 已有 2× 事实格；**新增**整页 2× 滚动到「今日记录」并检查分节标签不越屏。
- 设置：**新增** 360dp × 2× 整页滚动到「提醒稳定性」，行级动作可点高度与右边界双断言。
- 引导：**新增** 2× 主题步滚动到主/次出口，主按钮不越屏、不低于触控下限。
- 结论：四页均可滚动读完，未发现截断/覆盖；首页仍守「1.3× 一屏读完」的既有规则，2× 允许滚动。

### 2.3 减少动态（无缺陷）

逐处核对：开屏（减少动态直接交棒）、`GradientBackdrop` 呼吸、`FloatingParticles`、`BokehLayer`、`AgslMistLayer`、`CatFigure` 呼吸与情绪、`LightPocket`、首页环微光/蝴蝶结、拖环预览收场、开屏拖环提示、`BurstParticles`、主题色插值（`animateEach`）、底栏选中缩放、页面转场、文案分组展开/收起、引导步进、水滴插画呼吸——全部有 `reduceMotion` 分支；装饰层在减少动态下整层静默或直显。
未发现需要新增 guard 的动画源。

### 2.4 TalkBack（一处修正）

- **修正**：`ProgressRing` 点按语义补 `onClickLabel = "记一杯"`。环心文案只陈述进度（「20% 今日进度」），此前读屏聚焦到环只知道「按钮」，不知道按下会发生什么。
- 复核无需改动：猫（`contentDescription` + 「摸摸胆大王」动作）、时间线长按删除（`customActions` 单指替代）、文案库长按删除（`customActions`）、步进器（`contentDescription`）、文案分组箭头（展开/收起）、底栏图标（页签名）。
- 开屏「任意点击跳过」是隐藏手势，但开屏总时长约 1.2s 且自动交棒，减少动态下直接跳过——不构成阻断，不为此新增可见文案。

## 3. 非目标

- 不改任何视觉参数、布局、文案、业务、持久化、导航、权限。
- 不新增全局触控审计框架（Robolectric 语义 bounds 在部分节点不可靠，逐处定点断言更稳）。
- 不为开屏跳过补可见提示。
- 不调整 2.12.0 首页节奏（依赖真机反馈）。

## 4. 验收

自动化：

- [x] `SettingsEditorialLayoutTest` 新增 2× 整页测试（2 例全绿）。
- [x] `StatsWeekSummaryLayoutTest` 新增 2× 整页测试（2 例全绿）。
- [x] `OnboardingLayoutTest` 新增 2× 主题步出口测试（4 例全绿）。
- [x] `ProgressRingDragGestureTest` 新增点按动作说明断言（4 例全绿）。
- [x] 相关模块 `testDebugUnitTest` 全绿；全仓库 `ktlintCheck build` 全绿。
- [x] 视觉基线：本次不改观感，统计/设置基线 `AWAKE_VISUAL_BASELINE=check` 通过。

真机待验收：

- [ ] TalkBack 聚焦环：读作「进度…，按钮，双击即可记一杯」；点按仍记一杯。
- [ ] 设置 / 统计 / 引导在 1.3× / 2× 下滚动到底，无截断、无横向越屏。
- [ ] 减少动态全页复核：开屏直接进首页，首页/统计/设置装饰静止。
- [ ] 撤回（2.11.0）尾端动作在 TalkBack 下独立聚焦、播报量、双击可撤回。

## 5. 回退

- 唯一行为修正是环的 `onClickLabel`，移除该参数即回退；新增测试可整体删除，不影响生产代码。

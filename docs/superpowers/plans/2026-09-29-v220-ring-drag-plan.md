# 2.2.0 · 手感升级：环即把手（拖环注水）计划（2026-09-29）

> 状态：实施中。方向出处：`docs/design/2026-09-29-future-directions-research.md` 候选 C × v2.0 突破方向稿**方向三「环即把手」**（使用者确认按队列推进，并否决桌面小部件候选）。一步一发版，真机闸门照旧。

## 1. 目标与非目标

- 目标：环从显示件变操纵件——**沿环拖动即注水**，拖多少记多少（整圈 = 一杯容量）；配一阶弹簧物理让弧线「到位有 settle」。识别级交互，回答「这个 app 因什么被记住」。
- 非目标：不改业务写入路径（复用 `scheduleLog` 闸门）、持久化键、导航、权限；不动 Splash 交棒常量与 132dp 环径；不动三通道分工与 D10；记一杯按钮与快捷量路径原样保留。

## 2. 设计决策

1. **手势仲裁（风险最高点）**：按下后先不认领事件（纵向滚动照常）；累计切向扫过 > 8° 且未被滚动流消费时认领拖拽（一次 CLOCK_TICK 轻震），此后消费移动事件使 `clickable` 点按自然取消；若事件已被滚动消费（`isConsumed`）则放弃认领。松手时 |swept| ≥ 15° 提交，否则静默取消——预览消失本身就是反馈。
2. **换算归 feature 层**：`sweepToMl(swept, cupMl) = roundTo10(round(cupMl × swept/360))`，整圈=一杯；15° 起记（250ml 杯 ≈ 10ml）。纯函数进 `feature/home/RingDragMath.kt`；`roundTo10` 从 QuickSipsRow 提为 internal 复用（第二处使用，随行收敛）。**designsystem 只收发几何角度，不碰毫升**。
3. **记账复用既有闸门**：提交调用 `scheduleLog(ml)`——防抖合并、环心引文、猫语、音效、达标缎带全数复用；环区点按（`tapRing`）原样保留为无障碍等价路径（Role.Button 语义不变）。
4. **预览只进 draw 相**：拖拽中预览弧（自当前弧端起、主色 35%、圆头）由 ProgressRing 内部状态驱动、仅作 draw 失效；环心临时态显示 `+Nml · 松手记录`（随取整后的 ml 更新，重组范围仅环心小组件）；落账后环心照常交还引文通道。
5. **弹簧一阶（M3 Expressive 克制版）**：环进度 600ms tween → `spring(dampingRatio 0.9 / stiffness MediumLow)`（近临界，settle 不晃荡，数值 clamp 不变）；预览收场 150ms 退出。减少动态：全部直显直隐，手势仍可用（输入不是动画）。
6. ProgressRing 新参均为可选默认 null，SplashMorph 调用点零改动。

## 3. 自动化验收

- `RingDragMathTest`：360°→杯容量；90°/250ml→60ml；15° 边界→10ml；更小→取消。
- `HomeViewModelTest`：预览状态随拖更新；提交走既有闸门（按换算量记账、防抖窗口内回显「刚刚记过了」不加账）；低于最小量静默取消。
- `ProgressRing` 手势（Robolectric compose）：弧线拖拽 → `onRingDragEnd` 收到扫过角度且点按不触发；纵向直线拖拽 → 两者均不触发（让位滚动）；纯点按 → 点按触发。
- 既有六模块 + 全仓库 `ktlintCheck build` 强制重跑全绿；真机闸门：滚动与拖拽仲裁手感、预览可读性、八主题对比度、减少动态。

## 4. 发布

- 版本：`2.2.0 / versionCode 46`；发布 APK 与 SHA-256 随发布提交入库 `dist/`（使用者要求：推送必须携带 APK）。

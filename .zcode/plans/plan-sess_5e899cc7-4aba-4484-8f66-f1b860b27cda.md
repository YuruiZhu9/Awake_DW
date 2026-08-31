# 显示效果优化迭代计划（微交互 + 转场 + 排版质感 + 深夜主题）

动效基调统一为**克制**：150–250ms、位移 ≤12dp、每屏只编排一次入场，与现有呼吸感一致。

## 阶段 0 · 文档先行（项目协议）
design.md：§2.1 色板表补「D · 深夜墨青」一行；§9 追加本轮四项决议。单独 docs 提交。

## 阶段 1 · 深夜主题（工作量最大，先行）
**色板取向**：保持品牌绿基因的「墨青底 + 薄荷强调」——
底渐变 `#0B1412→#0F1D1A→#122621`，primary `#3ECFA5`，环值文字 `#7FE7C6`，
问候 `#CDEFE2`/副 `#7FA99C`，按钮渐变 `#2FB98F→#4ADBB0`，chip `#172B25`/`#A8D8C6`，
粒子族降亮度适配暗底。

改动点：
- `core/model`：`ThemeId.NIGHT`、`ThemeChoice.FIXED_NIGHT`
- `ThemePalette/Themes`：第四套 ThemeSpec（halo 沿用 primary，暗底上即柔光）
- `ResolveThemeUseCase`：FOLLOW_TIME 细化——EVENING 槽内 22:00–05:59 路由 NIGHT，18–21 仍焦糖（假设：接入点定 22 点，可后调）；`FIXED_NIGHT` 直映射
- 设置页：主题 chips 加「固定深夜墨青」+ 色点
- **系统栏动态同步**：`AwakeApp` 根部按当前 spec 设 statusBarColor/navigationBarColor + 浅/深色图标标志（夜里深色栏 + 白图标；XML 静态浅色仅作首帧兜底；Android 15 edge-to-edge 下颜色被忽略、布局照旧由 Scaffold insets 兜底）
- 测试：ResolveThemeUseCase 边界（21:59/22:00/05:59/06:00）、FIXED_NIGHT、色板对比度健全性

## 阶段 2 · 页面切换动效
`AwakeNavHost` 的 NavHost 转场：页签间淡入 200ms + 8dp 上移；引导→首页交叉淡入稍长。退出 180ms 淡出。

## 阶段 3 · 微交互与手感
- **触感反馈**（目前全 app 零 haptics）：记一杯按钮与环点按 = `CONTEXT_CLICK` 轻震；达标庆祝瞬间 = `CLOCK_TICK`。经 LocalView，克制不喧哗
- **引导页按钮手感对齐**：PrimaryButton 补按压缩放 spring（与 LogButton 同语言）
- 统计页入场编排：周柱状图**自基线生长**（逐列 stagger ~40ms、250ms EaseOutCubic）、目标线随后淡入；今日时间线逐条 8dp 上移淡入；徽章行轻弹入
- 首页首次入场：问候/日期、徽章行、健康贴士单次 fade-up（不重复打扰）

## 阶段 4 · 排版与质感细节
- 环心：数值字重/行高微调，「今日已喝」字距拉开，层次更清晰
- 日期副行：透明度与字重分层
- 设置卡/文案组卡：统一 1–2dp 柔阴影 + 细描边（主题色 8% alpha），替纯平面色块
- 各屏间距节奏复核（44/28/20/12 的栅格一致性）

## 阶段 5 · 收尾
CHANGELOG 补记；全量 `test + ktlintCheck + lint`；双包重建刷 `dist/`（含新图标/深夜主题验证）；按阶段小步提交后推送。

## 假设声明（供复核）
1. FOLLOW_TIME 深夜接入点取 22:00–05:59（18–21 保持焦糖），如需前移到 21 点说一声
2. 深夜主题走「墨青+薄荷」而非纯黑，保持品牌一致性
3. 深夜主题下启动闪屏仍是浅色（仅冷启动一瞬，改动成本高收益低，暂不做动态闪屏）
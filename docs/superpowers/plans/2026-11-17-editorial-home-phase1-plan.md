# Phase 1 实施计划：首页纸面化（2026-11-17）

> 状态：**代码侧完成（2026-11-17），待真机验收**。使用者确认按方向稿继续开发；背景结合取 **D+** 为真机初值。
> 方向依据：`docs/design/major-ui-overhaul-direction.md` §2 A、§3.1–§3.3。
> 边界：只改首页展示层与共享绘制件；业务、持久化键、导航、权限零改动。

## 1. 目标

把首页从「主题壁纸 + Material 控件」改成「一张铺在主题场景上的纸」：

1. **纸面**：纸在版心 97.5% 不透明，向页边渐透到 88%；左右各留 8dp 桌面，主题图从纸外与纸边呼吸。
2. **书口**：右缘 18dp 内画发丝页口线；纸面右上角以 19% 印一角主题图（水印），与纸外场景、环内窗景构成同一张图的三种尺度。
3. **窗环**：进度环内嵌 2.2–2.3× 放大的主题角花，中心 88dp 纸面圆牌显示百分比；60 格刻度 + 原值弧 + 珠扣；拖拽/点按语义不变。
4. **压印按钮**：主按钮由渐变小胶囊改为 58dp 压印长条（近直角、1dp 内高光、底部内阴影、左侧印章）；尾端杯量/撤回位置与语义不变。
5. **撕票快捷量**：两枚快捷量由胶囊改为页边票根（虚线顶边 + 竖分隔），保留 48dp 触控与原有文案。
6. **页框**：日期行右侧 folio，日期下通栏发丝线；页末「AWAKE · 每日一页 / 第 NN 页」页脚。页脚与 folio 只是版面家具，不承载业务语义。

## 2. 已定参数（`PaperTokens` 初值）

| token | 值 | 说明 |
|---|---|---|
| `insetHorizontal` | 10dp | 纸外桌面；文字仍从屏幕边 28dp 起，内容列宽不变 |
| `paperCenterAlpha` | 0.975 | 版心实底 |
| `paperEdgeAlpha` | 0.88 | 纸边透气（D+） |
| `edgeBandWidth` | 26dp | 纸边 88% → 版心 97.5% 的横向呼吸带（D+ 修正：不用大半径径向渐变，否则左右边到不了页边 alpha） |
| `foreEdgeWidth` | 18dp | 页口发丝区，落在内容 28dp 边距内，不挤文字 |
| `ghostOpacity` | 0.19 | 纸上角花水印（D+） |
| `ghostSize` | 220dp | 水印绘制区，超出纸右上角裁掉 |
| `grainAlpha` | 0.045 | 纤维噪点，只进 draw 相 |

调整纪律：抢字先降 `ghostOpacity`（回 0.14）再降 `paperEdgeAlpha`（回 0.90）；不要低于 0.86。
真机版把方向稿的「34dp 桌边」拆成 **10dp 桌面 + 26dp 纸内呼吸带**：总视觉带宽 36dp，同时守住 360dp 下内容列 28dp 边距与主按钮 ≥82% 屏宽的门禁。

## 3. 影响文件

新增：

- `core/designsystem/paper/PaperTokens.kt`
- `core/designsystem/paper/EditorialSheet.kt`（纸面/书口/水印/噪点）
- `core/designsystem/lolita/ArtworkCrop.kt`（归一化裁切 + 纯函数源矩形）
- `core/designsystem/paper/EditorialArtwork.kt`（八主题窗环/水印角花表）
- `core/designsystem/ring/RingLens.kt`（环内窗景参数）

修改：

- `core/designsystem/ring/ProgressRing.kt`（可选窗景、纸面圆牌、60 格刻度；默认 null 时行为不变）
- `feature/home/HomeScreen.kt`（图层顺序、窗环接线、页脚、移除 ReadingField）
- `feature/home/components/EditorialHome.kt`（folio/通栏线/页脚/环心字号）
- `feature/home/components/LogButton.kt`（压印长条，保留 2.11.0 撤回）
- `feature/home/components/QuickSipsRow.kt`（撕票票根）

## 4. 验收门禁

- 360dp × 520dp、fontScale 1.3、有记录：整页无滚动读完；主按钮仍 ≥ 屏宽 82%；猫栏单行。
- 主按钮 ≥ 56dp 高；撤回动作 ≥ 48dp 高、可独立点击、不穿透主按钮；2× 字体不越屏。
- 撕票每枚 ≥ 48dp 高，点按毫升数与旧胶囊一致；2× 字体可滚动不裁切。
- 环内窗景不改变点按语义（`onClickLabel = "记一杯"`）、拖拽认领与 15°/8° 几何。
- 减少动态：圆牌/遮罩淡入直切；窗景与纸张本身不新增逐帧动画；水印/噪点只进 draw 相。
- 深色（NIGHT/GOTHIC）与浅色（EMERALD/CLERIC）各做隔离渲染诊断；八主题真机走查仍待验收。
- 统计/设置视觉基线不受影响（`AWAKE_VISUAL_BASELINE=check`）。

## 5. 代码侧验收记录（2026-11-17）

- 新增测试：`EditorialPageChromeTest`（folio/页脚存在、520dp×1.3 页脚屏内、两枚撕票 ≥48dp、窗环仍 132dp）、`ProgressRingDragGestureTest` 新增「窗环不改点按语义」、`EditorialHomeLayoutTest` 新增 folio/页脚文案与角花裁切越界测试。
- `:feature:home:testDebugUnitTest` 全绿；`:feature:stats` / `:feature:settings` 视觉基线 `AWAKE_VISUAL_BASELINE=check` 通过（两页未改）。
- `ktlintCheck build` 全绿（1266 任务）。
- 诊断渲染：`HomeScreenRenderDiagnosticTest` 用真实素材在 EMERALD/NIGHT 各出一张 411dp PNG，已目检纸边呼吸、水印、窗环、压印按钮、撕票与页脚层级；留档 `docs/design/mockups/editorial-home-emerald-render.png`、`editorial-home-night-render.png`（整页：含猫栏与真实背景，不含底栏；非像素基线）。
- 真机待验收：八主题纸边/水印对比度、窗环角花是否可辨、1.3×/2×、TalkBack、减少动态、底栏衔接。

## 6. 明确不做

- 不动统计/设置页面，不重排首页信息顺序，不加水位尺/第三枚快捷量。
- 不做纸边高斯模糊、网络素材、3D、奖励语义。
- 不让水印/页脚进入读屏主叙事；页脚不承载状态与操作。

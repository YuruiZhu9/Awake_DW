# Phase 2 实施计划：统计/设置的纸面与器物（2026-11-19）

> 状态：**代码侧完成（2026-11-19），待真机验收**。
> 方向依据：`docs/design/major-ui-overhaul-direction.md` §2 A、§3.2、§3.3。
> 边界：只改统计/设置展示层与共享绘制件；业务、持久化键、导航、权限零改动。

## 1. 目标

首页在 Phase 1 已成为「一张铺在主题场景上的纸」；Phase 2 把同一套页面语法带到另外两页，
同时把「器物控件」从首页主按钮/撕票扩展到设置页的日常操作：

1. **统计页**：纸面层 + 页眉 folio + 卷首插图 + 页脚；统计从「壁纸上的列表」变成可翻阅的记录页。
2. **设置页**：纸面层 + 页眉 folio + 页脚；步进器、开关、时段滑杆、间隔档位从通用 Material
   形态换成打字机键帽、压印铜扣、刻度尺与印章票。
3. **共享件下沉**：页眉/页脚/日期-folio 函数从首页私有实现提升到 `core:designsystem`，
   首页改用同一份，保证三页的页框语法不再各自漂移。

## 2. 已定参数与决定

| 项 | 值/做法 | 说明 |
|---|---|---|
| 版心 | 28dp（`HomeHorizontalPadding`） | 与首页一致：纸外 10dp 桌面 + 纸内 18dp 边距 |
| 页眉 | 日期行 + 斜体 folio + 通栏发丝线（0.40 alpha）+ 衬线标题 + 副题 | 三页同一套；统计/设置可滚动，不参与首页一屏门禁 |
| 卷首 | 104dp 高、约 3:1 横裁、1dp 框线 + 内侧高光 + 图注 | 八主题 `EditorialCrops.plate` 各取角花/画框带；不新增素材 |
| 统计页脚 | 「AWAKE · 记录本 / 第 NN 页」 | 页脚是纸的收口，不承载状态 |
| 设置页脚 | 「AWAKE · 设置 / 第 NN 页」 | 同上 |
| 步进键帽 | 48dp、5dp 圆角、顶缘高光 + 底缘压暗 + 1dp 压印边 | 保留 `pressScale`、`contentDescription`、禁用态 |
| 压印开关 | 52×30dp 凹槽 + 22dp 铜钮，外层 48dp 触控 | 保留 `Role.Switch` / `ToggleableState`，减少动态直落位 |
| 刻度尺 | 滑杆上方 10dp 刻度带：整点短刻、每两小时长刻 | `RangeSlider` 本体的拖拽/键盘/读屏不变 |
| 间隔印章票 | 48dp 高、3dp 近直角、外框 + 内缩细框；选中主色盖印 | 保留 `selected` 语义与 56dp 最小宽度 |
| 主题入口/色卡 | 4dp 近直角、纸面票根 | 底部弹层仍是 Material 覆盖层，不改行为 |
| 文案分组 | 3dp 票根 + 1dp 边框 | 从 18dp 大圆角卡收成纸面分组 |

## 3. 影响文件

新增：

- `core/designsystem/components/EditorialPageFrame.kt`（日期/folio/页眉/页脚，三页共用）
- `core/designsystem/paper/ArtworkPlate.kt`（卷首图版）

修改：

- `core/designsystem/lolita/ArtworkCrop.kt`（任意宽高比裁切重载；1:1 时逐位不变）
- `core/designsystem/paper/EditorialCrops.kt`（`plate` 裁切；八主题取角）
- `feature/home/HomeScreen.kt`、`components/EditorialHome.kt`（改用共享页框；删除重复实现）
- `feature/stats/StatsScreen.kt`（纸面、卷首、页眉/页脚、衬线英雄数字；退役 `ReadingField`）
- `feature/settings/SettingsScreen.kt`（纸面、页眉/页脚；测试提醒改印章按钮）
- `feature/settings/components/SettingsControls.kt`（键帽、压印开关、刻度尺、印章票、主题票根）
- `feature/settings/components/ThemePickerEntry.kt`（入口票根）
- `feature/settings/copyeditor/CopyEditorSheet.kt`（分组票根与对话框圆角）

## 4. 验收门禁

- 统计/设置允许纵向滚动：1.3×/2× 字体下所有分区可滚到，不截断、不横向越屏。
- 所有行级动作 ≥48dp；印章票/键帽/压印开关/测试提醒的可点高度由测试量取。
- 压印开关保留 `Role.Switch` 与 ToggleableState，点击即时落库；步进器 ± 文案与语义不变。
- 卷首插图收在 28dp 版心内，图注可读；素材缺失时留纸色空版，版面不跳。
- 减少动态：铜钮直落位；纸面/卷首/刻度尺不新增逐帧动画。
- 视觉基线：统计 14 张、设置 13 张重拍后 `AWAKE_VISUAL_BASELINE=check` 通过。
- `ktlintCheck build` 全绿；`git diff --check` 通过。

## 5. 明确不做

- 不改统计信息层级（不重排为双栏记录跨页；留待 2.15.0 单独确认）。
- 不改设置项顺序、默认值、校验、提醒调度与持久化键。
- 不给统计页加第二张图、不给设置页加装饰遮挡；不引入网络素材/模糊/3D。

# v2.9.0 · 过渡补完（2026-10-22 立项）

## 背景与触发

2.8.0 把按压缩放铺满次级触点后，沿 Emil Kowalski 设计工程标尺做第二圈审计——这次扫的是「状态切换」维度，发现三处既定语言未收口：

1. **底栏仍是水波纹**：`NavigationBarItem` 走 M3 默认 ripple——1.6.0 底栏「零胶囊指示、安静得像画布的一部分」的既定方向下，灰波纹与整套按压语言都不合拍；§24「水波纹退役」在此处尚欠一笔。
2. **文案库分组展开/收起是硬切**：内容瞬现瞬失、箭头瞬转（`rotate(±90°)` 无插值）；分组头带 ripple 而卡内句子行 indication=null，同一张卡内两种口径。
3. **引导步进切换是硬切**：`when (state.step)` 直接换内容——首次使用的整页跳变。

## 设计决策（Emil 标尺逐条）

| 现状（Before） | 本版（After） | 为什么 |
| --- | --- | --- |
| 底栏 NavigationBarItem 默认 ripple | `NoIndication` 静默 + 图标接共享按压缩放（0.97 回弹）；选中态 1.08 spring 弹入不变 | 底栏自称画布一部分；按压 = 图标缩放回答，与全 app 同一语言 |
| 文案分组内容瞬现瞬失 | `AnimatedVisibility`：展开 240ms（expandVertically+fadeIn）、收起 170ms（shrinkVertically+fadeOut） | 出比入快（Emil 不对称节奏）；240 进与三通道/页签同拍 |
| 分组箭头 `rotate` 瞬转 ±90° | `animateFloatAsState` 240ms 插值 | 旋转是方向暗示，瞬转等于没暗示 |
| 分组头 / 「＋新增一句」带 ripple | indication 置空，与卡内句子行同一安静口径 | 同一张卡内不应有两种按压回答 |
| 引导步进 `when` 硬切 | `AnimatedContent` 淡切：入 240ms / 出 170ms（EasingStandard） | 两步都是 fillMaxSize 满屏淡切无位移；低频首次流程可承载轻仪式感 |

**减少动态**：三处全部 `motionDurationMillis(…, 0)` 直切直显；底栏图标按压本就恒 1f。

**不做**：统计 / 设置分节入场错峰——页签转场（240ms 淡入+视差+微缩放）已是页面级入场，再叠分节错峰是重复动画（Emil：不要给同一次出现叠两层）；设置步进器数值不滚动（快速连点场景，瞬时更新优先）；主题卡选中态保持瞬时（§24 已立）。

## 范围

- `core:designsystem`：新增 `animation/NoIndication.kt`（no-op `IndicationNodeFactory`，可复用的「无水波纹」原语）。
- `:app`（`AwakeNavHost`）：底栏 `LocalIndication provides NoIndication` + 逐签 `interactionSource` + 图标 `pressScale`。
- `feature/settings`（`CopyEditorSheet.CopyGroup`）：展开/收起 `AnimatedVisibility` + 箭头插值 + 头部/新增 indication 置空。
- `feature/onboarding`：步进 `AnimatedContent` 淡切。
- 测试：`CopyGroupTransitionTest`（动效开启下：展开后内容在、收起宽限拍内仍在、拍毕消失；既有触控断言在减少动态下保持）。

## 非目标

- 不动业务、持久化键、导航图、权限、素材、三通道分工。
- 不给句子行的删除/新增加离场宽限（低频维护场景，频率标尺不到）；不改主题卡选中态、底栏结构、引导步内容。

## 验收条件

1. 底栏按压无波纹、图标缩放回弹，选中弹入不变；文案分组展开/收起有方向感的过渡、箭头平滑旋转；引导两步淡切。
2. 减少动态下三处全部直切；既有设置 / 引导测试全绿。
3. `gradlew.bat ktlintCheck build` 全绿；视觉基线（静止态）零漂移。
4. 2.9.0 / versionCode 53 出包入 dist，归档后按 rules §八.4 清理 2.8.0 历史包。
5. 真机待验收项如实披露于版本 QA（底栏手感、分组过渡、步进淡切）。

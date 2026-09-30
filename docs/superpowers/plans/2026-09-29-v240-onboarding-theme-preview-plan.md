# 2.4.0 立项：引导页主题实时预览（候选 E，2026-09-29）

> 方向依据：`docs/design/2026-09-29-future-directions-research.md` §5（onboarding 2026 的 outcome preview）。
> versionCode 48。不增引导步数上限的边界以「主题步骤为第一屏、白名单步骤原样后移」实现。

## 1. 设计决策

- **现状**：引导页只有电池白名单一步，主题选择只能在事后进设置。本版在引导最前加「挑主题」一步。
- **选完即见所得**：主题选择点按即经 `prefs.setThemeChoice` 落库，app 壳的主题流（`ResolveThemeUseCase`）本就是响应式的——整屏随之即时换装，这是最强的实时预览，不需要任何额外的预览机制。
- **首页缩样**：主题步内嵌一块「首页缩样」面板——以所选主题的 `AwakeTheme` 局部覆写，画出问候行 + 进度环（45%）+「记一杯」小胶囊的迷你首页，复用设置页的缩略图策略（渐变 + 主题画 + 蕾丝覆层）。
- **复用**：`themeLabel`/`themeIdOf` 两个纯映射从 settings 内部提升到 `core:designsystem`（设置页同步改为引用，删内部副本）；swatch 视觉（渐变 + 画 + 蕾丝）在 onboarding 以 designsystem 原语自组，不搬 UI 组件。
- **边界**：不加账号、不加网络；「以后再说」触控下限等白名单步回归原样保持；跳过主题选择不写偏好（保持默认）。

## 2. 验收条件

1. `OnboardingViewModelTest` 新增：selectTheme 落库且状态跟随、continueToWhitelist 推进步骤、complete 语义不变。
2. `OnboardingLayoutTest` 更新：主题步首屏可见缩样与色卡、继续后白名单步元素齐备（既有触控下限断言保持）。
3. 全仓库强制构建全绿；发布 versionCode 48 / versionName 2.4.0，APK + SHA256SUMS 入 `dist/` 并推送。

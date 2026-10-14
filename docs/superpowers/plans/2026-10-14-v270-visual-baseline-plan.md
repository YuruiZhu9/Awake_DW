# 2.7.0 · 视觉回归基线版（roadmap P1 收口）

> 2026-10-14 立项。触发：使用者指示「继续，持续更新迭代」。范围取路线图 P1 的既定收尾项：「为首页／统计／设置建立八主题、空态／有数据、长文本的稳定截图对照」。不开新方向、不改任何观感。

## 动因

- 2.2.0 的统计页闪退（Robolectric 填充态从未渲染新组件）与 1.3.1 的方向翻车共同指向同一缺口：自动化渲染覆盖是「执行了吗」的检查，「看起来对不对」只有真机一双眼。基线比对让**跨版本的意外视觉漂移**在构建期就被抓到，真机验收可以专注于「该不该这样」而不是「有没有不小心变」。
- 现有视觉 review 测试只把截图落在 `build/reports/visual-review/`（不入库、不比对），且存在两类不确定性使截图不可比：
  1. **UI 层真实时钟**：日期刊头（`LocalDateTime.now()`）跨日必变；时段氛围（`rememberSceneSpec` 读 `LocalTime.now()`）随时段变——同一份代码早拍晚拍像素不同。
  2. **素材解码竞态**：首页测试未预载主题画，截图与异步解码赛跑（既有 QA 披露的「尽力而为的诊断产物」）。

## 范围

### 一、UI 时间缝（designsystem）

- 新增 `LocalUiTimeMillis`（默认真实时钟）与 `rememberUiNow()`：界面内时间读数的唯一入口。
- 改两处真实读取点：首页刊头（`EditorialHomeMasthead`）与 `rememberSceneSpec` 的时段锚点。默认行为与现状逐位一致（同一系统时钟，仅换读法）。
- `Greeting.kt` 的 `Greeting` 无调用方（既有死代码），不动；其时间读取点因此无需转换（记录待后续清理决策）。
- 测试侧经 `CompositionLocalProvider(LocalUiTimeMillis provides 固定值)` 钉住全部页面截图的日期与时段。

### 二、core:testsupport 模块（共享比对助手）

- 新模块 `:core:testsupport`（纯 android.jar 依赖，无 Compose），三处以上消费方，符合「第二使用方即下沉」守则。
- `VisualBaseline.verify(bitmap, module, label)`：
  - 基线入库于各模块 `visual-baselines/<module>/` 目录（随仓提交）；
  - **基线缺失 → 自动播种并通过**（新克隆自举，stdout 声明）；
  - 环境变量 `AWAKE_VISUAL_BASELINE=update` → 重拍基线（有意的设计变更后使用）；
  - 比对口径：同尺寸校验 + 逐像素通道差 >10 记差异，占比 >1% 判失败——是**绊网**不是像素级闸门，文案指路更新命令。

### 三、接入与八主题矩阵（roadmap P1 口径）

- **首页**（默认图形管线，减少动态路径）：空态 ×8 主题 + 有记录 ×8 主题；补八主题素材预载消除解码竞态；注入固定 UI 时间。
- **统计**（NATIVE 管线）：填充态场景 2→**8 主题**；达标态、空态、大字体两景保持并入库基线。
- **设置**（NATIVE 管线）：既有 4 张（cleric / preferences / blue / copy-library）入库；**新增八主题只读遍历**测试（每主题一张，预载素材，不做交互）。
- 基线总量约 40 张（360dp 级 mdpi，单张约 30–60KB，仓库增量约 1.5–2MB）。

## 验收条件

1. **两轮运行证明稳定**：首轮自播种全部基线；第二轮（不改任何代码）全量比对通过——同机连续两次渲染逐像素近一致，才配叫「稳定截图对照」。
2. 真机闸门职责不变：基线是回归绊网，不替代真机审美与帧率验收（1.3.1 教训入档的边界不动摇）。
3. 比对失败信息必须指路：`AWAKE_VISUAL_BASELINE=update gradlew <module>:testDebugUnitTest`。
4. 跨机/跨环境不承诺（字体渲染随环境）：QA 如实披露基线的机器绑定属性。

## 非目标

- 不改任何生产视觉与行为（时间缝仅换读法）；业务、持久化、导航、权限零改动。
- 不做像素级金图（1% 绊网口径）；不做跨环境基线；不做 CI（仓库私有、无远端流水线需求，先不做）。
- 不动 `Greeting.kt` 死代码；不动达标光环（1.2s 单次，2.6.0 已裁定）。

## 发布

- `versionCode 51 / versionName 2.7.0`；Release APK + SHA-256 入 `dist/`；CHANGELOG、roadmap、版本 QA 同步；按 docs/feat/chore 三段提交。

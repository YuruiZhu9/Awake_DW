# 0.6.0 · 精致化地基 plan

- 日期：2026-09-15
- 基线：继承 0.5.1（versionCode 22）与 `docs/design/v0.6-refinement-charter.md`（D1–D10 已于 2026-09-15 确认，记录见提案 §9.1）
- 编号：0.6.0（待发布）
- 触发：0.6 精致化提案经用户逐项确认。

## 0. 一句话方案

落地提案的两块地基与轨道一：**仓库清理与构建根治、真机补验闭环、引文库与文案库重写、深夜青黛主图接入**——不碰渲染架构（那是 0.7.0 起）。

## 1. 非目标（本轮明确不做）

- 分层视差、粒子扩容、SceneSpec、AGSL（0.7.0–0.8.0）；达标仪式动效（0.9.0）；任何新主题与新装饰规则（alpha13 moodboard 确认前）。
- 不引入网络、远程字体；不动导航、权限、持久化键。

## 2. 实施步骤

1. **清理（地基 B，D4）**：修订 rules §八.4（Release-only 入库 + dist 保留策略）→ 清理 `dist/`（保留 v0.5.1 双包 + alpha12 / 0.4.0 / 0.5.0 各 release 包 + 对应 SHA256SUMS）→ 删除 `assets/lolita/` 10 张零引用候选图（已复核：`ThemeArtwork.kt` 仅引用 8 张）→ `images/Lolita/` 去重一对 md5 相同 PNG → `.gitignore` 增补 `.zcode/` 并移除已跟踪会话产物。状态：**本 plan 落地当轮已执行**。
2. **构建根治（D6）**：✅ 已实施（2026-09-15）——实施方式与提案措辞有偏差并记录于此：原定「显式 android-all 测试依赖」，但 `BatteryIntentLauncherTest` pin 了 sdk 33/28、其余默认 35，同一 GAV 只能解析一个版本，显式依赖无法覆盖多 SDK；改为 **`robolectric.dependency.dir` 本地离线目录**（`.robolectric/offline/`，gitignore，`tools/sync-robolectric-jars.ps1` 从本机 m2 缓存同步，4 个构件约 560MB），9 个模块的阿里云镜像属性一并替换。意图不变：运行时零下载、零锁文件。生效证据：连续全量构建无锁文件失败——首次验证已通过（2026-09-15 全量 `build` 5m52s 全绿，含全部模块测试与 lint）。
3. **引文库重写（轨道一 5.1，D2 = 混编、D3 = 逐条圈选）**：语料为**现代白话**（现代中文散文摘句〔公有领域、原文逐字〕+ 公有领域西方短句自译 + 少量原创；**不含任何文言**——2026-09-15 用户二次澄清，v1 文言草案已作废）；长度以环心两行为限逐句实测；禁「你/您」逐句筛；落款 ≤ 6 字。草案：`2026-09-15-v060-praise-corpus-draft.md`（v2）→ 用户逐条圈选 → 落码（`ShortCopies.praise*`、`PraiseQuote` KDoc、`ShortCopiesTest` 例句、`CopyLibraryRepositoryTest` 与 `RingNoteRenderTest` 例句、visual-baseline §11/§12 对应句改写、CHANGELOG）。
4. **文案库重写（D8 已确认随本轮）**：108 句第三轮整体重写，仍第一人称、10–24 字、唯一性与起手规则不变；草案同样逐条圈选后落码，只动 `DefaultCopies` 默认值，不动编辑器与持久化键。
5. **深夜青黛主图（S1）**：✅ 已执行（2026-09-15）——衍生图 `midnight_indigo.jpg`（810×1440、约 205KB）已生成；`ThemeArtwork` NIGHT 改为 PAINTED 0.72；`INVERTED_INK` 处理档与 `gothic.jpg` 移除；S2 分层素材两张已存档待 0.7.0（中景层结构合格、入库前清理噪点；前景层需重生成或中央遮罩）。真机对比度验收挂补验清单 A 组。
6. **cat 衍生图压缩评估**：✅ 已执行（2026-09-15）——`beforedrink` 1024²→640²（1.59MB→387KB）、`afterdrink` 1254²→640²（1.73MB→404KB），合计省 2.43MB；显示 108dp（4x 密度 432px）下有余量，afterdrink 原尺寸本超运行时 1024 解码上限；边缘与透明通道质检合格，测试只断言文件映射不断言尺寸，designsystem 测试全绿。原图存档 `images/cat/` 不动。
7. **收口**：真机补验走 `docs/superpowers/checklists/v0.4-v0.5-device-acceptance-backlog.md` → 升 versionCode 23 / 0.6.0 → 出 Release 包入 `dist/`（Debug 仅本地）→ 更新 CHANGELOG / QA。

## 3. 验收条件

- 补验清单 A–F 全组回传销账；不通过项修复或登记到下一轮。
- 引文库 / 文案库全部语料规则更新后测试全绿；`gradlew.bat ktlintCheck build` 全绿。
- release APK 体积小于 0.5.1（8.26 MB；死素材删除 + cat 衍生图生效）；`dist/` 符合修订后规则。
- 连续 3 次全量构建无 Robolectric 锁文件失败（D6 生效证据）。

## 4. 依赖与等待项

- 等用户：S1 成品图、补验清单回传、引文与文案草案圈选。
- 不等（可直接推进）：清理、构建根治、两库草案起草。
- 另行执行（不混入版本提交）：git 历史改写回收 APK blob（D5，需 force push，待用户确认可强推的时点）。

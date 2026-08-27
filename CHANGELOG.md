# 更新日志

所有显著变更记录于此。格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [v0.1.0-alpha1] - 2026-08-27

地基版本（尚无用户界面，仅冒烟屏）。

### Added
- Gradle 多模块工程骨架：`:app` + `:core:model/common/domain/data/notification/designsystem` + `:feature:*`
- 领域层：WaterRecord/UserSettings 等七类型、时钟抽象与时段边界判定
- 数据层：Room 水记录仓储（今日统计/周柱状聚合）、DataStore 设置、内置 30 句关心文案库（去重抽取）
- 用例层：打卡庆祝一次语义、连续达标天数、主题解析、下一提醒时刻计算
- 设计系统：翡翠绿/草莓雾光/焦糖奶茶三主题全量色板、渐变+光晕+噪点背景、分层粒子引擎、治愈进度环、迸发粒子
- 测试：40 例单测全绿；ktlint 全绿

### Download
- `dist/Awake_DW-v0.1.0-alpha1-debug.apk`（调试签名）

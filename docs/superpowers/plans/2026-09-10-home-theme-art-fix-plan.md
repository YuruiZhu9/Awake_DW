# 2026-09-10 · 首页主题背景修复与新素材接入计划

## 用户确认与范围
- 2026-09-10：用户提供 `images/Lolita/new/` 的四张新主题背景，要求接入晨雾蓝瓷、午后藕荷、黄昏奶茶、雾紫玫瑰，并修复首页背景完全不显示的问题。
- 用户暂未提供深夜青黛主图，本轮保留现有安全回退，不伪造新素材。
- 用户进一步确认：白色圣职、黑哥特、薄荷巧克力在首页也必须显示背景；主题选择栏现有可见效果不能作为首页已生效的判断。

## 设计决策
1. 修复 `LolitaBackdrop` 的布局承载：把 `matchParentSize` 约束施加在 `Crossfade` 容器本身，避免背景绘制层因无测量尺寸而得到 0×0；内容层再填满该容器。
2. 新 PNG 仅保留在 `images/Lolita/new/` 作为源素材；运行时转换成最长边 1440px、单图小于 350KB 的 JPEG，放入 `app/src/main/assets/lolita/`。
3. 四张新图作为四个主题的主图，使用 `PAINTED` 合成而非 `Multiply`，保持边缘插画和中央低细节纸面可见；不改变主题映射、业务状态、持久化键和导航。
4. 白色圣职、黑哥特、薄荷巧克力继续使用专属图框，不额外叠加 code-native 蕾丝；修复后首页与设置页共享同一背景绘制结果。
5. 深夜青黛继续使用现有素材和回退逻辑；在用户提供主图前不新增伪造素材。

## 验收条件
- 八个主题在 `HomeScreen` 中均有可见背景层：四个新主题、深夜青黛回退、黑哥特/白圣职/薄巧专属图框。
- 主题选择栏和首页使用同一主图映射；所有运行时素材可解码且单图小于 350KB。
- 中央问候、进度环、猫咪提示、记一杯按钮和统计摘要保持可读；图片不遮挡语义内容。
- 低动态模式下背景保持静止；普通模式仅按已有候选规则低频轮换。
- 补充背景映射、布局尺寸和资源解码自动化覆盖，并执行相关测试与 `git diff --check`。
- 真机首页、OLED/深色主题、横屏、大字体和减少动态仍列为人工验收项。

## Completion record (2026-09-10)
- Fixed the shared backdrop container so full-page artwork receives the page constraints instead of collapsing inside `Crossfade`.
- Added and mapped the four user-supplied theme backgrounds; retained the existing deep-night fallback and the three dedicated frames.
- Core design-system tests, eight-theme home visual review, theme-picker Debug/Release tests, `git diff --check`, `ktlintCheck`, and the full Debug/Release build pass.
- Real-device OLED, landscape, large-font, reduced-motion, and navigation-mode checks remain manual acceptance items.

## 0.4.2 发布确认（2026-09-10）
- 用户确认本轮直接作为新的 0.4.x 版本发布，版本定为 0.4.2 / versionCode 20。
- 最终只归档一个正式 Release APK；不为 0.4.2 另存 Debug、Alpha、Beta 或其他 APK 类型。
- 发布包继续使用现有签名配置；记录包名、版本、签名证书和 SHA-256，真机验收项不因版本发布自动完成。
- 0.4.2 的最终发布仅归档 dist/Awake_DW-v0.4.2-release.apk；未另存同版本 Debug、Alpha、Beta 或其他 APK 类型。


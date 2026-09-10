# 2026-09-10 · 首页主题背景修复 QA

## 自动化证据
- [x] `LolitaBackdropLayoutTest`：背景根节点获得父页面完整尺寸，不在 Crossfade 内容层坍缩。
- [x] `LolitaBackdropTest`：八主题运行时主图映射稳定，四张新图分别归入正确主题。
- [x] `ThemeArtworkTest`：全部主图/候选图已打包、可解码、单图小于 350KB；三套专属图框不重复绘制 code-native 边框。
- [x] `HomeVisualReviewTest`：360dp / 1.3× 字体下八主题生成原生渲染诊断图，猫、常驻提示、记一杯按钮仍在首屏可达。
- [x] `ThemePreviewVisualTest`：主题选择缩略图使用更新后的主图并保持可选。
- [x] `gradlew.bat ktlintCheck build`。

## 本地视觉复核
- [x] 晨雾蓝瓷：蓝瓷花、缎带、鸟笼和器皿沿边缘清晰，中央环与文案可读。
- [x] 午后藕荷：藕荷花、莲花、珍珠和缎带可见，中央粉纸留白未被装饰侵占。
- [x] 黄昏奶茶：棕褐烛台、镜框、书本和茶具建立黄昏层次，主操作仍突出。
- [x] 雾紫玫瑰：不再复用蓝瓷主图，紫玫瑰、薰衣草和银饰与色板一致。
- [x] 黑哥特、白色圣职、薄荷巧克力：修复布局后专属图框均在首页显示。
- [x] 深夜青黛：专属新图缺失时仍显示现有暗色安全回退，不跨主题伪造素材。

## 真机待验收
- [ ] Android 真机逐一切换八主题，首页、统计、我的三页背景均持续显示，无短暂旧主题串图。
- [ ] OLED 低亮度检查黑哥特与深夜青黛的暗部层次和正文对比度。
- [ ] 360dp、横屏、1.5×/2× 字体检查图案不遮挡问候、猫咪、按钮与摘要。
- [ ] 正常动态等待 12 秒、减少动态保持静止；确认无闪切、亮度跳变或明显掉帧。
- [ ] 不同系统导航模式（手势/三键）检查底部背景裁切与导航栏接缝。

## 0.4.2 单一 Release 发布
- [x] 用户于 2026-09-10 确认版本 0.4.2 / versionCode 20。
- [x] 发布范围仅包含 `com.awakedw.app` 的正式 Release APK；不归档 0.4.2 Debug、Alpha、Beta 或其他 APK 类型。
- [x] `:app:assembleRelease` 通过，`aapt dump badging`：包名 `com.awakedw.app`，versionName `0.4.2`，versionCode `20`，minSdk 26，targetSdk 35。
- [x] Release APK 通过 `apksigner verify --verbose --print-certs`；APK Signature Scheme v2 验证通过，证书 SHA-256 `25991b6c80a27b9d07f4e31cb1d2ae605c7aeb533121a1ba0534d5883fa8d666`，与既有发布链一致。
- [x] 唯一发布包复制到 `dist/Awake_DW-v0.4.2-release.apk`；大小 8,261,028 字节，SHA-256 `70074CF410A0930E014676C06132A096F74D925A6CD86D8851A467ABC2B6743D`。


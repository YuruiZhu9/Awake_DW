# alpha13 Moodboard 草案（2026-09-16，待用户确认）

> 采集人：AI（受使用者委托按自身判断选取）。图片全部来自 Wikimedia Commons 的公有领域 / 开放授权藏品，仅作内部设计参考，**不入 APK、不对外发布**；逐张来源与授权见 `images/moodboard/MANIFEST.md`。C 类（动效）无法以静态图采集，以参考链接代替。

## 我定下的方向（一句话）

**「深夜信纸，会呼吸」**——以现有八主题的纸面气质为底，把单层插画升级为**有时间感的分层场景**：远处有天色（渐变在流动），中间有装饰（缓慢视差），近景有失焦的碎光（粒子环境）。不用 3D、不追求网页级的炫技；借的是博物馆藏品里那种「装饰围合出安静中心」的古典构图，和浮世绘渐变（ぼかし）那种克制的氛围演化。

## 四类参考与设计映射

### A 氛围场景（8 张）→ alpha13 场景层的情绪与色彩基调

| 图 | 借什么 |
| --- | --- |
| `A-utagawa-hiroshige-night-rain-at-kara-*.jpg`（广重·夜雨） | 自上而下的雨幕渐变——整屏氛围渐层（AGSL 可复现的 bokashi），非对称留白 |
| `A-night-rain-at-karasaki-*-58759184.jpg`（夜雨另一版） | 同上，双色渐变的过渡处理 |
| `A-yoshitoshi-100-aspects-of-the-moon-*.jpg`（月百姿） | 低饱和蓝灰 × 一轮月——「大面积安静 + 一个发光焦点」正是首页（环 = 月）的构图 |
| `A-tsukioka-yoshitoshi-murasaki-shikibu-*.jpg` | 夜色的层次：建筑暗部、中景树、天色亮带——三层视差的深度分配 |
| `A-utagawa-hiroshige-eight-views-of-kan-*.jpg` | 横向天/水/地的色彩分段与薄雾 |
| `A-brooklyn-museum-enjoying-the-insect-*.jpg` | 黄昏过渡色（纸黄 × 青绿）——昼转晚的时段演化参考 |
| `A-van-gogh-starry-night-google-art-pro-*.jpg` | 星空的「旋转笔触密度」——粒子群的运动感上限，仅借势不借形 |

### B 纸面材质与花边（7 张）→ 既有八主题的「升级版」质感依据

| 图 | 借什么 |
| --- | --- |
| `B-02-mucha-documentsdecoratifs-1901-*.jpg`（慕夏） | **左边框竖条 + 中央拱形留白**——与现有蕾丝边角装饰同构，但更完整；alpha13 花边构图的原型 |
| `B-alfons-mucha-documents-decoratifs-19-*.jpg` | 装饰元素的疏密节奏（密在角、疏在边、空在心） |
| `B-collar-lacma-m-65-61-24-1/2-*.jpg`（LACMA 蕾丝领） | 真蕾丝的针法密度与半透明层次——代码绘制蕾丝的细节目标 |
| `B-evangeliarium-*` / `B-book-of-hours-*.jpg`（时祷书） | 「装饰框住中央文字区」的原型；金线藤蔓 = 粒子串连成线的灵感 |
| `B-embroidery-and-lace-pattern-book-*.jpg` | 图案的重复与变奏——花边连续纹样的生成逻辑 |
| `B-the-lace-embroidery-collector-*.jpg` | 蕾丝分类图谱——不同主题配不同针法的差异化思路 |

### C 动效与交互（链接参考，静态图采不到）

- Dribbble 搜索 [`water reminder`](https://dribbble.com/search/water-reminder)、[`habit celebration`](https://dribbble.com/search/habit-tracker-celebration)、[`particle animation`](https://dribbble.com/search/particles-mobile)：达标时刻、徽章微光、粒子庆祝的克制版式。
- Codrops（tympanus.net/codrops）`particles`、`ambient` 合集：网页端氛围演化的手法拆解（缓动、密度、色彩过渡），转译为 Compose 可行项。
- 已入库试点：0.7.0 分层素材（`images/Lolita/中景装饰层.png` 合格待清理；前景层待重生成）。

### D 色板方向（3 张）→ 深夜青黛与主题演化的实物色证

| 图 | 借什么 |
| --- | --- |
| `D-indigo-dyed-under-kimono-*.jpg`（靛蓝和服） | **靛蓝底 × 月白碎点**——「深夜青黛 + 粒子」的实物色彩配比，粒子密度与亮度的天然标尺 |
| `D-isaak-soreau-*-138103118.jpg` / `D-still-life-of-plums-*-180143671.jpg`（青花静物） | 「白瓷上_BLUE」与「蓝底上白」两种正反配色——暗色主题与浅色主题的对比度策略 |

## alpha13 基线预判（确认 moodboard 后细化为正式基线增补）

1. **场景层级**：背景层（现有主图）/ 中景装饰层（S2 素材）/ 近景失焦层，三层视差 + 慢速漂移；
2. **色彩演化**：主题色板不动，在「主题 × 时段」上做连续氛围插值（借 A 类的天色分段）；
3. **粒子预算**：QUIET 14→约 24、STANDARD 24→约 80（借靛蓝和服的碎点密度定观感，最终按真机帧率校准）；
4. **AGSL（API 33+）**：夜空渐变 bokashi、纸纤维噪声、柔焦 bloom 三项优先；26–32 回退静态渐变 + 噪点；
5. **边界不变**：中央阅读区留白、减少动态全关、无网络资源、装饰不进语义树。

## 待确认

- 方向本身（「深夜信纸，会呼吸」）：认可 / 修改；
- 四类映射是否有你不想要的（比如不想要浮世绘气质、不想要慕夏的花边密度）；
- 确认后我把本草案升格为 alpha13 基线增补（visual-baseline §13），进入 0.7.0 plan。

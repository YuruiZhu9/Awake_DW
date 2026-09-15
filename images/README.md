# 视觉素材源文件

这里保存本轮由用户提供的视觉素材原文件，方便后续继续做视觉迭代：

- `cat/beforedrink.png`：记录喝水前的猫咪状态。
- `cat/afterdrink.png`：记录喝水后的猫咪状态。
- `Lolita/`：蕾丝、蝴蝶结、花朵、鸟笼和哥特风格的氛围参考图。

APK 使用的文件位于 `app/src/main/assets/`：猫咪素材保留透明背景，Lolita 素材经过尺寸与 JPEG 体积优化，并按主题以低透明度乘法混合作为页面背景装饰。这里的素材不对应收藏、解锁或任何产品状态。

素材由用户提供，本轮不需要再收集外部图片或资料。

## alpha10 专属素材（2026-09-07）

| 用户原图 | 运行时用途 |
| --- | --- |
| `Lolita/薄巧/*17_12_20.png` | 薄巧完整边框 → `lolita/thin_mint.jpg` |
| `Lolita/黑哥特与白圣职/*17_12_02.png` | 透明哥特元素：取黑玫瑰、窗棂和银饰，重排为 `lolita/gothic_frame.jpg` |
| `Lolita/黑哥特与白圣职/*17_12_08.png` | 透明圣职边框铺冷白底 → `lolita/cleric.jpg` |
| `Lolita/黑哥特与白圣职/*17_11_55.png` | 黑/白帷幔和材质参考；不整张铺入独立主题，保留原图供后续迭代 |

### 衍生图构建

在 Windows PowerShell 中运行 `tools/prepare-theme-art.ps1`，使用系统 System.Drawing，不安装外部图像依赖。
执行策略限制时，可经授权仅为该进程使用 `powershell -NoProfile -ExecutionPolicy Bypass -File tools/prepare-theme-art.ps1`；不更改系统策略。

- 原始 PNG 不覆盖、不改名、不裁剪；APK 仅打包 app/assets 中的衍生图。
- JPEG 品质 88，最长边 1440px，单张小于 350KB；哥特裁片边缘做 28px 透明羽化。
- 圣职图先正确合成透明通道再转 JPEG，避免透明区域落成黑底。
- 薄巧、圣职仍是纸面混合；哥特保留原始暗色与银饰，不做负片反相。
- 本轮新增三张背景共约 546KiB；原始大图不会进入 APK。
- 图片来自用户；无需额外收集材料，没有在线下载或生成新图片。

## 0.4.1 background candidates (2026-09-09)

This iteration uses only existing user-provided originals. `tools/prepare-theme-candidates.ps1` derives same-theme frames from the existing blue, rose and warm paper artwork; no external download or network dependency is introduced. Generated JPEGs use a 1440px longest edge and stay below the 350KB per-image budget.

| Runtime files | Theme use |
| --- | --- |
| `blue_alt.jpg`, `blue_soft.jpg` | Morning blue porcelain candidates |
| `rose_alt.jpg`, `rose_soft.jpg` | Afternoon lotus candidates |
| `warm_alt.jpg` | Evening milk-tea candidate |
| `lavender.jpg` | Misty lavender candidate |

`ThemeArtwork` keeps each primary frame first and rotates available same-theme candidates every 12 seconds in normal motion mode. Reduced motion keeps the primary frame still. Theme cards show the primary frame only; Gothic, Cleric and Thin Mint keep their dedicated frame without a second code-native border.

> 2026-09-15 清理注记：候选轮换机制已被 0.5.0 新背景取代，上表候选图及旧主图（`blue/green/rose/warm` 系列，共 10 张）经复核确认零代码引用，已从 `app/src/main/assets/lolita/` 移除，不再打入 APK；用户原图仍全部保留在 `images/Lolita/`。另：`Lolita/` 下 `15_29_11.png` 与 `15_29_13.png` 为 md5 完全相同的重复文件，已删除后者。

## alpha11 圣职可见度调整

2026-09-08：处理脚本对原透明圣职图中的银饰/蕾丝色素作冷银灰增强，再按原 alpha 轮廓合成瓷白底。原 PNG 不覆盖；`cleric.jpg` 变为 246352 字节。运行时使用正常绘画叠加（96%）并降低中心洗染，使边框可见、中心仍可读。
晨雾蓝瓷沿用已有 `blue.jpg`，替代旧清晨薄荷的绿色氛围图；无需用户额外收集素材。

## 2026-09-10 new theme backgrounds

The user-supplied source PNG files live in `Lolita/new/` and remain untouched. `tools/prepare-new-theme-art.ps1` creates the runtime JPEG derivatives below at 810×1440, JPEG quality 86, and below the 350KB per-file budget.

| Source | Runtime asset | Theme |
| --- | --- | --- |
| `Lolita/new/晨雾蓝瓷.png` | `lolita/morning_blue_porcelain.jpg` | 晨雾蓝瓷 |
| `Lolita/new/午后藕荷.png` | `lolita/afternoon_lotus.jpg` | 午后藕荷 |
| `Lolita/new/黄昏奶茶.png` | `lolita/twilight_milk_tea.jpg` | 黄昏奶茶 |
| `Lolita/new/雾紫玫瑰.png` | `lolita/mist_lavender_rose.jpg` | 雾紫玫瑰 |

These four images use their own quiet central paper area and edge-weighted painted decoration, so runtime rendering uses normal painted compositing instead of the former low-opacity multiply treatment. Deep-night indigo continues to use the existing fallback until a dedicated source is confirmed.

## 2026-09-15 深夜青黛专属主图（0.6.0）

- `Lolita/new/深夜青黛.png` → `lolita/midnight_indigo.jpg`（810×1440、JPEG 品质 86、约 205KB），深夜青黛不再用哥特图反色回退；`tools/prepare-new-theme-art.ps1` 已加入该条目，`gothic.jpg` 退出运行时素材。
- `Lolita/中景装饰层.png`、`Lolita/近景前景层.png`：0.7.0 分层视差试点素材初版。经 alpha 通道检测两张均为真透明 PNG：中景层结构合格（56% 全透明、元素稀疏、色板正确），入库前需低透明噪点清理；前景层约 64% 画面带半透明灰雾（含中央），接入时需中央遮罩或重新生成。两图暂按原图存档，不入 APK。
- 2026-09-15 cat 衍生图重制：`cat/beforedrink.png`（1024²→640²，1.59MB→387KB）与 `cat/afterdrink.png`（1254²→640²，1.73MB→404KB），双三次缩放、保留透明通道；显示为 108dp 见方（4x 密度 432px，640px 仍有 1.5 倍余量），afterdrink 原尺寸本就超过运行时 1024 解码上限（实际被采样到 627px），观感无损。合计 3.23MB→0.80MB。

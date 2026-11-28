# Apple Music 设计规范与 iOS 26 液态玻璃执行准则

> 本规范专为 **SeteaseCloudMusic** 开发及 AI Agent 自动化 UI 设计/编码制定。
>
> **第一视觉准则**：本项目以 **iOS 26 Liquid Glass（液态玻璃）** 作为核心视觉风格与决策中枢，结合 **Miuix Compose 组件体系** 与 **Apple Music 人机排版基准**。
>
> **关于旧版 iOS（iOS 17/18）的定位**：旧版 iOS 规范（列表信息排布、44dp 触控热区、大标题折叠断点、内容留白网格等）仅作为功能与布局的人机工效学基准（Ergonomic Baseline），严禁在视觉材质上退化为生硬不透明大卡片或一刀切硬边分割线。

---

## 一、核心设计哲学与规范出处

### 1. 核心设计哲学

1. **Content-First（内容即界面）**
   - 界面本身克制收敛、大面积留白（浅色模式纯白、深色模式 OLED 纯黑），不加多余重色装饰，让高清专辑封面、艺人写真与音乐内容成为视觉第一焦点。
2. **三维空间悬浮与物理光学仿真（3D Levitation & Physical Optics）**
   - 导航栏、Mini Player、控制托盘及关键浮层不再是扁平的二维色块，而是漂浮在音乐流光之上的光学透镜：组合 **色彩提鲜（Vibrancy）**、**高斯模糊（Blur）**、**透镜折射（Lens Refraction）**、**边缘色散（Chromatic Aberration）**、**防色带噪点抖动（Noise Dither）** 与 **重力联动 3D 棱边高光（BloomStroke）**。
3. **纯净透光与反泛白原则（Anti-Milky Transparency）**
   - 杜绝传统模糊常见的“泛白起雾”质感与一刀切硬边；通过精准底色透明度、全局通透度调节与垂直渐变溶解遮罩（`BlendMode.DstIn`），保持透光层次纯净通透。
4. **杂志化排版（Bold Editorial Typography）**
   - 通过 `34.sp` 大标题（Large Title）、精致眉标（Eyebrow Tag）与正文的高对比字阶建立清晰阅读秩序。
5. **液态弹性与物理弹簧动效（Fluid Spring & Gooey Dynamics）**
   - 拒绝生硬线性过渡：按压微缩、滑块拉丝形变、封面呼吸缩放、大标题平滑折叠与播放器共享元素展开均由物理阻尼弹簧驱动。
6. **平滑连续曲率（Continuous Squircle）**
   - 玻璃容器统一采用 `com.kyant.shapes.RoundedRectangle`，常规卡片与封面采用 `miuix-squircle`（`squircleClip` / `squircleBorder`），避免机械感直角。

### 2. 权威参考出处

- **Apple Official**：[Apple Human Interface Guidelines (HIG)](https://developer.apple.com/design/human-interface-guidelines/)、Apple iOS UI Kit 与 SF Typography / SF Symbols 层级
- **Figma 社区复刻基准**：Joey Banks *iOS & iPadOS UI Kit*（条目高度、连续圆角曲率、双层阴影、Navigation Bar 折叠断点）
- **开源工程参考**：
  - [Cider](https://github.com/ciderapp/Cider)：Apple Music 桌面端 Design Tokens 与布局参考
  - [AMLL](https://github.com/amll-dev/applemusic-like-lyrics)：逐字歌词渲染曲线与 TTML 词库规范（本项目采用纯原生 Compose 实现）
  - [Kyant Backdrop / Shapes](https://github.com/Kyant0/AndroidLiquidGlass)：Android Compose 液态玻璃折射与 Squircle 连续曲率底座
  - [Miuix](https://github.com/compose-miuix-ui/miuix)：Compose 基础控件、图标、设置项（Preference）、弹窗与平滑圆角体系

---

## 二、全局 Design Tokens（Compose 参数规范）

### 1. 语义色彩令牌（与 `AppTheme.kt` 对齐）

项目已在 [`core/ui/theme/AppTheme.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/theme/AppTheme.kt) 完整落地浅色/深色语义色板（`LightAppColors` / `DarkAppColors`），并通过 [`MiuixAdapters.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/components/MiuixAdapters.kt) 桥接到 `MiuixTheme`。编写界面时必须通过 `AppTheme.colors` 读取，严禁写死单模式色值：

| 语义 Token（`AppTheme.colors.*`） | 浅色模式（Light） | 深色模式（Dark） | 语义说明 |
| :--- | :--- | :--- | :--- |
| `accent` | `#FA233B` | `#FF2D55` | Apple Music 主强调色（珊瑚玫红） |
| `accentSubtle` | `#FA233B` @ 12% (`0x1F`) | `#FF2D55` @ 16% (`0x29`) | 胶囊选中态、标签微底色 |
| `accentBlue` | `#007AFF` | `#0A84FF` | 系统辅助主按钮强调蓝 |
| `destructive` | `#FF3B30` | `#FF453A` | 删除、移除、退出等危险操作红 |
| `background` | `#FFFFFF` | `#000000` | 页面主背景（深色为严格 OLED 纯黑） |
| `backgroundSecondary` | `#F7F7FA` | `#0D0D10` | 次级浅/深底色 |
| `groupedBackground` | `#F2F2F7` | `#141416` | 分组页、设置组底色（Grouped Background） |
| `surface` / `surfaceVariant` | `#FFFFFF` / `#F5F5F7` | `#1C1C1E` / `#2C2C2E` | 卡片、表面层、输入区与次级底色 |
| `surfaceElevated` | `#FFFFFF` | `#2C2C2E` | 悬浮卡片、模态层底色 |
| `textPrimary` | `#111111` | `#FFFFFF` | 主标题、正文、高对比主文案 |
| `textSecondary` | `#8E8E93` | `#98989F` | 艺人名、专辑名、副标题 |
| `textTertiary` | `#C7C7CC` | `#636366` | 发行年份、次级元数据、占位符、箭头 |
| `divider` / `dividerSubtle` | `#E5E5EA` / `#F0F0F2` | `#38383A` / `#262629` | `0.5.dp` 极细缩进分割线 |
| `errorContainer` / `errorContent` | `#FFF2F0` / `#D32F2F` | `#2C1618` / `#FF6961` | 错误提示容器与文本 |
| `glassSurface` | `#F7F7FA` | `#1C1C24` | 液态玻璃铺底基色 |
| `scrim` | `#000000` @ 24% | `#000000` @ 52% | 弹窗与底部面板遮罩层 |

> 显性内容 `E` 标（Explicit Badge）沿用 `#8E8E93` 灰底白字；警告状态色使用 `#FF9500`。

### 2. 字体阶梯（Typography Scale）

对标 Apple SF Pro Display / SF Pro Text 字阶，日常开发中配合 `MiuixTheme.textStyles` 与明确字重/字距使用：

| 文本角色 | 字号（sp） | 字重（FontWeight） | 字距（LetterSpacing） | 行高（LineHeight） | 应用场景 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Large Title** | `34.sp` | `Bold` (700) | `-0.8.sp` | `41.sp` | 页面一级大标题（首屏未滚动时） |
| **Collapsed Title** | `21.sp` | `Bold` (700) | `-0.5.sp` | `26.sp` | 上滑折叠后吸顶的顶栏小标题（左对齐） |
| **Section Title** | `20.sp ~ 22.sp` | `Bold` (700) | `-0.4.sp` | `26.sp ~ 28.sp` | 分区标题（“为你推荐”、“排行榜”） |
| **Eyebrow Tag** | `11.sp ~ 12.sp` | `SemiBold` (600) | `+0.8.sp` | `13.sp ~ 14.sp` | 杂志化眉标（“独家”、“编辑精选”、“每日推荐”） |
| **Item Headline** | `16.sp ~ 17.sp` | `SemiBold` (600) | `-0.2.sp ~ -0.4.sp` | `22.sp` | 歌曲名、歌单名、卡片标题 |
| **Subhead / Artist** | `14.sp ~ 15.sp` | `Normal` (400) | `0.sp ~ -0.2.sp` | `18.sp ~ 20.sp` | 艺人名、专辑名、副标文字 |
| **Footnote / Caption** | `12.sp ~ 13.sp` | `Normal` (400) | `0.sp` | `16.sp ~ 18.sp` | 发行年份、曲目数、序号 |
| **Micro / Digits** | `11.sp` | `Medium` (500) | `0.sp` | `13.sp` | 播放器时间戳（等宽数字 `tnum`，如 `-03:21`） |

### 3. 圆角与连续曲率体系（Squircle Metrics）

所有圆角均遵循平滑连续曲率（优先 `com.kyant.shapes.RoundedRectangle` 或 `miuix-squircle` 的 `squircleClip` / `squircleBorder`）：

| 视觉元素 | 推荐圆角 | 说明 |
| :--- | :--- | :--- |
| **小封面 / 缩略图**（`44~56.dp`） | `8.dp ~ 10.dp` | 歌曲列表、Mini Player 缩略图 |
| **横滑中型唱片卡片**（`140~160.dp`） | `12.dp` | 首页 / 电台横滑推荐卡片 |
| **杂志级 Hero 卡片**（`200.dp+`） | `16.dp` | 巨幅推荐卡片、歌单详情封面 |
| **全屏播放器封面**（约 `311.dp` 基准） | `24.dp` | 播放页主专辑封面 |
| **悬浮 Mini Player** | `16.dp`（或胶囊圆角） | 底部悬浮播放条 |
| **药丸按钮 / 标签** | `CircleShape` | 播放 / 随机胶囊、标签按钮 |
| **底部面板（`WindowBottomSheet`）** | 顶部双圆角 `28.dp` | 队列、歌曲操作、添加歌单等底部抽屉 |

### 4. 阴影与环境光参数（Shadow & Ambient Glow）

- **全屏播放器封面立体投影**：
  - 播放态（`scale = 1.0f`）：Y 轴偏移 `12.dp`、模糊半径 `24.dp`、颜色 `Color.Black.copy(alpha = 0.25f)`（可叠合封面主色柔和环境投影）；
  - 暂停态（`scale = 0.85f`）：投影强度与透明度衰减至约 `0.10f`。
- **底部 Mini Player 悬浮投影**：偏移 `Y = 4.dp`、模糊半径 `16.dp`、颜色 `Color.Black.copy(alpha = 0.12f)`。
- **常规玻璃外阴影（`GlassDefaults`）**：模糊半径 `10.dp`、偏移 `Y = 2.dp`，浅色背景透明度 `0.16f`、深色背景 `0.10f`；大屏浮层面板（`LiquidGlassPanel`）外阴影弥散半径 `18.dp`。

### 5. 间距与布局网格（Metrics & Spacing）

- **页面左右边距**：全局统一 `20.dp`（严禁各页参差不齐）。
- **区块外间距（Section Gap）**：模块与模块之间保持 `28.dp ~ 32.dp`。
- **区块内间距（Title to Content）**：分区标题到卡片内容保持 `10.dp ~ 12.dp`。
- **列表项规格**：单曲条目垂直高度 `56.dp ~ 64.dp`，垂直 padding `8.dp`；缩进分割线左侧对齐正文（`padding(start = 20.dp)` 或与封面右侧文字对齐），高度 `0.5.dp`。
- **最小触控热区**：所有独立图标按钮热区不得低于 `44.dp × 44.dp`（Apple HIG 标准）。

---

## 三、iOS 26 液态玻璃三层物理架构与组件代码规范

### 1. 三层物理光学结构

```text
┌─────────────────────────────────────────────────────────────┐
│ 3. 光学透镜层：RoundedRectangle + Vibrancy + Blur + Noise   │
│              + Lens 折射/色散 + 重力 3D 棱边高光 (BloomStroke)│
├─────────────────────────────────────────────────────────────┤
│ 2. 半透明底板层：按深浅主题与全局通透度动态计算 surfaceAlpha  │
├─────────────────────────────────────────────────────────────┤
│ 1. 悬浮投影层：柔和弥散外阴影，建立漂浮于内容之上的空间景深    │
└─────────────────────────────────────────────────────────────┘
```

- **默认光学参数（[`GlassSurface.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/components/GlassSurface.kt) 的 `GlassDefaults`）**：
  - `BlurRadius = 1.8.dp`，`LensRefractionHeight = 24.dp`，`LensRefractionAmount = 24.dp`（通过 `clampLensRefractionHeightPx` 自动钳制不超过组件短边的 `40%`，避免窄胶囊中心线折痕）；
  - `SurfaceAlpha = 0.14f`（亮面 `SurfaceAlphaBright = 0.20f`），`NoiseDitherCoeff = 0.0045f`（抑制高斯模糊 8-bit 色带）；
  - 支持 RenderEffect 降级：低版本或不支持设备自动抬高 `fallbackSurfaceAlpha` 保障可读性；
  - 全局通透度/磨砂调节：自动读取 `AppTheme.liquidGlass`（`opacityLevel ∈ [-1f, 1f]`，设置界面见 [`LiquidGlassSettingsSection.kt`](app/src/main/java/com/example/seteasecloudmusic/feature/auth/presentation/LiquidGlassSettingsSection.kt)）。

### 2. 标准封装调用示例（优先复用，禁止散写硬底卡片）

#### 通用液态玻璃容器：`GlassSurface` 与 `LiquidGlassCircleButton`

```kotlin
// 1. 标准液态玻璃卡片（内置防色带噪点、3D 重力高光棱边、可选指尖聚光灯）
GlassSurface(
    backdrop = backdrop,
    cornerRadius = 24.dp,
    surfaceAlpha = GlassDefaults.SurfaceAlpha,
    borderWidth = 1.dp,
    isDarkSurface = AppTheme.isDark,
    interactiveHighlight = true
) {
    // 业务内容（文字、列表、操作按钮）
}

// 2. 顶部返回 / 更多等圆形液态玻璃按钮（按压 0.92f 刚性弹簧回弹）
LiquidGlassCircleButton(
    onClick = onBackClick,
    backdrop = backdrop,
    size = 38.dp,
    contentDescription = "返回"
) {
    Icon(MiuixIcons.Back, contentDescription = null, tint = AppTheme.colors.iconPrimary)
}
```

#### 修饰符快捷扩展：`Modifier.liquidGlass`、`Modifier.glassBorder` 与 `LiquidGlassPanel`

```kotlin
// 自定义容器叠加液态玻璃折射 + 色散
Box(
    modifier = Modifier
        .fillMaxWidth()
        .liquidGlass(
            backdrop = backdrop,
            cornerRadius = 28.dp,
            refractionHeight = 16.dp,
            refractionAmount = 24.dp,
            chromaticAberration = true,
            enableNoiseDither = true,
            isDarkSurface = AppTheme.isDark
        )
)

// 大屏浮层/占位面板（无 backdrop 时自动降级为垂直渐变 + BloomStroke）
LiquidGlassPanel(
    modifier = Modifier.fillMaxWidth(),
    backdrop = LocalGlassBackdrop.current,
    cornerRadius = 28.dp
) {
    // 面板内容
}
```

#### 弹窗、底部抽屉与 Miuix 基础控件

- **弹窗与底部抽屉**：统一使用 [`AdaptiveModalSheet.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/adaptive/AdaptiveModalSheet.kt)（紧凑屏 Miuix `WindowBottomSheet` / `WindowDialog`，大屏自动限宽与折痕避让）以及 [`Ios26LiquidGlassDialog.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/components/Ios26LiquidGlassDialog.kt)（基于 Miuix `WindowDialog` 的通用提示/输入弹窗）。
- **基础控件与设置项**：统一使用 **Miuix**（`Text`、`Icon`、`Button`、`TextField`、`Card`、`Surface`、`ArrowPreference`、`SwitchPreference`、`RadioButtonPreference`、`SliderPreference`），加载圈与下拉刷新走 [`MiuixAdapters.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/components/MiuixAdapters.kt) 的 `AppLoadingIndicator` / `AppPullToRefresh`。

---

## 四、各核心页面视觉与交互规格

### 1. 首页 / 现在就听（Home）
- **连续一体式大标题顶栏**：使用 [`AppleMusicTopBar.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/components/AppleMusicTopBar.kt) 的 `AppleMusicMorphingTopBar` 与 `rememberAppleMusicCollapseFraction`（默认阈值 `64.dp ~ 80.dp`）。大标题从 `34.sp` 连续缩放到 `21.sp`，锚点严格固定在左侧（`TransformOrigin(0f, 0.5f)`），右侧头像同步从 `46.dp` 缩放到 `34.dp`（`TransformOrigin(1f, 0.5f)`），**严禁居中跳变**。
- **顶部氛围光晕**：底层叠加 [`AmbientPageBackground.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/components/AmbientPageBackground.kt)（支持用户通过 [`TopAmbientColorDialog.kt`](app/src/main/java/com/example/seteasecloudmusic/feature/auth/presentation/TopAmbientColorDialog.kt) 的 HSV 调色盘自定义氛围色）。
- **Hero 推荐与海报流墙**：主推卡片采用 `16:9` 或 `4:3` 比例，自上而下为 `11.sp` 眉标 → `22.sp` 粗体标题 → `15.sp` 副标 → 全幅唱片封面；动态海报流墙必须挂载视口可见性门控，滑出视口即挂起动画。
- **横滑唱片货架**：封面 `140~160.dp`（圆角 `12.dp`），卡片间距 `12.dp`，首尾外边距 `20.dp`；下方固定双行单行省略（第 1 行 `16.sp` 粗体标题，第 2 行 `14.sp` 次级灰艺人/发布者）。
- **缩进分割线**：分区之间使用 `0.5.dp` 细线，左侧缩进 `20.dp` 与正文边缘对齐。

### 2. 电台（Radio）
- **9 大横滑板块**：覆盖排行榜、雷达歌单、歌曲漫游（封面取色卡片）、歌手推荐、推荐歌单、艺人热门金曲、每周新热趋势、心情氛围与关注艺人新动向。
- **漫游与推荐卡**：封面主色提取结果须做内存缓存，避免滑动期重复解码；支持点击打开歌单详情（宽屏触发统一取色双栏）或直接起播。

### 3. 发现 / 浏览（Discover）
- **风格与场景色块（Genre Tiles）**：双列等宽网格（宽高比约 `1.6:1`），采用柔和渐变色块，白色粗体类别名左下角对齐（padding `12.dp`）。
- **排行榜行组（Top Charts）**：3~4 行横滑组，左侧大字号粗体排名数字，次席 `48.dp` 封面，右侧双行歌曲名与艺人。

### 4. 资料库 / 我的（Library / Mine）
- **用户中心与统计卡**：展示头像、昵称、UID 及喜欢/创建/收藏/本地曲目统计；“我喜欢的音乐”置顶 Hero 卡片。
- **结构化导航与分组**：自建 / 收藏 / 本地三 Tab 切换，行高与间距规整，右侧配 `MiuixIcons.ChevronForward` 与 `0.5.dp` 缩进分割线；列表项按压提供弹性微缩反馈。
- **本地音乐与下载**：支持多目录递归扫描、自定义下载目录（[`StorageDirectoryDialogs.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/components/StorageDirectoryDialogs.kt)）与下载任务状态展示。

### 5. 歌单与专辑详情页（Playlist & Album Detail）
- **居中 Hero 封面区**：窄屏顶部居中 `200~220.dp` 封面（圆角 `16.dp`），配柔和封面原色环境投影；下方居中 `22.sp` 粗体标题、强调色可点击作者/艺人名与元信息；支持 3 种歌单样式与按歌单独立记忆取色背景（[`PlaylistStyleDialog.kt`](app/src/main/java/com/example/seteasecloudmusic/feature/mine/presentation/PlaylistStyleDialog.kt)）。
- **双胶囊主操作区**：等宽并排「播放」（主色填充 + 白色图标）与「随机播放」（次级浅灰/深灰底或玻璃底）两颗高 `44.dp` 胶囊按钮。
- **曲目列表**：左侧序号或 `44.dp` 缩略图（专辑模式可隐藏重复封面），标题旁按需带 `E` 显性内容标，右侧 `...` 触发复用 [`TrackActionSheet.kt`](app/src/main/java/com/example/seteasecloudmusic/feature/mine/presentation/TrackActionSheet.kt)。
- **宽屏/折叠屏展开**：由详情接管内容区并左右重排封面与曲目，[`ExpandedPlaylistBackdrop.kt`](app/src/main/java/com/example/seteasecloudmusic/feature/main/ExpandedPlaylistBackdrop.kt) 将取色背景无缝延伸至侧边玻璃操作区下方。

### 6. 艺人详情页（Artist Detail）
- **沉浸式写真头图**：顶部半屏艺人通栏写真，底部多阶垂直渐变自然融入当前主题底色（浅色白 / 深色黑）。
- **信息与动效**：`36.sp+` 超粗体艺人大名配胶囊操作按钮，下方衔接热门歌曲、精选专辑与相似艺人，支持预测性返回手势与圆角阴影平滑缩放。

### 7. 全屏原生播放器（Now Playing）
- **动态流体网格背景**：由 `feature/player/presentation/mesh/` 基于封面主色驱动，暂停时自动冻结动画以降低功耗。
- **封面呼吸弹簧物理**：播放中为 `1.0f` 伴随立体阴影，暂停平滑微缩至 `0.85f` 且阴影弱化；切回播放时弹性回弹。
- **胶囊进度条（Scrubber）**：默认纤细 `3.dp` 轨，拖动时触感放大至 `5.dp`；下方左右使用 `11.sp` 等宽数字分别展示已播时间与倒数剩余时间（`-mm:ss`），中间嵌合可点击的音质徽章（点击弹出 [`AudioQualityLiquidGlassPopup.kt`](app/src/main/java/com/example/seteasecloudmusic/feature/player/presentation/AudioQualityLiquidGlassPopup.kt)）。
- **播放控制与底部工具栏**：居中大尺寸播放/暂停键 + 左右切歌键；底部提供实时歌词、音频输出/音量与待播队列入口，右上/曲目行更多操作复用 `TrackActionSheet`（可展开查看真实容器编码与音频参数）。

### 8. 纯原生逐字沉浸歌词（AMLL TTML / YRC Lyrics）
- **数据源与渲染**：纯原生 Compose 实现（[`LyricsColumn.kt`](app/src/main/java/com/example/seteasecloudmusic/feature/player/presentation/LyricsColumn.kt)），优先并发竞速 AMLL TTML 逐字词库，自动降级网易云 YRC / LRC + 翻译。
- **状态反差**：当前激活行 `28.sp ~ 32.sp` 粗体纯白高亮，字符随毫秒级时间戳逐字辉光推进与轻微上浮；非激活行 `22.sp ~ 24.sp` 低透明度（约 `0.35`）并叠加 `2~3.dp` 高斯虚化。
- **性能与沉浸交互**：非激活模糊行不订阅逐帧时钟，降低重组开销；用户自由滚动浏览歌词时暂停自动跟随，脱离触控后以弹簧曲线自动回滚并进入沉浸态。

### 9. 底部 Dock 与悬浮 Mini Player
- **形态与位置**：窄屏/展开竖屏悬浮于底部安全区上方，Mini Player 高约 `56~60.dp`（左右留边、圆角 `16.dp`），内含 `40.dp` 缩略图（圆角 `8.dp`）、歌名/艺人单行显示（空间不足跑马灯）、播放/暂停（带离散步进进度环）与切歌/队列入口。
- **手势与宽屏协同**：上滑 Mini Player 通过共享元素展开至全屏播放器；横向模式与底栏共用 `horizontalChromeFrame`（最大 `620.dp`，收缩播放条保留两侧按钮安全间距）；侧栏模式下随导航进入外缘侧边栏（详见 [`docs/iPhone-Duo官方方案对齐.md`](docs/iPhone-Duo官方方案对齐.md)）。

---

## 五、动效物理与渐变溶解标准（Motion & Haptics）

### 1. 弹簧参数与液态弹性

```kotlin
// 1. 卡片 / 按钮按压微缩回弹（按下 0.94f ~ 0.96f）
val ApplePressSpring = spring<Float>(
    dampingRatio = 0.72f,
    stiffness = 450f
)

// 2. 圆形玻璃图标按钮快速回弹（按下 0.92f，参见 LiquidGlassCircleButton）
val CircleButtonSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 700f
)

// 3. 页面下钻 / 播放器展开过渡
val AppleScreenTransitionSpring = spring<Float>(
    dampingRatio = 0.85f,
    stiffness = 300f
)
```

### 2. 滑块液态拉丝形变（Gooey Stretch）

导航指示器（[`GlassSegmentedSlider.kt`](app/src/main/java/com/example/seteasecloudmusic/core/ui/components/GlassSegmentedSlider.kt)）在拖拽或快速切换 Tab 时，依据目标位移与当前动画位移差动态拉伸宽度，并叠加 3D 水滴着色器（冷启动后离屏预热以避免首按编译掉帧）：

```kotlin
val offsetDiff = (targetOffsetX - animatedOffsetX).absoluteValue
val renderedWidth = baseWidth + offsetDiff * stretchFactor // 典型 stretchFactor 约 0.35f
```

### 3. 顶栏边缘优雅溶解（Zero-Hardline Dissolve）

顶栏渐进模糊底层通过 `BlendMode.DstIn` 垂直渐变遮罩将模糊与背景平滑融化至 `0f`，严禁在顶栏底部出现生硬水平分割线：

```kotlin
Modifier.drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.verticalGradient(
            0.0f to Color.Black,
            0.60f to Color.Black.copy(alpha = 0.90f),
            0.85f to Color.Black.copy(alpha = 0.35f),
            1.0f to Color.Transparent
        ),
        blendMode = BlendMode.DstIn
    )
}
```

### 4. 性能门控守则

- 零透明度顶栏模糊必须跳过渲染绘制；
- 全屏播放器覆盖时跳过底层页面与毛玻璃绘制；
- 迷你播放条常规进度环推进使用离散步进，离屏海报墙与非激活歌词行不得持续订阅帧时钟。

---

## 六、AI Agent 决策红线与自检清单

### ❌ 绝对禁止
1. **禁止引入 Material3 / Material 依赖**（已全面迁移至 Miuix）。
2. **禁止硬编码单浅色模式色值**（如直接写死 `Color.White` / `Color(0xFF111111)` 导致深色模式失效，必须走 `AppTheme.colors`）。
3. **禁止在需要液态玻璃层级的悬浮容器上使用死板不透明纯色矩形或生硬无弥散阴影**。
4. **禁止使用机械尖锐直角或普通矩形圆角替代平滑连续圆角**（应使用 `RoundedRectangle` / `squircleClip`）。
5. **禁止顶栏出现一刀切的水平硬边分割线**（必须采用渐进模糊与渐变溶解）。
6. **禁止用无阻尼线性动画处理交互展开与按钮反馈**，或编写无视口门控的常驻高帧率离屏动画。

### ✅ 提交前自检清单
- [ ] **1. 组件体系合规**：基础控件、设置项、弹窗与下拉刷新是否优先使用 Miuix 及 `MiuixAdapters` 封装？
- [ ] **2. 浅色/深色双模验证**：背景、文字层级（`textPrimary` / `textSecondary` / `textTertiary`）、分割线与玻璃底色是否均通过 `AppTheme.colors` 正确适配纯白与 OLED 纯黑？
- [ ] **3. 大标题与边距一致**：页面左右边距是否统一 `20.dp`？区块外间距是否 `28~32.dp`？顶部 `34.sp` 大标题滚动是否以左锚点平滑收缩至 `21.sp`？
- [ ] **4. 圆角与玻璃层次统一**：封面是否符合 `8/12/16/24.dp` 平滑圆角标准？玻璃容器是否复用 `GlassSurface` / `Modifier.liquidGlass` / `LiquidGlassPanel` 并响应 `AppTheme.liquidGlass` 全局通透度调节？
- [ ] **5. 动效与阴影自然**：按钮按压与界面展开是否使用阻尼弹簧曲线？封面播放/暂停态、悬浮条与面板阴影是否柔和弥散？
- [ ] **6. 触控与多形态适配**：所有独立图标按钮热区是否 `≥ 44.dp × 44.dp`？宽屏/折叠屏（外屏、横屏、书本/桌面姿态）及 `1.5x` 大字号下是否无裁切、无重叠？

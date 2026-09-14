# Apple Music 设计规范与页面审美执行准则 (Design System & Aesthetic Guidelines)
# Apple Music 开源社区与官方设计规范 (Apple Music Design System Specification)
# iOS 26 液态玻璃 (Liquid Glass) 设计系统与审美决策规范

> 本规范专为 **SeteaseCloudMusic** 开发及 AI Agent 自动化 UI 设计/编码制定。
> 任何参与 UI 界面开发与改版的 Agent **必须严格遵守本规范中的视觉参数、排版层级、动效曲线与组件标准**。
> 本规范基于 **Apple 官方 Human Interface Guidelines (HIG)**、**Figma 社区权威复刻套件 (Joey Banks iOS UI Kit)**、开源高赞项目 **AMLL (Apple Music-like Lyrics)**、**Cider (跨平台 Apple Music 客户端)** 及 Android 开源液态渲染库 **Kyant0 Backdrop/Shapes** 逆向工程参数统一提炼而成。
> **项目第一视觉准则**：本项目（SeteaseCloudMusic）以 **iOS 26 Liquid Glass（液态玻璃）** 作为唯一的**核心视觉风格与决策中枢**。
>
> 专为 **SeteaseCloudMusic** 开发及 AI Agent 自动化 UI 设计/编码制定。所有参与 UI 开发的 Agent 必须以此规范为最高执行准则。
> ⚠️ **关于旧版 iOS（iOS 17/18）的唯一定位**：
> 旧版 iOS 的规范（如列表信息排布、44dp 触控热区、大标题折叠等）**仅作为功能与布局的人机工效学参考（Ergonomic Baseline）**，**严禁使用旧版那种死板不透明卡片、简单纯色灰底或僵硬分割线，不得影响任何视觉决策与界面质感！**

---

## 一、核心设计哲学 (Design Philosophy)
## 一、规范数据源与权威出处 (Sources of Truth)
## 一、iOS 26 液态玻璃核心视觉哲学 (Design Philosophy)

1. **Content-First (内容即界面)**
   - 界面本身克制收敛，大面积留白（浅色模式纯白、深色模式纯黑），不加多余的重色装饰，让高清专辑封面、艺人写真与高品质音乐内容成为视觉第一焦点。
2. **Liquid Glass & Spatial Hierarchy (液态毛玻璃与悬浮层级)**
   - 顶部导航栏、底部 Dock 导航栏、Mini Player 及浮层菜单采用多层通透毛玻璃（Frosted Glass / Backdrop Blur）。
   - 渐变溶解：顶栏磨砂效果在底部自然淡化融解，不应出现生硬的底部分割线或刺眼的泛白雾罩。
3. **Bold Editorial Typography (杂志化排版系统)**
   - 采用大标题（Large Title）、精致眉标（Overline/Eyebrow Tag）与正文的高对比层级，打造兼具现代科技感与典雅杂志质感的版式。
4. **Fluid Spring Motion (物理流体弹性动效)**
   - 遵循自然物理阻尼弹簧，拒绝生硬线性的过渡。按钮触摸微缩反馈、卡片轻微按压弹性、播放器展开与收起时的连续流体形变。
5. **Continuous Squircle (平滑连续圆角)**
   - 严格采用 Apple 连续曲率平滑圆角（Squircle），而非普通机械圆角矩形。
1. **官方基准 (Apple Official)**:
   - [Apple Human Interface Guidelines (HIG)](https://developer.apple.com/design/human-interface-guidelines/)
   - [Apple Design Resources - iOS 17 & iOS 18 (Official Figma UI Kit)](https://www.figma.com/@apple)
   - [SF Symbols 5 & 6 (Typography & Iconography Hierarchy)](https://developer.apple.com/sf-symbols/)
2. **Figma 社区黄金复刻基准 (Figma Community Gold Standard)**:
   - **Joey Banks (前 Figma 官方设计布道师 / 前 Twitter 设计系统负责人)**: *iOS & iPadOS 17/18 UI Kit*
   - 提供像素级反推（Pixel-perfect metrics）：歌曲条目高度、连续圆角曲率、阴影模型、Navigation Bar 折叠断点。
3. **开源工程反向实现 (Open Source Implementations)**:
   - **AMLL (`amll-dev/applemusic-like-lyrics`)**: Apple Music 实时歌词与流体着色器管线（已集成至本项目）。
   - **Cider (`ciderapp/Cider` & `Cider-2`)**: GitHub 著名开源 Apple Music 桌面端客户端，其 Design Tokens 及布局规范。
   - **Kyant (`kyant0/backdrop` & `kyant0/shapes`)**: Android Compose 上最完美的 iOS Liquid Glass 与 Squircle 连续曲率复刻库（本项目底层依赖）。
1. **三维空间悬浮 (3D Levitation & Spatial Depth)**
   - 界面不再是扁平的二维纸张堆叠，而是由一块块漂浮在多彩音乐流光之上的**物理厚度光学透镜**。
2. **物理光学仿真 (Physical Optics Simulation)**
   - 不是普通死板的高斯模糊！全面结合 **透镜折射 (Lens Refraction)**、**边缘色散 (Chromatic Aberration)**、**鲜艳度提亮 (Vibrancy)** 与 **各向异性棱边高光 (Specular Border)**。
3. **液态表面张力与流体弹性 (Gooey Fluid Dynamics)**
   - 元素在移动时具备液态水银/粘性流体的物理形变（如滑块移动时的“拉丝拉长”加速效果、按钮按压时的“触觉微挤压”弹性反弹）。
4. **纯净透光与反泛白原则 (Anti-Milky Transparency)**
   - 杜绝传统 Android 模糊常见的“泛白起雾”病态质感，通过精准计算的半透明度、垂直渐变溶解遮罩与底层多层采样，让色彩纯净通透。

---

## 二、全局 Design Tokens (Compose 参数规范)
## 二、全局 Design Tokens (严谨数值对照表)
## 二、iOS 26 液态玻璃三层物理架构 (3-Tier Physical Model)

### 1. 色彩规范 (Color Palette)
### 1. 颜色令牌 (Color Tokens)
所有 Agent 在封装或编写卡片、浮层、弹窗、播放条时，必须还原标准的三层物理光学结构（参考 [`Ios26LiquidGlassDialog.kt`](file:///Users/halo/AndroidStudioProjects/SeteaseCloudMusic/app/src/main/java/com/example/seteasecloudmusic/core/ui/components/Ios26LiquidGlassDialog.kt) 与 [`GlassSurface.kt`](file:///Users/halo/AndroidStudioProjects/SeteaseCloudMusic/app/src/main/java/com/example/seteasecloudmusic/core/ui/components/GlassSurface.kt)）：

```kotlin
object AppleMusicColors {
    // 标志性强调色 (Signature Accent)
    val Accent = Color(0xFFFA233B)          // Apple Music 核心玫红 / 珊瑚红
    val AccentDark = Color(0xFFFF2D55)      // 深色模式下稍亮红
    val AccentContainer = Color(0x1FFA233B) // 强调色半透明容器背景 (12% alpha)
依据 Apple HIG 语义化颜色定义与 Apple Music 客户端色彩反推：

    // 浅色模式 (Light Mode)
    val LightBackground = Color(0xFFFFFFFF)       // 纯白背景
    val LightSurface = Color(0xFFF6F6F8)          // 卡片 / 分组轻灰底色
    val LightElevated = Color(0xFFFFFFFF)         // 悬浮卡片
    val LightTextPrimary = Color(0xFF111111)      // 主标题 / 正文 (高对比)
    val LightTextSecondary = Color(0xFF8E8E93)    // 艺人 / 副标题 / 描述
    val LightTextTertiary = Color(0xFFC7C7CC)     // 次级元数据 / 占位符
    val LightDivider = Color(0xFFE5E5EA)          // 0.5dp 极细分割线
| Token 名称 | 浅色模式 (Light) | 深色模式 (Dark) | 语义说明与来源 |
| :--- | :--- | :--- | :--- |
| `SystemAccent` | `#FA233B` | `#FF2D55` | Apple Music 官方主品牌色（Pink-Red 珊瑚玫红） |
| `SystemAccentSubtle` | `#FA233B` @ 12% | `#FF2D55` @ 15% | 胶囊选中态、标签微底色 |
| `BackgroundPrimary` | `#FFFFFF` | `#000000` | 页面主背景（深色模式下为严格的 OLED 纯黑） |
| `BackgroundSecondary`| `#F2F2F7` | `#1C1C1E` | 分组卡片、设置组、海报背景（Apple Grouped Background） |
| `BackgroundElevated` | `#FFFFFF` | `#2C2C2E` | 悬浮卡片、Modal 模态层底色 |
| `LabelPrimary` | `#000000` / `#111111` | `#FFFFFF` | 主文字、标题、播放控制按键（100% 对比） |
| `LabelSecondary` | `#8E8E93` | `#8E8E93` | 艺人名、副标（Apple 官方标准次级灰色） |
| `LabelTertiary` | `#AEAEB2` | `#48484A` | 发行年份、音质标签、时间戳倒数 |
| `Separator` | `#E5E5EA` | `#38383A` | 0.5dp 细分割线（Indented 分隔线） |
| `ExplicitBadge` | `#8E8E93` | `#8E8E93` | 显性内容 “E” 标背景底色（文字白） |
| `Destructive` | `#FF3B30` | `#FF453A` | Apple System Red（删除、移除歌单） |

    // 深色模式 (Dark Mode)
    val DarkBackground = Color(0xFF000000)        // OLED 纯黑背景
    val DarkSurface = Color(0xFF1C1C1E)           // 分组卡片底色
    val DarkElevated = Color(0xFF2C2C2E)          // 悬浮卡片底色
    val DarkTextPrimary = Color(0xFFFFFFFF)       // 纯白主标题
    val DarkTextSecondary = Color(0xFF8E8E93)     // 灰色副文本
    val DarkTextTertiary = Color(0xFF48484A)      // 深灰辅助文字
    val DarkDivider = Color(0xFF38383A)           // 深色分割线
### 2. Apple HIG 官方字体阶梯 (Typography Scale)

    // 功能与状态色
    val ExplicitBadge = Color(0xFF8E8E93)         // 脏标 / 显性内容 'E' 灰标
    val Warning = Color(0xFFFF9500)
    val Destructive = Color(0xFFFF3B30)           // 删除 / 危险操作红
}
```
在 Android Compose 中对标 Apple SF Pro Display / SF Pro Text 的标准字阶配置：
┌─────────────────────────────────────────────────────────────┐
│ 3. 玻璃层 (Glass Prism): 连续超椭圆 + Lens 折射 + 色散 + 描边  │
├─────────────────────────────────────────────────────────────┤
│ 2. 中间层 (Translucent Base): 0.50~0.78 半透明减淡色底板      │
├─────────────────────────────────────────────────────────────┤
│ 1. 投影层 (Levitation Shadow): 尺寸 +50px 超大软弥散环境深阴影 │
└─────────────────────────────────────────────────────────────┘
```

### 2. 字体与字阶排版 (Typography Scale)
### 1. 第一层：底部悬浮投影层 (Levitation Shadow)
- **尺寸外扩**: 左右及下方外扩 `+50px`（约 `25.dp`）。
- **模糊度**: `36.dp` 软弥散模糊，模拟物体漂浮于界面上方 `10~16dp` 的物理高度。
- **颜色**: `Color.Black.copy(alpha = 0.22f ~ 0.35f)`，呈现高级的环境光遮蔽感。

| 文本角色 | 字体大小 (FontSize) | 字重 (FontWeight) | 字距 (LetterSpacing) | 行高 (LineHeight) | 应用场景 |
| 角色 (Role) | 字号 (sp) | 字重 (FontWeight) | 字距 (LetterSpacing) | 行高 (LineHeight) | HIG 规范对照 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Large Title** | `34.sp` | `Bold` | `-0.8.sp` | `41.sp` | 页面一级大标题（首页、资料库、搜索） |
| **Collapsed Title**| `21.sp` | `Bold` | `-0.5.sp` | `26.sp` | 滑动折叠后常驻顶栏的小标题（靠左） |
| **Section Title** | `20.sp ~ 22.sp` | `Bold` | `-0.4.sp` | `26.sp` | 分区标题（如“最近播放”、“精选推荐”） |
| **Eyebrow Tag** | `11.sp ~ 12.sp` | `SemiBold` | `+0.8.sp` | `14.sp` | 杂志化眉标（如 "编辑精选"、"每日推荐"），全大写 |
| **Title 1 / Item** | `16.sp ~ 17.sp` | `SemiBold` | `-0.2.sp` | `22.sp` | 歌曲名、歌单名、卡片标题 |
| **Subhead / Artist**| `14.sp ~ 15.sp` | `Normal` | `0.sp` | `18.sp` | 艺人名、副标文字 (Secondary Color) |
| **Caption** | `12.sp ~ 13.sp` | `Normal` | `0.sp` | `16.sp` | 专辑年份、曲目数量、播放数 |
| **Micro / Digits** | `11.sp` | `Medium` | `0.sp` | `13.sp` | 进度条时间戳（`-03:21`，建议等宽） |
| **Large Title** | `34.sp` | `Bold` (700) | `-0.8.sp` | `41.sp` | iOS Large Title，首屏未滚动时 |
| **Collapsed Title**| `21.sp` | `Bold` (700) | `-0.5.sp` | `26.sp` | 滚动后吸顶的小标题（靠左对齐） |
| **Section Title** | `22.sp` | `Bold` (700) | `-0.4.sp` | `28.sp` | Title 2（“为你推荐”、“精选电台”） |
| **Eyebrow Tag** | `11.sp` | `SemiBold` (600) | `+0.8.sp` | `13.sp` | All-Caps 杂志眉标（“独家”、“编辑精选”） |
| **Item Headline** | `17.sp` | `SemiBold` (600) | `-0.4.sp` | `22.sp` | 歌曲名、歌单名（Body SemiBold） |
| **Subhead / Artist**| `15.sp` | `Normal` (400) | `-0.2.sp` | `20.sp` | 艺人名、专辑名（Subheadline） |
| **Footnote / Info** | `13.sp` | `Normal` (400) | `0.sp` | `18.sp` | 发行年份、歌曲条目序号（Footnote） |
| **Caption / Digits**| `11.sp` | `Medium` (500) | `0.sp` | `13.sp` | 播放器时间戳（等宽数字 `Tabular`） |
### 2. 第二层：中间减淡底板层 (Translucent Base Sheet)
- **作用**: 避免直接透镜折射导致复杂背景文字互相干扰，保证阅读清晰度。
- **浅色模式**: `Color(0xFFF2F2F7).copy(alpha = 0.50f ~ 0.78f)`
- **深色模式**: `Color(0xFF1C1C1E).copy(alpha = 0.60f ~ 0.80f)`

### 3. 几何与圆角体系 (Corner Radii & Shapes)
### 3. 几何、圆角与连续曲率 (Squircle Metrics)
### 3. 第三层：液态光学透镜层 (Kyant Backdrop + Lens)
- **连续曲率**: 严禁普通矩形圆角，必须使用 `com.kyant.shapes.RoundedRectangle`（G² 连续连续曲率 Squircle）。
- **透镜参数**:
  - `blur`: `2.dp ~ 18.dp`
  - `lens`: `refractionHeight = 8.dp ~ 16.dp`, `refractionAmount = 16.dp ~ 32.dp`
  - `chromaticAberration`: 开启（边缘呈现微妙的红蓝分光色散）
  - `vibrancy`: 开启（增强透射背景的饱和度）
- **微光棱边描边 (Specular Edge Border)**:
  - 宽度 `1.dp`，使用垂直渐变模拟光源反射：
  ```kotlin
  Brush.verticalGradient(
      0.0f to Color.White.copy(alpha = 0.85f),
      0.45f to Color.White.copy(alpha = 0.25f),
      1.0f to Color.White.copy(alpha = 0.60f)
  )
  ```

> 依据 Apple iOS 规范，所有圆角必须是**连续平滑曲率（Corner Smoothing: 60%）**，非简单圆角矩形。在 Compose 中通过 `com.kyant.shapes.RoundedRectangle` 实现。
---

- **小封面/缩略图 (Small Thumb, 44~56dp)**: `8.dp ~ 10.dp`
- **横滑中型唱片 (Medium Card, 140~160dp)**: `12.dp`
- **杂志级 Hero 卡片 (Large Hero, 220dp+)**: `16.dp`
- **全屏播放器封面 (Now Playing Cover, 311dp 规格)**: `24.dp`
- **悬浮 MiniPlayer (Floating Bar)**: `16.dp`
- **药丸胶囊/操作按钮 (Pill Button)**: `CircleShape`（999.dp）
- **底部抽屉/弹窗 (Action Sheet)**: 顶部双圆角 `28.dp`
## 三、全局代码组件执行规范 (Compose Components)

### 4. 阴影与环境光参数 (Shadow & Ambient Glow)
Agent 在编写界面时，**严禁自行散落编写生硬背景**，必须统一使用项目中已封装的液态玻璃核心基建：

Joey Banks 及 Cider 反推的 Apple Music 标志性双层阴影模型：

### 1. 通用卡片与容器：[`GlassSurface.kt`](file:///Users/halo/AndroidStudioProjects/SeteaseCloudMusic/app/src/main/java/com/example/seteasecloudmusic/core/ui/components/GlassSurface.kt)
```kotlin
object AppleMusicShapes {
    val ThumbnailSmall = RoundedCornerShape(8.dp)   // 40-56dp 列表小图
    val CoverMedium = RoundedCornerShape(12.dp)     // 120-160dp 唱片横滑卡片
    val CardLarge = RoundedCornerShape(16.dp)       // 200dp+ 巨幅推荐卡片
    val NowPlayingArtwork = RoundedCornerShape(24.dp)// 全屏播放器封面
    val MiniPlayer = RoundedCornerShape(16.dp)      // 底部悬浮 MiniPlayer
    val Pill = CircleShape                          // 药丸胶囊按钮 / 标签
    val ActionSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) // 底部面板
// 标准液态玻璃卡片
GlassSurface(
    backdrop = backdrop,
    cornerRadius = 24.dp,
    surfaceAlpha = GlassDefaults.SurfaceAlpha, // 0.5f
    borderWidth = 1.dp
) {
    // 业务内容（文字、列表、操作按钮）
}
// 1. 全屏播放器封面立体投影 (Play 状态)
// 复合参数：Y 轴偏移 12dp，模糊半径 24dp，扩散 -4dp，颜色 rgba(0, 0, 0, 0.25)
// 暂停 (Pause) 状态：缩放降至 0.85f，Alpha 降至 0.10f
```

// 2. 底部 MiniPlayer 悬浮投影
// 偏移：Y = 4dp, Blur = 16dp, 颜色 rgba(0, 0, 0, 0.12)
### 2. 修饰符快捷扩展：`Modifier.liquidGlass` 与 `Modifier.glassBorder`
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .liquidGlass(
            backdrop = backdrop,
            cornerRadius = 28.dp,
            refractionHeight = 16.dp,
            refractionAmount = 32.dp,
            chromaticAberration = true
        )
        .glassBorder(cornerRadius = 28.dp)
)
```

### 4. 间距与布局网格 (Metrics & Spacing)
### 3. 弹窗与确认框：[`Ios26LiquidGlassDialog.kt`](file:///Users/halo/AndroidStudioProjects/SeteaseCloudMusic/app/src/main/java/com/example/seteasecloudmusic/core/ui/components/Ios26LiquidGlassDialog.kt)
- 带有完整的 Scale 0.86 → 1.0f 弹簧入场、尺寸外扩 +50px 阴影以及 Lens 色散折射。

- **页面左右边距**: 全局统一 `20.dp`（严禁参差不齐）。
- **区块外间距 (Section Gap)**: 模块与模块之间距离保持 `28.dp ~ 32.dp`。
- **区块内间距 (Title to Content)**: 分区标题到卡片内容距离 `10.dp ~ 12.dp`。
- **列表项内边距**: 单曲条目垂直高度 `56.dp ~ 64.dp`，垂直 padding `8.dp`。
- **可点击热区**: 所有独立触控图标/按钮热区不低于 `44.dp × 44.dp`（Apple HIG 标准）。

---

## 三、所有核心页面的审美与交互规范
## 三、各核心页面权威审美概要 (Page-by-Page Specifications)
## 四、各核心页面的 iOS 26 风格演进规范

### 1. 首页 / 现在就听 (Listen Now / Home)
- **大标题动效**: 页面顶部常驻 34sp 粗体「现在就听」大标题，右侧挂载 36dp 用户头像。当用户上滑时，通过 `rememberAppleMusicCollapseFraction` 将大标题平滑缩放并淡入至顶栏 21sp 小标题，左对齐自然衔接。
- **Hero 巨幅海报轮播**:
  - 顶部主推位采用 16:9 或 4:3 比例的精选卡片。
  - 卡片顶部为 11sp 浅灰/强调色眉标（如 "独家发布"），中部为 22sp 粗体标题，底部为副标题与全幅高清专辑封面。
- **双排/单排横滑唱片架**:
  - “最近播放”、“为你推荐” 采用 140~160dp 封面卡片。
  - 封面下方只保留两行文字：第 1 行为歌曲/歌单名（16sp 粗体），第 2 行为艺人/发布者（14sp 浅灰）。超出单行强行省略号。
- **分区细线**: 分区之间使用 0.5dp 极细分割线，起始位置缩进与文字对齐（`padding(start = 20.dp)`）。
### 1. 首页 / 现在就听 (Listen Now)
- **大标题折叠曲线 (Large Title Transition)**:
  - 滚动距离阈值固定为 `80.dp`（使用 `rememberAppleMusicCollapseFraction`）。
  - 大标题从 `34sp` 缩放到 `21sp`，锚点始终为左侧（`TransformOrigin(0f, 0.5f)`），平滑淡入顶栏小标题，**严禁居中跳变**。
- **Hero 精选推荐轮播 (Featured Carousel)**:
  - 卡片宽高比严格为 `16 : 9` 或 `4 : 3`。
  - 结构自上而下：`11sp 眉标` → `22sp 粗体标题` → `15sp 副标` → `全幅唱片写真`。
- **横滑卡片货架 (Horizontal Scroller)**:
  - 封面 `150 × 150dp`，卡片间距 `12dp`，首尾外边距 `20dp`。
  - 文本固定双行：第 1 行歌曲/歌单名（16sp 粗体），第 2 行艺人名（14sp 灰），超长一律结尾省略号。
- **Indented 分割线**: 分割线左侧缩进 `20dp`（与文本边缘齐平），高度严格为 `0.5dp`。
各页面在重构或新建时，必须严格执行以下 **iOS 26 液态玻璃化决策**：

### 2. 发现 / 浏览 (Discover / Browse)
- **分类色块卡片 (Genre Grid)**:
  - 2 列等宽圆角卡片（宽高比约 1.6 : 1）。
  - 采用高饱和度但舒适的柔和渐变色块（如蓝紫渐变、橘红渐变、翡翠绿渐变），大号粗体类别名称固定在卡片左下角。
- **排行榜模块 (Top Charts)**:
  - 3-4 行横向滑动组。每行最左侧为高对比大字号粗体排名数字（`#1`、`#2`、`#3`），数字颜色从深黑过渡到次级灰，彰显权威感。
### 2. 浏览 / 发现 (Browse / Discover)
- **风格与场景色块 (Genre Tiles)**:
  - 双列等宽网格，宽高比 `1.6 : 1`。
  - 纯色/双色微渐变圆角底色（深紫、珊瑚红、宝蓝、青绿），白色超粗体文本靠左下角对齐（Padding 12dp）。
- **排行榜行组 (Top 100 Charts)**:
  - 3~4 行横滑组。左侧固定大字号排名数字（字号 24sp、字重 Heavy），次席为 48dp 专辑图，右侧为歌曲名与艺人。
```
┌─────────────────────────────────────────────────────────────┐
│ 1. 首页 / 推荐                                              │
│    • 顶部：纯透明渐变融解液态顶栏，大标题随动左对齐平滑缩小     │
│    • 卡片：推荐歌单与雷达卡片全面采用 GlassSurface 容器包裹    │
│    • 背景：底层透出柔和弥散的彩色光晕，供玻璃折射采样           │
├─────────────────────────────────────────────────────────────┤
│ 2. 电台 / 私人 FM (🔥当前优先开发)                          │
│    • 全屏沉浸动态流体背景（随着歌曲不断微动态缓慢流转）          │
│    • 唱片悬浮于正中，四周投射深度环境光阴影                      │
│    • 控制栏：三颗操作按钮置于悬浮液态玻璃药丸胶囊内             │
├─────────────────────────────────────────────────────────────┤
│ 3. 全屏播放器 (Now Playing)                                 │
│    • 播放/暂停具有液态水银般呼吸弹簧（Play 1.0f / Pause 0.85f） │
│    • 底部三键（歌词/路由/队列）镶嵌在液态玻璃磨砂浮动托盘中      │
│    • 进度条拖动时，手柄产生液态水滴拉伸变形                      │
├─────────────────────────────────────────────────────────────┤
│ 4. 底部 Mini Player & 导航栏 (Dock)                         │
│    • MiniPlayer：悬浮玻璃胶囊，边缘带有色散高光与上滑手势展开    │
│    • GlassSlider：Tab 切换指示器具备物理拉丝弹性（Gooey Stretch）│
├─────────────────────────────────────────────────────────────┤
│ 5. 资料库 / 我的 (Mine)                                     │
│    • 分类条目与歌单列表摒弃生硬实色白底，统一装载入 GlassSurface│
│    • 列表项点击时具有 0.96f 微缩物理弹簧反馈                    │
└─────────────────────────────────────────────────────────────┘
```

### 3. 电台 / 私人 FM (Radio)
- **电台直播卡片 (Apple Music 1 风格)**:
  - 采用横幅卡片，左上角标红色的「LIVE」呼吸灯指示器与电台标识。
- **Apple Music 1 风格直播电台**:
  - 横幅节目封面，左上角标红色的呼吸灯圆形标与 `LIVE` 字样。
- **私人 FM 模式**:
  - 页面极致纯净，居中展示高质感唱片封面。
  - 底部提供 3 颗经典操作按钮（喜爱、跳过、不感兴趣），背景伴随音乐主色调柔和呼吸。
  - 全屏沉浸居中唱片封面，伴随封面提取的微动态环境光流动。
  - 底部提供 3 颗经典操作按键：喜爱（Heart/Star）、跳过（Next）、不喜欢（Trash）。

### 4. 资料库 / 我的 (Library / Mine)
- **经典结构化目录 (TableView Navigation)**:
  - 顶部固定列表项：“播放列表”、“艺人”、“专辑”、“歌曲”、“已下载”、“本地音乐”。
  - 每一项高 52dp，左侧为 Apple SF 风格强调色线性图标与文字，右侧为浅灰色 Chevron 右箭头（`>`），项与项之间带有 0.5dp 分割线。
- **标准 TableView 导航组**:
  - 固定条目：播放列表、艺人、专辑、歌曲、已下载、本地音乐。
  - 行高严格为 `52dp`，左侧 SF 风格强调色图标 + 17sp 标题，右侧浅灰色 Chevron 箭头（`>`），行间以 0.5dp 线隔开。
- **最近添加 (Recently Added)**:
  - 2 列网格展示最近收藏的歌单与专辑。
  - 1:1 封面圆角 12dp，下方双行说明，排布如同整齐陈列的实体黑胶架。
  - 2 列对称正方形网格，模拟实体黑胶唱片架陈列。

### 5. 歌单与专辑详情页 (Playlist & Album Detail)
- **居中 Hero 封面区**:
  - 顶部居中 200dp~220dp 封面，外围带有 20dp 模糊的专辑原色柔和氛围投影（Ambient Shadow）。
  - 封面下方居中排列：22sp 超粗体专辑名、红/粉色可点击艺人名、年份与音质标识（如 `无损` / `Hi-Res` / `杜比全景声` 细灰边胶囊）。
- **双胶囊主操作区**:
  - 居中并排两个 50% 宽度的药丸按钮：
    - 左侧「播放」：实体黑色或强调红填充，带有白色播放三角。
    - 右侧「随机播放」：轻灰表面色（`Color(0xFFF2F2F7)`）或玻璃磨砂底，黑字。
- **曲目列表区**:
  - 每行左侧显示轻量轨道序号（1, 2, 3...）或 44dp 小缩略图。
  - 标题旁若有显性内容附带灰色粗体 `E` 方形微标（Explicit）。
  - 右侧为 `...`（三点菜单）触控键。
### 5. 歌单与专辑详情页 (Album / Playlist Detail)
- **居中 Hero 区**:
  - 顶部居中 `200dp ~ 220dp` 封面，圆角 16dp，附带柔和环境投影。
  - 居中信息：22sp 大标题专辑名、强调红可点击艺人名、发行年份与音质小胶囊（如 `无损` / `Hi-Res`）。
- **双胶囊主操作键 (Primary Action Buttons)**:
  - 居中等宽并排两颗胶囊按钮（高 `44dp`）：
    - 左侧「播放」：黑色/红色高亮填充，白色播放图标。
    - 右侧「随机播放」：轻灰表面色（`#F2F2F7`）或毛玻璃底，黑色图标。
- **曲目列表 (Track Rows)**:
  - 左侧轨道序号（或 44dp 封面），中间歌曲名（如有脏标带 `E`），右侧 `...` 菜单。

### 6. 艺人详情页 (Artist Detail)
- **巨幅沉浸式写真 (Parallax Hero)**:
  - 顶部半屏采用艺人通栏写真，底部使用多阶垂直渐变无缝融入页面底色。
  - 艺人大名以超粗 36sp+ 字体横卧在写真下方，旁边配备圆形「关注/已关注」毛玻璃药丸按钮。
- **代表作与精选专辑**:
  - Top 5 最热单曲置顶，下方接横滑精选专辑（包含发行年份）。
### 6. 全屏播放器 (Now Playing - 灵魂核心)
- **流体环境光背景 (Fluid Mesh Glow)**:
  - 基于 Palette 提取封面 2~3 个主色，在底层 Canvas 进行 80dp+ 高斯扩散。
- **封面弹簧物理 (Artwork Spring Physics)**:
  - 播放中：原大 `1.0f`（尺寸约 311dp），伴随深层投影。
  - 暂停中：平滑微缩至 `0.85f`，投影弱化；点击播放时伴随弹簧弹性回弹放大。
- **纤细进度轨 (Scrubber)**:
  - 默认高度 `3dp`，拖动时触感放大至 `5dp`；下方左右分别标注已播时间与剩余时间（`-mm:ss`）。
- **三键式播放控制**:
  - 居中播放/暂停键直径显著增大（48dp+ 图标），左右切歌键 32dp。
- **底部快捷栏**:
  - 固定 3 颗微标：实时歌词（对话气泡）、隔空播放（AirPlay/路由）、播放队列（抽屉清单）。

### 7. 全屏播放器 (Now Playing Screen)
- **动态流体彩色背景**:
  - 使用 Palette 提取当前曲目封面的 2-3 个主色调，在背景渲染超大半径（80dp+）的流体径向模糊渐变，暗部微沉，形成深邃沉浸的舞台感。
- **封面呼吸物理定律 (Artwork Spring Physics)**:
  - 播放中：封面为原尺寸（1.0f），伴随柔和的深度投影（Elevation Shadow）。
  - 暂停中：封面优雅微缩至 `0.85f`，投影深度减半；点击播放时伴随自然的弹簧弹跳（Spring）恢复原状。
- **Scrubber 进度条**:
  - 进度轨厚度 3dp（极其精致纤细），拖动时瞬时平滑变厚至 5dp，拖动手柄带触觉放大反馈。
  - 下方左右为 11sp 等宽数字，左侧为已播放时间，右侧为倒数剩余时间（带负号 `-03:42`）。
- **控制按键与底部工具栏**:
  - 播放/暂停键位于中心，尺寸显著大于左右切歌键。
  - 底部固定 3 个轻量图标：歌词开关（双引号气泡）、隔空播放（音箱/路由）、播放列表（清单抽屉）。
### 7. 实时沉浸歌词 (AMLL Lyrics Pipeline)
- **当前行**: `28sp ~ 32sp`，粗体纯白高亮，字符跟随毫秒级时间戳逐字点亮（Karaoke Gradient Mask）。
- **非当前行**: `22sp ~ 24sp`，透明度 `0.35`，叠加 `2~3dp` 高斯虚化。
- **交互回滚**: 滚动浏览歌词时显示定位锚点；脱离触控 3 秒后，以弹簧曲线自动回滚至当前句。

### 8. 时间轴实时沉浸歌词 (Dynamic Lyrics)
- **字号与状态反差**:
  - 当前激活行：`28.sp ~ 32.sp`，超粗体（Bold），纯白高光，字符跟随音频毫秒级发光推进。
  - 非激活行：`22.sp ~ 24.sp`，半透明灰色（alpha 0.35），轻微高斯虚化（Blur 2-3dp）。
- **弹性回滚**: 用户自由滚动浏览歌词时显示定位针；停止交互 3 秒后，平滑以弹簧曲线自动回弹滚动至正在唱的当前行。
### 8. 悬浮 Mini Player
- **形态**: 悬浮于底栏上方 `8dp`，高度 `56dp`，圆角 `16dp`，左右边距 `12dp`。
- **材质**: Liquid Glass（Backdrop 采样模糊 + 0.5dp 微光半透明描边）。
- **手势**: 向上滑动直接以共享元素形变展开为全屏播放器。

### 9. 底部悬浮 Mini Player
- **形态与位置**:
  - 悬浮在底部 Dock 导航栏上方 `8.dp` 处，左右留边 `12.dp`，高度 `56.dp`，圆角 `16.dp`。
  - 采用 Liquid Glass（Backdrop 实时动态采样模糊 + 0.5dp 微光描边）。
- **操作元素**:
  - 左侧 40dp 微型封面（圆角 8dp）。
  - 中间歌曲名与艺人单行居中排布（空间不足时自动跑马灯 Marquee）。
  - 右侧并列「播放/暂停」与「下一首」图标。
- **手势驱动**:
  - 向上拖拽滑动无缝插值缩放，直接升起展开至全屏播放器。

---

## 四、交互动效与手势规范 (Motion & Haptics)
## 四、动效曲线与触觉反馈规范 (Motion & Haptics)
## 五、动效物理标准：液态水滴与弹簧 (Fluid Motion)

1. **Spring 物理弹簧参数推荐**:
1. **滑块与指示器的液态拉丝弹性 (Gooey Stretch Effect)**:
   - 当指示器在横向或纵向快速位移时，根据当前位移动画差值动态扩展宽度或高度：
   ```kotlin
   // 适合触控轻微回弹（按压卡片、按钮）
   val ApplePressSpring = spring<Float>(
       dampingRatio = 0.8f,
       stiffness = 400f
   )
依据 Apple HIG 弹性物理参数标准：

   // 适合界面展开与下钻过渡
   val AppleScreenTransitionSpring = spring<Float>(
       dampingRatio = 0.85f,
       stiffness = 300f
   )
   val offsetDiff = targetOffsetX - animatedOffsetX
   val stretchFactor = 0.35f
   val renderedWidth = baseWidth + offsetDiff.value.absoluteValue.dp * stretchFactor
   ```
2. **按压反馈 (Touch Feedback)**:
   - 按钮或卡片按压时，整体缩放系数平滑降至 `0.96f`，松开时以弹簧回弹。
3. **顶栏毛玻璃渐变遮罩标准实现**:
2. **触控微挤压 (Tactile Squish)**:
   - 按钮按压时：缩放至 `0.94f`，松开时以高刚度、自然阻尼弹簧迅速回弹：
   ```kotlin
   // 顶栏磨砂平滑融化，绝不能有一刀切的硬边
   spring(dampingRatio = 0.72f, stiffness = 650f)
   ```
3. **顶栏边缘优雅融解 (Zero-Hardline Dissolve)**:
   - 导航栏底部使用垂直渐变遮罩将模糊与透明度平滑融化为 0，严禁出现任何一刀切的水平硬边：
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
```kotlin
// 1. 卡片与按钮按压反馈 (Touch Down Scale)
// 按下时缩放到 0.96f，松开时以阻尼弹簧迅速回弹
val AppleButtonPressSpring = spring<Float>(
    dampingRatio = 0.80f,
    stiffness = 450f
)

// 2. 页面下钻 / 播放器展开物理弹簧
val AppleSheetTransitionSpring = spring<Float>(
    dampingRatio = 0.85f,
    stiffness = 320f
)
```

---

## 五、AI Agent 执行守则 (Agent Execution Checklist)
## 五、AI Agent 执行检查清单 (Checklist)
## 六、AI Agent 决策红线 (Do's & Don'ts for Agents)

当 Agent 为本项目设计或编写新页面（如私人 FM、歌单详情、设置页等）时，必须执行以下自查：
所有 Agent 在提交代码前，必须对照本清单自检：
### ❌ 严禁出现（绝对禁止）：
1. **禁止使用纯实色非透明大卡片**（如普通的纯白 `#FFFFFF` 或纯灰 `#EEEEEE` 实体不透明矩形），这会直接毁掉液态玻璃的通透感。
2. **禁止使用没有柔和外扩的生硬阴影**（必须是软弥散的大半径立体浮动阴影）。
3. **禁止使用生硬直角或普通圆角**（必须使用 `RoundedRectangle` 连续超圆角 Squircle）。
4. **禁止使用简单线性动画**（必须使用带有物理阻尼感与张力的 spring 曲线）。
5. **禁止顶栏出现生硬水平分割线**（必须是渐变遮罩自然溶解）。

- [ ] **1. 大标题是否合规？** 页面顶部是否具备 34sp 大标题？上滑时是否平滑微缩成 21sp 顶栏标题且保持左对齐？（参考 `AppleMusicTopBar.kt`）
- [ ] **2. 边距是否一致？** 页面主体两边 Padding 是否严格为 `20.dp`？分区垂直间距是否为 `28.dp` 以上？
- [ ] **3. 圆角是否统一？** 封面是否使用对应规范的圆角（8dp/12dp/16dp/24dp），杜绝生硬直角和不合标准的任意圆角？
- [ ] **4. 文字层级是否分明？** 是否遵循 Primary (#111111 / #FFFFFF) 与 Secondary (#8E8E93) 的明确反差？是否使用了全大写紧凑眉标？
- [ ] **5. 毛玻璃与微光描边？** 浮层、导航栏、MiniPlayer 是否应用了 Backdrop 模糊及 0.5dp 细致半透明描边（`Color.Black.copy(0.08f)` 或 `Color.White.copy(0.12f)`）？
- [ ] **6. 动效是否具有流体弹簧感？** 是否避免使用生硬无物理阻尼的线性动画？
- [ ] **7. 触摸热区达标？** 所有 Icon 点击热区是否满足不低于 `44.dp`？
- [ ] **1. 边距对齐**: 页面左右全局边距是否严格统一为 `20.dp`？模块垂直间距是否保持在 `28~32.dp`？
- [ ] **2. 大标题折叠**: 页面顶部是否具备 34sp 大标题？滑动折叠是否平滑缩小至 21sp 顶栏左侧小标题？（直接复用 `AppleMusicTopBar.kt`）
- [ ] **3. 连续曲率圆角**: 封面缩略图是否为 8dp/12dp/16dp，播放器封面是否为 24dp？是否严禁机械尖锐直角？
- [ ] **4. 文字层级反差**: 主文字与次级艺人文字是否满足 `#111111` 与 `#8E8E93` 的明确明暗反差？眉标是否全大写且微拉开字距？
- [ ] **5. 毛玻璃与渐变遮罩**: 顶栏是否采用渐变遮罩融化（透明度与模糊度向下渐变为 0，不泛白不切硬线）？
- [ ] **6. 触控热区达标**: 所有可点击 Icon 热区是否大于等于 `44.dp × 44.dp`？

### ✅ 必须执行（强制要求）：
1. **所有卡片与浮动条统一使用 [`GlassSurface`](file:///Users/halo/AndroidStudioProjects/SeteaseCloudMusic/app/src/main/java/com/example/seteasecloudmusic/core/ui/components/GlassSurface.kt) 或 `liquidGlass()` 修饰符**。
2. **透镜必须包含 Lens 凸透折射与色散（Chromatic Aberration）**，凸显厚度感与玻璃折射光泽。
3. **必须配备 1dp 的高光渐变描边（`glassBorder`）**，模拟玻璃受光边缘的物理反光。
4. **滑块切换必须支持粘性拉丝形变（Gooey Stretch）**。

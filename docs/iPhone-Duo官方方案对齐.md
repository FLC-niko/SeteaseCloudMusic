# iPhone Duo 官方布局方案对齐

参考 Apple 设计团队 [Design for iPhone Duo](https://developer.apple.com/videos/play/tech-talks/111466/) 的折叠屏人机交互原则（控件外缘定位、内屏分栏、弹层限宽与折痕避让），本项目基于 Android Jetpack WindowManager 1.5、安全区 API、Miuix 弹窗体系与 Liquid Glass 材质完成对应实现。

## 一、窗口与形态规则

策略集中在 [`AdaptiveWindowInfo`](../app/src/main/java/com/example/seteasecloudmusic/core/ui/adaptive/AdaptiveLayout.kt)，由真实窗口宽高、多窗口位置与 `FoldingFeature` 动态驱动：

| 窗口场景 | 导航与操作区 | 内容与歌单详情 | 播放页 |
|---|---|---|---|
| **普通手机竖屏** | 悬浮底部 Dock + Mini Player | 单栏下钻 | 竖向封面 / 信息 / 控制，可切歌词页 |
| **折叠设备合拢外屏** | 外缘悬浮侧边栏（导航组靠下、迷你播放器在其上方） | 单栏下钻 | 竖向封面 / 信息 / 控制 |
| **展开内屏竖屏** | 保留横向底部 Dock + 限宽 Mini Player | 打开歌单后由详情接管内容区并左右双栏 | 竖向居中限宽列（最大 `680.dp`），可切歌词页 |
| **宽横屏** | 外缘悬浮侧边栏，内容区避让操作区 | 同上，侧边下方延伸同一封面取色底座 | 专辑页封面与控制双栏；歌词页内容双栏 + 底部玻璃控制带（最大 `960.dp`） |
| **桌面半折叠（Tabletop）** | 横向导航位于下半屏 | 上半屏封面样式，下半屏标题、播放入口与曲目 | 专辑封面在上半屏，信息/控制与歌词在下半屏 |
| **书本半折叠（Book）** | 按方向选择横栏或侧栏，浮层避让折痕 | 左右跨折痕双栏 | 封面与控制/歌词跨折痕分区，歌词控制带位于折痕一侧 |
| **分屏左侧窗口** | 侧边操作区自动镜像至窗口左外缘 | 按实际窗口尺寸重排 | 按实际窗口空间重排 |

## 二、核心对齐要点

### 1. 展开歌单详情与统一取色背景
- 首页、电台、我的及搜索页共用 `showDetailAlongside`：未选歌单时显示完整内容流；宽屏打开歌单详情后，详情接管完整内容区并左右重排封面与曲目（不将普通列表页与取色详情硬拼并排）。
- [`ExpandedPlaylistBackdrop.kt`](../app/src/main/java/com/example/seteasecloudmusic/feature/main/ExpandedPlaylistBackdrop.kt) 将当前歌单主色延伸至侧边操作区下方，使玻璃导航与迷你播放器采样到连续色彩；关闭“取色背景”开关时同步恢复当前主题背景色（`AppTheme.colors.background`）。
- 保留 3 种歌单详情样式与按歌单独立记忆的偏好设置；展开双栏保持大封面比例突出，返回后保留原页面滚动位置，窗口重排保留选中歌单与曲目状态。

### 2. 展开播放页与歌词控制带
- [`NativePlayerScreen.kt`](../app/src/main/java/com/example/seteasecloudmusic/feature/player/presentation/NativePlayerScreen.kt) 与 [`ExpandedNativePlayer.kt`](../app/src/main/java/com/example/seteasecloudmusic/feature/player/presentation/ExpandedNativePlayer.kt) 统一依据 `useExpandedPlayer` 切换布局，歌词视图状态在窗口重排后保留。
- 竖屏展开时将歌词、曲目信息与播放控制居中收束在最大 `680.dp` 内容列内，防止进度条与按钮被过度拉伸。
- 宽横屏进入歌词页后，封面与歌词组成内容区，播放操作集中在最大 `960.dp` 的底部玻璃控制带（[`ExpandedPlaybackBar.kt`](../app/src/main/java/com/example/seteasecloudmusic/feature/player/presentation/ExpandedPlaybackBar.kt)），保留上一首、播放/暂停、下一首、音频输出、歌词、队列与更多入口，按钮触控区不低于 `44.dp`。
- 桌面半折叠将专辑封面置于上半屏、收藏/更多与播放控制置于下半屏；窄分区自动将音量收纳至独立面板。

### 3. 侧边导航与收缩迷你播放条宽度框架
- 右侧/外缘侧边栏（[`AdaptiveNavigation.kt`](../app/src/main/java/com/example/seteasecloudmusic/feature/main/AdaptiveNavigation.kt)）保留玻璃透镜滑块与独立搜索入口，迷你播放器随导航进入侧边，不再重复显示底部播放条；短横屏或软键盘压缩高度时，导航与播放入口自动收进 Miuix `WindowListPopup` 菜单确保可达。
- 横向导航栏与迷你播放条共用 [`HorizontalChromeLayout.kt`](../app/src/main/java/com/example/seteasecloudmusic/feature/main/navigation/HorizontalChromeLayout.kt) 的 `horizontalChromeFrame`：先确定居中的最大 `620.dp` 范围，再在播放条内部为两侧按钮预留 `54.dp` + `10.dp` 间距（例如 `620.dp` 时收缩播放条宽 `492.dp`），避免收缩后播放条侵入左右按钮下方，同时在 1.5 倍大字号中英混排下不发生纵向裁切。

### 4. 弹层与折痕避让
- 账户、歌单风格、添加到歌单、歌曲操作、播放队列与目录设置统一走 [`AdaptiveModalSheet.kt`](../app/src/main/java/com/example/seteasecloudmusic/core/ui/adaptive/AdaptiveModalSheet.kt)（基于 Miuix `WindowBottomSheet` / `WindowDialog`），宽屏限宽居中并自动避让折痕。

## 三、验证方式

构建应用与测试包，运行 JVM 单元测试和折叠屏设备回归（macOS / Linux 使用 `./gradlew`，Windows 使用 `.\gradlew.bat`）：

```bash
# 构建 Debug APK、测试 APK 并运行 JVM 单元测试
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain

# 针对导航、播放页与歌单详情的折叠屏矩阵验证
python3 scripts/verify_foldable_layouts.py --pages chrome,native_music --output artifacts/duo/review
```

完整 9 组窗口配置、后台模拟器启动与 ADB 调试命令见 [后台模拟器调试](后台模拟器调试.md)。验证报告与截图默认输出至 Git 忽略的 `artifacts/` 目录，并记录源码与 APK 的 SHA256 哈希。

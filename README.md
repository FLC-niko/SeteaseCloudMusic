# SeteaseCloudMusic

> Apple Music 视觉语言 × 网易云音乐曲库：为 Android 打造的沉浸式音乐客户端。

单 Activity + Jetpack Compose 全量重构，iOS 26 Liquid Glass 为核心视觉语言：大标题排版、玻璃拟态导航、流体弹簧动效、封面取色沉浸式播放器。登录、播放、歌单、艺人、本地音乐与折叠屏自适应均已跑通主链路。

[![Android minSdk 29](https://img.shields.io/badge/Android-minSdk%2029-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-FF6D00)](https://developer.android.com/jetpack/androidx/releases/media3)
[![Hilt](https://img.shields.io/badge/DI-Hilt-009688)](https://dagger.dev/hilt/)
[![License: 学习用途](https://img.shields.io/badge/License-仅学习交流-lightgrey)](#免责声明)

## 为什么是这个项目

- **视觉先行**：以 iOS 26 Liquid Glass 三层物理结构（投影层 / 半透明底板 / 透镜折射 + 色散 + 高光描边）统一全应用，拒绝生硬实色卡片与一刀切分割线。完整 Tokens 见 [APPLE_MUSIC_DESIGN_SPEC.md](APPLE_MUSIC_DESIGN_SPEC.md)。
- **主链路已闭环**：搜索 → 播放 → 登录 → 首页推荐 → 电台 → 我的资料库 → 艺人详情，全部可点可播，不是 Demo 壳。
- **播放器是重头戏**：Media3 后台播放、秒切预加载、5 档音质热切换、AMLL 风格逐字歌词、待播队列、听歌打卡（PLV/PLD）、断点续播。
- **折叠屏不是顺带**：侧边导航栏、双栏详情、桌面/书本姿态、字号放大回归，均有真机矩阵验证（见 [docs/折叠屏适配.md](docs/折叠屏适配.md)）。
- **工程化认真**：Feature-based Clean Architecture，data / domain / presentation / di 四层自治，Hilt 全量注入。

同类项目参考：桌面端 [Cider](https://github.com/ciderapp/Cider)、歌词管线 [AMLL](https://github.com/amll-dev/applemusic-like-lyrics)、Android 液态玻璃 [Kyant Backdrop / Shapes](https://github.com/Kyant0/AndroidLiquidGlass)。本项目的设计 Tokens 与动效曲线正是从这三者反推而来。

## 功能一览

| 模块 | 状态 | 说明 |
|---|---|---|
| 首页 | 已完成 | 每日推荐海报流墙（无限滚动封面矩阵）、按喜好推荐、雷达 / 心动 / 私人 DJ / 相似歌曲 / 相似艺人大卡，点击即播，支持歌单详情下钻与双栏 |
| 电台 | 已完成 | 排行榜、雷达歌单、歌曲漫游（封面取色卡片）、歌手推荐、推荐歌单、艺人热门金曲、每周新热趋势、心情氛围、关注艺人新动向 9 大横滑板块，卡片可开详情、可直接播放 |
| 我的 | 已完成 | 未登录引导 → 登录后用户中心（头像 / UID / 喜欢 / 创建 / 收藏 / 本地统计）、我喜欢的音乐 Hero 卡、自建 / 收藏 / 本地三 Tab、本地音乐扫描与目录设置、歌单详情 |
| 搜索 | 已完成 | 联想建议（300ms 防抖）+ 自动搜索（500ms 防抖）、分类结果、错误/空态、点击歌曲按当前结果组队列播放 |
| 全屏播放器 | 已完成 | 专辑大封面 ↔ 歌词页连续弹簧过渡、封面呼吸缩放（播放 1.0 / 暂停 0.85）、流体取色背景、胶囊进度条、音质弹窗、待播队列弹窗、更多操作弹窗 |
| 歌词 | 已完成 | 原生逐行歌词 + AMLL 风格逐字辉光（Web 管线见 `package.json`），支持点击跳转、无歌词态、歌词页 4.8s 无操作自动沉浸 |
| 登录 | 基本完成 | 手机号验证码登录、二维码登录（801/802/803/800 全状态轮询）、游客兜底、会话持久化与资料补全、退出登录确认；**邮箱登录接口保留但仓库层返回未实现** |
| 艺人详情 | 已完成 | 沉浸式写真头图、热门歌曲、专辑、相似艺人、分页加载，点击歌曲直接组队列播放 |
| 发现页 | 模块已实现 | 推荐歌单 / 榜单 / 推荐新歌 / 热门歌单四个板块（独立页面，当前主导航聚焦首页 / 电台 / 我的 + 搜索入口） |
| 本地音乐 | 已完成 | MediaStore 全库扫描 + 指定目录递归扫描、在线↔本地匹配（省流/离线用本地路径播放）、权限申请与目录持久化 |
| 导航与 Mini Player | 已完成 | Liquid Glass 底部 Dock（首页 / 电台 / 我的 + 右侧搜索变形展开）、滚动驱动的紧凑/展开、悬浮 Mini Player 上滑共享元素展开全屏 |
| 折叠屏 / 大屏 | 已完成 | 侧边导航栏、详情双栏、弹层限宽、桌面/书本姿态、1.5 倍字号回归，验证矩阵见 [docs](docs/折叠屏适配.md) |

### 播放器深度能力

- 5 档在线音质：标准 / 极高 / 无损 / Hi-Res / 杜比全景声，播放中热切换并保持进度
- 秒切优化：命中内存缓存 0ms 起播 + 后台静默预加载上一首/下一首（随机模式预加载随机候选与历史回溯）
- 直链自愈：CDN 403 过期自动失效旧直链并重新解析续播（单请求最多重试 1 次）
- 听歌打卡：起播上报 PLV 会话，累计有效收听 30s 上报 PLD（含本地歌曲反查网易云 ID）
- 状态恢复：冷启动恢复队列 / 序号 / 进度 / 播放模式（短时效直链剥离落盘，避免恢复 403）
- 播放模式：顺序 / 随机两档，随机模式「上一首」按真实听歌历史逐首回退

## 技术栈

| 领域 | 选型 | 版本 |
|---|---|---|
| 语言 / UI | Kotlin + Jetpack Compose + Material3 | Kotlin 2.3.20 / Compose BOM 2026.02.01 |
| 播放 | AndroidX Media3（ExoPlayer + Session + UI） | 1.5.1 |
| 网络 | Retrofit + Gson + OkHttp（拦截器统一 Cookie） | Retrofit 3.0 |
| 异步 | Coroutines + Flow | 1.9.0 |
| 图片 | Coil Compose（200MB 磁盘缓存）+ Palette 取色 | Coil 2.7.0 |
| 液态玻璃 | Kyant Backdrop + Shapes（Lens 折射 / 色散 / vibrancy） | backdrop 2.0.1 / shapes 1.2.0 |
| 注入 | Hilt + Hilt Navigation Compose | 2.59.2 |
| 导航 / 生命周期 | Navigation Compose + Lifecycle Runtime/ViewModel Compose | Navigation 2.9.0 / Lifecycle 2.10.0 |
| 歌词 Web 管线 | AMLL React（`@applemusic-like-lyrics/react` + Pixi） | 见 `package.json` |
| 构建 | AGP + Gradle Wrapper + JDK | AGP 9.3.2 / Gradle 9.5.0 / Java 11 |

## 架构与目录

Feature-based Clean Architecture：每个 feature 自治 `data / domain / presentation / di`，共享能力下沉 `core/`。

```text
app/src/main/java/com/example/seteasecloudmusic/
├── core/                      # 全局基础能力
│   ├── di/                    # Hilt 仓库绑定
│   ├── model/                 # Track / Album / Artist 等领域模型
│   ├── network/               # Retrofit / OkHttp / AuthInterceptor / NcblReporter
│   ├── player/                # MusicService + MusicPlayerController + TrackPlaybackPreparer
│   ├── local/                 # LocalMusicScanner / LocalMusicRepository
│   ├── cache/                 # DataCacheManager / PlaybackCacheManager
│   ├── settings/              # PlayerSettingsManager（音质 / 歌词开关 / 歌单风格 / 预测返回）
│   ├── auth/                  # AuthSession / LoginMethod
│   └── ui/                    # GlassSurface / LiquidGlassPanel / AppleMusicTopBar / 自适应组件
├── di/
│   └── NetworkServiceModule.kt# 7+ Retrofit Service 统一 provide
├── feature/
│   ├── main/                  # MainActivity + AppNavigation（Dock / MiniPlayer / 账号弹层 / 全屏播放器容器）
│   ├── home/                  # 每日推荐 / 喜好推荐 / 雷达 / 私人 DJ
│   ├── radio/                 # 9 大横滑板块 + 歌单详情下钻
│   ├── mine/                  # 资料库 + 本地音乐 + 歌单详情（含多风格切换）
│   ├── discover/              # 推荐歌单 / 榜单 / 新歌 / 热门歌单（独立页面模块）
│   ├── search/                # 建议 + 结果 + 队列播放
│   ├── auth/                  # 验证码 / 二维码 / 游客 / 会话 / 账号详情
│   ├── artist/                # 艺人详情（歌曲 / 专辑 / 相似艺人）
│   └── player/                # NowPlaying + NativePlayer + 歌词 + 队列 + 音质 + Mesh 背景
└── util/                      # 通用工具（预留）
```

## 环境要求

- Android Studio（最新稳定版）
- JDK 17（Gradle 9.x 运行所需，工具链按 Android Studio 默认配置即可）
- Android SDK：`minSdk = 29`，`targetSdk = 36`，`compileSdk = 37（minorApiLevel 1）`
- Gradle Wrapper 9.5.0（请优先使用仓库自带 Wrapper）

依赖下载源为腾讯云镜像（`settings.gradle.kts`），感谢腾讯云镜像提供稳定的加速服务。

## 快速开始

```bash
git clone <your-fork-or-repo-url>
cd SeteaseCloudMusic
./gradlew assembleDebug
./gradlew installDebug
# 可选：清理后重装
./gradlew clean installDebug
```

### 切换后端地址

默认 Base URL 在 `app/build.gradle.kts` 的 `buildConfigField("BASE_URL", ...)` 中配置，当前指向自建增强 API。如需切换，修改该字段后重新构建即可。Cookie 自动附加 / 回收逻辑在网络拦截器中统一处理（含部分登录接口从响应体 `cookie` 字段兜底回收）。

## 自适应与折叠屏

- 窄屏：悬浮底部 Dock + Mini Player；宽屏（≥600dp）：侧边导航栏
- 首页 / 电台 / 我的 / 歌单详情 / 播放器均支持双栏：详情在右栏打开，窄屏全屏下钻
- 分隔折痕优先于宽度阈值：书本姿态左右分栏，桌面姿态上下分栏
- 队列 / 音质 / 账号 / 目录 / 提示弹层统一走适配弹窗宿主，大屏限宽可滚动
- 验证：`AdaptiveLayoutsTest` 全页面截图 + 设备回归（Pixel Fold 系模拟器九组配置），详见 [docs/折叠屏适配.md](docs/折叠屏适配.md) 与 [docs/后台模拟器调试.md](docs/后台模拟器调试.md)

## 当前状态与路线图

已完成：上述功能一览中标记为「已完成」的全部内容。

已知限制：

- 邮箱登录未真正启用（`AuthRepositoryImpl.loginByEmail` 抛 `NotImplementedError`）
- 仅顺序 / 随机两档播放模式，无单曲循环
- 歌单数据为运行时内存 + 播放状态持久化，无 Room 离线缓存
- 深色模式与 Edge-to-Edge 尚未全面适配

下一步建议：

1. 认证收口：邮箱登录落地、401 自动续期与重登录引导强化
2. 播放体验：单曲循环、睡眠定时、均衡器（如需）
3. 资料库：Room 离线缓存、收藏 / 歌单管理写操作
4. 视觉打磨：深色模式 Design Tokens 全量适配

## 相关文档

- [Apple Music 设计规范与 iOS 26 液态玻璃决策红线](APPLE_MUSIC_DESIGN_SPEC.md)
- [折叠屏页面、布局与验证说明](docs/折叠屏适配.md)
- [折叠屏液态玻璃视觉统一](docs/折叠屏液态玻璃优化.md)
- [iPhone Duo 官方布局方案对齐](docs/iPhone-Duo官方方案对齐.md)
- [后台模拟器启动、ADB 调试与截图](docs/后台模拟器调试.md)
- 开发必看：`app/src/main/java/com/example/seteasecloudmusic/开发必看.md`
- Hilt 约定：`app/src/main/java/com/example/seteasecloudmusic/HILT_GUIDELINES.md`

## 开源致谢

| 生态 | 用途 | 上游仓库 |
|---|---|---|
| AndroidX / JetBrains | Core、Activity、Compose、Navigation、Lifecycle、Media3、Hilt-Navigation | https://android.googlesource.com/platform/frameworks/support |
| Material | Material3 组件 | https://github.com/material-components/material-components-android |
| Retrofit / OkHttp | 网络与 JSON | https://github.com/square/retrofit · https://github.com/square/okhttp |
| Coroutines | 异步与 Flow | https://github.com/Kotlin/kotlinx.coroutines |
| Coil | 图片加载 | https://github.com/coil-kt/coil |
| Hilt | 依赖注入 | https://github.com/google/dagger |
| Kyant Backdrop / Shapes | Liquid Glass 与 Squircle 连续圆角 | https://github.com/Kyant0/AndroidLiquidGlass · https://github.com/Kyant0/Shapes |
| AMLL | Apple Music-like 歌词管线 | https://github.com/amll-dev/applemusic-like-lyrics |
| Cider | Apple Music 桌面端设计参考 | https://github.com/ciderapp/Cider |
| JUnit | 测试 | https://github.com/junit-team/junit4 |

感谢以上开源作者与贡献者的长期维护。

## 免责声明

本项目仅用于学习与技术验证。接口来源于开源增强 API，请遵循相关服务条款与法律法规，不用于任何商业侵权用途。

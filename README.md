# SeteaseCloudMusic

> Apple Music 视觉语言 × 网易云音乐曲库：为 Android 打造的沉浸式音乐客户端。

单 Activity + Jetpack Compose + Miuix 全量构建，以 iOS 26 Liquid Glass 为核心视觉语言：大标题排版、玻璃拟态导航、流体弹簧动效、封面取色沉浸式播放器。内置本地直连网易云引擎、深浅色主题与液态玻璃通透度调节、歌曲后台下载与多目录本地扫描，登录、播放、歌单、艺人、专辑与折叠屏自适应均已跑通主链路。

[![Android minSdk 29](https://img.shields.io/badge/Android-minSdk%2029-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Miuix%200.9.5--rc01-4285F4?logo=jetpackcompose&logoColor=white)](https://github.com/compose-miuix-ui/miuix)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer%201.5.1-FF6D00)](https://developer.android.com/jetpack/androidx/releases/media3)
[![Hilt](https://img.shields.io/badge/DI-Hilt%202.59.2-009688)](https://dagger.dev/hilt/)
[![License: 学习用途](https://img.shields.io/badge/License-仅学习交流-lightgrey)](#免责声明)

## 为什么是这个项目

- **视觉先行**：以 iOS 26 Liquid Glass 光学层次（投影 / 半透明底板 / 透镜折射 + 色散 + 重力 3D 高光描边）结合 Miuix 原生组件与平滑圆角统一全应用，并支持深色模式、顶部氛围调色与玻璃通透度实时调节。完整 Tokens 见 [APPLE_MUSIC_DESIGN_SPEC.md](APPLE_MUSIC_DESIGN_SPEC.md)。
- **主链路已闭环**：搜索 → 播放 → 下载 → 登录 → 首页推荐 → 电台 → 我的资料库 → 艺人/专辑详情，全部可点可播，不是 Demo 壳。
- **内置本地直连引擎**：默认通过本地加解密与 43 路协议适配直连网易云接口（支持离线二维码生成），同时可在设置中一键切换外部代理与主备节点自动故障转移。
- **播放器是重头戏**：Media3 后台播放与前台服务生命周期接管、秒切预加载、4 档在线音质热切换、纯原生 AMLL TTML/YRC 逐字歌词、SuperLyric/蓝牙歌词推送、待播队列、听歌打卡（PLV/PLD）、断点续播。
- **折叠屏不是顺带**：外屏/内屏/横屏侧边导航栏、详情双栏、桌面/书本姿态、1.5 倍字号回归，均有真机与模拟器矩阵验证（见 [docs/iPhone-Duo官方方案对齐.md](docs/iPhone-Duo官方方案对齐.md) 与 [docs/折叠屏适配.md](docs/折叠屏适配.md)）。
- **工程化认真**：Feature-based Clean Architecture（`data / domain / presentation / di` 四层自治）、Hilt 全量注入、Baseline Profile 与分阶段冷启动惰性预热优化。

同类项目参考：桌面端 [Cider](https://github.com/ciderapp/Cider)、歌词规范与词库 [AMLL](https://github.com/amll-dev/applemusic-like-lyrics)、Android 液态玻璃 [Kyant Backdrop / Shapes](https://github.com/Kyant0/AndroidLiquidGlass)、Compose UI 组件库 [Miuix](https://github.com/compose-miuix-ui/miuix)。

## 功能一览

| 模块 | 状态 | 说明 |
|---|---|---|
| 首页 | 已完成 | 每日推荐海报流墙（含视口可见性门控）、按喜好推荐、新歌雷达与推荐卡片直接播放、心动 / 私人 DJ / 相似歌曲 / 相似艺人大卡，支持歌单详情下钻与双栏 |
| 电台 | 已完成 | 排行榜、雷达歌单、歌曲漫游（封面取色卡片）、歌手推荐、推荐歌单、艺人热门金曲、每周新热趋势、心情氛围、关注艺人新动向 9 大横滑板块，卡片可开详情、可直接播放 |
| 我的 / 资料库 | 已完成 | 未登录引导 → 登录后用户中心（头像 / UID / 喜欢 / 创建 / 收藏 / 本地统计）、我喜欢的音乐 Hero 卡、自建 / 收藏 / 本地三 Tab、歌单创建/编辑/增删曲目与多选管理、歌单独立风格与取色记忆、本地多目录扫描与下载任务管理 |
| 搜索 | 已完成 | 热搜与搜索历史、联想建议（300ms 防抖）+ 自动搜索（500ms 防抖）、分类结果、搜索结果匹配歌词提取展示、单曲操作菜单、点击歌曲按当前结果组队列播放 |
| 全屏播放器 | 已完成 | 专辑大封面 ↔ 歌词页连续弹簧过渡、封面呼吸缩放（播放 1.0 / 暂停 0.85）、流体网格取色背景、胶囊进度条、液态玻璃音质弹窗、待播队列弹窗、复用歌曲操作弹窗（展开查看音频编码与详细信息） |
| 歌词 | 已完成 | 纯原生 Compose 逐字/逐行歌词引擎（优先并发竞速 AMLL TTML 逐字词库，自动降级网易云 YRC / LRC + 翻译），支持点击跳转、4.8s 无操作自动沉浸、SuperLyric 状态栏歌词与蓝牙歌词推送 |
| 登录与设置 | 基本完成 | 手机号验证码登录（含冷却倒计时与风控切扫码引导）、二维码登录（离线生成矩阵 + 801/802/803/800 全状态轮询）、游客兜底、会话持久化与资料补全、外观模式（跟随系统/浅色/深色）、顶部氛围调色盘、液态玻璃通透度调节、本地直连开关、目录设置；**邮箱登录接口保留但仓库层返回未实现** |
| 艺人 & 专辑详情 | 已完成 | 沉浸式写真头图、热门歌曲、专辑、相似艺人、预测性返回手势，以及全局专辑详情浮层（支持从歌曲操作菜单一键跳转歌手/专辑） |
| 发现页 | 模块已实现 | 推荐歌单 / 榜单 / 推荐新歌 / 热门歌单四个板块（独立页面，当前主导航聚焦首页 / 电台 / 我的 + 搜索入口） |
| 本地音乐与下载 | 已完成 | MediaStore 全库扫描 + 多自定义目录递归扫描、在线↔本地匹配播放、WorkManager 后台歌曲下载队列、自定义下载目录、`jaudiotagger` 写入音频元数据标签与封面 |
| 导航与 Mini Player | 已完成 | Liquid Glass 底部 Dock（3D 水滴着色器 + 首页 / 电台 / 我的 + 右侧搜索变形展开）、滚动驱动的紧凑/展开、悬浮 Mini Player 上滑共享元素展开全屏 |
| 折叠屏 / 大屏 | 已完成 | 对齐 iPhone Duo 交互方案：侧边导航栏、详情双栏与统一取色底座、弹层限宽、桌面/书本姿态、1.5 倍字号回归，验证矩阵见 [docs/iPhone-Duo官方方案对齐.md](docs/iPhone-Duo官方方案对齐.md) |

### 播放器深度能力

- 4 档在线音质：标准（128 kbps MP3）/ 极高（320 kbps HQ）/ 无损（FLAC SQ）/ Hi-Res（24bit/96kHz），播放中热切换并保持进度，按直链后缀与真实容器/ExoPlayer 轨道精确解析音频编码格式
- 秒切优化：命中内存缓存 0ms 起播 + 后台静默预加载上一首/下一首（随机模式预加载随机候选与历史回溯）
- 直链自愈：CDN 403 过期自动失效旧直链并重新解析续播（单请求最多重试 1 次）
- 听歌打卡：起播上报 PLV 会话，累计有效收听 30s 上报 PLD（含本地歌曲反查网易云 ID）
- 状态恢复：冷启动惰性恢复队列 / 序号 / 进度 / 播放模式（短时效直链剥离落盘，避免恢复 403）
- 播放模式：顺序 / 随机两档，随机模式「上一首」按真实听歌历史逐首回退

## 技术栈

| 领域 | 选型 | 版本 |
|---|---|---|
| 语言 / UI | Kotlin + Jetpack Compose + Miuix（ui / icons / preference / squircle） | Kotlin 2.3.20 / Compose BOM 2026.02.01 / Miuix 0.9.5-rc01 |
| 播放 | AndroidX Media3（ExoPlayer + Session + UI） | 1.5.1 |
| 网络 | 本地直连加解密引擎 + Retrofit + Gson + OkHttp（主备节点切换 / Cookie 拦截） | Retrofit 3.0.0 |
| 异步 | Coroutines + Flow | 1.9.0 |
| 图片 | Coil Compose（200MB 磁盘缓存）+ Palette 取色 | Coil 2.7.0 |
| 液态玻璃 | Kyant Backdrop + Shapes（Lens 折射 / 色散 / vibrancy / 3D 高光） | backdrop 2.0.1 / shapes 1.2.0 |
| 下载与标签 | AndroidX WorkManager + jaudiotagger + ZXing Core（离线二维码） | WorkManager 2.10.1 / jaudiotagger 3.0.1 / ZXing 3.5.3 |
| 注入 | Hilt + Hilt Navigation Compose | Hilt 2.59.2 / Hilt-Nav 1.3.0 |
| 导航 / 生命周期 | Navigation Compose + Lifecycle Runtime/ViewModel Compose | Navigation 2.9.0 / Lifecycle 2.10.0 |
| 启动优化 | Baseline Profile + ProfileInstaller | Benchmark 1.5.0 / ProfileInstaller 1.4.1 |
| 构建 | AGP + Gradle Wrapper + KSP | AGP 9.5.3 / Gradle 9.6.0 / KSP 2.3.6 / Java 11 字节码目标 |

## 架构与目录

Feature-based Clean Architecture：每个 feature 自治 `data / domain / presentation / di`，共享能力下沉 `core/`；另有 `:baselineprofile` 模块用于生成启动与交互基线配置文件。

> 注：应用发布包名（`applicationId`）为 `com.setease.cloudmusic`，Kotlin 源码命名空间（`namespace`）为 `com.example.seteasecloudmusic`。

```text
app/src/main/java/com/example/seteasecloudmusic/
├── core/                        # 全局基础能力
│   ├── SeteaseCloudMusicApp.kt  # @HiltAndroidApp + 全局 Coil ImageLoader + 后台惰性初始化
│   ├── auth/                    # AuthCookieStore / AuthSession / AuthStateProvider / LoginMethod
│   ├── cache/                   # PlaybackCacheManager（播放状态与直链缓存）
│   ├── common/                  # CancellableResult / ErrorMessageResolver / ImageUtils
│   ├── download/                # WorkManager 下载队列 / AudioTagWriter / DownloadPathResolver
│   ├── local/                   # LocalMusicScanner / LocalMusicRepository / LocalTrackMatcher
│   ├── model/                   # Track / Album / Artist 等核心领域模型
│   ├── network/                 # NetworkModule / AuthInterceptor / Failover / NcblReporter
│   │   └── local/               # 本地直连网易云加解密引擎 / 43 路协议适配 / 离线二维码生成
│   ├── player/                  # MusicService / MusicPlayerController / TrackPlaybackPreparer / FavoriteTracksManager
│   ├── settings/                # PlayerSettingsManager（音质/歌词/外观/氛围色/玻璃通透度/直连/目录）
│   ├── ui/                      # AppTheme / MiuixAdapters / GlassSurface / LiquidGlassPanel / AppleMusicTopBar / 自适应组件
│   └── util/                    # BitmapResolver
├── di/
│   └── NetworkServiceModule.kt  # 8 个 Retrofit Service 统一 provide
└── feature/
    ├── main/                    # MainActivity / AppNavigation / AdaptiveNavigation / PlayerEntryPoints
    ├── home/                    # 每日推荐 / 喜好推荐 / 新歌雷达 / 私人 DJ
    ├── radio/                   # 9 大横滑板块 + 歌单详情下钻
    ├── mine/                    # 资料库 / 歌单增删改 / 本地多目录扫描 / 歌单详情（多风格与取色记忆）/ 歌曲操作弹窗
    ├── discover/                # 推荐歌单 / 榜单 / 新歌 / 热门歌单（独立页面模块）
    ├── search/                  # 热搜 / 历史 / 建议 / 分类结果 / 匹配歌词 / 队列播放
    ├── auth/                    # 验证码 / 二维码 / 游客 / 账号详情 / 外观与液态玻璃设置
    ├── artist/                  # 艺人详情与全局专辑详情浮层（含预测性返回）
    └── player/                  # NowPlaying / NativePlayer / 纯原生逐字歌词 / 队列 / 音质弹窗 / Mesh 流体背景
```

## 环境要求

- Android Studio（最新稳定版）
- **JDK 21+**（AGP 9.5.x 与 Miuix 编译运行所需；`compileOptions` 目标为 Java 11）
- Android SDK：`minSdk = 29`，`targetSdk = 36`，`compileSdk = 37（minorApiLevel 2）`
- Gradle Wrapper `9.6.0`（请优先使用仓库自带 Wrapper）

依赖下载源配置了腾讯云镜像与 Maven Central（`settings.gradle.kts`），以提升国内网络下的构建稳定性。

## 快速开始

```bash
git clone <your-fork-or-repo-url>
cd SeteaseCloudMusic
./gradlew assembleDebug
./gradlew installDebug
# 可选：清理后重装
./gradlew clean installDebug
```

如需部署到纯血鸿蒙（HarmonyOS 7.0 + 卓易通容器）真机，请参考 [docs/真机部署-Pura70-卓易通.md](docs/真机部署-Pura70-卓易通.md) 与随仓 `scripts/deploy-zhuoyitong.ps1`。

### 网络引擎与后端地址

- **本地直连模式（默认开启）**：通过 `core/network/local/` 拦截器在本地完成协议加解密并直连网易云上游，不依赖外部代理服务器。
- **外部代理模式**：在应用内「账号/设置」关闭本地直连后，请求指向 [app/build.gradle.kts](app/build.gradle.kts) 中配置的 `BASE_URL`（主节点）与 `BACKUP_BASE_URL`（备用节点），由 `ApiEndpointFailoverInterceptor` 自动处理异常切换。Cookie 自动附加与兜底回收由 `AuthInterceptor` 与 `AuthCookieStore` 统一管理。

## 自适应与折叠屏

- 折叠外屏 / 宽横屏：外缘悬浮玻璃侧边栏 + 侧边迷你播放器；展开竖屏与普通手机：悬浮底部 Dock + Mini Player
- 首页 / 电台 / 我的 / 歌单详情 / 播放器均支持双栏与无缝封面取色底座：宽屏打开歌单时由详情接管内容区并左右分栏，窄屏全屏下钻
- 分隔折痕优先于宽度阈值：书本姿态左右分栏，桌面姿态上下分栏
- 队列 / 音质 / 账号 / 目录 / 提示弹层统一走适配弹窗宿主（基于 Miuix `WindowBottomSheet` / `WindowDialog`），大屏限宽可滚动
- 验证：`AdaptiveLayoutsTest` 全页面截图 + 设备回归（Pixel Fold 系模拟器九组配置），详见 [docs/iPhone-Duo官方方案对齐.md](docs/iPhone-Duo官方方案对齐.md)、[docs/折叠屏适配.md](docs/折叠屏适配.md) 与 [docs/后台模拟器调试.md](docs/后台模拟器调试.md)

## 当前状态与路线图

已完成：上述功能一览中标记为「已完成」的全部内容（含深色模式全面适配、Miuix 组件体系迁移、本地直连引擎、歌曲下载与多目录管理、歌单增删改与收藏、冷启动与渲染性能优化）。

已知限制：

- 邮箱登录未真正启用（`AuthRepositoryImpl.loginByEmail` 返回 `NotImplementedError`）
- 仅顺序 / 随机两档播放模式，暂无单曲循环
- 离线缓存基于磁盘 JSON / SharedPreferences，未引入 Room 数据库
- 登录态 401 自动续期与重登录引导仍可继续强化

下一步建议：

1. 认证收口：补全邮箱登录（如需要）、强化 401 自动续期与过期重登录引导
2. 播放体验：单曲循环、睡眠定时、均衡器（按需）
3. 数据层演进：评估将下载记录与歌单缓存迁移至 Room 以支持更大规模本地曲库

## 相关文档

- [Apple Music 设计规范与 iOS 26 液态玻璃决策红线](APPLE_MUSIC_DESIGN_SPEC.md)
- [iPhone Duo 官方布局方案对齐（最新大屏与折叠屏规则）](docs/iPhone-Duo官方方案对齐.md)
- [折叠屏页面、布局与历史验证说明](docs/折叠屏适配.md)
- [折叠屏液态玻璃视觉统一](docs/折叠屏液态玻璃优化.md)
- [后台模拟器启动、ADB 调试与截图](docs/后台模拟器调试.md)
- [真机部署：Pura 70 Pro（HarmonyOS 7.0 + 卓易通）](docs/真机部署-Pura70-卓易通.md)
- [开发必看](app/src/main/java/com/example/seteasecloudmusic/开发必看.md)
- [Hilt 注入判断指南](app/src/main/java/com/example/seteasecloudmusic/HILT_GUIDELINES.md)

## 开源致谢

| 生态 | 用途 | 上游仓库 |
|---|---|---|
| AndroidX / JetBrains | Core、Activity、Compose、Navigation、Lifecycle、Media3、WorkManager、Benchmark、Hilt-Navigation | https://android.googlesource.com/platform/frameworks/support |
| Miuix | Compose UI 组件库、图标、设置项与 Squircle 圆角 | https://github.com/compose-miuix-ui/miuix |
| Retrofit / OkHttp | 网络与 JSON | https://github.com/square/retrofit · https://github.com/square/okhttp |
| Coroutines | 异步与 Flow | https://github.com/Kotlin/kotlinx.coroutines |
| Coil | 图片加载 | https://github.com/coil-kt/coil |
| Hilt | 依赖注入 | https://github.com/google/dagger |
| Kyant Backdrop / Shapes | Liquid Glass 与连续曲率圆角 | https://github.com/Kyant0/AndroidLiquidGlass · https://github.com/Kyant0/Shapes |
| AMLL | Apple Music-like 逐字歌词规范与 TTML 词库 | https://github.com/amll-dev/applemusic-like-lyrics |
| Cider | Apple Music 桌面端设计参考 | https://github.com/ciderapp/Cider |
| ZXing / jaudiotagger | 离线二维码矩阵生成 / 音频标签写入 | https://github.com/zxing/zxing · https://bitbucket.org/ijabz/jaudiotagger |
| JUnit | 单元测试 | https://github.com/junit-team/junit4 |

感谢以上开源作者与贡献者的长期维护。

## 免责声明

本项目仅用于学习与技术验证。网络协议与接口仅作技术研究用途，请遵循相关服务条款与法律法规，不用于任何商业或侵权用途。

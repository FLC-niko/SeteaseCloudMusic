<div align="center">

# SeteaseCloudMusic

**把 Apple Music 的质感与手感带到 Android，直接听你的网易云曲库。**

纯原生 Kotlin + Jetpack Compose + Miuix 打造 · 内置本地直连引擎，装上就能用。

[![Android](https://img.shields.io/badge/Android-10%2B%20(minSdk%2029)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Miuix](https://img.shields.io/badge/UI-Miuix%200.9.5--rc01-4285F4?style=flat-square)](https://github.com/compose-miuix-ui/miuix)
[![Media3](https://img.shields.io/badge/Media3-1.5.1-FF6D00?style=flat-square)](https://developer.android.com/jetpack/androidx/releases/media3)

</div>

---

## 它有什么不一样

- **零配置开箱即听**：内置本地网易云协议加解密与 43 路接口适配，连扫码登录的二维码也是本地离线生成的，不需要先折腾自建后端；如果你有自己的 API 服务，也可以在设置里一键切回外部代理（支持主备节点自动故障切换）。
- **真正可调的 Liquid Glass 质感**：底栏滑块带 3D 水滴着色器与重力 3D 棱边高光，顶部大标题随滚动无硬边溶解，全屏播放页跑着封面取色的流体网格背景；觉得玻璃太透或太磨砂，设置里拉一下滑块就能实时改。支持浅色、OLED 纯黑深色与自定义顶部氛围色盘。
- **纯原生 Compose 逐字歌词**：去掉了沉重的 WebView，优先并发竞速拉取 AMLL TTML 逐字词库，没命中自动回退网易云 YRC / LRC 与翻译；同时也支持 SuperLyric 状态栏歌词和车载/耳机蓝牙歌词。
- **按主力播放器标准打磨的细节**：
  - 标准 / 极高 / 无损 / Hi-Res 四档在线音质热切换不断进度，按真实容器与 ExoPlayer 轨道识别格式；
  - 后台静默预加载上下曲，命中缓存直接开播，CDN 403 链接过期自动重新解析续播；
  - 支持后台歌曲下载（WorkManager）并用 `jaudiotagger` 自动把标签和封面写进音频文件；支持多自定义本地目录扫描，并自动匹配在线歌曲优先播本地省流量。
- **手机、平板与折叠屏一套到位**：外屏侧边玻璃操作栏、展开双栏与歌单无缝取色底座、书本/桌面半折叠避让铰链，外加 Baseline Profile 和分阶段冷启动预热。全站 UI 已从 Material3 全面迁移到更轻快的 **Miuix** 组件体系。

## 功能速览

| 区域 | 你能用到什么 |
|---|---|
| **首页 & 电台** | 每日推荐海报流墙、按喜好推荐、新歌雷达直播、私人 FM，以及排行榜/歌曲漫游/心情氛围等 9 个电台板块 |
| **我的 & 歌单** | 手机验证码 / 扫码 / 游客登录，歌单创建、改名、增删曲目与多选管理，3 种歌单详情风格且按歌单独立记忆取色背景 |
| **搜索 & 详情** | 热搜、历史、防抖联想、结果内直接显示匹配歌词；从任意歌曲菜单一键直达歌手页或全局专辑浮层 |
| **播放 & 队列** | Media3 后台播放与通知栏接管、断点续播、听歌打卡（PLV/PLD）、液态玻璃音质弹窗、待播队列与详细音频参数展开 |
| **本地 & 下载** | MediaStore 全库扫描 + 多自定义目录递归扫描、在线↔本地匹配播放、自定义下载目录与下载任务管理 |

## 3 步跑起来

需要 **JDK 21+**（AGP 9.5.3 要求）与 Android SDK（`minSdk 29` / `targetSdk 36` / `compileSdk 37 minorApiLevel 2`），直接用仓库自带的 Gradle Wrapper 9.6.0（已配腾讯云镜像加速）：

```bash
git clone <your-repo-url>
cd SeteaseCloudMusic
./gradlew installDebug
```

> 仅编译 APK 运行 `./gradlew assembleDebug`，跑单元测试运行 `./gradlew :app:testDebugUnitTest`。
>
> 用 **HarmonyOS 7.0（卓易通容器）** 真机的朋友可以直接看一键部署脚本说明：[docs/真机部署-Pura70-卓易通.md](docs/真机部署-Pura70-卓易通.md)。

<details>
<summary>关于后端代理地址（可选，本地直连模式无需配置）</summary>

应用默认走内置本地直连引擎。如果在「账号 / 设置」里关闭了本地直连，请求会切到 [app/build.gradle.kts](app/build.gradle.kts) 里的 `BASE_URL` 与 `BACKUP_BASE_URL`，按需改成你自己的服务地址重新构建即可。

</details>

## 技术栈与代码结构

核心栈：Kotlin 2.3.20 · Jetpack Compose (BOM `2026.02.01`) · Miuix `0.9.5-rc01` · Kyant Backdrop / Shapes · AndroidX Media3 `1.5.1` · Hilt `2.59.2` · Retrofit `3.0.0` · Coil `2.7.0` · WorkManager `2.10.1` · jaudiotagger `3.0.1` · ZXing Core `3.5.3` · Baseline Profile。

应用包名为 `com.setease.cloudmusic`（代码命名空间 `com.example.seteasecloudmusic`），按业务拆分为 `feature/*`，共享能力集中在 `core/*`：

```text
app/src/main/java/com/example/seteasecloudmusic/
├── core/
│   ├── auth/         # Cookie 存储与会话状态
│   ├── cache/        # 播放状态与直链缓存
│   ├── download/     # WorkManager 后台下载、路径解析、音频标签写入
│   ├── local/        # 本地音乐多目录扫描与在线匹配
│   ├── network/      # Retrofit/OkHttp、主备切换、AuthInterceptor 与 local/ 本地直连引擎
│   ├── player/       # MusicService、MusicPlayerController、红心歌曲同步
│   ├── settings/     # 音质、歌词、深浅色、氛围色、玻璃通透度、目录等偏好管理
│   └── ui/           # AppTheme、Miuix 适配、液态玻璃（GlassSurface/LiquidGlassPanel）与折叠屏容器
├── di/               # NetworkServiceModule（8 个 Retrofit Service 统一注入）
└── feature/
    ├── main/         # MainActivity、底部/侧边导航、MiniPlayer 与全局浮层
    ├── home/         # 首页与每日推荐详情
    ├── radio/        # 电台 9 大板块
    ├── mine/         # 我的、歌单详情与管理、本地音乐、歌曲操作面板
    ├── discover/     # 发现页（独立页面模块）
    ├── search/       # 搜索、热搜与匹配歌词
    ├── auth/         # 登录流与外观/液态玻璃设置
    ├── artist/       # 艺人详情与全局专辑浮层
    └── player/       # 原生播放页、纯原生逐字歌词、Mesh 流体背景、音质与队列弹窗
```

另有 `:baselineprofile` 模块用于生成冷启动与滚动优化配置。

## 目前还没做完的地方

项目还在持续迭代，当前已知限制：

- 邮箱登录尚未实现（`AuthRepositoryImpl.loginByEmail` 会返回未实现），目前可用手机验证码、二维码或游客登录；
- 播放模式暂时只有「顺序 / 随机」两档，还没加单曲循环；
- 歌单缓存与下载记录目前用本地文件和 SharedPreferences 存储，暂未迁到 Room；
- Cookie 过期（401）后的自动续期流程还可以再打磨。

## 延伸阅读

- [开发必看](app/src/main/java/com/example/seteasecloudmusic/开发必看.md) · [Hilt 注入判断指南](app/src/main/java/com/example/seteasecloudmusic/HILT_GUIDELINES.md)
- [UI 与 iOS 26 液态玻璃设计规范](APPLE_MUSIC_DESIGN_SPEC.md)
- [iPhone Duo 布局对齐](docs/iPhone-Duo官方方案对齐.md) · [折叠屏适配](docs/折叠屏适配.md) · [折叠屏液态玻璃优化](docs/折叠屏液态玻璃优化.md)
- [后台模拟器与 ADB 调试](docs/后台模拟器调试.md) · [Pura 70 Pro 卓易通一键部署](docs/真机部署-Pura70-卓易通.md)

## 致谢

感谢这些优秀的开源项目提供的灵感与底层能力：

[Miuix](https://github.com/compose-miuix-ui/miuix) · [Kyant Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) & [Shapes](https://github.com/Kyant0/Shapes) · [AMLL](https://github.com/amll-dev/applemusic-like-lyrics) · [Cider](https://github.com/ciderapp/Cider) · AndroidX / Media3 / Hilt / Retrofit / OkHttp / Coil / ZXing / jaudiotagger

---

<sub>本项目仅用于学习与技术交流，请遵守相关平台服务条款与法律法规，请勿用于商业或侵权用途。</sub>

# SeteaseCloudMusic

仿 Apple Music 视觉风格的 Android 音乐播放器，基于网易云音乐增强 API 构建。

当前仓库重点已经从“纯搜索 Demo”升级到“可登录、可播放、可看艺人详情”的主链路。

## 项目定位

- 视觉目标：Apple Music 风格的大标题、玻璃质感导航、沉浸式播放体验
- 架构目标：按 Feature 拆分，保持 data/domain/presentation 分层
- 业务目标：优先打通 搜索 -> 播放 -> 账号 -> 推荐 的核心闭环

## 当前进展（截至 2026-04）

| 模块 | 状态 | 说明 |
|---|---|---|
| 主框架与导航 | 已完成 | 单 Activity + Compose，底部 Liquid Glass 导航，支持搜索栏展开/收起动画 |
| 搜索 | 已完成 | 搜索建议、防抖自动搜索、结果列表、错误态处理 |
| 播放核心 | 已完成 | Media3 后台播放、队列播放、进度同步、Mini Player、Now Playing 页面 |
| 认证（Auth） | 部分完成 | 验证码登录、二维码登录、登录态观察、退出登录、会话持久化已落地；邮箱登录暂未实现 |
| 艺人详情 | 已完成 | 艺人信息、热门歌曲、专辑、相似艺人与分页加载 |
| 主页/电台/我的内容页 | 进行中 | 当前仍是占位背景，真实业务内容待接入 |
| 推荐流与资料库 | 计划中 | 每日推荐、用户歌单、收藏体系等尚未正式落地 |

## 已实现能力速览

### 1) 搜索与播放闭环

- 输入联想建议（防抖）
- 自动触发搜索（防抖）
- 点击歌曲后按当前结果生成播放队列
- 自动下一首与基础进度控制

### 2) 登录体系（阶段性）

- 手机号验证码登录
- 二维码登录（含轮询状态）
- 登录态持久化与恢复
- 支持退出登录

说明：邮箱密码登录接口和用例已保留入口，但仓库实现当前返回未实现状态。

### 3) 艺人页

- 艺人头图与基本信息
- 歌曲、专辑、相似艺人分区展示
- 支持“查看更多”增量加载
- 点击歌曲直接进入播放队列

## 技术栈

- Kotlin 2.3.10
- Jetpack Compose + Material3
- AndroidX Media3（ExoPlayer + MediaSessionService）
- Retrofit 3.0 + OkHttp
- Coroutines + Flow
- Coil Compose
- Hilt（应用与主要仓库/用例注入已接入）
- kyant Backdrop + Shapes（Liquid Glass 视觉效果）

## 依赖仓库（下载源）

当前工程在 settings.gradle.kts 中配置了以下依赖下载仓库（均为腾讯云镜像）：

- https://mirrors.cloud.tencent.com/nexus/repository/google/
- https://mirrors.cloud.tencent.com/nexus/repository/maven-public/
- https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/

致谢：感谢腾讯云镜像提供稳定、可用的依赖下载加速服务。

## 开源依赖仓库与致谢

以下为当前项目显式依赖所对应的主要上游仓库（按生态归类）：

| 依赖生态 | 当前使用范围（示例） | 上游仓库 |
|---|---|---|
| AndroidX / Jetpack | Core、AppCompat、Activity、ConstraintLayout、Compose、Navigation、Lifecycle、Media3、Hilt Navigation Compose、AndroidX Test | https://android.googlesource.com/platform/frameworks/support |
| Material Components | com.google.android.material:material | https://github.com/material-components/material-components-android |
| Retrofit | com.squareup.retrofit2:retrofit、converter-gson | https://github.com/square/retrofit |
| OkHttp（Retrofit 依赖链） | 网络底层 HTTP 客户端能力 | https://github.com/square/okhttp |
| Kotlin Coroutines | kotlinx-coroutines-core、kotlinx-coroutines-android | https://github.com/Kotlin/kotlinx.coroutines |
| Coil | io.coil-kt:coil-compose | https://github.com/coil-kt/coil |
| Dagger / Hilt | hilt-android、hilt-android-compiler | https://github.com/google/dagger |
| Liquid Glass Backdrop | io.github.kyant0:backdrop | https://github.com/Kyant0/AndroidLiquidGlass |
| Shapes | io.github.kyant0:shapes | https://github.com/Kyant0/Shapes |
| JUnit 4 | junit:junit | https://github.com/junit-team/junit4 |

致谢：感谢以上开源项目作者与贡献者长期维护与持续迭代，让本项目能够在 UI、架构、网络、播放与工程化层面快速推进。

## 架构与目录

项目采用 Feature-based 分包，核心目录如下：

```text
app/src/main/java/com/example/seteasecloudmusic/
├── core/                      # 全局基础能力
│   ├── di/                    # Hilt 依赖绑定
│   ├── model/                 # Track 等核心领域模型
│   ├── network/               # Retrofit/OkHttp/拦截器
│   └── player/                # MusicService + MusicPlayerController
├── feature/
│   ├── main/                  # 应用入口与全局导航壳
│   ├── search/                # 搜索与搜索建议
│   ├── auth/                  # 登录与账号相关流程
│   ├── artist/                # 艺人详情页
│   └── player/                # 全屏播放页（Now Playing）
└── util/                      # 通用工具（预留）
```

## 环境要求

- Android Studio（建议最新稳定版）
- JDK 17（仓库工具链配置为 17）
- Android SDK：
  - minSdk = 29
  - targetSdk = 36
  - compileSdk = 36
- Gradle Wrapper：9.2.1（请优先使用仓库自带 Wrapper）

## 快速开始

### 1. 克隆与打开

```bash
git clone <your-fork-or-repo-url>
cd SeteaseCloudMusic
```

### 2. 构建 Debug 包

```bash
./gradlew assembleDebug
```

### 3. 安装到设备/模拟器

```bash
./gradlew installDebug
```

### 4. 可选：清理后重装

```bash
./gradlew clean installDebug
```

## API 与鉴权说明

- 默认 API 基地址在 NetworkModule 中配置
- 拦截器会自动：
  - 请求前附加 Cookie
  - 响应后保存服务端返回 Cookie
  - 对部分登录接口，支持从响应体回收 cookie 字段作为兜底

如果你要切换后端地址，请修改网络模块中的 Base URL。

## 当前已知限制

- 主页/电台/我的仍为占位内容
- 邮箱登录尚未实现
- 推荐流、资料库（歌单/收藏/离线）尚未落地
- 全屏播放器视觉仍偏基础版，歌词与动态背景待完善

## 里程碑建议（下一阶段）

1. 完成认证链路收口（邮箱登录、登录失效刷新策略）
2. 接入主页推荐与个性化内容
3. 完善播放器体验（歌词、模式切换、动态背景）
4. 建设资料库能力（我的歌单、收藏、缓存）

## 相关文档

- [折叠屏页面、布局与验证说明](docs/折叠屏适配.md)
- [后台模拟器启动、ADB 调试与截图](docs/后台模拟器调试.md)

- 详细开发说明：app/src/main/java/com/example/seteasecloudmusic/开发必看.md
- Hilt 约定文档：app/src/main/java/com/example/seteasecloudmusic/HILT_GUIDELINES.md

## 免责声明

本项目仅用于学习与技术验证。接口来源于开源增强 API，请遵循相关服务条款与法律法规，不用于任何商业侵权用途。

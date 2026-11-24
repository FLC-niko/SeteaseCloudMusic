# 真机部署：Pura 70 Pro（HarmonyOS 7.0 + 卓易通）

> 目标设备：HBN-AL00（Pura 70 Pro），HarmonyOS 7.0.0.111，纯血鸿蒙（无 Android/AOSP 兼容层）。
> 结论先行：**adb 直连调试在这台设备上不存在**（下有实测证据），可行路径是
> **卓易通安卓容器 + 本仓库的一键部署脚本**，可做到"零手工"部署/启动/验证；
> **断点级调试仍需模拟器**（仓库已配好 AVD：`Medium_Phone_API_37.0`）。

## TL;DR

```powershell
# Windows（Android Studio 自带终端或 PowerShell 5.1 均可）
.\scripts\deploy-zhuoyitong.ps1

# macOS（先 brew install powershell；首次跑前手机上完成一次 USB 调试授权）
pwsh scripts/deploy-zhuoyitong.ps1

# 常用组合
.\scripts\deploy-zhuoyitong.ps1 -SkipBuild        # 不重新构建，直接装现成 APK
.\scripts\deploy-zhuoyitong.ps1 -NoLaunch         # 只装不启动
.\scripts\deploy-zhuoyitong.ps1 -HdcPath <path>   # 手动指定 hdc
```

脚本流程：**构建 APK → hdc USB 直推内部存储 → 自动打开文件管理并导航 → 卓易通接管安装 →
自动点"继续安装/打开" → 控件树验证 app 存活**。UI 自动化全部走 `uitest dumpLayout`
的**文本匹配**（无固定坐标），页面状态与分辨率变化可自适应。

## 一次性准备

1. **手机**（约 1 分钟）：
   `设置 → 关于本机 → 连点"软件版本号" 7 次` → `设置 → 系统和更新 → 开发人员选项 → 打开 USB 调试`；
   数据线连接电脑，选"传输文件"模式；首次连接弹出的"允许 USB 调试"框点**允许**（勾选一律允许）。
2. **电脑**：

   | 依赖 | Windows | macOS |
   |---|---|---|
   | PowerShell | 自带 5.1 即可 | `brew install powershell` |
   | hdc | 仓库自带 `tools\hdc\hdc.exe` | 仓库自带 `tools/hdc/hdc`（arm64；Intel Mac 用 DevEco 自带，`-HdcPath` 指定） |
   | JDK 21+（构建用，`-SkipBuild` 可免） | jdk-23/21 安装目录自动发现 | `brew install --cask temurin@21` 或 Android Studio 自带 jbr |
   | Gradle | 项目 `gradlew.bat` | 项目 `gradlew` |

3. 手机保持**亮屏解锁**（PIN 锁屏无法自动解锁；脚本会先发 `power-shell wakeup` 唤醒）。

## 为什么不能像普通安卓机那样调试

鸿蒙 5.0 起彻底移除 AOSP 兼容层，7.0 连残余兼容代码也删净。实测（hdc 3.2.0f，uid=shell）：

| 隔离层 | 实测证据 |
|---|---|
| 安装层 | APK 无法 `hdc install`（只认 .hap）；但文件管理点击 APK 会自动路由给卓易通安装 |
| PID 命名空间 | 宿主 `ps` 看不到容器内 zygote/adbd/system_server |
| 网络入站 | 容器在 `172.17.1.2`（宿主 `ancowlan0` 网桥 172.17.1.1），ICMP 与 5555/5037 等端口全部超时丢弃 |
| adbd 暴露 | 宿主 netns 无 5555 监听；`hdc fport` 转发后 adb 握手 `failed to connect` |
| broker 通道 | 宿主↔容器无任何 TCP/UDP 流量（走内核级私有通道），shell 侧不可达 |
| 提权 | `uid=2000(shell) CapEff=0`，量产机锁 BL → nsenter 等路径为零 |

卓易通容器本体：`iSulad + LXC`（`anco_hmos.img`，Android 16），共享鸿蒙内核，独立 namespace。
网上"华为手机 adb 识别不到"的 RNDIS/HiSuite 驱动等解法**只适用 HarmonyOS 4.X 双框架时代**，
7.0 上无意义（系统内根本没有 adbd）。

## 已验证的能力边界

- ✅ **部署**：构建产物一键装进卓易通容器（本脚本，实测含覆盖重装）
- ✅ **运行验证**：app 在容器内启动、Compose UI 渲染、经容器出站 NAT 访问
  `BASE_URL(57.158.26.135:3000)` 拉到真实歌单数据（"说唱热门榜第一名/南征北战NZBZ/STAY…"）
- ✅ **UI 级自动化**：`uitest dumpLayout` 可**透视容器内安卓 UI 树**（`android.widget.*`），
  意味着可以继续写脚本自动化 app 内操作（点歌、切页……），控件树转储在 `%TEMP%/setease_last_layout.json`
- ❌ **adb / JDWP 断点 / logcat / 应用私有目录**：容器无调试通道（见上表），
  断点级调试用模拟器；如需真机侧运行日志，可后续在 app 内嵌 debug-only 日志回传桥（出站 NAT 已验证可用）

## 故障排查

| 现象 | 处理 |
|---|---|
| 脚本停在"允许 USB 调试"等待 | 手机上点允许（授权一次即可，之后不再弹） |
| `list targets` 为空 | 换数据线/口；确认手机 USB 调试开关未回退；保持"传输文件"模式 |
| 文件列表里找不到 APK | 媒体库扫描延迟：等 30s 重跑；或手机上手动打开一次文件管理再跑 |
| 安装按钮文案不同 | 把 `$env:TEMP/setease_uitest_dump.json` 里的按钮文字告诉我，加进脚本正则 |
| 构建失败 JDK | 确认 JDK ≥ 21（AGP 9.3.2 要求）；或 `-SkipBuild` |
| mac 上 hdc 无执行权限 | `chmod +x tools/hdc/hdc`（脚本也会自动补） |

## 工具链出处

- hdc 3.2.0f：OpenHarmony 7.0-Release SDK 公开镜像（免登录），随仓分发于 `tools/hdc/`，
  版本/SHA256/获取方法见 `tools/hdc/README.md`。hdc ≥ 3.0.0b 才支持鸿蒙 7 设备授权，勿用旧版。
- 本文档中卓易通架构结论参考社区公开分析（iSulad/LXC/anco_hmos.img，掘金/博客园/知乎，2025-2026），
  隔离性证据为 2026-11 在本机实测。

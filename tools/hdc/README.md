# tools/hdc — 随仓分发的鸿蒙调试工具链

`hdc`（HarmonyOS Device Connector，Apache-2.0，OpenHarmony 官方组件）随仓分发，
`scripts/deploy-zhuoyitong.ps1` 优先从这里取用，免去每台机器单独下载 3GB+ 的 SDK。

| 文件 | 平台 | 版本 | SHA256 |
|---|---|---|---|
| `hdc.exe` + `libusb_shared.dll` | Windows x64 | Ver 3.2.0f | exe: `9D32D2BC6BB61ECF99C9211022D7693C4E3F3B42B5D558ACBB47C9D530A0D3EB` |
| `hdc` + `libusb_shared.dylib` | macOS arm64 (Apple Silicon) | Ver 3.2.0f | hdc: `8DCC254599840B874EE72E415FED27C0C91F52018FA4CAB3A51D913A020E60B4` |

注意：
- Windows 上 `hdc.exe` 依赖同目录的 `libusb_shared.dll`；macOS 上 `hdc` 依赖同目录的
  `libusb_shared.dylib`——**整目录一起用，勿单独拷可执行文件**。
- `NOTICE.txt` 为上游 Apache-2.0 许可声明。
- mac 包在校验时发现镜像站发布的 `L2-SDK-MAC-M1-PUBLIC_20260829.tar.gz.sha256`
  与实际文件不符（Windows 包校验正常）。本仓二进制通过"定向抽取 + zip CRC 展开校验"
  确认数据完好，以本表 SHA256 为准。

## 来源（免登录公开镜像）

OpenHarmony **7.0-Release** SDK（与 Pura 70 Pro 的鸿蒙 7.0.0.111 同代；hdc ≥ 3.0.0b 才支持设备授权）：

- Windows: `https://repo.huaweicloud.com/openharmony/os/7.0-Release/ohos-sdk-windows_linux-public_20260829.tar.gz`
  （内部 `ohos-sdk/windows/toolchains-windows-x64-26.0.0.38-Release.zip` → `toolchains/hdc.exe`）
- macOS:   `https://repo.huaweicloud.com/openharmony/os/7.0-Release/L2-SDK-MAC-M1-PUBLIC_20260829.tar.gz`
  （内部 toolchains 组件包 → `hdc`，arm64）

自行获取/更新时：下载 → 校验同目录 `.sha256` → 只抽 toolchains 组件 → 替换本目录对应文件。
macOS 上如 exec 位丢失：`chmod +x tools/hdc/hdc`（脚本运行时也会自动补）。

## 注意

- Intel Mac：本仓不提供 x86_64 版本；请从 DevEco Studio
  (`/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains/hdc`)
  或 PATH 取用，或用 `-HdcPath` 指定。
- 运行环境（官方建议）：Windows 10/11 x64；macOS 12+。低版本 Windows 缺 WinUSB 时用 Zadig 补库。

#Requires -Version 5.1
<#
.SYNOPSIS
    SeteaseCloudMusic 一键部署到 华为 Pura 70 Pro (HarmonyOS 7.0 + 卓易通容器)。
    支持 Windows 与 macOS（需要 PowerShell 7+，mac 上 brew install powershell）。

.DESCRIPTION
    该设备是纯血鸿蒙（无 Android 运行时），APK 无法直接 adb install；
    本脚本走实测打通的链路：
        构建 APK → hdc USB 直推到内部存储 → 文件管理 UI 自动导航
        → 卓易通接管安装 → 自动启动 → 控件树验证 app 存活。
    全程无需手机侧手工操作（首次 USB 调试授权除外，见 docs/真机部署-Pura70-卓易通.md）。

    UI 自动化基于 uitest dumpLayout 的控件树"文本匹配 + 坐标点击"，
    不依赖固定坐标，页面状态/分辨率变化均可自适应。

.PARAMETER ApkPath
    APK 路径，默认 app\build\outputs\apk\debug\app-debug.apk。

.PARAMETER SkipBuild
    跳过 gradlew 构建，直接使用现成 APK。

.PARAMETER NoLaunch
    安装完成后不启动 app（点"完成"而不是"打开"）。

.PARAMETER HdcPath
    hdc 可执行文件路径。默认依次找 tools\hdc\（仓库自带）→ 本机 SDK 安装目录 → PATH。

.PARAMETER StepTimeoutSec
    每个 UI 等待步骤的超时秒数，默认 40。

.EXAMPLE
    # Windows（Android Studio 自带 pwsh 或 Windows PowerShell 5.1 均可）
    .\scripts\deploy-zhuoyitong.ps1

.EXAMPLE
    # macOS（先 brew install powershell）
    pwsh scripts/deploy-zhuoyitong.ps1 -SkipBuild

.NOTES
    首次运行需在手机上完成一次"允许 USB 调试"授权（脚本会暂停等你点允许）。
    手机全程保持亮屏解锁（PIN 锁屏无法自动解锁）。
#>
[CmdletBinding()]
param(
    [string]$ApkPath,
    [switch]$SkipBuild,
    [switch]$NoLaunch,
    [string]$HdcPath,
    [int]$StepTimeoutSec = 40
)

# ---------- 平台检测（PS 5.1 无 $Is* 自动变量） ----------
if ($PSVersionTable.PSVersion.Major -le 5) { $IsWindows = $true; $IsMacOS = $false; $IsLinux = $false }

$repo = Split-Path -Parent $PSScriptRoot
if (-not $ApkPath) { $ApkPath = Join-Path $repo 'app\build\outputs\apk\debug\app-debug.apk' }
$ApkName = Split-Path -Leaf $ApkPath
$TempDir  = [IO.Path]::GetTempPath()          # Windows: %TEMP%  mac/linux: /tmp
$DumpLocal = Join-Path $TempDir 'setease_uitest_dump.json'

function Write-Step { param($m) Write-Host "`n==> $m" -ForegroundColor Cyan }
function Write-Ok   { param($m) Write-Host "    [OK] $m" -ForegroundColor Green }
function Write-Warn2{ param($m) Write-Host "    [!]  $m" -ForegroundColor Yellow }
function Fail        { param($m) Write-Host "    [FAIL] $m" -ForegroundColor Red; exit 1 }
function Wait-UserTap {
    # 非交互环境（CI/agent）下自动等待重试，避免 Read-Host 报错刷屏
    param($Prompt)
    if ([Environment]::GetCommandLineArgs() -match '-NonInteractive') {
        Write-Host "    $Prompt （非交互环境：5 秒后自动重试）"
        Start-Sleep -Seconds 5
    } else {
        Read-Host "    $Prompt" | Out-Null
    }
}

# ---------- hdc 定位（跨平台） ----------
function Resolve-Hdc {
    $exe = if ($IsWindows) { 'hdc.exe' } else { 'hdc' }
    $cands = @()
    if ($HdcPath) { $cands += $HdcPath }
    $cands += (Join-Path $repo "tools/hdc/$exe")          # 仓库自带（推荐）
    if ($IsWindows) {
        $cands += 'D:\Android\ohos\toolchains\toolchains\hdc.exe'
    } else {
        # DevEco Studio 自带
        $cands += '/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains/hdc'
        # 命令行工具包解压位置
        $cands += "$HOME/HarmonyOS/command-line-tools/sdk/default/openharmony/toolchains/hdc"
        $cands += "$HOME/command-line-tools/sdk/default/openharmony/toolchains/hdc"
    }
    $gc = Get-Command hdc -ErrorAction SilentlyContinue
    if ($gc) { $cands += $gc.Source }
    foreach ($c in $cands) {
        if ($c -and (Test-Path $c)) {
            $p = (Resolve-Path $c).Path
            if (-not $IsWindows) { & chmod +x $p 2>$null }   # mac/linux 保险补可执行位
            return $p
        }
    }
    return $null
}

# ---------- JDK 定位（AGP 9.x 要求 JDK 21+，跨平台） ----------
function Test-Jdk21 {
    param([string]$Home)
    $exe = Join-Path $Home 'bin/java.exe'
    if (-not (Test-Path $exe)) { $exe = Join-Path $Home 'bin/java' }
    if (-not (Test-Path $exe)) { return $false }
    $out = & $exe -version 2>&1 | Out-String
    if ($out -match 'version "(\d+)') { return ([int]$Matches[1] -ge 21) }
    return $false
}
function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
        if (Test-Jdk21 -Home $env:JAVA_HOME) { return $env:JAVA_HOME }
    }
    $cands = @()
    if ($IsWindows) {
        $cands += Get-ChildItem 'C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue |
                  Sort-Object Name -Descending | ForEach-Object { $_.FullName }
        $cands += 'C:\Program Files\Android\Android Studio\jbr'
    } else {
        $cands += Get-ChildItem '/Library/Java/JavaVirtualMachines' -Directory -ErrorAction SilentlyContinue |
                  ForEach-Object { Join-Path $_.FullName 'Contents/Home' }
        $cands += '/Applications/Android Studio.app/Contents/jbr'
    }
    foreach ($c in $cands) {
        if ($c -and (Test-Path $c) -and (Test-Jdk21 -Home $c)) { return $c }
    }
    return $null
}

# ---------- 设备在线/授权检查 ----------
function Get-DeviceState {
    param($Hdc)
    $out = & $Hdc list targets -v 2>$null
    foreach ($line in @($out)) {
        if ($line -match '^(\S+)\s+USB\s+(\w+)') { return @{ Serial = $Matches[1]; State = $Matches[2] } }
    }
    return $null
}

# ---------- 控件树工具 ----------
function Get-Layout {
    param($Hdc)
    & $Hdc shell 'uitest dumpLayout -p /data/local/tmp/setease_dump.json' | Out-Null
    & $Hdc file recv /data/local/tmp/setease_dump.json $DumpLocal | Out-Null
    if (Test-Path $DumpLocal) {
        return ([IO.File]::ReadAllText($DumpLocal, [Text.Encoding]::UTF8) | ConvertFrom-Json)
    }
    return $null
}

function Find-Node {
    # 递归查找：-Pattern 支持正则；-Type 可选（如 Button）
    param($Node, [string]$Pattern, [string]$Type)
    $a = $Node.attributes
    if ($a) {
        $t = [string]$a.text
        $ty = [string]$a.type
        $textHit = ($Pattern -and $t -match $Pattern)
        $typeHit = ($Type -and $ty -match $Type)
        if ($textHit -and (-not $Type -or $typeHit)) { return $a }
    }
    foreach ($c in @($Node.children)) {
        if ($c) { $r = Find-Node -Node $c -Pattern $Pattern -Type $Type; if ($r) { return $r } }
    }
    return $null
}

function Wait-Node {
    param($Hdc, [string]$Pattern, [string]$Type, [int]$TimeoutSec = 40)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $tree = Get-Layout -Hdc $Hdc
        if ($tree) {
            $n = Find-Node -Node $tree -Pattern $Pattern -Type $Type
            if ($n) { return $n }
        }
        Start-Sleep -Seconds 2
    }
    return $null
}

function Get-BoundsCenter {
    param([string]$Bounds)
    if ($Bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        $l=[int]$Matches[1]; $t=[int]$Matches[2]; $r=[int]$Matches[3]; $b=[int]$Matches[4]
        return @{ X = [int](($l+$r)/2); Y = [int](($t+$b)/2) }
    }
    return $null
}

function Click-Node {
    param($Hdc, $Node)
    $c = Get-BoundsCenter -Bounds ([string]$Node.bounds)
    if (-not $c) { return $false }
    & $Hdc shell ("uitest uiInput click {0} {1}" -f $c.X, $c.Y) | Out-Null
    return $true
}

# ---------- 主流程 ----------
Write-Host "SeteaseCloudMusic -> Pura 70 Pro (HarmonyOS 7.0 / 卓易通) 一键部署" -ForegroundColor Magenta

# 1. hdc
Write-Step '1/6 定位 hdc 工具链'
$Hdc = Resolve-Hdc
if (-not $Hdc) { Fail '找不到 hdc：请用 -HdcPath 指定，或把对应平台二进制放到 tools\hdc\ 下（见 docs/真机部署-Pura70-卓易通.md）' }
$ver = (& $Hdc -v) 2>$null
Write-Ok "hdc = $Hdc ($ver)"

# 2. 设备
Write-Step '2/6 检查设备连接'
$dev = $null
for ($i = 0; $i -lt 5; $i++) {
    $dev = Get-DeviceState -Hdc $Hdc
    if ($dev -and $dev.State -eq 'Connected') { break }
    if ($dev -and $dev.State -match 'Unauth') {
        Write-Warn2 '手机屏幕上应弹出"允许 USB 调试"授权框 —— 请点【允许】（建议勾选一律允许）'
        Wait-UserTap '点完后按回车继续'
    } else {
        Write-Warn2 '未检测到 USB 设备：确认数据线已插、手机上 USB 调试已开，然后回车重试'
        Wait-UserTap '按回车重试'
    }
}
if (-not $dev -or $dev.State -ne 'Connected') { Fail "设备未就绪（当前: $(if ($dev) { $dev.State } else { '未检测到' })）" }
Write-Ok "设备 $($dev.Serial) 已连接"
& $Hdc shell 'power-shell wakeup' | Out-Null
Write-Warn2 '提醒：手机需保持亮屏解锁状态（PIN 码锁屏无法自动解锁）'

# 3. 构建
Write-Step '3/6 构建 debug APK'
if ($SkipBuild -and (Test-Path $ApkPath)) {
    Write-Ok "跳过构建，使用现成 APK: $ApkPath"
} else {
    $jh = Resolve-JavaHome
    if (-not $jh) { Fail '找不到 JDK 21+（AGP 9.3.2 要求）。Windows 安装 jdk-23/21；mac: brew install --cask temurin@21 或用 Android Studio 自带 jbr；或加 -SkipBuild' }
    Write-Ok "JAVA_HOME = $jh"
    Push-Location $repo
    try {
        $env:JAVA_HOME = $jh
        if ($IsWindows) {
            & .\gradlew.bat ':app:assembleDebug' '--console=plain' 2>&1 | Select-Object -Last 5 | ForEach-Object { Write-Host "    $_" }
        } else {
            & sh './gradlew' ':app:assembleDebug' '--console=plain' 2>&1 | Select-Object -Last 5 | ForEach-Object { Write-Host "    $_" }
        }
        if ($LASTEXITCODE -ne 0) { Fail 'gradle 构建失败，请向上翻日志排查' }
    } finally { Pop-Location }
    if (-not (Test-Path $ApkPath)) { Fail "构建完成但找不到 $ApkPath" }
    Write-Ok "构建完成 -> $ApkName"
}

# 4. 推送
Write-Step '4/6 USB 直推 APK 到手机内部存储'
$r = (& $Hdc file send $ApkPath '/storage/media/100/local/files/Docs/app-debug.apk') -join ' '
if ($r -notmatch 'FileTransfer finish') { Fail "推送失败：$r" }
Write-Ok '已推送到内部存储（hdcd 有媒体目录写权限，文件管理立即可见）'
Start-Sleep -Seconds 3   # 给媒体库一点扫描时间

# 5. 引导安装（UI 自动化）
Write-Step '5/6 打开文件管理并引导卓易通安装'
& $Hdc shell 'aa start -b com.huawei.hmos.filemanager -a MainAbility' | Out-Null
Start-Sleep -Seconds 3

# 5.1 当前页面若已能看到目标 APK（如上次停留在文件列表）直接点
$node = Wait-Node -Hdc $Hdc -Pattern ([regex]::Escape($ApkName)) -TimeoutSec 10
if (-not $node) {
    # 5.2 导航：浏览 Tab -> 我的手机
    $tab = Wait-Node -Hdc $Hdc -Pattern '^浏览$' -TimeoutSec 15
    if ($tab) { Click-Node -Hdc $Hdc -Node $tab | Out-Null; Start-Sleep -Seconds 2 }
    $row = Wait-Node -Hdc $Hdc -Pattern '我的手机' -TimeoutSec 15
    if (-not $row) { Fail '找不到"我的手机"入口（文件管理页面结构变化？）' }
    Click-Node -Hdc $Hdc -Node $row | Out-Null
    $node = Wait-Node -Hdc $Hdc -Pattern ([regex]::Escape($ApkName)) -TimeoutSec $StepTimeoutSec
}
if (-not $node) { Fail "文件列表中未出现 $ApkName（媒体库扫描延迟：稍等后重跑，或先打开一次文件管理手动刷新）" }
Click-Node -Hdc $Hdc -Node $node | Out-Null
Write-Ok '已点击 APK，等待安装器...'

# 5.3 安全提醒/覆盖提示 -> 继续安装
$btn = Wait-Node -Hdc $Hdc -Pattern '继续安装|仍然安装|覆盖安装|^安装$' -Type 'Button' -TimeoutSec $StepTimeoutSec
if (-not $btn) { Fail '未出现安装确认按钮（截取 /tmp/setease_uitest_dump.json 排查文案）' }
Click-Node -Hdc $Hdc -Node $btn | Out-Null
Write-Ok '已确认安装，等待卓易通完成...'

# 5.4 安装结果
$done = Wait-Node -Hdc $Hdc -Pattern '安装成功' -TimeoutSec 120
if (-not $done) {
    $err = Wait-Node -Hdc $Hdc -Pattern '安装失败|解析失败|已损坏' -TimeoutSec 3
    if ($err) { Fail "安装失败：$([string]$err.text)" }
    Fail '等待安装完成超时'
}
Write-Ok '安装成功'

# 6. 启动验证
if ($NoLaunch) {
    $b = Wait-Node -Hdc $Hdc -Pattern '完成|确定' -Type 'Button' -TimeoutSec 15
    if ($b) { Click-Node -Hdc $Hdc -Node $b | Out-Null }
    Write-Ok '按 -NoLaunch 完成，不启动'
} else {
    Write-Step '6/6 启动并验证 app 存活'
    $open = Wait-Node -Hdc $Hdc -Pattern '^打开$' -Type 'Button' -TimeoutSec 15
    if ($open) { Click-Node -Hdc $Hdc -Node $open | Out-Null } else { Write-Warn2 '未找到"打开"按钮' }
    $marker = Wait-Node -Hdc $Hdc -Pattern '首页|电台|我的|每日推荐|未在播放' -TimeoutSec 40
    if ($marker) {
        Write-Ok "app 已在卓易通容器内运行（识别到界面元素：$([string]$marker.text)）"
        if (Test-Path $DumpLocal) {
            $save = Join-Path $TempDir 'setease_last_layout.json'
            Copy-Item $DumpLocal $save -Force
            Write-Ok "当前界面控件树已存: $save"
        }
    } else {
        Write-Warn2 '未识别到 app 界面元素 —— 请看手机屏幕确认是否启动成功/是否停在加载页'
    }
}

Write-Host "`n==== 部署流程结束 ====" -ForegroundColor Magenta
Write-Host "注意：卓易通容器无 adb/JDWP 通道（详见 docs/真机部署-Pura70-卓易通.md），" -ForegroundColor DarkGray
Write-Host "      断点级调试请用模拟器；本脚本提供部署+启动+UI 级存活验证。" -ForegroundColor DarkGray

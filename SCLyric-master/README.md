# FlamingoLrc

Apple Music 风格的 Jetpack Compose 歌词显示组件，支持逐字高亮、对唱布局、翻译展示等效果。

## 功能特性

- **LRC 歌词解析**：支持标准 LRC 格式及逐字时间戳歌词
- **逐字高亮动画**：当前演唱字词实时高亮，带渐变过渡效果
- **对唱自动布局**：自动识别 `[歌手:]` 标记或行尾冒号，切换左右对齐
- **翻译展示**：支持主歌词与翻译双行显示
- **非当前行模糊**：可开启非高亮行的模糊效果（Android 12+）
- **点击跳转进度**：点击歌词行可回调跳转到对应播放进度，带震动反馈
- **平滑滚动**：歌词切换时自动平滑滚动到可视区域

## 环境要求

| 项目 | 版本 |
|------|------|
| minSdk | 23 |
| compileSdk | 34 |
| Kotlin | 2.0.0 |
| Jetpack Compose | 1.7.0-beta07 |

> [!IMPORTANT]
> 你的项目必须使用 **Jetpack Compose** 才能使用该库。

## 集成方式

### 本地模块导入

1. 将 `flamingolrc` 文件夹复制到你 Android 项目的根目录下。
2. 在项目的 `settings.gradle.kts`（或 `settings.gradle`）中添加：
   ```kotlin
   include(":flamingolrc")
   ```
3. 在 App 模块的 `build.gradle.kts` 中添加依赖：
   ```kotlin
   implementation(project(":flamingolrc"))
   ```



## 使用示例

### 1. 解析歌词

```kotlin
import com.flamingo.lrc.LyricParser
import com.flamingo.lrc.LyricResult

val lrcText = """
[00:00.00]歌曲名
[00:12.34]这是第一句歌词
[00:15.67]这是第二句歌词
[00:18.90]这是[00:19.20]逐[00:19.50]字[00:19.80]歌[00:20.10]词
""".trimIndent()

val parser = LyricParser()
val result: LyricResult = parser.parse(lrcText)
```

### 2. 显示歌词组件

```kotlin
import com.flamingo.lrc.FlamingoLyricView
import com.flamingo.lrc.LyricUIConfig
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PlayerScreen(player: MediaPlayer) {
    FlamingoLyricView(
        lyrics = result.entries,
        sideFlags = result.sideFlags,
        currentTimeMs = { player.currentPosition },
        onSeek = { positionMs ->
            player.seekTo(positionMs)
        },
        translationEnabled = true,
        blurEnabled = true,
        uiConfig = LyricUIConfig(
            mainTextSize = 34,
            subTextSize = 16,
            mainTextBasicColor = 0xFFF2F2F2,
            subTextBasicColor = 0xFF919191,
            fontWeight = FontWeight.ExtraBold,
            lineBalance = false
        )
    )
}
```

## API 说明

### LyricParser

歌词解析器，支持标准 LRC 以及 Apple Music 风格的逐字歌词格式。

```kotlin
class LyricParser(private val formatText: Boolean = true) {
    fun parse(lrcText: String): LyricResult
}
```

### LyricResult

解析结果数据结构。

```kotlin
data class LyricResult(
    val entries: List<List<Pair<Float, String>>>,  // 时间戳(毫秒)与文本
    val sideFlags: List<Boolean>                    // 对唱标记，true 表示靠右对齐
)
```

### FlamingoLyricView

歌词显示组件。

```kotlin
@Composable
fun FlamingoLyricView(
    lyrics: List<List<Pair<Float, String>>>,    // 解析后的歌词数据
    sideFlags: List<Boolean> = emptyList(),     // 对唱标记
    currentTimeMs: () -> Int,                   // 当前播放进度（毫秒）
    onSeek: (Int) -> Unit,                      // 点击歌词行时的跳转回调
    translationEnabled: Boolean = true,         // 是否显示翻译
    blurEnabled: Boolean = false,               // 是否启用非当前行模糊
    isCompact: Boolean = false,                 // 紧凑模式（无歌词时高度比例）
    uiConfig: LyricUIConfig = LyricUIConfig(),  // UI 配置
    modifier: Modifier = Modifier,
    onEmptyAreaClick: () -> Unit = {}           // 点击空白区域回调
)
```

### LyricUIConfig

UI 配置类。

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| edgeFade | Boolean | true | 是否启用边缘渐隐效果 |
| formatText | Boolean | true | 是否启用歌词规整功能 |
| noLrcText | String | "No lyrics" | 无歌词时的提示文本 |
| blankHeight | Int | 70 | 列表首尾填充高度（dp） |
| mainTextSize | Int | 34 | 主歌词字号（sp） |
| subTextSize | Int | 16 | 翻译/副歌词字号（sp） |
| mainTextBasicColor | Long | 0xFFF2F2F2 | 主歌词底色（ARGB） |
| subTextBasicColor | Long | 0xFF919191 | 副歌词底色（ARGB） |
| fontWeight | FontWeight | ExtraBold | 歌词字重 |
| lineBalance | Boolean | false | 是否启用平衡行模式 |

## 对唱标记规则

库会自动识别以下两种对唱切换方式：

1. **行尾冒号**：歌词行以 `:` 或 `：` 结尾时，自动切换左右对齐状态。
2. **歌手标签**：歌词行内包含 `歌手名:` 格式时，自动识别为对唱并切换布局。

示例：
```
[00:10.00]男：
[00:12.00]这是男声部分
[00:20.00]女：
[00:22.00]这是女声部分
```

## License

[MIT](LICENSE)

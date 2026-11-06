package com.example.seteasecloudmusic.feature.player

import androidx.compose.ui.geometry.Rect
import com.example.seteasecloudmusic.core.model.Album
import com.example.seteasecloudmusic.core.model.Artist
import com.example.seteasecloudmusic.core.model.Track
import com.example.seteasecloudmusic.feature.player.data.parser.LrcParser
import com.example.seteasecloudmusic.feature.player.data.parser.YrcParser
import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.LyricWord
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.player.presentation.amllEmpEasing
import com.example.seteasecloudmusic.feature.player.presentation.amllWordFloatEasing
import com.example.seteasecloudmusic.feature.player.presentation.appleMusicArtworkFrame
import com.example.seteasecloudmusic.feature.player.presentation.calculateEmphasizeParams
import com.example.seteasecloudmusic.feature.player.presentation.groupVisualWords
import com.example.seteasecloudmusic.feature.player.presentation.isExplicitTrack
import com.example.seteasecloudmusic.feature.player.presentation.isLineEffectivelyWordTimed
import com.example.seteasecloudmusic.feature.player.presentation.normalizeHighResCoverUrl
import com.example.seteasecloudmusic.feature.player.presentation.playerArtworkRect
import com.example.seteasecloudmusic.feature.player.presentation.playerContainerCornerRadius
import com.example.seteasecloudmusic.feature.player.presentation.playerContainerRect
import com.example.seteasecloudmusic.feature.player.presentation.playerTopAnchoredArtworkRect
import com.example.seteasecloudmusic.feature.player.presentation.shouldEmphasizeWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerMotionAndLyricsTest {

    @Test
    fun yrcParser_parsesWordTimingsBackgroundVocalsAndTranslation() {
        val yrc = """
            [10000,3200](10000,600,0)You (10600,800,0)handle (11400,400,0)it (11800,1400,0)beautifully
            [13300,1500](13300,700,0)(Yeah, (14000,800,0)oh yeah)
        """.trimIndent()
        val tlyric = "[00:10.00]你处理得如此美丽"

        val lines = YrcParser.parse(yrcText = yrc, tlyricText = tlyric)
        assertEquals(2, lines.size)

        val main = lines[0]
        assertFalse(main.isBG)
        assertEquals("你处理得如此美丽", main.translatedLyric)
        assertEquals(4, main.words.size)
        assertEquals("You ", main.words[0].word)
        assertEquals(10000, main.words[0].startTime)
        assertEquals(10600, main.words[0].endTime)
        assertEquals("beautifully", main.words[3].word)

        val bg = lines[1]
        assertTrue(bg.isBG)
        assertEquals("Yeah, oh yeah", bg.words.joinToString("") { it.word })
    }

    @Test
    fun lrcParser_parsesMultiTimestampsAndMatchesNearestTranslation() {
        val lrc = """
            [00:12.34][00:42.50]风才感觉寂寞
            [00:16.80]整个冬天
        """.trimIndent()
        val tlyric = """
            [00:12.40]Only then does the wind feel lonely
            [00:42.55]Only then does the wind feel lonely
        """.trimIndent()

        val lines = LrcParser.parse(lrcText = lrc, tlyricText = tlyric)
        assertEquals(3, lines.size)
        assertEquals(12340, lines[0].startTime)
        assertEquals("风才感觉寂寞", lines[0].words.single().word)
        assertEquals("Only then does the wind feel lonely", lines[0].translatedLyric)
        assertEquals(16800, lines[1].startTime)
        assertEquals(42500, lines[2].startTime)
    }

    @Test
    fun amllEmphasis_matchesLongSustainedWordsLikeWifeAndBoostsLastWord() {
        val wifeWord = LyricWord(word = "wife", startTime = 98000, endTime = 99600)
        val shortWord = LyricWord(word = "or ", startTime = 97500, endTime = 97850)
        val longMultiSyllable = LyricWord(word = "beautifully", startTime = 90000, endTime = 91800)
        val cjkSustained = LyricWord(word = "寂寞", startTime = 35000, endTime = 36500)

        assertTrue(shouldEmphasizeWord(wifeWord))
        assertFalse(shouldEmphasizeWord(shortWord))
        assertFalse(shouldEmphasizeWord(longMultiSyllable))
        assertTrue(shouldEmphasizeWord(cjkSustained))

        val midLineParams = calculateEmphasizeParams(wifeWord, isLastWordOfLine = false)
        val endLineParams = calculateEmphasizeParams(wifeWord, isLastWordOfLine = true)
        assertTrue(endLineParams.amount > midLineParams.amount)
        assertTrue(endLineParams.blur > midLineParams.blur)

        assertEquals(0f, amllEmpEasing(0f), 0.0001f)
        assertEquals(1f, amllEmpEasing(0.5f), 0.0001f)
        assertEquals(0f, amllEmpEasing(1f), 0.0001f)
    }

    @Test
    fun explicitBadgeDetection_distinguishesExplicitFromCleanTracks() {
        val lavenderTrack = Track(
            id = 1L,
            title = "Lavender Haze",
            artists = listOf(Artist(1L, "Taylor Swift")),
            album = Album(10L, "Midnights")
        )
        val lavenderLyrics = ParsedLyrics(
            lines = listOf(
                LyricLine(
                    words = listOf(LyricWord("All this shit is new to me", 0, 3000)),
                    startTime = 0,
                    endTime = 3000
                )
            )
        )
        assertTrue(isExplicitTrack(lavenderTrack, lavenderLyrics))

        val luoxueTrack = Track(
            id = 2L,
            title = "落雪",
            artists = listOf(Artist(2L, "尹昔眠")),
            album = Album(20L, "落雪")
        )
        val luoxueLyrics = ParsedLyrics(
            lines = listOf(
                LyricLine(
                    words = listOf(LyricWord("看漫天落雪覆盖了长街", 0, 4000)),
                    startTime = 0,
                    endTime = 4000
                )
            )
        )
        assertFalse(isExplicitTrack(luoxueTrack, luoxueLyrics))
    }

    @Test
    fun meiloXArtworkMotion_isExactAndBidirectionalAtEndpoints() {
        val miniArtwork = Rect(48f, 2080f, 148f, 2180f)
        val fullAlbumArtwork = Rect(84f, 260f, 996f, 1172f)
        val miniContainer = Rect(36f, 2060f, 1044f, 2200f)
        val fullHost = Rect(0f, 0f, 1080f, 2400f)
        val lyricHeaderArtwork = Rect(84f, 160f, 246f, 322f)

        val atZero = playerArtworkRect(miniArtwork, fullAlbumArtwork, 0f)
        assertEquals(miniArtwork.left, atZero.left, 0.01f)
        assertEquals(miniArtwork.top, atZero.top, 0.01f)
        assertEquals(miniArtwork.right, atZero.right, 0.01f)
        assertEquals(miniArtwork.bottom, atZero.bottom, 0.01f)

        val atOne = playerArtworkRect(miniArtwork, fullAlbumArtwork, 1f)
        assertEquals(fullAlbumArtwork.left, atOne.left, 0.01f)
        assertEquals(fullAlbumArtwork.top, atOne.top, 0.01f)
        assertEquals(fullAlbumArtwork.right, atOne.right, 0.01f)
        assertEquals(fullAlbumArtwork.bottom, atOne.bottom, 0.01f)

        // 验证弧线轨迹：p=0.5 时 X 轴 easeOutCubic (0.875) 比线性中点更靠拢目标中心
        val mid = playerArtworkRect(miniArtwork, fullAlbumArtwork, 0.5f)
        val linearMidX = (miniArtwork.center.x + fullAlbumArtwork.center.x) * 0.5f
        assertTrue(mid.center.x > linearMidX)

        // 验证歌词页顶部小封面锚定轨迹双向精确闭合
        val topAnchoredZero = playerTopAnchoredArtworkRect(
            miniArtwork, lyricHeaderArtwork, miniContainer, fullHost, 0f
        )
        assertEquals(miniArtwork.top, topAnchoredZero.top, 0.01f)
        assertEquals(miniArtwork.left, topAnchoredZero.left, 0.01f)

        val topAnchoredOne = playerTopAnchoredArtworkRect(
            miniArtwork, lyricHeaderArtwork, miniContainer, fullHost, 1f
        )
        assertEquals(lyricHeaderArtwork.top, topAnchoredOne.top, 0.01f)
        assertEquals(lyricHeaderArtwork.left, topAnchoredOne.left, 0.01f)

        val containerFull = playerContainerRect(miniContainer, fullHost, 1f)
        assertEquals(fullHost, containerFull)
        assertEquals(0f, playerContainerCornerRadius(80f, 96f, 1f), 0.001f)

        // 验证播放页内部 大封面 <-> 歌词顶栏小封面 插值
        val pausedAlbumFrame = appleMusicArtworkFrame(
            normalBounds = fullAlbumArtwork,
            headerBounds = lyricHeaderArtwork,
            normalRadius = 42f,
            headerRadius = 24f,
            normalShadow = 72f,
            playbackScale = 0.78f,
            progress = 0f
        )
        assertEquals(fullAlbumArtwork.width * 0.78f, pausedAlbumFrame.bounds.width, 0.05f)

        val settledHeaderFrame = appleMusicArtworkFrame(
            normalBounds = fullAlbumArtwork,
            headerBounds = lyricHeaderArtwork,
            normalRadius = 42f,
            headerRadius = 24f,
            normalShadow = 72f,
            playbackScale = 0.78f,
            progress = 1f
        )
        assertEquals(lyricHeaderArtwork.left, settledHeaderFrame.bounds.left, 0.01f)
        assertEquals(lyricHeaderArtwork.width, settledHeaderFrame.bounds.width, 0.01f)
        assertEquals(0f, settledHeaderFrame.shadowElevation, 0.01f)
    }

    @Test
    fun visualWordGrouping_mergesMultiSyllableWordsLikeAwayWithoutMergingSeparateWords() {
        val rawWords = listOf(
            LyricWord("Baby, ", 1000, 1450),
            LyricWord("you've ", 1450, 1850),
            LyricWord("gone ", 1850, 2250),
            LyricWord("far ", 2250, 2650),
            LyricWord("a", 2650, 2900),
            LyricWord("way", 2900, 4200)
        )

        val groups = groupVisualWords(rawWords)
        assertEquals(5, groups.size)
        assertEquals("far ", groups[3].mergedWord.word)

        val awayGroup = groups[4]
        assertEquals("away", awayGroup.mergedWord.word)
        assertEquals(2, awayGroup.syllables.size)
        assertEquals(2650, awayGroup.mergedWord.startTime)
        assertEquals(4200, awayGroup.mergedWord.endTime)
        assertTrue(shouldEmphasizeWord(awayGroup.mergedWord))

        val awayParams = calculateEmphasizeParams(awayGroup.mergedWord, isLastWordOfLine = true)
        assertTrue(awayParams.blur in 0.15f..0.38f)
    }

    @Test
    fun wholeLineLyrics_neverTriggerWordTimedModeOrGlow() {
        val wholeSentenceTokenLine = LyricLine(
            words = listOf(LyricWord("看着乌鸦密密麻麻的 选择朝我斗嘴", 12000, 16500)),
            startTime = 12000,
            endTime = 16500
        )
        val cjkWholeLineNoSpaces = LyricLine(
            words = listOf(LyricWord("喧嚣把我推向了深渊逼我后退", 16600, 21000)),
            startTime = 16600,
            endTime = 21000
        )
        val realWordTimedLine = LyricLine(
            words = listOf(
                LyricWord("何", 43000, 43300),
                LyricWord("必", 43300, 43600),
                LyricWord("一", 43600, 43900),
                LyricWord("厢", 43900, 44300)
            ),
            startTime = 43000,
            endTime = 44300
        )

        assertFalse(isLineEffectivelyWordTimed(wholeSentenceTokenLine, songHasWordTiming = false))
        assertFalse(isLineEffectivelyWordTimed(wholeSentenceTokenLine, songHasWordTiming = true))
        assertFalse(isLineEffectivelyWordTimed(cjkWholeLineNoSpaces, songHasWordTiming = true))
        assertTrue(isLineEffectivelyWordTimed(realWordTimedLine, songHasWordTiming = true))
        assertFalse(isLineEffectivelyWordTimed(realWordTimedLine, songHasWordTiming = false))
    }

    @Test
    fun smoothFloatAndGlowEasings_haveZeroInitialJumpAndMonotonicFloat() {
        assertEquals(0f, amllWordFloatEasing(0f), 0.0001f)
        assertEquals(1f, amllWordFloatEasing(1f), 0.0001f)
        // 起步阶段斜率趋近于 0，杜绝“直接位移跳变感”
        assertTrue(amllWordFloatEasing(0.05f) < 0.025f)
        var prev = 0f
        for (step in 1..20) {
            val cur = amllWordFloatEasing(step / 20f)
            assertTrue(cur >= prev)
            prev = cur
        }

        // C∞ 辉光包络两端同样缓起缓落
        assertTrue(amllEmpEasing(0.05f) < 0.03f)
        assertTrue(amllEmpEasing(0.95f) < 0.03f)
    }

    @Test
    fun normalizeHighResCoverUrl_stripsThumbnailParamsAndPreservesLocalUris() {
        assertNull(normalizeHighResCoverUrl(null))
        assertNull(normalizeHighResCoverUrl("   "))
        assertEquals(
            "https://p2.music.126.net/abc/cover.jpg",
            normalizeHighResCoverUrl("http://p2.music.126.net/abc/cover.jpg?param=140y140")
        )
        assertEquals(
            "content://media/external/audio/albumart/42",
            normalizeHighResCoverUrl("content://media/external/audio/albumart/42")
        )
    }

    @Test
    fun playbackMode_togglesBetweenSequentialAndShuffle() {
        fun nextMode(mode: com.example.seteasecloudmusic.core.player.PlaybackMode): com.example.seteasecloudmusic.core.player.PlaybackMode {
            return when (mode) {
                com.example.seteasecloudmusic.core.player.PlaybackMode.SEQUENTIAL -> com.example.seteasecloudmusic.core.player.PlaybackMode.SHUFFLE
                com.example.seteasecloudmusic.core.player.PlaybackMode.SHUFFLE -> com.example.seteasecloudmusic.core.player.PlaybackMode.SEQUENTIAL
            }
        }
        assertEquals(com.example.seteasecloudmusic.core.player.PlaybackMode.SHUFFLE, nextMode(com.example.seteasecloudmusic.core.player.PlaybackMode.SEQUENTIAL))
        assertEquals(com.example.seteasecloudmusic.core.player.PlaybackMode.SEQUENTIAL, nextMode(com.example.seteasecloudmusic.core.player.PlaybackMode.SHUFFLE))
    }
}


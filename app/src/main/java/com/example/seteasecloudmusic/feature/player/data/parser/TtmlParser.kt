package com.example.seteasecloudmusic.feature.player.data.parser

import com.example.seteasecloudmusic.feature.player.domain.model.LyricLine
import com.example.seteasecloudmusic.feature.player.domain.model.LyricSource
import com.example.seteasecloudmusic.feature.player.domain.model.LyricWord
import com.example.seteasecloudmusic.feature.player.domain.model.ParsedLyrics
import com.example.seteasecloudmusic.feature.player.domain.model.RubyWord
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 完整兼容 Apple Music TTML 与 AMLL (amll-ttml-db) 规范的解析器：
 * - 支持 <iTunesMetadata> 侧挂翻译/音译（含逐词时间轴音译对齐）
 * - 支持行内 <span ttm:role="x-translation"> 与 <span ttm:role="x-roman">
 * - 支持背景人声和声 <span ttm:role="x-bg">（自动剥离外层括号并紧随主句输出）
 * - 支持 ttm:agent 对唱角色识别（person / group / other 状态机）
 * - 支持 tts:ruby 振假名注音、amll:empty-beat、amll:obscene 以及 span 间空白保留
 */
object TtmlParser {

    private const val NS_TT = "http://www.w3.org/ns/ttml"
    private const val NS_ITUNES = "http://music.apple.com/lyric-ttml-internal"
    private const val NS_TTM = "http://www.w3.org/ns/ttml#metadata"
    private const val NS_TTS = "http://www.w3.org/ns/ttml#styling"
    private const val NS_AMLL = "http://www.example.com/ns/amll"
    private const val NS_XML = "http://www.w3.org/XML/1998/namespace"

    private val TIME_REGEX = Regex("""^(?:(?:(\d+):)?(\d+):)?(\d+(?:\.\d+)?)$""")
    private val BEGIN_PAREN = Regex("""^[（(]+""")
    private val END_PAREN = Regex("""[）)]+$""")

    private data class AgentInfo(val id: String, val type: String)

    private data class ParsedPBlock(
        val sortStart: Int,
        val mainLine: LyricLine?,
        val bgLines: List<LyricLine>
    )

    fun parse(ttmlXml: String): ParsedLyrics {
        val normalizedXml = ttmlXml
            .replace(" </span><span", "</span> <span")
            .replace(",</span><span", ",</span> <span")

        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(normalizedXml))

        val translationMap = mutableMapOf<String, String>()
        val transliterationMap = mutableMapOf<String, String>()
        val transliterationSpansMap = mutableMapOf<String, List<Pair<IntRange, String>>>()
        val agents = LinkedHashMap<String, AgentInfo>()
        val pNodes = mutableListOf<RawPNode>()
        var timingMode = "Line"

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tagName = parser.name
                val ns = parser.namespace
                when {
                    tagName == "tt" -> {
                        val timing = parser.getAttr(NS_ITUNES, "timing") ?: parser.getAttr(null, "itunes:timing")
                        if (!timing.isNullOrBlank()) timingMode = timing
                    }
                    tagName == "agent" || tagName.endsWith(":agent") -> {
                        val id = parser.getAttr(NS_XML, "id") ?: parser.getAttr(null, "xml:id") ?: parser.getAttr(null, "id")
                        val type = parser.getAttr(null, "type") ?: "person"
                        if (!id.isNullOrBlank()) {
                            agents[id] = AgentInfo(id, type)
                        }
                    }
                    tagName == "translation" || tagName.endsWith(":translation") -> {
                        parseSidecarTextMap(parser, translationMap, null)
                    }
                    tagName == "transliteration" || tagName.endsWith(":transliteration") -> {
                        parseSidecarTextMap(parser, transliterationMap, transliterationSpansMap)
                    }
                    tagName == "p" && (ns == NS_TT || ns.isEmpty()) -> {
                        pNodes.add(parseRawPNode(parser))
                    }
                }
            }
            eventType = parser.next()
        }

        // AMLL 对唱状态机：group 始终居左且不翻转状态；首个独唱为 other 时居右，此后每次切换独唱角色翻转左右
        val resolvedDuetByAgent = mutableMapOf<String, Boolean>()
        var firstPersonSeen = false
        var lastPersonIsDuet = false
        var lastPersonId: String? = null

        fun resolveIsDuet(agentId: String?): Boolean {
            if (agentId.isNullOrBlank()) return false
            resolvedDuetByAgent[agentId]?.let { return it }
            val info = agents[agentId]
            if (info?.type == "group" || agentId == "v1000") {
                resolvedDuetByAgent[agentId] = false
                return false
            }
            val isDuet = if (!firstPersonSeen) {
                firstPersonSeen = true
                lastPersonId = agentId
                lastPersonIsDuet = info?.type == "other"
                lastPersonIsDuet
            } else if (agentId == lastPersonId) {
                lastPersonIsDuet
            } else {
                lastPersonId = agentId
                lastPersonIsDuet = !lastPersonIsDuet
                lastPersonIsDuet
            }
            resolvedDuetByAgent[agentId] = isDuet
            return isDuet
        }

        val blocks = pNodes.mapNotNull { raw ->
            val isDuet = resolveIsDuet(raw.agent)
            val key = raw.lineKey

            val sidecarTrans = key?.let { translationMap[it] }.orEmpty()
            val sidecarRoman = key?.let { transliterationMap[it] }.orEmpty()
            val romanSpans = key?.let { transliterationSpansMap[it] }.orEmpty()

            val mainWords = if (romanSpans.isNotEmpty() && raw.mainWords.isNotEmpty()) {
                alignRomanWords(raw.mainWords, romanSpans)
            } else {
                raw.mainWords
            }

            val finalTranslation = raw.inlineTranslation.ifBlank { sidecarTrans }
            val finalRoman = raw.inlineRoman.ifBlank { sidecarRoman }

            val mainLine = if (mainWords.isNotEmpty()) {
                val start = if (raw.begin >= 0) minOf(raw.begin, mainWords.first().startTime) else mainWords.first().startTime
                val end = maxOf(raw.end, mainWords.last().endTime)
                LyricLine(
                    words = mainWords,
                    translatedLyric = finalTranslation,
                    romanLyric = finalRoman,
                    isBG = false,
                    isDuet = isDuet,
                    startTime = start.coerceAtLeast(0),
                    endTime = end.coerceAtLeast(start + 1)
                )
            } else null

            val bgLines = raw.bgEntries.mapIndexedNotNull { idx, bg ->
                if (bg.words.isEmpty()) return@mapIndexedNotNull null
                val cleanedWords = stripOuterParentheses(bg.words)
                if (cleanedWords.isEmpty() || cleanedWords.all { it.word.isBlank() }) return@mapIndexedNotNull null
                val bgTrans = bg.inlineTranslation.ifBlank {
                    key?.let { translationMap["$it-bg"] ?: if (idx == 0) extractBracketTranslation(sidecarTrans) else null }.orEmpty()
                }
                val bgRoman = bg.inlineRoman.ifBlank {
                    key?.let { transliterationMap["$it-bg"] }.orEmpty()
                }
                val start = if (bg.begin >= 0) bg.begin else cleanedWords.first().startTime
                val end = if (bg.end > 0) bg.end else cleanedWords.last().endTime
                LyricLine(
                    words = cleanedWords,
                    translatedLyric = bgTrans,
                    romanLyric = bgRoman,
                    isBG = true,
                    isDuet = isDuet,
                    startTime = start.coerceAtLeast(0),
                    endTime = end.coerceAtLeast(start + 1)
                )
            }

            if (mainLine == null && bgLines.isEmpty()) {
                null
            } else {
                val cleanMain = if (mainLine != null && bgLines.isNotEmpty() && raw.inlineTranslation.isBlank() && sidecarTrans.endsWith("）")) {
                    val stripped = stripTrailingBracketTranslation(sidecarTrans)
                    mainLine.copy(translatedLyric = stripped)
                } else {
                    mainLine
                }
                val sortStart = cleanMain?.startTime ?: bgLines.first().startTime
                ParsedPBlock(sortStart = sortStart, mainLine = cleanMain, bgLines = bgLines)
            }
        }.sortedBy { it.sortStart }

        val finalLines = ArrayList<LyricLine>(blocks.size * 2)
        for (block in blocks) {
            if (block.mainLine != null) {
                finalLines.add(block.mainLine)
                finalLines.addAll(block.bgLines)
            } else {
                // 没有主歌词行时，首个孤立和声行提升为主行以保证正常滚动展示
                block.bgLines.forEachIndexed { index, bg ->
                    finalLines.add(if (index == 0 && finalLines.isEmpty()) bg.copy(isBG = false) else bg)
                }
            }
        }

        val hasWordTiming = timingMode.equals("Word", ignoreCase = true) ||
            finalLines.any { it.words.size > 1 }

        return ParsedLyrics(
            lines = finalLines,
            hasWordTiming = hasWordTiming,
            source = LyricSource.TTML_DB
        )
    }

    private data class BgEntry(
        val begin: Int,
        val end: Int,
        val words: List<LyricWord>,
        val inlineTranslation: String,
        val inlineRoman: String
    )

    private data class RawPNode(
        val lineKey: String?,
        val agent: String?,
        val begin: Int,
        val end: Int,
        val mainWords: List<LyricWord>,
        val inlineTranslation: String,
        val inlineRoman: String,
        val bgEntries: List<BgEntry>
    )

    private fun parseRawPNode(parser: XmlPullParser): RawPNode {
        val lineKey = parser.getAttr(NS_ITUNES, "key") ?: parser.getAttr(null, "itunes:key") ?: parser.getAttr(null, "key")
        val agent = parser.getAttr(NS_TTM, "agent") ?: parser.getAttr(null, "ttm:agent")
        val lineBegin = parseTime(parser.getAttr(null, "begin")).coerceAtLeast(0)
        val lineEnd = parseTime(parser.getAttr(null, "end")).let { if (it > 0) it else lineBegin + 3000 }

        val mainWords = mutableListOf<LyricWord>()
        val bgEntries = mutableListOf<BgEntry>()
        val inlineTransBuilder = StringBuilder()
        val inlineRomanBuilder = StringBuilder()
        val plainTextFallback = StringBuilder()

        val pDepth = parser.depth
        var eventType = parser.next()

        while (!(eventType == XmlPullParser.END_TAG && parser.depth == pDepth && parser.name == "p") &&
            eventType != XmlPullParser.END_DOCUMENT
        ) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "span") {
                        val role = parser.getRoleAttr()
                        when (role) {
                            "x-translation" -> {
                                val text = readSubtreeText(parser).trim()
                                if (text.isNotEmpty()) {
                                    if (inlineTransBuilder.isNotEmpty()) inlineTransBuilder.append(' ')
                                    inlineTransBuilder.append(text)
                                }
                            }
                            "x-roman" -> {
                                val text = readSubtreeText(parser).trim()
                                if (text.isNotEmpty()) {
                                    if (inlineRomanBuilder.isNotEmpty()) inlineRomanBuilder.append(' ')
                                    inlineRomanBuilder.append(text)
                                }
                            }
                            "x-bg" -> {
                                bgEntries.add(parseBgSpan(parser, lineBegin, lineEnd))
                            }
                            else -> {
                                parseWordSpan(parser, lineBegin, lineEnd)?.let { mainWords.add(it) }
                            }
                        }
                    } else if (parser.name == "br") {
                        plainTextFallback.append(' ')
                    }
                }
                XmlPullParser.TEXT -> {
                    val txt = parser.text.orEmpty()
                    if (txt.isNotEmpty()) {
                        if (mainWords.isNotEmpty()) {
                            // 保留相邻词 span 之间的空格（如英文歌词词间空格）
                            if (txt.any { it.isWhitespace() } && !mainWords.last().word.endsWith(' ')) {
                                val last = mainWords.last()
                                mainWords[mainWords.lastIndex] = last.copy(word = last.word + " ")
                            } else if (txt.trim().isNotEmpty()) {
                                val prevEnd = mainWords.last().endTime
                                mainWords.add(LyricWord(word = txt, startTime = prevEnd, endTime = maxOf(prevEnd, lineEnd)))
                            }
                        } else {
                            plainTextFallback.append(txt)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (mainWords.isEmpty()) {
            val plain = plainTextFallback.toString().trim()
            if (plain.isNotEmpty()) {
                mainWords.add(
                    LyricWord(
                        word = plain,
                        startTime = lineBegin,
                        endTime = lineEnd
                    )
                )
            }
        } else {
            val last = mainWords.last()
            if (last.word.length > 1 && last.word.endsWith(' ')) {
                mainWords[mainWords.lastIndex] = last.copy(word = last.word.trimEnd(' '))
            }
        }

        return RawPNode(
            lineKey = lineKey,
            agent = agent,
            begin = lineBegin,
            end = lineEnd,
            mainWords = mainWords,
            inlineTranslation = inlineTransBuilder.toString().trim(),
            inlineRoman = inlineRomanBuilder.toString().trim(),
            bgEntries = bgEntries
        )
    }

    private fun parseBgSpan(parser: XmlPullParser, fallbackBegin: Int, fallbackEnd: Int): BgEntry {
        val bgBegin = parseTime(parser.getAttr(null, "begin")).let { if (it >= 0) it else fallbackBegin }
        val bgEnd = parseTime(parser.getAttr(null, "end")).let { if (it > 0) it else fallbackEnd }
        val words = mutableListOf<LyricWord>()
        val transBuilder = StringBuilder()
        val romanBuilder = StringBuilder()
        val plainText = StringBuilder()

        val bgDepth = parser.depth
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.depth == bgDepth && parser.name == "span") &&
            eventType != XmlPullParser.END_DOCUMENT
        ) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "span") {
                        when (parser.getRoleAttr()) {
                            "x-translation" -> {
                                val t = readSubtreeText(parser).trim()
                                if (t.isNotEmpty()) transBuilder.append(t)
                            }
                            "x-roman" -> {
                                val r = readSubtreeText(parser).trim()
                                if (r.isNotEmpty()) romanBuilder.append(r)
                            }
                            else -> {
                                parseWordSpan(parser, bgBegin, bgEnd)?.let { words.add(it) }
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val txt = parser.text.orEmpty()
                    if (txt.isNotEmpty()) {
                        if (words.isNotEmpty()) {
                            if (txt.any { it.isWhitespace() } && !words.last().word.endsWith(' ')) {
                                val last = words.last()
                                words[words.lastIndex] = last.copy(word = last.word + " ")
                            } else if (txt.trim().isNotEmpty()) {
                                val prevEnd = words.last().endTime
                                words.add(LyricWord(word = txt, startTime = prevEnd, endTime = maxOf(prevEnd, bgEnd)))
                            }
                        } else {
                            plainText.append(txt)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (words.isEmpty()) {
            val fallback = plainText.toString().trim()
            if (fallback.isNotEmpty()) {
                words.add(LyricWord(word = fallback, startTime = bgBegin, endTime = bgEnd))
            }
        } else {
            val last = words.last()
            if (last.word.length > 1 && last.word.endsWith(' ')) {
                words[words.lastIndex] = last.copy(word = last.word.trimEnd(' '))
            }
        }

        return BgEntry(
            begin = bgBegin,
            end = bgEnd,
            words = words,
            inlineTranslation = transBuilder.toString().trim(),
            inlineRoman = romanBuilder.toString().trim()
        )
    }

    private fun parseWordSpan(parser: XmlPullParser, fallbackBegin: Int, fallbackEnd: Int): LyricWord? {
        val spanBegin = parseTime(parser.getAttr(null, "begin")).let { if (it >= 0) it else fallbackBegin }
        val spanEnd = parseTime(parser.getAttr(null, "end")).let { if (it > 0) it else fallbackEnd }
        val obscene = (parser.getAttr(NS_AMLL, "obscene") ?: parser.getAttr(null, "amll:obscene")) == "true"
        val emptyBeat = (parser.getAttr(NS_AMLL, "empty-beat") ?: parser.getAttr(null, "amll:empty-beat"))?.toIntOrNull()
        val rubyType = parser.getAttr(NS_TTS, "ruby") ?: parser.getAttr(null, "tts:ruby")

        val baseText = StringBuilder()
        val rubyList = mutableListOf<RubyWord>()

        val spanDepth = parser.depth
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.depth == spanDepth && parser.name == "span") &&
            eventType != XmlPullParser.END_DOCUMENT
        ) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "span") {
                        val innerRuby = parser.getAttr(NS_TTS, "ruby") ?: parser.getAttr(null, "tts:ruby")
                        when (innerRuby) {
                            "base" -> baseText.append(readSubtreeText(parser))
                            "textContainer" -> {
                                // 继续进入内部读取 ruby="text"
                            }
                            "text" -> {
                                val rBegin = parseTime(parser.getAttr(null, "begin")).let { if (it >= 0) it else spanBegin }
                                val rEnd = parseTime(parser.getAttr(null, "end")).let { if (it > 0) it else spanEnd }
                                val rText = readSubtreeText(parser)
                                if (rText.isNotEmpty()) {
                                    rubyList.add(RubyWord(word = rText, startTime = rBegin, endTime = rEnd))
                                }
                            }
                            else -> {
                                baseText.append(readSubtreeText(parser))
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (rubyType != "textContainer") {
                        baseText.append(parser.text.orEmpty())
                    }
                }
            }
            eventType = parser.next()
        }

        val text = baseText.toString()
        if (text.isEmpty()) return null
        return LyricWord(
            word = text,
            startTime = spanBegin,
            endTime = maxOf(spanBegin, spanEnd),
            obscene = obscene,
            emptyBeat = emptyBeat,
            ruby = rubyList.takeIf { it.isNotEmpty() }
        )
    }

    private fun parseSidecarTextMap(
        parser: XmlPullParser,
        lineTextMap: MutableMap<String, String>,
        timedSpansMap: MutableMap<String, List<Pair<IntRange, String>>>?
    ) {
        val rootDepth = parser.depth
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.depth == rootDepth) &&
            eventType != XmlPullParser.END_DOCUMENT
        ) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "text") {
                val forKey = parser.getAttr(NS_XML, "for") ?: parser.getAttr(null, "for")
                val textDepth = parser.depth
                val mainBuffer = StringBuilder()
                val bgBuffer = StringBuilder()
                val timedSpans = mutableListOf<Pair<IntRange, String>>()

                var innerEvent = parser.next()
                while (!(innerEvent == XmlPullParser.END_TAG && parser.depth == textDepth && parser.name == "text") &&
                    innerEvent != XmlPullParser.END_DOCUMENT
                ) {
                    when (innerEvent) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name == "span") {
                                val role = parser.getRoleAttr()
                                if (role == "x-bg") {
                                    bgBuffer.append(readSubtreeText(parser))
                                } else {
                                    val sBegin = parseTime(parser.getAttr(null, "begin"))
                                    val sEnd = parseTime(parser.getAttr(null, "end"))
                                    val sTxt = readSubtreeText(parser)
                                    mainBuffer.append(sTxt)
                                    if (sBegin >= 0 && sEnd > 0 && sTxt.isNotBlank()) {
                                        timedSpans.add((sBegin..sEnd) to sTxt.trim())
                                    }
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            mainBuffer.append(parser.text.orEmpty())
                        }
                    }
                    innerEvent = parser.next()
                }

                if (!forKey.isNullOrBlank()) {
                    val mainStr = mainBuffer.toString().trim()
                    if (mainStr.isNotEmpty()) lineTextMap[forKey] = mainStr
                    val bgStr = stripOuterParensString(bgBuffer.toString().trim())
                    if (bgStr.isNotEmpty()) lineTextMap["$forKey-bg"] = bgStr
                    if (timedSpansMap != null && timedSpans.isNotEmpty()) {
                        timedSpansMap[forKey] = timedSpans
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun alignRomanWords(
        words: List<LyricWord>,
        romanSpans: List<Pair<IntRange, String>>
    ): List<LyricWord> {
        if (words.size == romanSpans.size) {
            return words.mapIndexed { idx, w -> w.copy(romanWord = romanSpans[idx].second) }
        }
        return words.map { w ->
            val match = romanSpans.firstOrNull { (range, _) ->
                abs(range.first - w.startTime) <= 20
            }?.second
            if (match != null) w.copy(romanWord = match) else w
        }
    }

    private fun stripOuterParentheses(words: List<LyricWord>): List<LyricWord> {
        if (words.isEmpty()) return words
        val result = words.toMutableList()
        val firstTrimmed = result.first().word.trimStart()
        val lastTrimmed = result.last().word.trimEnd()
        if (BEGIN_PAREN.containsMatchIn(firstTrimmed) && END_PAREN.containsMatchIn(lastTrimmed)) {
            result[0] = result[0].copy(word = result[0].word.replaceFirst(BEGIN_PAREN, ""))
            val lastIdx = result.lastIndex
            result[lastIdx] = result[lastIdx].copy(word = result[lastIdx].word.replace(END_PAREN, ""))
        }
        return result.filter { it.word.isNotEmpty() }
    }

    private fun stripOuterParensString(text: String): String {
        return text.replaceFirst(BEGIN_PAREN, "").replace(END_PAREN, "").trim()
    }

    private fun extractBracketTranslation(text: String): String? {
        if (!text.endsWith('）') && !text.endsWith(')')) return null
        val openIdx = maxOf(text.lastIndexOf('（'), text.lastIndexOf('('))
        if (openIdx <= 0) return null
        return text.substring(openIdx + 1, text.length - 1).trim().takeIf { it.isNotEmpty() }
    }

    private fun stripTrailingBracketTranslation(text: String): String {
        if (!text.endsWith('）') && !text.endsWith(')')) return text
        val openIdx = maxOf(text.lastIndexOf('（'), text.lastIndexOf('('))
        if (openIdx <= 0) return text
        return text.substring(0, openIdx).trim()
    }

    private fun readSubtreeText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        val startDepth = parser.depth
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.depth == startDepth) &&
            event != XmlPullParser.END_DOCUMENT
        ) {
            if (event == XmlPullParser.TEXT) {
                sb.append(parser.text.orEmpty())
            }
            event = parser.next()
        }
        return sb.toString()
    }

    private fun XmlPullParser.getAttr(ns: String?, name: String): String? {
        if (ns != null) {
            getAttributeValue(ns, name)?.let { return it }
        }
        for (i in 0 until attributeCount) {
            val attrName = getAttributeName(i)
            if (attrName == name || attrName.endsWith(":$name")) {
                return getAttributeValue(i)
            }
        }
        return null
    }

    private fun XmlPullParser.getRoleAttr(): String? = getAttr(NS_TTM, "role")

    private fun parseTime(timeStr: String?): Int {
        if (timeStr.isNullOrBlank()) return -1
        val clean = timeStr.trim()
        if (clean.endsWith("ms")) {
            return clean.removeSuffix("ms").toDoubleOrNull()?.roundToInt() ?: -1
        }
        if (clean.endsWith("s")) {
            return ((clean.dropLast(1).toDoubleOrNull() ?: return -1) * 1000.0).roundToInt()
        }
        val match = TIME_REGEX.matchEntire(clean) ?: return -1
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val minutes = match.groupValues[2].toIntOrNull() ?: 0
        val seconds = match.groupValues[3].toDoubleOrNull() ?: return -1
        return ((hours * 3600 + minutes * 60 + seconds) * 1000.0).roundToInt()
    }
}

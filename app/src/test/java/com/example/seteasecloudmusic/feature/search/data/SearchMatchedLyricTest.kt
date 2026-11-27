package com.example.seteasecloudmusic.feature.search.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchMatchedLyricTest {

    private val gson = Gson()

    @Test
    fun gsonDeserialization_withLyrics_extractsCleanedMatchedLyric() {
        val json = """
            {
              "code": 200,
              "result": {
                "songs": [
                  {
                    "id": 3400749588,
                    "name": "七里香",
                    "lyrics": [
                      "<b>窗外的麻雀</b>在电线杆上多嘴",
                      "你说这一句很有夏天的感觉"
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, SearchResultResponse::class.java)
        val song = response.result?.songs?.single()

        assertEquals(
            listOf("<b>窗外的麻雀</b>在电线杆上多嘴", "你说这一句很有夏天的感觉"),
            song?.lyrics
        )
        assertEquals(
            "窗外的麻雀在电线杆上多嘴",
            SearchRepositoryImpl.extractMatchedLyric(song?.lyrics)
        )
    }

    @Test
    fun gsonDeserialization_withoutLyrics_returnsNull() {
        val json = """
            {
              "code": 200,
              "result": {
                "songs": [
                  {
                    "id": 3460454823,
                    "name": "十万飞花"
                  }
                ]
              }
            }
        """.trimIndent()

        val response = gson.fromJson(json, SearchResultResponse::class.java)
        val song = response.result?.songs?.single()

        assertNull(song?.lyrics)
        assertNull(SearchRepositoryImpl.extractMatchedLyric(song?.lyrics))
    }

    @Test
    fun extractMatchedLyric_prefersHighlightedLineOverEarlierUnhighlightedLines() {
        val lyrics = listOf(
            "作词 : 方文山",
            "<em>窗外的麻雀</em>在电线杆上多嘴",
            "<b>另一行高亮</b>"
        )

        assertEquals(
            "窗外的麻雀在电线杆上多嘴",
            SearchRepositoryImpl.extractMatchedLyric(lyrics)
        )
    }

    @Test
    fun extractMatchedLyric_fallsBackToFirstNonBlankLineWhenNoHighlightExists() {
        val lyrics = listOf(
            null,
            "   ",
            "你就是星辰大海",
            "我眼中炽热的恒星"
        )

        assertEquals(
            "你就是星辰大海",
            SearchRepositoryImpl.extractMatchedLyric(lyrics)
        )
    }

    @Test
    fun extractMatchedLyric_cleansHtmlEntitiesTimestampsAndWhitespace() {
        val lyrics = listOf(
            "[00:12.34] <b>It&#39;s</b> &quot;magic&quot; &amp; &apos;love&apos;&nbsp;&nbsp;for   you "
        )

        assertEquals(
            "It's \"magic\" & 'love' for you",
            SearchRepositoryImpl.extractMatchedLyric(lyrics)
        )
        assertNull(SearchRepositoryImpl.extractMatchedLyric(emptyList()))
        assertNull(SearchRepositoryImpl.extractMatchedLyric(listOf("   ", "<b></b>", "[01:02.345]")))
    }
}

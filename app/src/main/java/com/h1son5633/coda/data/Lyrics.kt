package com.h1son5633.coda.data

import com.h1son5633.coda.net.LyricRaw
import org.json.JSONObject

data class LyricWord(val startMs: Long, val durationMs: Long, val text: String)

data class LyricLine(
    val startMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    var translation: String = ""
)

/**
 * Parses NetEase lyric payloads into timed lines.
 *
 * The API returns three shapes:
 *  - plain LRC (`[mm:ss.xx]text`) in lrc / tlyric,
 *  - word-timed YRC (`[startMs,durMs](absMs,durMs,flag)word...`) in yrc,
 *  - JSON credit lines (`{"t":0,"c":[{"tx":"作词: "},...]}`) which can head
 *    any of the fields.
 * Like the original Coda, credit lines (作词/作曲/出品…) are dropped so the
 * lyric view shows only the song itself.
 */
object Lyrics {

    private val CREDIT_PREFIX = Regex(
        "^(作词|作曲|编曲|制作人?|出品|出品人|版权|OP|SP|录音|混音|母带|吉他|贝斯|鼓|键盘|和声|弦乐|监制|统筹|发行|策划|文案|词|曲)\\s*[:：]"
    )
    private val LRC_TAG = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val YRC_HEAD = Regex("^\\[(\\d+),(\\d+)]")
    private val YRC_WORD = Regex("\\((\\d+),(\\d+),\\d+\\)([^(]*)")

    fun parse(raw: LyricRaw): List<LyricLine> {
        val lines = when {
            raw.yrc.isNotBlank() -> parseMixed(raw.yrc, wordTimed = true)
            raw.lrc.isNotBlank() -> parseMixed(raw.lrc, wordTimed = false)
            else -> emptyList()
        }
        if (lines.isEmpty() || raw.tlyric.isBlank()) return lines
        val translations = parseMixed(raw.tlyric, wordTimed = false)
            .associate { it.startMs to it.text }
        for (line in lines) {
            val exact = translations[line.startMs]
            val nearest = exact ?: translations.keys
                .filter { kotlin.math.abs(it - line.startMs) <= 600 }
                .minByOrNull { kotlin.math.abs(it - line.startMs) }
                ?.let { translations[it] }
            if (!nearest.isNullOrBlank() && nearest != line.text) line.translation = nearest
        }
        return lines
    }

    private fun parseMixed(content: String, wordTimed: Boolean): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            when {
                line.startsWith("{") -> parseJsonLine(line)?.let { out.add(it) }
                wordTimed && line.startsWith("[") -> parseYrcLine(line)?.let { out.add(it) }
                line.startsWith("[") -> out.addAll(parseLrcLine(line))
            }
        }
        return out.filter { it.text.isNotBlank() && !CREDIT_PREFIX.containsMatchIn(it.text) }
            .sortedBy { it.startMs }
    }

    private fun parseJsonLine(line: String): LyricLine? = try {
        val obj = JSONObject(line)
        val start = obj.optLong("t", -1)
        if (start < 0) return null
        val segments = obj.optJSONArray("c") ?: return null
        val text = (0 until segments.length()).joinToString("") {
            segments.getJSONObject(it).optString("tx")
        }
        LyricLine(start, text.trim())
    } catch (e: Exception) {
        null
    }

    private fun parseYrcLine(line: String): LyricLine? {
        val head = YRC_HEAD.find(line) ?: return null
        val start = head.groupValues[1].toLong()
        val words = YRC_WORD.findAll(line).map { m ->
            LyricWord(
                startMs = m.groupValues[1].toLong(),
                durationMs = m.groupValues[2].toLong(),
                text = m.groupValues[3]
            )
        }.toList()
        if (words.isEmpty()) return null
        return LyricLine(start, words.joinToString("") { it.text }.trim(), words)
    }

    private fun parseLrcLine(line: String): List<LyricLine> {
        val tags = LRC_TAG.findAll(line).toList()
        if (tags.isEmpty()) return emptyList()
        val text = line.substring(tags.last().range.last + 1).trim()
        if (text.isEmpty()) return emptyList()
        return tags.map { tag ->
            val minutes = tag.groupValues[1].toLong()
            val seconds = tag.groupValues[2].toLong()
            val fraction = tag.groupValues[3]
            val millis = when (fraction.length) {
                0 -> 0
                1 -> fraction.toLong() * 100
                2 -> fraction.toLong() * 10
                else -> fraction.take(3).toLong()
            }
            LyricLine((minutes * 60 + seconds) * 1000 + millis, text)
        }
    }
}

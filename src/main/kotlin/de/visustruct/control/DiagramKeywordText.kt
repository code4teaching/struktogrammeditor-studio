package de.visustruct.control

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.geom.Rectangle2D
import java.util.Locale
import java.util.Optional
import kotlin.math.ceil

object DiagramKeywordText {

    class KeywordParts(val keyword: String, val rest: String)

    @JvmStatic
    fun normalizeDoUntilDisplayLine(line: String?): String {
        if (line == null) {
            return ""
        }
        val t = line.trim()
        val lower = t.lowercase(Locale.ROOT)
        if (lower == "until") {
            return ""
        }
        if (lower.startsWith("until ")) {
            return t.substring(5).trim()
        }
        if (lower.startsWith("until(")) {
            return t.substring(5)
        }
        return line
    }

    @JvmStatic
    fun ensureLeadingKeyword(keyword: String, line: String?): String {
        if (line == null) {
            return "$keyword "
        }
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            return "$keyword "
        }
        val lower = trimmed.lowercase(Locale.ROOT)
        val k = keyword.lowercase(Locale.ROOT)
        if (lower == k || lower.startsWith("$k ")) {
            return line
        }
        return "$keyword $trimmed"
    }

    @JvmStatic
    fun lineForDisplay(struktogrammTyp: Int, lineIndex: Int, raw: String?): String {
        if (raw == null) {
            return ""
        }
        var line = raw
        if (struktogrammTyp == Struktogramm.typDoUntilSchleife) {
            line = normalizeDoUntilDisplayLine(line)
        }
        if (lineIndex != 0) {
            return line
        }
        return when (struktogrammTyp) {
            Struktogramm.typVerzweigung -> ensureLeadingKeyword("if", line)
            Struktogramm.typWhileSchleife -> ensureLeadingKeyword("while", line)
            Struktogramm.typForSchleife -> ensureLeadingKeyword("for", line)
            Struktogramm.typFallauswahl -> ensureLeadingKeyword("switch", line)
            Struktogramm.typDoUntilSchleife -> ensureLeadingKeyword("while", line)
            else -> line
        }
    }

    @JvmStatic
    fun splitKeywordLine(line: String?): Optional<KeywordParts> {
        if (line.isNullOrEmpty()) {
            return Optional.empty()
        }
        val trimmed = line.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        for (prefix in arrayOf("input:", "output:", "print:")) {
            if (lower == prefix) {
                return Optional.of(KeywordParts(trimmed.substring(0, prefix.length), ""))
            }
            if (lower.startsWith("$prefix ")) {
                val kw = trimmed.substring(0, prefix.length)
                val rest = trimmed.substring(prefix.length).trim()
                return Optional.of(KeywordParts(kw, rest))
            }
        }
        val sp = indexOfFirstSpace(trimmed)
        if (sp < 0) {
            if (isBareKeyword(trimmed)) {
                return Optional.of(KeywordParts(trimmed, ""))
            }
            return Optional.empty()
        }
        val head = trimmed.substring(0, sp)
        if (!isBareKeyword(head)) {
            return Optional.empty()
        }
        val rest = trimmed.substring(sp + 1).trim()
        return Optional.of(KeywordParts(head, rest))
    }

    private fun indexOfFirstSpace(s: String): Int {
        for (i in s.indices) {
            if (s[i].isWhitespace()) {
                return i
            }
        }
        return -1
    }

    private fun isBareKeyword(w: String): Boolean {
        val lower = w.lowercase(Locale.ROOT)
        return lower == "if" || lower == "for" || lower == "while" ||
            lower == "until" || lower == "switch" || lower == "do" ||
            lower == "true" || lower == "false" || lower == "default"
    }

    @JvmStatic
    fun measureLineWidth(g: Graphics2D?, line: String): Int {
        val gfx = g ?: return maxOf(line.length * 7, 4 * line.length)
        val parts = splitKeywordLine(line)
        if (parts.isEmpty) {
            return ceil(stringWidth(gfx, gfx.font, line)).toInt()
        }
        val p = parts.get()
        val plain = stripBold(gfx.font)
        val bold = plain.deriveFont(Font.BOLD)
        val wKw = stringWidth(gfx, bold, p.keyword)
        val wRest = if (p.rest.isEmpty()) 0.0 else stringWidth(gfx, plain, " ${p.rest}")
        return ceil(wKw + wRest).toInt()
    }

    @JvmStatic
    fun drawKeywordAwareLine(g: Graphics2D?, color: Color, x: Int, y: Int, line: String) {
        val gfx = g ?: return
        gfx.color = color
        val parts = splitKeywordLine(line)
        if (parts.isEmpty) {
            gfx.drawString(line, x, y)
            return
        }
        val p = parts.get()
        val plain = stripBold(gfx.font)
        val bold = plain.deriveFont(Font.BOLD)
        gfx.font = bold
        gfx.drawString(p.keyword, x, y)
        val x2 = x + ceil(stringWidth(gfx, bold, p.keyword)).toInt()
        if (p.rest.isNotEmpty()) {
            gfx.font = plain
            gfx.drawString(" ${p.rest}", x2, y)
        } else {
            gfx.font = plain
        }
    }

    private fun stripBold(f: Font): Font = f.deriveFont(f.style and Font.BOLD.inv())

    private fun stringWidth(g: Graphics2D, font: Font, s: String): Double {
        val frc: FontRenderContext = g.fontRenderContext
        return font.getStringBounds(s, frc).width
    }
}

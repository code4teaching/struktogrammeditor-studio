package de.visustruct.struktogrammelemente

import de.visustruct.control.CanvasStyle
import de.visustruct.control.DiagramKeywordText
import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import de.visustruct.other.JTextAreaEasy
import de.visustruct.view.CodeErzeuger
import java.awt.Font
import java.awt.Graphics2D
import kotlin.math.ceil

class DoUntilSchleife(g: Graphics2D?) : Schleife(g) {

    init {
        setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typDoUntilSchleife))
    }

    override fun gibSchleifenLinkenRandInnen(): Int = 44

    override fun zeichne() {
        super.zeichne()
        zeichneDoObenLinks()
    }

    private fun zeichneDoObenLinks() {
        val gfx = g ?: return
        val stroke = ceil(CanvasStyle.DIAGRAM_LINE_WIDTH.toDouble()).toInt()
        val gutterLeft = gibX() + stroke + DO_LABEL_PAD_AUSSEN
        val innerBodyLeft = gibX() + gibSchleifenLinkenRandInnen()
        val halfStroke = ceil((CanvasStyle.DIAGRAM_LINE_WIDTH / 2f).toDouble()).toInt()
        var gutterRight = innerBodyLeft - halfStroke - DO_LABEL_PAD_VOR_RUMPF
        if (gutterRight <= gutterLeft) {
            gutterRight = gutterLeft + 1
        }
        val saved = gfx.font
        try {
            gfx.font = saved.deriveFont(Font.BOLD)
            val doWide = DiagramKeywordText.measureLineWidth(gfx, "do")
            var x = gutterLeft
            if (doWide < gutterRight - gutterLeft) {
                x = gutterLeft + (gutterRight - gutterLeft - doWide) / 2
            }
            val y = gibY() + stroke + DO_LABEL_PAD_AUSSEN + gfx.fontMetrics.ascent
            DiagramKeywordText.drawKeywordAwareLine(gfx, getFarbeSchrift(), x, y, "do")
        } finally {
            gfx.font = saved
        }
    }

    override fun textZeichnen() {
        val gfx = g ?: return
        val texthoehe = gibTexthoehe(text[0])
        var yVerschiebungAktuell = gibHoehe() - 15
        val typ = Struktogramm.typDoUntilSchleife
        for (i in text.indices.reversed()) {
            val display = DiagramKeywordText.lineForDisplay(typ, i, text[i])
            val x = gibX() + gibXVerschiebungFuerTextInMitte(i, display)
            DiagramKeywordText.drawKeywordAwareLine(gfx, getFarbeSchrift(), x, gibY() + yVerschiebungAktuell, display)
            yVerschiebungAktuell -= texthoehe
        }
    }

    override fun neuesElementMussOberhalbPlatziertWerden(y: Int): Boolean =
        y < gibY() + gibHoehe() - getUntererRand() / 2

    override fun randGroesseSetzen() {
        setUntererRand(obererRandZusatz + text.size * gibTexthoehe(text[0]))
    }

    override fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        if (typ == CodeErzeuger.typPython) {
            val vorher = "while True:\n"
            val bodyStart = anzahlEingerueckt + anzahlEinzuruecken
            val nachher = quellcodeMitKommentarVorspann("if not (", "):\n", typ, bodyStart, alsKommentar) +
                wandleZuAusgabe("break\n", typ, bodyStart + anzahlEinzuruecken, alsKommentar)
            textarea.hinzufuegen(wandleZuAusgabe(vorher, typ, anzahlEingerueckt, alsKommentar))
            liste.quellcodeAllerUnterelementeGenerieren(
                typ,
                anzahlEingerueckt + anzahlEinzuruecken,
                anzahlEinzuruecken,
                alsKommentar,
                textarea,
            )
            textarea.hinzufuegen(wandleZuAusgabe(nachher, typ, 0, alsKommentar))
        } else {
            val vorher = "do{\n"
            val nachher = quellcodeMitKommentarVorspann("}while(", ");\n", typ, anzahlEingerueckt, alsKommentar)
            textarea.hinzufuegen(wandleZuAusgabe(vorher, typ, anzahlEingerueckt, alsKommentar))
            liste.quellcodeAllerUnterelementeGenerieren(
                typ,
                anzahlEingerueckt + anzahlEinzuruecken,
                anzahlEinzuruecken,
                alsKommentar,
                textarea,
            )
            textarea.hinzufuegen(wandleZuAusgabe(nachher, typ, anzahlEingerueckt, alsKommentar))
        }
    }

    override fun getObererRand(): Int = 0

    override fun getUntererRand(): Int = super.getUntererRand() + getYVergroesserung()

    companion object {
        private const val DO_LABEL_PAD_AUSSEN = 6
        private const val DO_LABEL_PAD_VOR_RUMPF = 10
    }
}

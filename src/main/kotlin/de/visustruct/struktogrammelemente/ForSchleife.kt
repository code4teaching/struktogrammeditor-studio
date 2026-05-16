package de.visustruct.struktogrammelemente

import de.visustruct.control.DiagramKeywordText
import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import de.visustruct.other.JTextAreaEasy
import de.visustruct.view.CodeErzeuger
import java.awt.Graphics2D

class ForSchleife(g: Graphics2D?) : WhileSchleife(g) {

    init {
        setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typForSchleife))
    }

    override fun randGroesseSetzen() {
        if (hatMehrteiligenKopf()) {
            setObererRand(obererRandZusatz + gibTexthoehe(text[0]))
        } else {
            super.randGroesseSetzen()
        }
    }

    override fun gibBreiteDerBreitestenTextzeile(): Int {
        if (!hatMehrteiligenKopf()) {
            return super.gibBreiteDerBreitestenTextzeile()
        }
        val display = DiagramKeywordText.lineForDisplay(Struktogramm.typForSchleife, 0, gibKopfText())
        return if (objGesetzt(g)) {
            DiagramKeywordText.measureLineWidth(g, display)
        } else {
            maxOf(display.length * 7, 4 * display.length)
        }
    }

    override fun textZeichnen() {
        if (!hatMehrteiligenKopf()) {
            super.textZeichnen()
            return
        }
        val gfx = g ?: return
        val kopfText = gibKopfText()
        val texthoehe = gibTexthoehe(text[0])
        val display = DiagramKeywordText.lineForDisplay(Struktogramm.typForSchleife, 0, kopfText)
        val x = gibX() + gibXVerschiebungFuerTextInMitte(0, display)
        DiagramKeywordText.drawKeywordAwareLine(gfx, getFarbeSchrift(), x, gibY() + texthoehe - 5, display)
    }

    override fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        val vorher: String
        val nachher: String
        when {
            typ == CodeErzeuger.typPython -> {
                val pythonFor = CodeGenRules.pythonForLineFromJavaLikeLoopFields(text)
                vorher = if (pythonFor != null) {
                    quellcodeMitKommentarVorspann("", "\n", pythonFor, typ, anzahlEingerueckt, alsKommentar)
                } else {
                    quellcodeMitKommentarVorspann("for ", ":\n", typ, anzahlEingerueckt, alsKommentar)
                }
                nachher = ""
            }
            typ == CodeErzeuger.typJavaScript && hatMehrteiligenKopf() -> {
                vorher = quellcodeMitKommentarVorspann(
                    "for(", "){\n",
                    CodeGenRules.javaScriptForLoopHeader(text),
                    typ,
                    anzahlEingerueckt,
                    alsKommentar,
                )
                nachher = "}\n"
            }
            hatMehrteiligenKopf() -> {
                vorher = quellcodeMitKommentarVorspann(
                    "for(", "){\n",
                    CodeGenRules.cStyleForLoopHeader(text),
                    typ,
                    anzahlEingerueckt,
                    alsKommentar,
                )
                nachher = "}\n"
            }
            else -> {
                vorher = quellcodeMitKommentarVorspann("for(", "){\n", typ, anzahlEingerueckt, alsKommentar)
                nachher = "}\n"
            }
        }
        textarea.hinzufuegen(wandleZuAusgabe(vorher, typ, anzahlEingerueckt, alsKommentar))
        liste.quellcodeAllerUnterelementeGenerieren(
            typ,
            anzahlEingerueckt + anzahlEinzuruecken,
            anzahlEinzuruecken,
            alsKommentar,
            textarea,
        )
        if (nachher.isNotEmpty()) {
            textarea.hinzufuegen(wandleZuAusgabe(nachher, typ, anzahlEingerueckt, alsKommentar))
        }
    }

    private fun quellcodeMitKommentarVorspann(
        linkerTeil: String,
        rechterTeil: String,
        codeText: String,
        typ: Int,
        anzahlEingerueckt: Int,
        alsKommentar: Boolean,
    ): String {
        val sb = StringBuilder()
        if (alsKommentar) {
            sb.append(wandleZuAusgabe(co("kommentar") + co("text") + co("kommentarzu") + "\n", typ, anzahlEingerueckt, true))
        }
        sb.append(einruecken(linkerTeil + codeText + rechterTeil, anzahlEingerueckt))
        return sb.toString()
    }

    private fun hatMehrteiligenKopf(): Boolean = text.size > 1

    private fun gibKopfText(): String = text.joinToString(KOPF_TRENNER)

    companion object {
        private const val KOPF_TRENNER = "; "
    }
}

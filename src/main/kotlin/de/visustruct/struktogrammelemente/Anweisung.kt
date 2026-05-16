package de.visustruct.struktogrammelemente

import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import de.visustruct.other.JTextAreaEasy
import java.awt.Graphics2D
import java.awt.Rectangle

open class Anweisung : StruktogrammElement {

    constructor(text: String, g: Graphics2D?) : super(g) {
        obererRandZusatz = 10
        setzeText(text)
    }

    constructor(g: Graphics2D?) : this(GlobalSettings.gibElementBeschriftung(Struktogramm.typAnweisung), g)

    override fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        val zeile = if (alsKommentar) {
            wandleZuAusgabe(co("kommentar") + co("text") + co("kommentarzu") + "\n", typ, anzahlEingerueckt, true) +
                generiereAnweisungscode(typ, anzahlEingerueckt)
        } else {
            generiereAnweisungscode(typ, anzahlEingerueckt)
        }
        textarea.hinzufuegen("$zeile\n")
    }

    private fun generiereAnweisungscode(typ: Int, anzahlEingerueckt: Int): String {
        val code = StringBuilder()
        for (i in text.indices) {
            if (i > 0) {
                code.append('\n')
            }
            code.append(CodeGenRules.generateInstructionLine(text[i], typ, anzahlEingerueckt))
        }
        return code.toString()
    }

    override fun gibMindestbreite(): Int =
        gibBreiteDerBreitestenTextzeile() + getXVergroesserung() + 30

    override fun zeichenbereichAktualisieren(x: Int, y: Int): Rectangle {
        bereich.setBounds(x, y, gibMindestbreite(), getObererRand() + getYVergroesserung())
        return bereich
    }
}

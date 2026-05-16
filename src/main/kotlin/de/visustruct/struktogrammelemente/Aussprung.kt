package de.visustruct.struktogrammelemente

import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import de.visustruct.other.JTextAreaEasy
import de.visustruct.view.CodeErzeuger
import java.awt.Graphics2D

class Aussprung(g: Graphics2D?) : Anweisung(g) {

    init {
        setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typAussprung))
    }

    override fun zeichne() {
        super.zeichne()
        val gx = gibX()
        val gy = gibY()
        val gh = gibHoehe()
        g?.drawLine(gx + 10, gy, gx, gy + gh / 2)
        g?.drawLine(gx + 10, gy + gh, gx, gy + gh / 2)
    }

    override fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        val s = if (typ == CodeErzeuger.typPython) {
            co("kommentar") + "break  # / return  " + co("text") + co("kommentarzu")
        } else {
            co("kommentar") + "break;/return; " + co("text") + co("kommentarzu")
        }
        textarea.hinzufuegen(wandleZuAusgabe(s, typ, anzahlEingerueckt, alsKommentar) + "\n")
    }
}

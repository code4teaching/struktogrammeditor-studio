package de.visustruct.struktogrammelemente

import de.visustruct.other.JTextAreaEasy
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle

class LeerElement(g: Graphics2D?) : Anweisung("ø", g) {

    override fun setzeText(text: Array<String>) {
        super.setzeText("ø")
    }

    protected override fun setzeText(textEineZeile: String) {
        super.setzeText("ø")
    }

    override fun gibVorschauRect(vorschauPoint: Point): Rectangle =
        Rectangle(gibX(), gibY(), gibBreite(), gibHoehe())

    override fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        // LeerElement erzeugt keinen Quellcode
    }
}

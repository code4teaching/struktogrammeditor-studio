package de.visustruct.struktogrammelemente

import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import java.awt.Graphics2D

class Aufruf(g: Graphics2D?) : Anweisung(g) {

    init {
        setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typAufruf))
    }

    override fun zeichne() {
        super.zeichne()
        g?.drawLine(gibX() + 10, gibY(), gibX() + 10, gibY() + gibHoehe())
        g?.drawLine(gibX() + gibBreite() - 10, gibY(), gibX() + gibBreite() - 10, gibY() + gibHoehe())
    }
}

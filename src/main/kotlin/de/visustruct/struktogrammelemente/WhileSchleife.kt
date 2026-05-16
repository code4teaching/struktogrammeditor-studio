package de.visustruct.struktogrammelemente

import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import de.visustruct.other.JTextAreaEasy
import de.visustruct.view.CodeErzeuger
import java.awt.Graphics2D

open class WhileSchleife(g: Graphics2D?) : Schleife(g) {

    init {
        setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typWhileSchleife))
    }

    override fun neuesElementMussOberhalbPlatziertWerden(y: Int): Boolean =
        y < gibY() + getObererRand() / 2

    override fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        val (vorher, nachher) = if (typ == CodeErzeuger.typPython) {
            quellcodeMitKommentarVorspann("while ", ":\n", typ, anzahlEingerueckt, alsKommentar) to ""
        } else {
            quellcodeMitKommentarVorspann("while(", "){\n", typ, anzahlEingerueckt, alsKommentar) to "}\n"
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

}

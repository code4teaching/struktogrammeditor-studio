package de.visustruct.struktogrammelemente

import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import de.visustruct.other.JTextAreaEasy
import de.visustruct.view.CodeErzeuger
import java.awt.Graphics2D

class Endlosschleife(g: Graphics2D?) : Schleife(g) {

    init {
        setUntererRand(40)
        setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typEndlosschleife))
    }

    override fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        val (vorher, nachher) = if (typ == CodeErzeuger.typPython) {
            "while True" + co("zwangkommentar") + co("text") + co("zwangkommentarzu") + ":\n" to ""
        } else {
            "while(true)" + co("zwangkommentar") + co("text") + co("zwangkommentarzu") + "{\n" to "}\n"
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

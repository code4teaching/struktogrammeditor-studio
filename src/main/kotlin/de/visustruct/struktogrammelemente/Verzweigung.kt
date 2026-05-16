package de.visustruct.struktogrammelemente

import de.visustruct.control.GlobalSettings
import de.visustruct.control.Struktogramm
import de.visustruct.other.JTextAreaEasy
import de.visustruct.view.CodeErzeuger
import java.awt.Graphics2D

class Verzweigung(g: Graphics2D?) : Fallauswahl(g, 2) {

    private var seitenSindVertauscht: Boolean = false

    init {
        gibLinkeSeite().setzeBeschreibung(JA_TEXT)
        gibRechteSeite().setzeBeschreibung(NEIN_TEXT)
        xVerschiebungFuerTrennlinie = 0
        yVerschiebungFuerTrennLinie = 0
        obererRandZusatz = 20
        seitenSindVertauscht = false
        setzeText(GlobalSettings.gibElementBeschriftung(Struktogramm.typVerzweigung))
    }

    override fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        val (vorher, zwischenStueck, nachher) = if (typ == CodeErzeuger.typPython) {
            Triple(
                quellcodeMitKommentarVorspann("if ", ":\n", typ, anzahlEingerueckt, alsKommentar),
                "else:\n",
                "",
            )
        } else {
            Triple(
                quellcodeMitKommentarVorspann("if(", "){\n", typ, anzahlEingerueckt, alsKommentar),
                "}else{\n",
                "}\n",
            )
        }
        textarea.hinzufuegen(wandleZuAusgabe(vorher, typ, anzahlEingerueckt, alsKommentar))
        val (jaSeite, neinSeite) = if (seitenSindVertauscht) {
            gibRechteSeite() to gibLinkeSeite()
        } else {
            gibLinkeSeite() to gibRechteSeite()
        }
        jaSeite.quellcodeAllerUnterelementeGenerieren(
            typ,
            anzahlEingerueckt + anzahlEinzuruecken,
            anzahlEinzuruecken,
            alsKommentar,
            textarea,
        )
        textarea.hinzufuegen(wandleZuAusgabe(zwischenStueck, typ, anzahlEingerueckt, alsKommentar))
        neinSeite.quellcodeAllerUnterelementeGenerieren(
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

    private fun gibLinkeSeite(): StruktogrammElementListe = listen[0]

    private fun gibRechteSeite(): StruktogrammElementListe = listen[1]

    fun seitenVertauschen() {
        listenTauschen(0, 1)
        seitenSindVertauscht = !seitenSindVertauscht
    }

    override fun erstelleNeueSpalte() {
    }

    companion object {
        private const val JA_TEXT = "true"
        private const val NEIN_TEXT = "false"
    }
}

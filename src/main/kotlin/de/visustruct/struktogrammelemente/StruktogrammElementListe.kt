package de.visustruct.struktogrammelemente

import de.visustruct.other.JTextAreaEasy
import de.visustruct.view.CodeErzeuger
import org.jdom2.Element
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Rectangle

class StruktogrammElementListe(g: Graphics2D?) : ArrayList<StruktogrammElement>() {

    private var bereich: Rectangle = Rectangle()
    private var g: Graphics2D? = g
    private var beschreibung: String = ""

    init {
        add(LeerElement(g))
    }

    fun setzeBeschreibung(beschr: String) {
        beschreibung = beschr
    }

    fun gibBeschreibung(): String = beschreibung

    fun gibRectangle(): Rectangle = bereich

    /** Breite der Spaltenüberschrift (z. B. „true“) plus Rand – für Mindestbreite der Kopfzeile. */
    fun breiteFuerBeschriftungMitRand(horizontalerRand: Int): Int =
        gibTextbreite(gibBeschreibung()) + horizontalerRand

    fun gibAnzahlUnterelemente(): Int = size

    fun quellcodeAllerUnterelementeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
        if (typ == CodeErzeuger.typJava) {
            CodeGenRules.enterJavaGenerationScope()
        }
        try {
            for (i in 0 until size) {
                get(i).quellcodeGenerieren(typ, anzahlEingerueckt, anzahlEinzuruecken, alsKommentar, textarea)
            }
        } finally {
            if (typ == CodeErzeuger.typJava) {
                CodeGenRules.leaveJavaGenerationScope()
            }
        }
    }

    fun schreibeXMLDatenAllerUnterElemente(parent: Element) {
        for (i in 0 until size) {
            get(i).schreibeXMLDaten(parent)
        }
    }

    private fun gibTextbreite(s: String): Int =
        if (g != null) {
            g!!.fontMetrics.getStringBounds(s, g).bounds.width.toInt()
        } else {
            s.length * 4
        }

    fun alleZeichnen() {
        for (i in 0 until size) {
            get(i).zeichne()
        }
    }

    fun graphicsAllerUnterlementeSetzen(g: Graphics2D?) {
        this.g = g
        for (i in indices) {
            get(i).setzeGraphics(g)
        }
    }

    fun istUnterelement(eventuellesUnterelement: StruktogrammElement): Boolean {
        for (i in 0 until size) {
            if (get(i) === eventuellesUnterelement || get(i).istUnterelement(eventuellesUnterelement)) {
                return true
            }
        }
        return false
    }

    fun hinzufuegen(neues: StruktogrammElement) {
        hinzufuegen(neues, null, true)
    }

    fun hinzufuegen(
        neues: StruktogrammElement,
        naechstesOderVorheriges: StruktogrammElement?,
        vorDemAltenEinfuegen: Boolean,
    ) {
        val list = ArrayList<StruktogrammElement>()
        list.add(neues)
        hinzufuegen(list, naechstesOderVorheriges, vorDemAltenEinfuegen)
    }

    fun hinzufuegen(
        neue: ArrayList<StruktogrammElement>,
        naechstesOderVorheriges: StruktogrammElement?,
        vorDemAltenEinfuegen: Boolean,
    ) {
        var leeres: Any? = null

        if (isNotEmpty() && get(0) is LeerElement) {
            leeres = get(0)
        }

        if (naechstesOderVorheriges != null) {
            var position = indexOf(naechstesOderVorheriges)

            if (!vorDemAltenEinfuegen) {
                position++
            }

            for (strElem in neue) {
                add(position, strElem)
                position++
            }
        } else {
            for (strElem in neue) {
                add(strElem)
            }
        }

        if (leeres != null) {
            removeAt(indexOf(leeres))
        }
    }

    fun entfernen(zuLoeschen: StruktogrammElement) {
        removeAt(indexOf(zuLoeschen))

        if (size == 0) {
            add(LeerElement(g))
        }
    }

    fun alleEntfernen() {
        clear()
        add(LeerElement(g))
    }

    fun gibElementAnPos(x: Int, y: Int, nurListe: Boolean): Any? {
        if (bereich.contains(x, y)) {
            for (i in 0 until size) {
                val tmp = get(i).gibElementAnPos(x, y, nurListe)
                if (tmp != null) {
                    return tmp
                }
            }

            if (nurListe) {
                return this
            }
        }

        return null
    }

    fun gibListeDieDasElementHat(element: StruktogrammElement): StruktogrammElementListe? {
        for (i in 0 until size) {
            if (get(i) === element) {
                return this
            }
            val tmp = get(i).gibListeDieDasElementHat(element)
            if (tmp != null) {
                return tmp
            }
        }

        return null
    }

    fun gibDimensionDerUnterelemente(): Dimension {
        var breite = 0
        var hoehe = 0

        for (i in 0 until size) {
            val rect = get(i).gibRectangle()

            if (rect.width > breite) {
                breite = rect.width
            }

            hoehe += rect.height
        }

        return Dimension(breite, hoehe)
    }

    fun xPosAllerUnterelementeSetzen(x: Int) {
        for (i in 0 until size) {
            get(i).setzeXPos(x)
        }

        bereich.x = x
    }

    fun breiteDerUnterelementeSetzen(neueBreite: Int) {
        for (i in 0 until size) {
            get(i).setzeBreite(neueBreite)
        }

        bereich.width = neueBreite
    }

    fun gesamtHoeheSetzen(neueHoehe: Int) {
        if (size > 0) {
            val hoeheVorher = get(size - 1).gibHoehe()
            get(size - 1).setzeHoehe(neueHoehe - (bereich.height - hoeheVorher))
        }

        bereich.height = neueHoehe
    }

    fun zeichenbereichAllerElementeAktualisieren(x: Int, y: Int): Rectangle {
        var neueYPos = y

        for (i in 0 until size) {
            neueYPos += get(i).zeichenbereichAktualisieren(x, neueYPos).height
        }

        val dim = gibDimensionDerUnterelemente()

        val minBreiteNachBeschriftung = gibTextbreite(gibBeschreibung()) + 16
        if (dim.width < minBreiteNachBeschriftung) {
            dim.width = minBreiteNachBeschriftung
        }

        breiteDerUnterelementeSetzen(dim.width)

        bereich.setSize(dim)
        bereich.setLocation(x, y)

        return bereich
    }

    fun gibBreite(): Int = bereich.width

    fun gibHoehe(): Int = bereich.height

    fun gibRechterRand(): Int = bereich.x + bereich.width

    fun gibX(): Int = bereich.x

    fun zoomsAllerElementeZuruecksetzen() {
        for (i in 0 until size) {
            get(i).zoomsZuruecksetzen()
        }
    }

    companion object {
        private const val serialVersionUID = -122818269830027765L
    }
}

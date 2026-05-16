package de.visustruct.struktogrammelemente

import org.jdom2.Element
import java.awt.Graphics2D
import java.awt.Rectangle

abstract class Schleife(g: Graphics2D?) : StruktogrammElement(g) {

    @JvmField
    protected var liste: StruktogrammElementListe = StruktogrammElementListe(g)

    private var untererRand: Int = 0

    /** Linker Rand bis zum Rumpf — [DoUntilSchleife] liefert mehr Platz für „do“. */
    protected open fun gibSchleifenLinkenRandInnen(): Int = SCHLEIFEN_LINKER_INNEN_RAND

    init {
        setObererRand(0)
        setUntererRand(0)
        setzeText("Schleife")
    }

    fun gibListe(): StruktogrammElementListe = liste

    override fun zusaetzlicheXMLDatenSchreiben(aktuelles: Element) {
        val neues = Element("schleifeninhalt")
        liste.schreibeXMLDatenAllerUnterElemente(neues)
        aktuelles.addContent(neues)
    }

    override fun istUnterelement(eventuellesUnterelement: StruktogrammElement): Boolean =
        liste.istUnterelement(eventuellesUnterelement)

    override fun setzeGraphics(g: Graphics2D?) {
        super.setzeGraphics(g)
        liste.graphicsAllerUnterlementeSetzen(g)
    }

    override fun setzeBreite(neueBreite: Int) {
        liste.breiteDerUnterelementeSetzen(neueBreite - gibSchleifenLinkenRandInnen())
        liste.xPosAllerUnterelementeSetzen(gibX() + gibSchleifenLinkenRandInnen())
        bereich.width = neueBreite
    }

    override fun setzeHoehe(neueHoehe: Int) {
        liste.gesamtHoeheSetzen(neueHoehe - getObererRand() - getUntererRand())
        bereich.height = neueHoehe
    }

    override fun gibElementAnPos(x: Int, y: Int, nurListe: Boolean): Any? {
        if (!bereich.contains(x, y)) {
            return null
        }
        val tmp = liste.gibElementAnPos(x, y, nurListe)
        if (objGesetzt(tmp)) {
            return tmp
        }
        return if (!nurListe) this else null
    }

    override fun gibListeDieDasElementHat(element: StruktogrammElement): StruktogrammElementListe? =
        liste.gibListeDieDasElementHat(element)

    override fun zeichne() {
        super.zeichne()
        if (liste.isNotEmpty()) {
            liste.alleZeichnen()
        }
    }

    override fun zeichenbereichAktualisieren(x: Int, y: Int): Rectangle {
        val rectListe = liste.zeichenbereichAllerElementeAktualisieren(
            x + gibSchleifenLinkenRandInnen(),
            y + getObererRand(),
        )
        val gesamtbreite = if (rectListe.width >= gibMindestbreite()) {
            rectListe.width
        } else {
            gibMindestbreite()
        } + gibSchleifenLinkenRandInnen()
        bereich.setBounds(x, y, gesamtbreite, getObererRand() + getUntererRand() + rectListe.height)
        return bereich
    }

    override fun setzeXPos(x: Int) {
        bereich.x = x
        liste.xPosAllerUnterelementeSetzen(x + gibSchleifenLinkenRandInnen())
    }

    override fun zoomsZuruecksetzen() {
        super.zoomsZuruecksetzen()
        liste.zoomsAllerElementeZuruecksetzen()
    }

    override fun getObererRand(): Int = super.getObererRand() + getYVergroesserung()

    fun setUntererRand(untererRand: Int) {
        this.untererRand = untererRand
    }

    open fun getUntererRand(): Int = untererRand

    companion object {
        const val SCHLEIFEN_LINKER_INNEN_RAND: Int = 20
    }
}

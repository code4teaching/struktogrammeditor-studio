package de.visustruct.struktogrammelemente

import de.visustruct.control.CanvasStyle
import de.visustruct.control.DiagramKeywordText
import de.visustruct.control.Struktogramm
import de.visustruct.control.XMLLeser
import de.visustruct.other.JTextAreaEasy
import de.visustruct.view.CodeErzeuger
import org.jdom2.Element
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.Stroke

abstract class StruktogrammElement(g: Graphics2D?) {

    @JvmField
    protected var text: Array<String> = arrayOf("")

    @JvmField
    protected var bereich: Rectangle = Rectangle()

    @JvmField
    protected var markiert: Boolean = false

    /** Simulationsmodus: Schritt-Hervorhebung im Diagramm (unabhängig von [markiert]). */
    @JvmField
    protected var simulationSpotlight: Boolean = false

    @JvmField
    protected var g: Graphics2D? = g

    @JvmField
    protected var obererRandZusatz: Int = 20

    private var obererRand: Int = 0
    private var xVergroesserung: Int = 0
    private var yVergroesserung: Int = 0

    /** Wenn false, kommen Schrift-/Hintergrundfarbe beim Zeichnen aus [CanvasStyle] (folgt Theme-Wechsel). */
    private var elementfarbenExplizit: Boolean = false
    private var farbeSchrift: Color? = null
    private var farbeHintergrund: Color? = null

    init {
        setzeText("")
    }

    protected fun wandleZuAusgabe(
        codierung: String,
        typ: Int,
        anzahlEinzuruecken: Int,
        alsKommentare: Boolean,
    ): String {
        var result = einruecken(codierung, anzahlEinzuruecken)
        if (alsKommentare) {
            result = result
                .replace(co("kommentar"), CodeErzeuger.gibKommentarZeichen(true, typ))
                .replace(co("kommentarzu"), CodeErzeuger.gibKommentarZeichen(false, typ))
        } else {
            result = result
                .replace(co("kommentar"), "")
                .replace(co("kommentarzu"), "")
        }
        var x = result.indexOf(co("text"))
        if (x > -1) {
            x = x - result.substring(0, x).lastIndexOf("\n") - 1
        } else {
            x = 0
        }
        return result
            .replace(co("text"), textzeilenAusgeben(anzahlEinzuruecken, x))
            .replace(co("zwangkommentar"), CodeErzeuger.gibKommentarZeichen(true, typ))
            .replace(co("zwangkommentarzu"), CodeErzeuger.gibKommentarZeichen(false, typ))
    }

    /**
     * Kopfzeilen mit Bedingung/Ausdruck in Klammern: Bei `alsKommentar` zuerst den Struktogramm-Text
     * als Kommentarzeile(n), danach `linkerTeil + Text + rechterTeil` ohne Kommentar in den Klammern
     * (vermeidet verschachteltes `if` plus auskommentierte Bedingung in einer Zeile).
     */
    protected open fun quellcodeMitKommentarVorspann(
        linkerTeil: String,
        rechterTeil: String,
        typ: Int,
        anzahlEingerueckt: Int,
        alsKommentar: Boolean,
    ): String {
        val sb = StringBuilder()
        if (alsKommentar) {
            sb.append(
                wandleZuAusgabe(
                    co("kommentar") + co("text") + co("kommentarzu") + "\n",
                    typ,
                    anzahlEingerueckt,
                    true,
                ),
            )
        }
        sb.append(wandleZuAusgabe(linkerTeil + co("text") + rechterTeil, typ, anzahlEingerueckt, false))
        return sb.toString()
    }

    protected fun co(s: String): String = "%%$s" + "SbGRXEJUz4ZbvaaN%%"

    protected fun textzeilenAusgeben(anzahlEinzuruecken: Int, xPosErsteZeile: Int): String {
        val b = StringBuilder()
        for (i in text.indices) {
            if (i > 0) {
                b.append(einruecken(text[i], xPosErsteZeile))
            } else {
                b.append(text[i])
            }
            if (i < text.size - 1) {
                b.append('\n')
            }
        }
        return b.toString()
    }

    open fun quellcodeGenerieren(
        typ: Int,
        anzahlEingerueckt: Int,
        anzahlEinzuruecken: Int,
        alsKommentar: Boolean,
        textarea: JTextAreaEasy,
    ) {
    }

    protected fun einruecken(codeZeile: String, anzahlStellen: Int): String {
        if (anzahlStellen <= 0) {
            return codeZeile
        }
        return " ".repeat(anzahlStellen) + codeZeile
    }

    fun schreibeXMLDaten(elem: Element) {
        val neues = Element("strelem")
            .setAttribute("typ", "" + Struktogramm.strElementZuTypnummer(this))
            .setAttribute("zx", "" + xVergroesserung)
            .setAttribute("zy", "" + yVergroesserung)
        if (elementfarbenExplizit) {
            neues.setAttribute("textcolor", "" + farbeSchrift!!.rgb)
            neues.setAttribute("bgcolor", "" + farbeHintergrund!!.rgb)
        }
        for (i in text.indices) {
            neues.addContent(Element("text").addContent(XMLLeser.encodeS(text[i])))
        }
        zusaetzlicheXMLDatenSchreiben(neues)
        elem.addContent(neues)
    }

    protected open fun zusaetzlicheXMLDatenSchreiben(aktuelles: Element) {
    }

    open fun istUnterelement(eventuellesUnterelement: StruktogrammElement): Boolean = false

    open fun gibFaelle(): Array<String> = arrayOf("")

    open fun setzeFaelle(faelle: Array<String>) {
    }

    open fun gibAnzahlListen(): Int = 0

    internal open fun setzeGraphics(g: Graphics2D?) {
        this.g = g
        randGroesseSetzen()
    }

    fun setzeMarkiert(markiert: Boolean) {
        this.markiert = markiert
    }

    fun setzeSimulationSpotlight(simulationSpotlight: Boolean) {
        this.simulationSpotlight = simulationSpotlight
    }

    fun istSimulationSpotlight(): Boolean = simulationSpotlight

    fun istMarkiert(): Boolean = markiert

    protected fun objGesetzt(obj: Any?): Boolean = obj != null

    open fun neuesElementMussOberhalbPlatziertWerden(y: Int): Boolean =
        y < gibY() + gibHoehe() / 2

    open fun gibVorschauRect(vorschauPoint: Point): Rectangle {
        val anYPos = if (neuesElementMussOberhalbPlatziertWerden(vorschauPoint.y)) {
            gibY() - vorschauHoehe / 2
        } else {
            gibY() + gibHoehe() - vorschauHoehe / 2
        }
        return Rectangle(gibX(), anYPos, gibBreite(), vorschauHoehe)
    }

    fun gibRectangle(): Rectangle = bereich

    protected open fun gibMindestbreite(): Int =
        gibBreiteDerBreitestenTextzeile() + xVergroesserung + 80

    protected fun getXVergroesserung(): Int = xVergroesserung

    protected fun getYVergroesserung(): Int = yVergroesserung

    fun setXVergroesserung(xVergroesserung: Int) {
        this.xVergroesserung = xVergroesserung
    }

    fun setYVergroesserung(yVergroesserung: Int) {
        this.yVergroesserung = yVergroesserung
    }

    protected open fun gibBreiteDerBreitestenTextzeile(): Int {
        val typ = Struktogramm.strElementZuTypnummer(this)
        var groessteBreite = 0
        for (i in text.indices) {
            val display = DiagramKeywordText.lineForDisplay(typ, i, text[i])
            val breite = if (objGesetzt(g)) {
                DiagramKeywordText.measureLineWidth(g, display)
            } else {
                maxOf(display.length * 7, 4 * display.length)
            }
            if (breite > groessteBreite) {
                groessteBreite = breite
            }
        }
        return groessteBreite
    }

    internal open fun gibElementAnPos(x: Int, y: Int, nurListe: Boolean): Any? {
        if (nurListe) {
            return null
        }
        return if (bereich.contains(x, y)) this else null
    }

    open fun gibListeDieDasElementHat(element: StruktogrammElement): StruktogrammElementListe? = null

    abstract fun zeichenbereichAktualisieren(x: Int, y: Int): Rectangle

    open fun setzeText(text: Array<String>) {
        this.text = text
        randGroesseSetzen()
    }

    protected open fun randGroesseSetzen() {
        setObererRand(obererRandZusatz + text.size * gibTexthoehe(text[0]))
    }

    protected open fun setzeText(textEineZeile: String) {
        text = arrayOf(textEineZeile)
        randGroesseSetzen()
    }

    fun gibText(): Array<String> = text

    open fun zeichne() {
        eigenenBereichZeichnen()
        textZeichnen()
    }

    protected open fun textZeichnen() {
        val texthoehe = gibTexthoehe(text[0])
        var yVerschiebungAktuell = texthoehe - 5
        val typ = Struktogramm.strElementZuTypnummer(this)
        for (i in text.indices) {
            val display = DiagramKeywordText.lineForDisplay(typ, i, text[i])
            val x = gibX() + gibXVerschiebungFuerTextInMitte(i, display)
            DiagramKeywordText.drawKeywordAwareLine(g, getFarbeSchrift(), x, gibY() + yVerschiebungAktuell, display)
            yVerschiebungAktuell += texthoehe
        }
    }

    protected fun eigenenBereichZeichnen() {
        val alt: Stroke = g!!.stroke
        try {
            g!!.stroke = BasicStroke(CanvasStyle.DIAGRAM_LINE_WIDTH)
            var fill = getFarbeHintergrund()
            if (simulationSpotlight) {
                fill = CanvasStyle.getSimulationStepHighlightFill()
            }
            g!!.color = fill
            g!!.fillRect(gibX(), gibY(), gibBreite(), gibHoehe())
            g!!.color = CanvasStyle.getElementBorder()
            g!!.drawRect(gibX(), gibY(), gibBreite(), gibHoehe())
            if (simulationSpotlight) {
                g!!.stroke = BasicStroke(CanvasStyle.SELECTION_LINE_WIDTH)
                g!!.color = CanvasStyle.getSelectionStroke()
                g!!.drawRect(gibX(), gibY(), gibBreite(), gibHoehe())
            } else if (markiert) {
                g!!.stroke = BasicStroke(CanvasStyle.SELECTION_LINE_WIDTH)
                g!!.color = CanvasStyle.getSelectionStroke()
                g!!.drawRect(gibX(), gibY(), gibBreite(), gibHoehe())
            }
        } finally {
            g!!.stroke = alt
        }
    }

    protected fun gibX(): Int = bereich.x

    protected fun gibY(): Int = bereich.y

    protected fun gibBreite(): Int = bereich.width

    internal fun gibHoehe(): Int = bereich.height

    open fun setzeBreite(neueBreite: Int) {
        bereich.width = neueBreite
    }

    open fun setzeHoehe(neueHoehe: Int) {
        bereich.height = neueHoehe
    }

    protected fun gibTextbreite(s: String): Int =
        if (g != null) {
            g!!.fontMetrics.getStringBounds(s, g).bounds.width.toInt()
        } else {
            s.length * 4
        }

    protected fun gibTexthoehe(s: String): Int =
        if (objGesetzt(g)) {
            g!!.fontMetrics.getStringBounds(s, g).bounds.height.toInt()
        } else {
            20
        }

    protected fun gibXVerschiebungFuerTextInMitte(lineIndex: Int, displayLine: String): Int {
        if (!objGesetzt(g)) {
            return (gibBreite() - displayLine.length * 7) / 2
        }
        return (gibBreite() - DiagramKeywordText.measureLineWidth(g, displayLine)) / 2
    }

    /** Zentriert eine Anzeigezeile (Rohzeile Zeile 0: führendes Keyword wie in Swift). */
    protected fun gibXVerschiebungFuerTextInMitte(rawEineZeile: String): Int {
        val typ = Struktogramm.strElementZuTypnummer(this)
        val display = DiagramKeywordText.lineForDisplay(typ, 0, rawEineZeile)
        return gibXVerschiebungFuerTextInMitte(0, display)
    }

    protected fun gibXVerschiebungFuerMittig(s: String, breiteUntergrund: Int): Int =
        ((breiteUntergrund - gibTextbreite(s)) / 2)

    open fun setzeXPos(x: Int) {
        bereich.x = x
    }

    fun zoomX(erhoeheXUm: Int) {
        if (xVergroesserung + erhoeheXUm >= 0) {
            xVergroesserung += erhoeheXUm
        }
    }

    fun zoomY(erhoeheYUm: Int) {
        if (yVergroesserung + erhoeheYUm >= 0) {
            yVergroesserung += erhoeheYUm
        }
    }

    open fun zoomsZuruecksetzen() {
        xVergroesserung = 0
        yVergroesserung = 0
    }

    open fun setObererRand(obererRand: Int) {
        this.obererRand = obererRand
    }

    open fun getObererRand(): Int = obererRand

    fun getFarbeSchrift(): Color =
        if (elementfarbenExplizit) farbeSchrift!! else CanvasStyle.getElementText()

    fun setFarbeSchrift(farbeSchrift: Color) {
        this.farbeSchrift = farbeSchrift
        elementfarbenExplizit = true
    }

    fun getFarbeHintergrund(): Color =
        if (elementfarbenExplizit) farbeHintergrund!! else CanvasStyle.getElementFill()

    fun setFarbeHintergrund(farbeHintergrund: Color) {
        this.farbeHintergrund = farbeHintergrund
        elementfarbenExplizit = true
    }

    /** Beim XML-Laden, wenn mindestens eine Farbe im Tag steht. */
    fun setzeFarbenAusXml(schrift: Color, hintergrund: Color) {
        farbeSchrift = schrift
        farbeHintergrund = hintergrund
        elementfarbenExplizit = true
    }

    fun sindElementfarbenExplizit(): Boolean = elementfarbenExplizit

    companion object {
        private const val vorschauHoehe = 20
    }
}

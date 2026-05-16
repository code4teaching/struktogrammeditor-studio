package de.visustruct.control

import de.visustruct.i18n.I18n
import de.visustruct.simulation.SimulationDocumentJdom
import de.visustruct.simulation.codec.XmlDecodeException
import de.visustruct.simulation.model.SimulationDocument
import de.visustruct.struktogrammelemente.Anweisung
import de.visustruct.struktogrammelemente.Aufruf
import de.visustruct.struktogrammelemente.Aussprung
import de.visustruct.struktogrammelemente.DoUntilSchleife
import de.visustruct.struktogrammelemente.Endlosschleife
import de.visustruct.struktogrammelemente.Fallauswahl
import de.visustruct.struktogrammelemente.ForSchleife
import de.visustruct.struktogrammelemente.LeerElement
import de.visustruct.struktogrammelemente.Schleife
import de.visustruct.struktogrammelemente.StruktogrammElement
import de.visustruct.struktogrammelemente.StruktogrammElementListe
import de.visustruct.struktogrammelemente.Verzweigung
import de.visustruct.struktogrammelemente.WhileSchleife
import de.visustruct.view.EingabeDialog
import de.visustruct.view.GUI
import de.visustruct.view.StrFileFilter
import de.visustruct.view.StrTabbedPane
import de.visustruct.view.StruktogrammPopup
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import java.awt.BasicStroke
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Stroke
import java.awt.Window
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource
import java.awt.dnd.DragSourceDragEvent
import java.awt.dnd.DragSourceDropEvent
import java.awt.dnd.DragSourceEvent
import java.awt.dnd.DragSourceListener
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class Struktogramm @JvmOverloads constructor(tabbedpane: StrTabbedPane? = null) : JPanel(true), MouseListener, MouseMotionListener, DropTargetListener, DragGestureListener, DragSourceListener {

    private var liste: StruktogrammElementListe
    private var g: Graphics2D? = null
    private var markiertesElement: StruktogrammElement? = null
    private var simulationSpotlightElement: StruktogrammElement? = null
    private var bild: BufferedImage? = null
    private var sperre = 0
    private var popupmenuSichtbar = false
    private var letzteDiagrammMausKoords: Point? = null
    private var ausgewaehlteEinfuegeKoords: Point? = null
    private var ausgewaehlteEinfuegeMarkierung: Rectangle? = null
    private var tabbedpane: StrTabbedPane?
    private var dimGroesse: Dimension
    private var canvasViewScale = 1.0
    private var dragSource: DragSource? = null
    private var popup: StruktogrammPopup? = null
    private var dragZwischenlagerElement: StruktogrammElement? = null
    private var dragZwischenlagerListe: StruktogrammElementListe? = null
    private var rectVorschau: Rectangle? = null
    private var aktuellerSpeicherpfad: String = ""
    private var vorgeschlagenerSpeicherBasisname: String = ""
    private lateinit var rueckgaengigListe: ArrayList<Document>
    private var posInRueckgaengigListe = 0
    private var posInRueckgaengigListeWoZuletztGespeichert = -1
    private var fontStr: Font = GlobalSettings.fontStandard
    private var struktogrammBeschreibung: String = ""

    init {
        background = CanvasStyle.getBackground()
        this.tabbedpane = tabbedpane
        setBounds(0, 0, 0, 0)
        dimGroesse = size
        liste = StruktogrammElementListe(null)
        liste.setzeBeschreibung("mainlist")
        rueckgaengigListeInitialisieren()
        addMouseListener(this)
        addMouseMotionListener(this)
        if (!GraphicsEnvironment.isHeadless()) {
            dragSource = DragSource()
            dragSource?.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this)
            DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this, true, null)
        }
        rueckgaengigPunktSetzen(false)
    }

    private fun gui(): GUI? = tabbedpane?.gibGUI()

    fun setStruktogrammBeschreibung(s: String) {
        struktogrammBeschreibung = s
    }

    fun getStruktogrammBeschreibung(): String = struktogrammBeschreibung

    fun canvasZoomIn() {
        canvasViewScale = min(CANVAS_ZOOM_MAX, canvasViewScale * CANVAS_ZOOM_STEP)
        applyCanvasViewLayoutSize()
    }

    fun canvasZoomOut() {
        canvasViewScale = max(CANVAS_ZOOM_MIN, canvasViewScale / CANVAS_ZOOM_STEP)
        applyCanvasViewLayoutSize()
    }

    private fun applyCanvasViewLayoutSize() {
        if (dimGroesse.width <= 0 || dimGroesse.height <= 0) {
            return
        }
        val view = Dimension(
            max(1, ceil(dimGroesse.width * canvasViewScale).toInt()),
            max(1, ceil(dimGroesse.height * canvasViewScale).toInt()),
        )
        setSize(view)
        preferredSize = view
        revalidate()
        parent?.revalidate()
        repaint()
    }

    private fun viewKoordsZuLogisch(vx: Int, vy: Int): Point {
        if (canvasViewScale <= 0.0 || abs(canvasViewScale - 1.0) < 1e-9) {
            return Point(vx, vy)
        }
        return Point((vx / canvasViewScale).roundToInt(), (vy / canvasViewScale).roundToInt())
    }

    private fun viewKoordsZuLogisch(e: MouseEvent): Point = viewKoordsZuLogisch(e.x, e.y)

    fun gibListe(): StruktogrammElementListe = liste

    fun elementFuerSimulationPfadSuchen(path: List<Int>?): StruktogrammElement? = elementFuerSimulationPfadSuchenExakt(path)

    fun elementFuerSimulationPfadSuchenExakt(path: List<Int>?): StruktogrammElement? {
        if (path.isNullOrEmpty()) {
            return null
        }
        var list = liste
        var pi = 0
        while (pi < path.size) {
            val idx = path[pi++]
            if (idx < 0 || idx >= list.size) {
                return null
            }
            val el = list[idx]
            if (pi >= path.size) {
                return el
            }
            when (el) {
                is Fallauswahl -> {
                    val col = path[pi++]
                    if (col < 0 || col >= el.gibAnzahlListen()) {
                        return null
                    }
                    list = el.gibListe(col)
                }

                is Schleife -> {
                    if (pi < path.size) {
                        pi++
                    }
                    list = el.gibListe()
                }

                else -> return null
            }
        }
        return null
    }

    private fun elementFuerSimulationPfadSuchenMitFallback(path: List<Int>?): StruktogrammElement? {
        if (path.isNullOrEmpty()) {
            return null
        }
        for (len in path.size downTo 1) {
            val el = elementFuerSimulationPfadSuchenExakt(path.subList(0, len))
            if (el != null) {
                return el
            }
        }
        return null
    }

    fun setzeSimulationSpotlightPfad(path: List<Int>?) {
        simulationSpotlightElement?.setzeSimulationSpotlight(false)
        simulationSpotlightElement = null
        if (path.isNullOrEmpty()) {
            zeichne()
            return
        }
        val el = elementFuerSimulationPfadSuchenMitFallback(path)
        if (el != null) {
            el.setzeSimulationSpotlight(true)
            simulationSpotlightElement = el
        }
        zeichne()
    }

    fun gibGraphics(): Graphics2D? = g

    fun gibTabbedPane(): StrTabbedPane? = tabbedpane

    fun setzePopupmenuSichtbar(neuerStatus: Boolean) {
        popupmenuSichtbar = neuerStatus
    }

    fun refreshAfterThemeChange() {
        background = CanvasStyle.getBackground()
        revalidate()
        zeichenbereichAktualisieren()
        zeichne()
    }

    fun graphicsInitialisieren(): Boolean {
        if (dimGroesse.width > 0 || dimGroesse.height > 0) {
            bild = createImage(dimGroesse.width, dimGroesse.height) as BufferedImage
            g = bild!!.createGraphics()
            applyCanvasRenderingHints(g!!)
            g!!.font = fontStr
            g!!.stroke = BasicStroke(CanvasStyle.DIAGRAM_LINE_WIDTH)
            liste.graphicsAllerUnterlementeSetzen(g)
            return true
        }
        return false
    }

    override fun paint(g: Graphics) {
        super.paint(g)
        zeichne(g)
    }

    fun zeichne() {
        repaint()
    }

    fun zeichne(panelGraphics: Graphics) {
        val g2 = g
        val offscreen = bild
        if (g2 != null && offscreen != null) {
            applyCanvasRenderingHints(g2)
            g2.color = CanvasStyle.getBackground()
            g2.fillRect(0, 0, dimGroesse.width, dimGroesse.height)
            if (struktogrammBeschreibung.isNotEmpty()) {
                val f = g2.font
                g2.font = Font(f.family, f.style, 20)
                g2.color = CanvasStyle.getTitleText()
                g2.drawString(struktogrammBeschreibung, getXVerschiebungForCenteredText(struktogrammBeschreibung, dimGroesse.width, g2), 35)
                g2.font = f
            }
            val frameStrokeAlt = g2.stroke
            try {
                g2.stroke = BasicStroke(CanvasStyle.DIAGRAM_LINE_WIDTH)
                g2.color = CanvasStyle.getDiagramFrame()
                val m = diagramOuterMargin
                g2.drawRect(m, m, dimGroesse.width - 2 * m, dimGroesse.height - 2 * m)
            } finally {
                g2.stroke = frameStrokeAlt
            }
            liste.alleZeichnen()
            rectVorschau?.let {
                g2.color = CanvasStyle.getDropPreview()
                g2.fillRect(it.x, it.y, it.width, it.height)
            }
            ausgewaehlteEinfuegeMarkierung?.let {
                g2.color = CanvasStyle.getDropPreview()
                g2.fillRect(it.x, it.y, it.width, it.height)
            }
            dragZwischenlagerElement?.let {
                val rectDragElement = it.gibRectangle()
                val alt = g2.stroke
                g2.stroke = BasicStroke(2f)
                g2.color = CanvasStyle.getDragFrame()
                g2.drawRect(rectDragElement.x, rectDragElement.y, rectDragElement.width, rectDragElement.height)
                g2.stroke = alt
            }
            val viewW = max(1, ceil(dimGroesse.width * canvasViewScale).toInt())
            val viewH = max(1, ceil(dimGroesse.height * canvasViewScale).toInt())
            if (panelGraphics is Graphics2D) {
                panelGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            }
            panelGraphics.drawImage(offscreen, 0, 0, viewW, viewH, 0, 0, dimGroesse.width, dimGroesse.height, this)
        } else {
            if (graphicsInitialisieren()) {
                zeichenbereichAktualisieren()
                zeichne()
            }
        }
        if (popupmenuSichtbar && popup != null) {
            popup!!.repaint()
        }
    }

    private fun getXVerschiebungForCenteredText(s: String, breiteUntergrund: Int, g: Graphics2D?): Int {
        val i = if (g != null) g.fontMetrics.getStringBounds(s, g).bounds.width.toInt() else s.length * 4
        return ((breiteUntergrund - i) / 2.0).toInt()
    }

    private fun gibCaptionTextBreitePx(): Int {
        if (struktogrammBeschreibung.isEmpty()) {
            return 0
        }
        val captionFont = Font(fontStr.family, fontStr.style, 20)
        val scratch = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g2 = scratch.createGraphics()
        try {
            applyCanvasRenderingHints(g2)
            g2.font = captionFont
            return ceil(g2.fontMetrics.getStringBounds(struktogrammBeschreibung, g2).width).toInt()
        } finally {
            g2.dispose()
        }
    }

    fun zeichenbereichAktualisieren() {
        val randLinksNeu = randLinks + if (struktogrammBeschreibung.isNotEmpty()) 20 else 0
        val randObenNeu = randOben + if (struktogrammBeschreibung.isNotEmpty()) 40 else 0
        dimGroesse = liste.zeichenbereichAllerElementeAktualisieren(randLinksNeu, randObenNeu).size
        dimGroesse.width += randLinksNeu * 2
        dimGroesse.height += randObenNeu * 2
        val captionBreite = gibCaptionTextBreitePx()
        if (captionBreite > 0) {
            val minBreiteFuerCaption = captionBreite + captionRandHorizontal * 2
            if (dimGroesse.width < minBreiteFuerCaption) {
                dimGroesse.width = minBreiteFuerCaption
            }
        }
        graphicsInitialisieren()
        applyCanvasViewLayoutSize()
    }

    private fun rueckgaengigListeInitialisieren() {
        rueckgaengigListe = ArrayList()
    }

    fun rueckgaengigPunktSetzen() {
        rueckgaengigPunktSetzen(true)
    }

    fun rueckgaengigPunktSetzen(tabbedpaneTitelAnpassen: Boolean) {
        for (i in rueckgaengigListe.size - 1 downTo (posInRueckgaengigListe + 1)) {
            rueckgaengigListe.removeAt(i)
            if (i == posInRueckgaengigListeWoZuletztGespeichert) {
                posInRueckgaengigListeWoZuletztGespeichert = -1
            }
        }
        rueckgaengigListe.add(xmlErstellen())
        posInRueckgaengigListe = rueckgaengigListe.size - 1
        if (tabbedpaneTitelAnpassen) {
            tabbedpane?.titelFuerStruktogrammBearbeitetMarkieren(this, true)
        }
    }

    fun schrittZurueck() {
        if (posInRueckgaengigListe > 0) {
            posInRueckgaengigListe--
            laden(rueckgaengigListe[posInRueckgaengigListe])
        }
    }

    fun schrittNachVorne() {
        if (posInRueckgaengigListe < rueckgaengigListe.size - 1) {
            posInRueckgaengigListe++
            laden(rueckgaengigListe[posInRueckgaengigListe])
        }
    }

    private fun elementAnPosBefuellen(x: Int, y: Int) {
        val element = liste.gibElementAnPos(x, y, false) as StruktogrammElement?
        gui()?.gibElementEditorPanel()?.setSelectedElement(this, element)
    }

    fun elementTextAusEditorSetzen(element: StruktogrammElement?, text: Array<String>?) {
        if (element == null || text == null) {
            return
        }
        if (element is LeerElement) {
            val g2 = gibGraphics() ?: return
            val zListe = liste.gibListeDieDasElementHat(element) ?: return
            val idx = zListe.indexOf(element)
            if (idx < 0) {
                return
            }
            val neu = Anweisung(g2)
            neu.setzeText(text)
            zListe[idx] = neu
            rueckgaengigPunktSetzen()
            zeichenbereichAktualisieren()
            elementAuswaehlen(neu)
            zeichne()
            return
        }
        element.setzeText(text)
        rueckgaengigPunktSetzen()
        zeichenbereichAktualisieren()
        gui()?.gibElementEditorPanel()?.setSelectedElement(this, element)
        zeichne()
    }

    fun elementBefuellen(element: StruktogrammElement?) {
        if (element != null && element !is LeerElement) {
            val text = eingabeBox(element)
            if (text != null) {
                element.setzeText(text)
                rueckgaengigPunktSetzen()
            }
            zeichenbereichAktualisieren()
            zeichne()
        }
    }

    fun eingabeBox(tmp: StruktogrammElement): Array<String>? {
        val parent = gui() ?: return null
        val dialog = EingabeDialog(parent, I18n.tr("dialog.elementEdit.title"), true, tmp)
        return dialog.gibTextArray()
    }

    private fun vorschauMarkierungAnzeigen(x: Int, y: Int, vorschauFuerNeuesElement: Boolean) {
        val tmp = liste.gibElementAnPos(x, y, false) as StruktogrammElement?
        entmarkieren()
        if (tmp != null) {
            markiertesElement = tmp
            tmp.setzeMarkiert(true)
            if (vorschauFuerNeuesElement) {
                rectVorschau = tmp.gibVorschauRect(Point(x, y))
            }
        }
    }

    private fun entmarkieren() {
        markiertesElement?.setzeMarkiert(false)
        rectVorschau = null
    }

    fun neuesStruktogrammElement(typ: Int): StruktogrammElement? =
        when (typ) {
            0 -> Anweisung(g)
            1 -> Verzweigung(g)
            2 -> Fallauswahl(g)
            3 -> ForSchleife(g)
            4 -> WhileSchleife(g)
            5 -> DoUntilSchleife(g)
            6 -> Endlosschleife(g)
            7 -> Aussprung(g)
            8 -> Aufruf(g)
            9 -> LeerElement(g)
            else -> null
        }

    fun neuesElementAnAktuellerStelleEinfuegen(typ: Int) {
        var p = ausgewaehlteEinfuegeKoords
        if (p == null) {
            val mp = mousePosition
            if (mp != null) {
                p = viewKoordsZuLogisch(mp.x, mp.y)
            }
        }
        if (p == null) {
            p = letzteDiagrammMausKoords
        }
        if (p != null && liste.gibElementAnPos(p.x, p.y, true) != null) {
            gezogenesElementEinfuegen(p.x, p.y, typ)
        } else {
            neuesElementAmEndeEinfuegen(typ)
        }
    }

    private fun neuesElementAmEndeEinfuegen(typ: Int) {
        val neues = neuesStruktogrammElement(typ)
        if (neues != null) {
            liste.hinzufuegen(neues)
            einfuegeMarkierungLoeschen()
            zeichenbereichAktualisieren()
            elementAuswaehlen(neues)
            zeichne()
            rueckgaengigPunktSetzen()
        }
    }

    fun elementAnAktuellerStelleLoeschen() {
        val mp = mousePosition ?: return
        val p = viewKoordsZuLogisch(mp.x, mp.y)
        val element = liste.gibElementAnPos(p.x, p.y, false) as StruktogrammElement?
        if (element != null) {
            elementLoeschen(element, false)
        }
    }

    fun zoomAktuellesElement(groesser: Boolean) {
        val mp = mousePosition
        if (mp != null) {
            val p = viewKoordsZuLogisch(mp.x, mp.y)
            val element = liste.gibElementAnPos(p.x, p.y, false) as StruktogrammElement?
            if (element != null) {
                zoom(if (groesser) 1 else -1, if (groesser) 1 else -1, element)
            }
        }
    }

    private fun gezogenesElementEinfuegen(x: Int, y: Int, typ: Int) {
        val neues = neuesStruktogrammElement(typ)
        if (neues != null) {
            elementEinfuegen(x, y, neues, null)
        }
    }

    private fun elementEinfuegen(x: Int, y: Int, neues: StruktogrammElement?, listeNeue: StruktogrammElementListe?) {
        val listeZumEinfuegen = liste.gibElementAnPos(x, y, true) as StruktogrammElementListe?
        if (listeZumEinfuegen != null) {
            val tmp = listeZumEinfuegen.gibElementAnPos(x, y, false) as StruktogrammElement?
            var oberhalbEinfuegen = false
            if (tmp != null) {
                oberhalbEinfuegen = tmp.neuesElementMussOberhalbPlatziertWerden(y)
            }
            if (neues != null || listeNeue != null) {
                if (neues != null) {
                    listeZumEinfuegen.hinzufuegen(neues, tmp, oberhalbEinfuegen)
                }
                if (listeNeue != null) {
                    val al = ArrayList<StruktogrammElement>(listeNeue)
                    listeZumEinfuegen.hinzufuegen(al, tmp, oberhalbEinfuegen)
                }
                einfuegeMarkierungLoeschen()
                zeichenbereichAktualisieren()
                if (neues != null) {
                    elementAuswaehlen(neues)
                }
                zeichne()
                rueckgaengigPunktSetzen()
            }
        }
    }

    fun gibZwischenlagerElement(): StruktogrammElement? = dragZwischenlagerElement

    private fun elementAusKopierFeldEinfuegen(x: Int, y: Int) {
        val xmlLeser = XMLLeser()
        val kopiert = gui()?.gibAuswahlPanel()?.gibKopiertesStrElement() ?: return
        val neue = xmlLeser.erstelleStruktogrammElementListe(kopiert, this)
        if (neue != null) {
            elementEinfuegen(x, y, null, neue)
        }
    }

    fun elementAusKopierFeldEinfuegenAnMausPos() {
        var p = mousePosition
        if (p == null) {
            p = letzteDiagrammMausKoords
        }
        if (p != null) {
            elementAusKopierFeldEinfuegen(p.x, p.y)
        } else {
            JOptionPane.showMessageDialog(
                gui(),
                I18n.tr("dialog.pasteNeedPosition.message"),
                I18n.tr("dialog.pasteNeedPosition.title"),
                JOptionPane.INFORMATION_MESSAGE,
            )
        }
    }

    fun elementAusKopierFeldEinfuegenAnKoordinaten(x: Int, y: Int) {
        elementAusKopierFeldEinfuegen(x, y)
    }

    private fun elementAusZwischenlagerEinfuegen(x: Int, y: Int) {
        val tmp = liste.gibElementAnPos(x, y, false) as StruktogrammElement?
        val dragEl = dragZwischenlagerElement
        if (tmp != null && dragEl != null && tmp !== dragEl && !dragEl.istUnterelement(tmp)) {
            elementAusZwischenlagerGanzEntfernen()
            elementEinfuegen(x, y, dragEl, null)
        }
    }

    fun elementAusZwischenlagerGanzEntfernen() {
        val dzl = dragZwischenlagerListe
        val dze = dragZwischenlagerElement
        if (dzl != null && dze != null) {
            dzl.entfernen(dze)
        }
    }

    fun elementLoeschen(zuLoeschen: StruktogrammElement, vorherFragen: Boolean) {
        val frage = if (zuLoeschen is Schleife || zuLoeschen is Fallauswahl) {
            I18n.tr("dialog.deleteBlock.messageNested")
        } else {
            I18n.tr("dialog.deleteBlock.message")
        }
        val opts = arrayOf(I18n.tr("dialog.deleteBlock.remove"), I18n.tr("dialog.deleteBlock.cancel"))
        val loeschen = !vorherFragen ||
            JOptionPane.showOptionDialog(
                gui(),
                frage,
                I18n.tr("dialog.deleteBlock.title"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                opts,
                opts[1],
            ) == 0
        if (loeschen) {
            val tmp = liste.gibListeDieDasElementHat(zuLoeschen)
            tmp?.entfernen(zuLoeschen)
            if (zuLoeschen === markiertesElement) {
                elementAuswaehlen(null)
            }
            zeichenbereichAktualisieren()
            zeichne()
            rueckgaengigPunktSetzen()
        }
    }

    fun markiertesElementLoeschen() {
        val el = markiertesElement
        if (el == null || el is LeerElement) {
            return
        }
        elementLoeschen(el, true)
    }

    private fun popupMenueZeigen(viewX: Int, viewY: Int) {
        val log = viewKoordsZuLogisch(viewX, viewY)
        val tmp = liste.gibElementAnPos(log.x, log.y, false) as StruktogrammElement?
        if (tmp != null) {
            popup = StruktogrammPopup(tmp, this, log.x, log.y)
            popup!!.show(this, viewX, viewY)
        }
    }

    fun gibAktuellenSpeicherpfad(): String = aktuellerSpeicherpfad

    private fun setzeAktuellerSpeicherpfad(pfad: String) {
        aktuellerSpeicherpfad = pfad
        vorgeschlagenerSpeicherBasisname = ""
        var name = File(pfad).name
        if (name.isEmpty()) {
            name = "document"
        }
        tabbedpane?.titelFuerStruktogrammSetzen(this, name)
    }

    fun setVorgeschlagenenSpeicherBasisnamen(name: String?) {
        vorgeschlagenerSpeicherBasisname = sanitizeDateiBasisname(name)
    }

    private fun extrahiereExtension(pfad: String): String {
        val dot = pfad.lastIndexOf('.')
        if (dot < 0 || dot >= pfad.length - 1) {
            return ""
        }
        return pfad.substring(dot + 1)
    }

    fun generateImage(mitRand: Boolean): BufferedImage {
        entmarkieren()
        zeichne()
        val b = bild!!
        return b.getSubimage(
            if (mitRand) 0 else randLinks,
            if (mitRand) 0 else randOben,
            liste.gibBreite() + if (mitRand) 2 * randLinks else 1,
            liste.gibHoehe() + if (mitRand) 2 * randOben else 1,
        )
    }

    fun alsBilddateiSpeichern(voreingestellterPfad: String): String =
        alsBilddateiSpeichernMitFiltern(voreingestellterPfad, intArrayOf(7, 6, 5, 4, 3))

    private fun alsBilddateiSpeichernMitFiltern(voreingestellterPfad: String, bildFilterNummern: IntArray): String {
        var pfad = saveFileChooser(bildFilterNummern, voreingestellterPfad, true)
        if (pfad.isEmpty()) {
            return pfad
        }
        if (extrahiereExtension(pfad).isEmpty()) {
            pfad += ".png"
        }
        val ausgabeBild = generateImage(false)
        val fmt = imageFormatFuerImageIO(pfad)
        try {
            FileOutputStream(pfad).use { fos ->
                if (!ImageIO.write(ausgabeBild, fmt, fos)) {
                    JOptionPane.showMessageDialog(
                        gui(),
                        I18n.trf("dialog.exportImage.formatFailed", fmt),
                        I18n.tr("dialog.exportImage.title"),
                        JOptionPane.ERROR_MESSAGE,
                    )
                }
            }
        } catch (ex: IOException) {
            JOptionPane.showMessageDialog(
                gui(),
                I18n.trf("dialog.exportImage.error", ex.message),
                I18n.tr("dialog.exportImage.title"),
                JOptionPane.ERROR_MESSAGE,
            )
        } catch (ex: RuntimeException) {
            JOptionPane.showMessageDialog(
                gui(),
                I18n.trf("dialog.exportImage.error", ex.message),
                I18n.tr("dialog.exportImage.title"),
                JOptionPane.ERROR_MESSAGE,
            )
        }
        return pfad
    }

    fun speichern(neunenSpeicherpfadAuswaehlenLassen: Boolean, voreingestellterPfad: String): String {
        if (neunenSpeicherpfadAuswaehlenLassen || aktuellerSpeicherpfad.isEmpty()) {
            xmlSpeichern(voreingestellterPfad)
        } else {
            xmlAbspeichernOhneFileChooser(aktuellerSpeicherpfad)
        }
        return aktuellerSpeicherpfad
    }

    fun laden(pfad: String) {
        val tmp = XMLLeser()
        tmp.ladeXML(pfad, this)
        setzeAktuellerSpeicherpfad(pfad)
        rueckgaengigListeInitialisieren()
        rueckgaengigPunktSetzen(false)
        posInRueckgaengigListeWoZuletztGespeichert = 0
        zeichenbereichAktualisieren()
        zeichne()
    }

    private fun laden(document: Document) {
        val tmp = XMLLeser()
        tmp.ladeXML(document, this)
        tabbedpane?.titelFuerStruktogrammBearbeitetMarkieren(this, posInRueckgaengigListeWoZuletztGespeichert != posInRueckgaengigListe)
        zeichenbereichAktualisieren()
        zeichne()
    }

    fun xmlErstellen(): Document {
        val element = Element("struktogramm")
        element.setAttribute("fontfamily", XMLLeser.encodeS(fontStr.family))
            .setAttribute("fontstyle", "${fontStr.style}")
            .setAttribute("fontsize", "${fontStr.size}")
            .setAttribute("caption", XMLLeser.encodeS(struktogrammBeschreibung))
        val myDocument = Document(element)
        liste.schreibeXMLDatenAllerUnterElemente(element)
        return myDocument
    }

    @Throws(XmlDecodeException::class)
    fun toSimulationDocument(): SimulationDocument = SimulationDocumentJdom.fromStruktogrammDocument(xmlErstellen())

    fun xmlErstellen(wurzelElement: StruktogrammElement): Document? {
        if (wurzelElement !is LeerElement) {
            val element = Element("struktogrammelement")
            val myDocument = Document(element)
            wurzelElement.schreibeXMLDaten(element)
            return myDocument
        }
        return null
    }

    private fun saveFileChooser(struktogrammFilterNummern: IntArray, voreingestellterPfad: String, bildExport: Boolean): String {
        val chooser = JFileChooser()
        chooser.dialogType = JFileChooser.SAVE_DIALOG
        if (bildExport) {
            chooser.isAcceptAllFileFilterUsed = false
        }
        for (num in struktogrammFilterNummern) {
            chooser.addChoosableFileFilter(StrFileFilter(num))
        }
        if (struktogrammFilterNummern.isNotEmpty()) {
            chooser.fileFilter = StrFileFilter(struktogrammFilterNummern[0])
        }
        chooser.currentDirectory = ermittleChooserStartVerzeichnis(voreingestellterPfad)
        if (!bildExport) {
            if (aktuellerSpeicherpfad.isNotEmpty()) {
                val cur = File(aktuellerSpeicherpfad)
                val par = cur.parentFile
                if (par != null && par.isDirectory) {
                    chooser.currentDirectory = par
                }
                chooser.selectedFile = cur
            } else {
                var dir = chooser.currentDirectory
                if (dir == null || !dir.isDirectory) {
                    dir = File(System.getProperty("user.home", "."))
                }
                var vorschlag = GlobalSettings.STANDARD_SPEICHERDATEI
                if (vorgeschlagenerSpeicherBasisname.isNotEmpty()) {
                    vorschlag = "$vorgeschlagenerSpeicherBasisname.visustruct"
                }
                chooser.selectedFile = File(dir, vorschlag)
            }
        }
        val parent = gui()
        chooserParentInDenVordergrund(parent)
        val returnVal = chooser.showSaveDialog(parent)
        var pfad = ""
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            pfad = chooser.selectedFile.absolutePath
            pfad = if (chooser.fileFilter is StrFileFilter) {
                (chooser.fileFilter as StrFileFilter).erweiterungBeiBedarfAnhaengen(pfad)
            } else if (!bildExport) {
                StrFileFilter.haengeStandardSpeicherendungAnFallsNoetig(pfad)
            } else {
                pfad
            }
            if (File(pfad).exists()) {
                val options = arrayOf(I18n.tr("dialog.overwriteFile.overwrite"), I18n.tr("dialog.overwriteFile.skip"))
                if (0 != JOptionPane.showOptionDialog(
                        gui(),
                        I18n.trf("dialog.overwriteFile.message", pfad),
                        I18n.tr("dialog.overwriteFile.title"),
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[1],
                    )
                ) {
                    pfad = ""
                }
            }
        }
        return pfad
    }

    private fun xmlSpeichern(voreingestellterPfad: String) {
        var pfad = if (aktuellerSpeicherpfad.isNotEmpty()) aktuellerSpeicherpfad else voreingestellterPfad
        pfad = saveFileChooser(
            intArrayOf(
                StrFileFilter.filterStruktogrammStudio,
                StrFileFilter.filterLegacyStrk,
                2,
                StrFileFilter.filterAlleSpeicherdateien,
            ),
            pfad,
            false,
        )
        if (pfad.isNotEmpty()) {
            setzeAktuellerSpeicherpfad(pfad)
            xmlAbspeichernOhneFileChooser(pfad)
        }
    }

    private fun xmlAbspeichernOhneFileChooser(pfad: String?) {
        if (pfad.isNullOrEmpty()) {
            JOptionPane.showMessageDialog(
                gui(),
                I18n.tr("dialog.saveNoPath.message"),
                I18n.tr("dialog.saveNoPath.title"),
                JOptionPane.WARNING_MESSAGE,
            )
            return
        }
        try {
            val ziel = File(pfad)
            val eltern = ziel.parentFile
            if (eltern != null && !eltern.isDirectory && !eltern.mkdirs()) {
                throw IOException("Verzeichnis konnte nicht angelegt werden: ${eltern.absolutePath}")
            }
            val myDocument = xmlErstellen()
            val outputter = XMLOutputter()
            outputter.format = Format.getPrettyFormat()
            OutputStreamWriter(FileOutputStream(ziel), StandardCharsets.UTF_8).use { writer: Writer ->
                outputter.output(myDocument, writer)
            }
            tabbedpane?.titelFuerStruktogrammBearbeitetMarkieren(this, false)
            posInRueckgaengigListeWoZuletztGespeichert = posInRueckgaengigListe
        } catch (e: Exception) {
            LOG.log(Level.SEVERE, "XML speichern fehlgeschlagen: $pfad", e)
            JOptionPane.showMessageDialog(
                gui(),
                I18n.trf("dialog.saveError.message", e.message),
                I18n.tr("dialog.saveError.title"),
                JOptionPane.ERROR_MESSAGE,
            )
        }
    }

    private fun mausBewegt(x: Int, y: Int, vorschauFuerNeuesElement: Boolean) {
        letzteDiagrammMausKoords = Point(x, y)
        if (sperre == 0) {
            vorschauMarkierungAnzeigen(x, y, vorschauFuerNeuesElement)
            zeichne()
            sperre = sperreAktualisierung
        } else {
            sperre--
        }
    }

    override fun mouseMoved(e: MouseEvent) {
        val p = viewKoordsZuLogisch(e)
        mausBewegt(p.x, p.y, false)
    }

    override fun mouseDragged(e: MouseEvent) {
        val p = viewKoordsZuLogisch(e)
        mausBewegt(p.x, p.y, false)
    }

    override fun mouseExited(e: MouseEvent) {}

    override fun mouseEntered(e: MouseEvent) {}

    override fun mouseReleased(e: MouseEvent) {}

    override fun mousePressed(e: MouseEvent) {}

    override fun mouseClicked(e: MouseEvent) {
        val log = viewKoordsZuLogisch(e)
        letzteDiagrammMausKoords = Point(log.x, log.y)
        if (SwingUtilities.isLeftMouseButton(e)) {
            einfuegeZielSetzen(log.x, log.y)
            vorschauMarkierungAnzeigen(log.x, log.y, false)
            elementAnPosBefuellen(log.x, log.y)
            zeichne()
        } else if (SwingUtilities.isRightMouseButton(e)) {
            popupMenueZeigen(e.x, e.y)
        }
    }

    private fun einfuegeZielSetzen(x: Int, y: Int) {
        ausgewaehlteEinfuegeKoords = Point(x, y)
        val zielElement = liste.gibElementAnPos(x, y, false) as StruktogrammElement?
        if (zielElement != null) {
            ausgewaehlteEinfuegeMarkierung = zielElement.gibVorschauRect(ausgewaehlteEinfuegeKoords!!)
            return
        }
        val zielListe = liste.gibElementAnPos(x, y, true) as StruktogrammElementListe?
        if (zielListe != null) {
            ausgewaehlteEinfuegeMarkierung = Rectangle(zielListe.gibRectangle())
            return
        }
        ausgewaehlteEinfuegeMarkierung = null
    }

    private fun einfuegeMarkierungLoeschen() {
        rectVorschau = null
        ausgewaehlteEinfuegeKoords = null
        ausgewaehlteEinfuegeMarkierung = null
    }

    private fun elementAuswaehlen(element: StruktogrammElement?) {
        if (markiertesElement != null && markiertesElement !== element) {
            markiertesElement!!.setzeMarkiert(false)
        }
        markiertesElement = element
        markiertesElement?.setzeMarkiert(true)
        gui()?.gibElementEditorPanel()?.setSelectedElement(this, element)
    }

    override fun dragGestureRecognized(evt: DragGestureEvent) {
        val mausPos = bildschirmKoordZuStruktogrammKoord(evt.dragOrigin)
        dragZwischenlagerListe = liste.gibElementAnPos(mausPos.x, mausPos.y, true) as StruktogrammElementListe?
        if (dragZwischenlagerListe != null) {
            dragZwischenlagerElement = dragZwischenlagerListe!!.gibElementAnPos(mausPos.x, mausPos.y, false) as StruktogrammElement?
            if (dragZwischenlagerElement != null && dragZwischenlagerElement !is LeerElement && dragSource != null) {
                val t: Transferable = StringSelection("z")
                dragSource!!.startDrag(evt, DragSource.DefaultCopyDrop, t, this)
            } else {
                dragZwischenlagerElement = null
            }
        }
    }

    override fun dragEnter(evt: DragSourceDragEvent) {}

    override fun dragOver(evt: DragSourceDragEvent) {}

    override fun dragExit(evt: DragSourceEvent) {}

    override fun dropActionChanged(evt: DragSourceDragEvent) {}

    override fun dragDropEnd(evt: DragSourceDropEvent) {
        dragZwischenlagerElement = null
        zeichne()
    }

    override fun drop(event: DropTargetDropEvent) {
        try {
            event.acceptDrop(event.sourceActions)
            val tr = event.transferable
            val dragTyp = tr.getTransferData(tr.transferDataFlavors[0]) as String
            when (dragTyp[0]) {
                'n' -> {
                    val typ = "${dragTyp[1]}".toInt()
                    val mausPos = bildschirmKoordZuStruktogrammKoord(event.location)
                    gezogenesElementEinfuegen(mausPos.x, mausPos.y, typ)
                }

                'z' -> {
                    val mausPos = bildschirmKoordZuStruktogrammKoord(event.location)
                    elementAusZwischenlagerEinfuegen(mausPos.x, mausPos.y)
                }

                'k' -> {
                    val mausPos = bildschirmKoordZuStruktogrammKoord(event.location)
                    elementAusKopierFeldEinfuegen(mausPos.x, mausPos.y)
                }
            }
            event.dropComplete(true)
        } catch (e: Exception) {
            LOG.log(Level.WARNING, "Drop auf Struktogramm fehlgeschlagen", e)
            event.rejectDrop()
        }
    }

    override fun dragExit(evt: DropTargetEvent) {
        entmarkieren()
        zeichne()
    }

    override fun dropActionChanged(evt: DropTargetDragEvent) {}

    override fun dragEnter(evt: DropTargetDragEvent) {}

    override fun dragOver(evt: DropTargetDragEvent) {
        val mausPos = bildschirmKoordZuStruktogrammKoord(evt.location)
        mausBewegt(mausPos.x, mausPos.y, true)
    }

    fun bildschirmKoordZuStruktogrammKoord(bildschirmKoord: Point): Point {
        val scrollpanePoint = parent.location
        val lx = bildschirmKoord.x - scrollpanePoint.x
        val ly = bildschirmKoord.y - scrollpanePoint.y
        return viewKoordsZuLogisch(lx, ly)
    }

    fun zoom(xMinusEinsNullOderEins: Int, yMinusEinsNullOderEins: Int, tmp: StruktogrammElement) {
        tmp.zoomX(GlobalSettings.getXZoomProSchritt() * xMinusEinsNullOderEins)
        tmp.zoomY(GlobalSettings.getYZoomProSchritt() * yMinusEinsNullOderEins)
        zeichenbereichAktualisieren()
        zeichne()
        tabbedpane?.titelFuerStruktogrammBearbeitetMarkieren(this, true)
    }

    fun zoomsZuruecksetzen() {
        rueckgaengigPunktSetzen(true)
        liste.zoomsAllerElementeZuruecksetzen()
        zeichenbereichAktualisieren()
        zeichne()
        rueckgaengigPunktSetzen(false)
    }

    fun getFontStr(): Font = fontStr

    fun setFontStr(fontStr: Font) {
        this.fontStr = fontStr
    }

    companion object {
        private val LOG = Logger.getLogger(Struktogramm::class.java.name)
        private const val serialVersionUID = 8269048981647964473L
        private const val sperreAktualisierung = 0
        private const val CANVAS_ZOOM_MIN = 0.4
        private const val CANVAS_ZOOM_MAX = 2.8
        private const val CANVAS_ZOOM_STEP = 1.12
        private const val randLinks = 28
        private const val randOben = 28
        private const val diagramOuterMargin = 16
        private const val captionRandHorizontal = 24

        const val typAnweisung = 0

        const val typVerzweigung = 1

        const val typFallauswahl = 2

        const val typForSchleife = 3

        const val typWhileSchleife = 4

        const val typDoUntilSchleife = 5

        const val typEndlosschleife = 6

        const val typAussprung = 7

        const val typAufruf = 8

        const val typLeerElement = 9

        @JvmStatic
        fun strElementZuTypnummer(str: StruktogrammElement): Int =
            when (str) {
                is Verzweigung -> typVerzweigung
                is Fallauswahl -> typFallauswahl
                is ForSchleife -> typForSchleife
                is WhileSchleife -> typWhileSchleife
                is DoUntilSchleife -> typDoUntilSchleife
                is Endlosschleife -> typEndlosschleife
                is Aussprung -> typAussprung
                is Aufruf -> typAufruf
                is LeerElement -> typLeerElement
                is Anweisung -> typAnweisung
                else -> -1
            }

        private fun applyCanvasRenderingHints(g2: Graphics2D) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
        }

        private fun sanitizeDateiBasisname(s: String?): String {
            if (s == null) {
                return ""
            }
            var t = s.trim()
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (t.length > 120) {
                t = t.substring(0, 120)
            }
            return t
        }

        private fun ermittleChooserStartVerzeichnis(pfadHinweis: String?): File {
            if (pfadHinweis.isNullOrEmpty()) {
                return File(System.getProperty("user.home", "."))
            }
            var f = File(pfadHinweis)
            try {
                f = f.canonicalFile
            } catch (_: IOException) {
            }
            if (f.isDirectory) {
                return f
            }
            if (f.isFile) {
                val eltern = f.parentFile
                if (eltern != null && eltern.isDirectory) {
                    return eltern
                }
            }
            val eltern = f.parentFile
            if (eltern != null && eltern.isDirectory) {
                return eltern
            }
            return File(System.getProperty("user.home", "."))
        }

        private fun chooserParentInDenVordergrund(parent: Component?) {
            var w: Window? = null
            if (parent != null) {
                w = SwingUtilities.getWindowAncestor(parent)
            }
            if (w != null) {
                w.toFront()
                w.requestFocus()
            }
        }

        private fun imageFormatFuerImageIO(pfad: String): String {
            val dot = pfad.lastIndexOf('.')
            if (dot < 0 || dot >= pfad.length - 1) {
                return "png"
            }
            return when (pfad.substring(dot + 1).lowercase()) {
                "jpeg", "jpg" -> "jpg"
                "png", "gif", "bmp" -> pfad.substring(dot + 1).lowercase()
                else -> "png"
            }
        }

        @JvmStatic
        fun oeffnenDialog(voreingestellterOrdnerpfad: String?, parentComponent: Component?): String {
            val chooser = JFileChooser()
            chooser.fileFilter = StrFileFilter(StrFileFilter.filterAlleSpeicherdateien)
            chooser.currentDirectory = ermittleChooserStartVerzeichnis(voreingestellterOrdnerpfad)
            chooserParentInDenVordergrund(parentComponent)
            val returnVal = chooser.showOpenDialog(parentComponent)
            var pfad = ""
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                pfad = chooser.selectedFile.absolutePath
            }
            return pfad
        }
    }
}

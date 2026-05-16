package de.visustruct.view

import com.formdev.flatlaf.extras.FlatSVGIcon
import de.visustruct.control.Controlling
import de.visustruct.i18n.I18n
import org.jdom2.Document
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Point
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
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JViewport
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager

class AuswahlPanel(private val controlling: Controlling) :
    JPanel(),
    DropTargetListener,
    DragGestureListener,
    DragSourceListener {

    private val panelElemente = arrayOfNulls<AuswahlPanelElement>(9)
    private var paletteCodeGenButton: JButton? = null
    private var paletteSimulationButton: JButton? = null
    private var paletteInfoButton: JButton? = null
    private val dragSource: DragSource = DragSource()
    private lateinit var muelleimer: JButton
    private var muelleimerIstAuf = false
    private var kopiertesStrElement: Document? = null
    private var paletteDragAktiv = false
    private var letzterPaletteDragEndeZeitpunkt = 0L

    init {
        layout = GridBagLayout()
        isOpaque = true
        val pal = UIManager.getColor(VisuStructTheme.KEY_PALETTE_BACKGROUND)
        background = pal ?: UIManager.getColor("Panel.background")
        var sep: Color? = UIManager.getColor("Separator.foreground")
        if (sep == null) {
            sep = Color(0xD1, 0xD5, 0xDB)
        }
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, sep),
            BorderFactory.createEmptyBorder(8, 10, 8, 12),
        )

        var c = GridBagConstraints().apply {
            gridwidth = 1
            gridheight = 1
            weightx = 1.0
            weighty = 0.0
            ipadx = 1
            ipady = 1
            anchor = GridBagConstraints.NORTH
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(1, 0, 3, 0)
            gridx = 0
            gridy = 0
            weightx = 1.0
        }

        for (i in panelElemente.indices) {
            val typ = StruktogrammPalette.TYPEN_REIHENFOLGE[i]
            val pel = AuswahlPanelElement(typ).apply {
                addActionListener { paletteElementGeklickt(typ) }
            }
            panelElemente[i] = pel
            add(pel, c)
            dragSource.createDefaultDragGestureRecognizer(
                pel,
                DnDConstants.ACTION_COPY_OR_MOVE,
                this@AuswahlPanel,
            )
            c.gridy++
        }

        c.insets = Insets(12, 0, 8, 0)
        c.fill = GridBagConstraints.HORIZONTAL
        val paletteBlockTrenner = JSeparator(SwingConstants.HORIZONTAL)
        add(paletteBlockTrenner, c)
        c.gridy++

        c.insets = Insets(2, 0, 4, 0)
        c.fill = GridBagConstraints.HORIZONTAL
        muelleimer = paletteTrashButton()
        muelleimerIstAuf = true
        muelleimerAuf(!muelleimerIstAuf)
        muelleimerTooltipSetzen()
        add(muelleimer, c)

        c.gridy++
        paletteSimulationButton =
            paletteAktionsButton(I18n.tr("menu.edit.simulation")).apply {
                wendePaletteAktionsIconAn(this, "circle-play")
                toolTipText = I18n.tr("palette.simulation.tooltip")
                accessibleContext.accessibleName = I18n.tr("menu.edit.simulation")
                addActionListener { controlling.toggleSimulationFromUi() }
            }
        add(paletteSimulationButton, c)

        c.gridy++
        paletteCodeGenButton =
            paletteAktionsButton(I18n.tr("palette.generateCode")).apply {
                wendePaletteAktionsIconAn(this, "code")
                toolTipText = I18n.tr("menu.file.generateCode")
                accessibleContext.accessibleName = I18n.tr("palette.generateCode")
                addActionListener {
                    val str = controlling.gibAktuellesStruktogramm() ?: return@addActionListener
                    CodeErzeuger(
                        controlling.getGUI(),
                        I18n.tr("menu.file.generateCode"),
                        true,
                        str,
                    )
                }
            }
        add(paletteCodeGenButton, c)

        c.gridy++
        c.insets = Insets(1, 0, 3, 0)
        paletteInfoButton =
            paletteAktionsButton(I18n.tr("palette.aboutVisuStruct")).apply {
                wendePaletteAktionsIconAn(this, "info")
                toolTipText = I18n.tr("palette.aboutTooltip")
                accessibleContext.accessibleName = I18n.tr("palette.aboutVisuStruct")
                addActionListener { controlling.showInfo() }
            }
        add(paletteInfoButton, c)
        c.gridy++

        c.weighty = 1000.0
        c.fill = GridBagConstraints.VERTICAL
        add(Box.createVerticalGlue(), c)

        DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this, true)
    }

    private fun paletteElementGeklickt(typ: Int) {
        if (paletteDragAktiv || System.currentTimeMillis() - letzterPaletteDragEndeZeitpunkt < 500L) {
            return
        }
        controlling.paletteElementEinfuegen(typ)
    }

    private companion object {
        val LOG: Logger = Logger.getLogger(AuswahlPanel::class.java.name)
        private const val serialVersionUID: Long = 3619714917985247680L

        fun paletteAktionsButton(text: String): JButton =
            JButton(text).apply {
                isFocusable = false
                font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
                PaletteButtonStyle.apply(this)
            }

        /** Lucide-Icons (Swift/SF Symbols: play.circle, code, info.circle). */
        fun wendePaletteAktionsIconAn(b: JButton, lucideDateiname: String) {
            val icon = FlatSVGIcon("icons/lucide/$lucideDateiname.svg", 18, 18)
            b.icon = icon
            b.margin = Insets(4, 10, 4, 10)
            b.verticalTextPosition = SwingConstants.CENTER
            b.horizontalTextPosition = SwingConstants.RIGHT
            b.horizontalAlignment = SwingConstants.LEFT
            b.iconTextGap = 10
        }

        fun paletteTrashBaseColor(): Color {
            var c: Color? = UIManager.getColor("Objects.Red")
            if (c != null) {
                return c
            }
            c = UIManager.getColor("Component.error.focusedRingColor")
            if (c != null) {
                return c
            }
            return Color(0xC4, 0x2B, 0x1E)
        }

        fun erzeugeMuelleimerIcon(hervorgehoben: Boolean): FlatSVGIcon {
            val icon = FlatSVGIcon("icons/lucide/trash-2.svg", 22, 22)
            val base = paletteTrashBaseColor()
            val use = if (hervorgehoben) base.brighter() else base
            icon.setColorFilter(FlatSVGIcon.ColorFilter { _ -> use })
            return icon
        }
    }

    /** Löschen: wie andere Paletten-Aktionen als Button, rot für destruktive Aktion. */
    private fun paletteTrashButton(): JButton =
        JButton(I18n.tr("palette.deleteElement")).apply {
            isFocusable = false
            font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
            PaletteButtonStyle.apply(this)
            foreground = paletteTrashBaseColor()
            iconTextGap = 10
            horizontalAlignment = SwingConstants.CENTER
            verticalTextPosition = SwingConstants.CENTER
            horizontalTextPosition = SwingConstants.RIGHT
            margin = Insets(6, 10, 6, 10)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            accessibleContext.accessibleName = I18n.tr("palette.deleteElement")
            addActionListener {
                val str = controlling.gibAktuellesStruktogramm()
                str?.markiertesElementLoeschen()
            }
        }

    fun aktualisiereBeschriftungen() {
        val sim = controlling.isSimulationMode()
        for (el in panelElemente) {
            el!!.isEnabled = !sim
        }
        muelleimer.text = I18n.tr("palette.deleteElement")
        muelleimer.accessibleContext.accessibleName = I18n.tr("palette.deleteElement")
        muelleimer.foreground = paletteTrashBaseColor()
        muelleimerTooltipSetzen()
        muelleimerIconZumAktuellenZustand()
        muelleimer.isEnabled = !sim
        paletteSimulationButton?.let { b ->
            val hasTab = controlling.getGUI().gibTabbedpane().tabCount > 0
            b.isEnabled = hasTab
            b.text = if (sim) I18n.tr("menu.edit.diagramMode") else I18n.tr("menu.edit.simulation")
            b.toolTipText = I18n.tr("palette.simulation.tooltip")
            b.accessibleContext.accessibleName =
                if (sim) {
                    I18n.tr("menu.edit.diagramMode")
                } else {
                    I18n.tr("menu.edit.simulation")
                }
        }
        paletteCodeGenButton?.apply {
            isEnabled = !sim
            text = I18n.tr("palette.generateCode")
            toolTipText = I18n.tr("menu.file.generateCode")
            accessibleContext.accessibleName = I18n.tr("palette.generateCode")
        }
        paletteInfoButton?.apply {
            isEnabled = !sim
            text = I18n.tr("palette.aboutVisuStruct")
            toolTipText = I18n.tr("palette.aboutTooltip")
            accessibleContext.accessibleName = I18n.tr("palette.aboutVisuStruct")
        }
        for (el in panelElemente) {
            el!!.aktualisiereBeschriftung()
        }
        for (ch in components) {
            if (ch is JComponent) {
                ch.revalidate()
            }
        }
        revalidate()
        repaint()
        val vp = SwingUtilities.getAncestorOfClass(JViewport::class.java, this) as? JViewport
        vp?.run {
            revalidate()
            repaint()
        }
        val sc = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this) as? JScrollPane
        sc?.run {
            revalidate()
            repaint()
        }
    }

    fun setzeKopiertesStrElement(doc: Document?) {
        kopiertesStrElement = doc
    }

    fun gibKopiertesStrElement(): Document? = kopiertesStrElement

    private fun muelleimerTooltipSetzen() {
        muelleimer.toolTipText =
            "<html>" +
                I18n.tr("palette.trashDrop") +
                "<br>" +
                I18n.tr("palette.trashClick") +
                "</html>"
    }

    private fun muelleimerAuf(oeffnen: Boolean) {
        if (muelleimerIstAuf != oeffnen) {
            muelleimerIstAuf = oeffnen
            muelleimer.icon = erzeugeMuelleimerIcon(oeffnen)
        }
    }

    private fun muelleimerIconZumAktuellenZustand() {
        muelleimer.icon = erzeugeMuelleimerIcon(muelleimerIstAuf)
    }

    fun kopiereGanzesStruktogramm() {
        val str = controlling.gibAktuellesStruktogramm() ?: return
        setzeKopiertesStrElement(str.xmlErstellen())
    }

    // Methoden Drag ausgelöst
    // http://www.java2s.com/Code/Java/Swing-JFC/MakingaComponentDraggable.htm
    override fun dragGestureRecognized(evt: DragGestureEvent) {
        val quelle = evt.component

        if (quelle is AuswahlPanelElement) {
            val typ = quelle.gibTyp()

            val t: Transferable = StringSelection("n$typ")

            paletteDragAktiv = true
            dragSource.startDrag(evt, DragSource.DefaultCopyDrop, t, this)
        }
    }

    override fun dragEnter(evt: DragSourceDragEvent?) {
    }

    override fun dragOver(evt: DragSourceDragEvent?) {
    }

    override fun dragExit(evt: DragSourceEvent) {
    }

    override fun dropActionChanged(evt: DragSourceDragEvent?) {
    }

    override fun dragDropEnd(evt: DragSourceDropEvent?) {
        paletteDragAktiv = false
        letzterPaletteDragEndeZeitpunkt = System.currentTimeMillis()
        PaletteButtonStyle.clearPressedArmedState(evt!!.dragSourceContext.component)
    }

    // Drop empfangen
    // http://www.java2s.com/Code/Java/Swing-JFC/PanelDropTarget.htm
    override fun drop(event: DropTargetDropEvent) {
        try {
            event.acceptDrop(event.sourceActions)

            val tr = event.transferable
            val dragTyp = tr.getTransferData(tr.transferDataFlavors[0]).toString()

            val dropUeberComponent = getComponentAt(bildschirmKoordZuLokalenKoord(event.location))
            val str = controlling.gibAktuellesStruktogramm()

            if (dragTyp[0] == 'z' && dropUeberComponent === muelleimer && str != null) {
                str.elementAusZwischenlagerGanzEntfernen()
                str.zeichenbereichAktualisieren()
                str.zeichne()
                str.rueckgaengigPunktSetzen()
            }

            muelleimerAuf(false)
            PaletteButtonStyle.clearPressedArmedState(muelleimer)
            event.dropComplete(true)
        } catch (e: Exception) {
            LOG.log(Level.WARNING, "Drop auf AuswahlPanel fehlgeschlagen", e)
            muelleimerAuf(false)
            PaletteButtonStyle.clearPressedArmedState(muelleimer)
            event.rejectDrop()
        }
    }

    override fun dragExit(evt: DropTargetEvent) {
        muelleimerAuf(false)
        PaletteButtonStyle.clearPressedArmedState(muelleimer)
    }

    override fun dropActionChanged(evt: DropTargetDragEvent?) {
    }

    override fun dragEnter(evt: DropTargetDragEvent?) {
    }

    override fun dragOver(evt: DropTargetDragEvent) {
        val tmp = getComponentAt(bildschirmKoordZuLokalenKoord(evt.location))
        muelleimerAuf(tmp === muelleimer)
    }

    // die Drag & Drop Methoden liefern Mauskoordinaten für den ganzen Bildschirm, hier werden sie zu Koordinaten des AuswahlPanel konvertiert
    // siehe: http://www.tutego.de/java/articles/Absolute-Koordinaten-Swing-Element.html
    fun bildschirmKoordZuLokalenKoord(bildschirmKoord: Point): Point {
        return Point(bildschirmKoord.x - x, bildschirmKoord.y - y)
    }
}

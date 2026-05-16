package de.visustruct.control

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import de.visustruct.i18n.I18n
import de.visustruct.other.XActionCommands
import de.visustruct.simulation.SimulationEngine
import de.visustruct.simulation.codec.XmlDecodeException
import de.visustruct.view.CodeErzeuger
import de.visustruct.view.EinstellungsDialog
import de.visustruct.view.FontChooser
import de.visustruct.view.GUI
import de.visustruct.view.SimulationEinstellungenDialog
import de.visustruct.view.UiTheme
import de.visustruct.view.ZoomEinstellungen
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.awt.image.BufferedImage
import java.awt.print.Printable
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.min
import javax.swing.JCheckBoxMenuItem
import javax.swing.JEditorPane
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.LookAndFeel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException
import javax.swing.event.HyperlinkEvent
import javax.swing.plaf.metal.MetalLookAndFeel
import javax.swing.plaf.nimbus.NimbusLookAndFeel

class Controlling(params: Array<String>?) : Konstanten, ActionListener, WindowListener, KeyListener {

    private lateinit var gui: GUI
    private var simulationMode = false

    private enum class Betriebssysteme {
        Windows,
        Mac,
        Linux,
    }

    init {
        handleOSSettingsAndTheme()
        gui = GUI(this)
        SwingUtilities.invokeLater { aktualisierePalettenBeschriftungen() }
        neuesStruktogramm()
        if (params != null) {
            for (path in params) {
                if (File(path).exists()) {
                    openStruktogramm(path)
                }
            }
        }
    }

    fun handleOSSettingsAndTheme() {
        try {
            applyConfiguredTheme()
            if (getOS() == Betriebssysteme.Mac) {
                MacHandler(this)
            }
        } catch (e: Exception) {
            LOG.log(Level.SEVERE, "Theme/OS-Initialisierung fehlgeschlagen", e)
        }
    }

    /** Setzt das gewählte Theme (über Swing [UIManager.setLookAndFeel]) und wendet [UiTheme] an. */
    private fun applyConfiguredTheme() {
        try {
            var lookAndFeel: LookAndFeel? = null

            when (GlobalSettings.getLookAndFeelAktuell()) {
                Konstanten.lookAndFeelOSStandard -> {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                    } catch (e1: ClassNotFoundException) {
                        LOG.log(Level.WARNING, "System-Look-and-Feel konnte nicht gesetzt werden", e1)
                    } catch (e1: InstantiationException) {
                        LOG.log(Level.WARNING, "System-Look-and-Feel konnte nicht gesetzt werden", e1)
                    } catch (e1: IllegalAccessException) {
                        LOG.log(Level.WARNING, "System-Look-and-Feel konnte nicht gesetzt werden", e1)
                    } catch (e1: UnsupportedLookAndFeelException) {
                        LOG.log(Level.WARNING, "System-Look-and-Feel konnte nicht gesetzt werden", e1)
                    }
                }

                Konstanten.lookAndFeelFlatLight -> {
                    try {
                        if (getOS() == Betriebssysteme.Mac) {
                            UIManager.setLookAndFeel(FlatMacLightLaf())
                        } else {
                            UIManager.setLookAndFeel(FlatLightLaf())
                        }
                    } catch (e: UnsupportedLookAndFeelException) {
                        LOG.log(Level.WARNING, "FlatLight-Look-and-Feel konnte nicht gesetzt werden", e)
                    }
                }

                Konstanten.lookAndFeelFlatDark -> {
                    try {
                        if (getOS() == Betriebssysteme.Mac) {
                            UIManager.setLookAndFeel(FlatMacDarkLaf())
                        } else {
                            UIManager.setLookAndFeel(FlatDarkLaf())
                        }
                    } catch (e: UnsupportedLookAndFeelException) {
                        LOG.log(Level.WARNING, "FlatDark-Look-and-Feel konnte nicht gesetzt werden", e)
                    }
                }

                Konstanten.lookAndFeelNimbus -> {
                    lookAndFeel = NimbusLookAndFeel()
                }

                Konstanten.lookAndFeelSwingStandard -> {
                    lookAndFeel = MetalLookAndFeel()
                }
            }

            if (lookAndFeel != null) {
                try {
                    UIManager.setLookAndFeel(lookAndFeel)
                } catch (e: UnsupportedLookAndFeelException) {
                    LOG.log(Level.WARNING, "Look-and-Feel (Metal/Nimbus) konnte nicht gesetzt werden", e)
                }
            }

            UiTheme.applyAfterTheme()
            I18n.applyFileChooserStrings()
        } catch (e: Exception) {
            LOG.log(Level.SEVERE, "Look-and-Feel-Konfiguration fehlgeschlagen", e)
        }
    }

    private fun getOS(): Betriebssysteme {
        val s = System.getProperty("os.name").lowercase()
        return when {
            s.startsWith("windows") -> Betriebssysteme.Windows
            s.startsWith("mac") -> Betriebssysteme.Mac
            s.startsWith("linux") -> Betriebssysteme.Linux
            else -> Betriebssysteme.Windows
        }
    }

    fun gibAktuellesStruktogramm(): Struktogramm? {
        if (!::gui.isInitialized) {
            return null
        }
        return gui.gibTabbedpane().gibAktuellesStruktogramm()
    }

    fun paletteElementEinfuegen(typ: Int) {
        val str = gibAktuellesStruktogramm()
        if (str != null) {
            str.neuesElementAnAktuellerStelleEinfuegen(typ)
            gui.gibTabbedpane().requestFocusInWindow()
        }
    }

    fun neuesStruktogramm(): Struktogramm {
        val str = gui.gibTabbedpane().struktogrammHinzufuegen()
        str.graphicsInitialisieren()
        str.zeichenbereichAktualisieren()
        str.zeichne()
        return str
    }

    fun speichern(neuenSpeicherpfadAuswaehlenLassen: Boolean) {
        val str = gibAktuellesStruktogramm() ?: return
        flushPendingDiagramEdits()
        val saveTask = Runnable {
            GlobalSettings.setzeSpeicherpfad(
                str.speichern(
                    neuenSpeicherpfadAuswaehlenLassen,
                    GlobalSettings.getZuletztGenutzterSpeicherpfad(),
                ),
            )
            val p = str.gibAktuellenSpeicherpfad()
            if (p.isNotEmpty()) {
                GlobalSettings.rememberRecentStruktogrammPath(p)
            }
            GlobalSettings.saveSettings()
            titelleisteAktualisieren()
            gui.rebuildMenuBar()
        }
        if (neuenSpeicherpfadAuswaehlenLassen || str.gibAktuellenSpeicherpfad().isEmpty()) {
            SwingUtilities.invokeLater(saveTask)
        } else {
            saveTask.run()
        }
    }

    /** Editor-Text ins Diagramm übernehmen (wie vor Simulation), damit Speichern den sichtbaren Stand schreibt. */
    private fun flushPendingDiagramEdits() {
        gui.gibElementEditorPanel().applyPendingTextToDiagram()
    }

    fun laden() {
        val pfad = Struktogramm.oeffnenDialog(GlobalSettings.getZuletztGenutzterSpeicherpfad(), gui)
        if (pfad != "") {
            openStruktogramm(pfad)
        }
    }

    private fun openStruktogramm(pfad: String) {
        val str = neuesStruktogramm()
        str.graphicsInitialisieren()
        str.laden(pfad)
        GlobalSettings.setzeSpeicherpfad(pfad)
        GlobalSettings.rememberRecentStruktogrammPath(pfad)
        GlobalSettings.saveSettings()
        titelleisteAktualisieren()
        gui.rebuildMenuBar()
    }

    /** Öffnet eine gespeicherte Datei aus dem Menü „Zuletzt geöffnet“. */
    fun oeffneStruktogrammAusZuletztListe(pfad: String?) {
        if (pfad == null || pfad.isBlank()) {
            return
        }
        val f = File(pfad)
        if (!f.isFile) {
            GlobalSettings.removeRecentStruktogrammPath(pfad)
            GlobalSettings.saveSettings()
            gui.rebuildMenuBar()
            JOptionPane.showMessageDialog(
                gui,
                I18n.trf("dialog.recentMissing.message", pfad),
                I18n.tr("dialog.recentMissing.title"),
                JOptionPane.WARNING_MESSAGE,
            )
            return
        }
        openStruktogramm(pfad)
    }

    /** Für macOS (OpenFilesHandler): Struktogramm aus Pfad öffnen, sobald die GUI steht. */
    fun oeffneStruktogrammDateiAusFinder(pfad: String?) {
        if (pfad != null && File(pfad).exists()) {
            openStruktogramm(pfad)
        }
    }

    fun bildSpeichern() {
        val str = gibAktuellesStruktogramm()
        if (str != null) {
            GlobalSettings.setzeBildSpeicherpfad(
                str.alsBilddateiSpeichern(GlobalSettings.getZuletztGenutzterPfadFuerBild()),
            )
            GlobalSettings.saveSettings()
        }
    }

    fun bildDrucken() {
        val str = gibAktuellesStruktogramm() ?: return

        val image = str.generateImage(false)
        val job = PrinterJob.getPrinterJob()
        job.jobName = GlobalSettings.APP_DISPLAY_NAME
        job.setPrintable(
            Printable { graphics, pageFormat, pageIndex ->
                if (pageIndex > 0) {
                    return@Printable Printable.NO_SUCH_PAGE
                }
                val g2 = graphics.create() as Graphics2D
                try {
                    var scale =
                        min(
                            pageFormat.imageableWidth / image.width,
                            pageFormat.imageableHeight / image.height,
                        )
                    scale = min(scale, 1.0)
                    val x =
                        pageFormat.imageableX +
                            (pageFormat.imageableWidth - image.width * scale) / 2.0
                    val y =
                        pageFormat.imageableY +
                            (pageFormat.imageableHeight - image.height * scale) / 2.0
                    g2.translate(x, y)
                    g2.scale(scale, scale)
                    g2.drawImage(image, 0, 0, null)
                } finally {
                    g2.dispose()
                }
                Printable.PAGE_EXISTS
            },
        )

        if (!job.printDialog()) {
            return
        }
        try {
            job.print()
        } catch (ex: PrinterException) {
            JOptionPane.showMessageDialog(gui, ex.message, I18n.tr("menu.file.print"), JOptionPane.ERROR_MESSAGE)
        }
    }

    fun titelleisteAktualisieren() {
        var pfad = ""
        val str = gibAktuellesStruktogramm()
        if (str != null) {
            pfad = str.gibAktuellenSpeicherpfad()
            if (pfad != "") {
                pfad = " [$pfad]"
            }
        }
        gui.title = GlobalSettings.guiTitel + pfad
    }

    override fun actionPerformed(e: ActionEvent) {
        val raw = e.actionCommand ?: return
        val cmd = try {
            XActionCommands.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            return
        }

        when (cmd) {
            XActionCommands.neu -> neuesStruktogramm()
            XActionCommands.oeffnen -> laden()
            XActionCommands.speichern -> speichern(false)
            XActionCommands.speicherUnter -> speichern(true)
            XActionCommands.bildSpeichern -> bildSpeichern()
            XActionCommands.bildDrucken -> bildDrucken()
            XActionCommands.bildInZwischenAblage ->
                copyImagetoClipBoard(gibAktuellesStruktogramm()!!.generateImage(false))

            XActionCommands.quellcodeErzeugen -> {
                flushPendingDiagramEdits()
                CodeErzeuger(gui, I18n.tr("menu.file.generateCode"), true, gibAktuellesStruktogramm()!!)
            }

            XActionCommands.struktogrammSchliessen -> gui.gibTabbedpane().aktuellesStruktogrammschliessen()
            XActionCommands.programmBeenden -> programmBeendenGeklickt()
            XActionCommands.rueckgaengig -> gibAktuellesStruktogramm()!!.schrittZurueck()
            XActionCommands.widerrufen -> gibAktuellesStruktogramm()!!.schrittNachVorne()
            XActionCommands.ganzesStruktogrammKopieren -> gui.gibAuswahlPanel().kopiereGanzesStruktogramm()
            XActionCommands.letztesElementStrecken -> letzteElementeStreckenGeklickt(e.source)
            XActionCommands.elementBeschriftungEinstellen -> EinstellungsDialog(gui, true)
            XActionCommands.simulationEinstellen -> SimulationEinstellungenDialog(gui, true)
            XActionCommands.schriftartAendern -> FontChooser(this, true)
            XActionCommands.zoomeinstellungen -> ZoomEinstellungen(gui)
            XActionCommands.vergroesserungenRuckgaengigMachen -> gibAktuellesStruktogramm()!!.zoomsZuruecksetzen()
            XActionCommands.elementShortcutsVerwenden -> elementEinfuegenShortcutsVerwendenGeklickt(e.source)
            XActionCommands.info -> showInfo()
            XActionCommands.lookAndFeelOSStandard -> changeTheme(Konstanten.lookAndFeelOSStandard)
            XActionCommands.lookAndFeelSwingStandard -> changeTheme(Konstanten.lookAndFeelSwingStandard)
            XActionCommands.lookAndFeelNimbus -> changeTheme(Konstanten.lookAndFeelNimbus)
            XActionCommands.lookAndFeelFlatLight -> changeTheme(Konstanten.lookAndFeelFlatLight)
            XActionCommands.lookAndFeelFlatDark -> changeTheme(Konstanten.lookAndFeelFlatDark)
            XActionCommands.languageEnglish -> changeUiLanguageIfNeeded("en")
            XActionCommands.languageGerman -> changeUiLanguageIfNeeded("de")
            XActionCommands.languagePortuguesePortugal -> changeUiLanguageIfNeeded("pt_PT")
            XActionCommands.struktogrammbeschreibungHinzufuegen -> addStruktogrammbeschriftung()
            XActionCommands.simulationToggle -> onSimulationToggle()
        }
    }

    /** UI-Sprache wechseln (Menü oder Einstellungsdialog). */
    fun applyUiLanguageChange(tag: String) {
        changeUiLanguageIfNeeded(tag)
    }

    private fun changeUiLanguageIfNeeded(tag: String) {
        val next = GlobalSettings.normalizeUiLanguageTag(tag)
        if (next == GlobalSettings.getUiLanguageTag()) {
            return
        }
        GlobalSettings.setUiLanguageTag(next)
        I18n.syncWithSettings()
        GlobalSettings.saveSettings()
        gui.rebuildMenuBar()
        SwingUtilities.updateComponentTreeUI(gui)
        gui.validate()
        I18n.applyFileChooserStrings()
        aktualisierePalettenBeschriftungen()
        SwingUtilities.invokeLater { aktualisierePalettenBeschriftungen() }
    }

    private fun aktualisierePalettenBeschriftungen() {
        if (!::gui.isInitialized) {
            return
        }
        gui.gibAuswahlPanel().aktualisiereBeschriftungen()
    }

    private fun addStruktogrammbeschriftung() {
        val str = gibAktuellesStruktogramm()
        val s = JOptionPane.showInputDialog(gui, I18n.tr("dialog.diagramCaption"), str!!.getStruktogrammBeschreibung())
        if (s == null) {
            return
        }
        str.setStruktogrammBeschreibung(s)
        str.rueckgaengigPunktSetzen()
        str.zeichenbereichAktualisieren()
        str.zeichne()
    }

    private fun changeTheme(themeIndex: Int) {
        GlobalSettings.setLookAndFeelAktuell(themeIndex)
        GlobalSettings.saveSettings()
        applyConfiguredTheme()
        SwingUtilities.updateComponentTreeUI(gui)
        gui.validate()
        I18n.applyFileChooserStrings()
        aktualisierePalettenBeschriftungen()
        SwingUtilities.invokeLater { aktualisierePalettenBeschriftungen() }
        gui.gibTabbedpane().refreshAllStruktogrammeNachThemeWechsel()
    }

    fun showInfo() {
        val projektUrl = "https://github.com/code4teaching/VisuStruct"
        val webUrl = "https://www.visustruct.org"
        val developerUrl = "https://www.sebastiao.org"
        val originalUrl = "https://github.com/kekru/struktogrammeditor"
        val lucideUrl = "https://lucide.dev/"

        val html =
            "<html><body style=\"font-family:sans-serif;font-size:11pt;\">" +
                "<p style=\"margin-top:0;margin-bottom:10px;\"><b>" +
                GlobalSettings.APP_DISPLAY_NAME + " " + GlobalSettings.versionsString + "</b></p>" +
                "<p style=\"margin-top:0;\">Holger Sebastiao<br/>" +
                "Web: <a href=\"" + webUrl + "\">www.visustruct.org</a><br/>" +
                "GitHub: <a href=\"" + projektUrl + "\">code4teaching/VisuStruct</a><br/>" +
                "Developer: <a href=\"" + developerUrl + "\">www.sebastiao.org</a></p>" +
                "<hr style=\"border:0;border-top:1px solid #ccc;\"/>" +
                "<p style=\"margin-bottom:6px;\"><b>Acknowledgement</b></p>" +
                "<p style=\"margin-top:0;\"><b>VisuStruct</b> builds on the open-source structure chart editor by Kevin Krummenauer (MIT).<br/>" +
                "Prior source: <a href=\"" + originalUrl + "\">kekru/struktogrammeditor</a> on GitHub.</p>" +
                "<p style=\"margin-top:0;\">Palette icons: <a href=\"" + lucideUrl + "\">Lucide Icons</a> " +
                "(ISC License; some icons derived from Feather/MIT).<br/>" +
                "License text is included in <code>licenses/LUCIDE.txt</code>.</p>" +
                "</body></html>"

        val pane = JEditorPane("text/html", html)
        pane.isEditable = false
        pane.isOpaque = false
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
        pane.addHyperlinkListener { ev ->
            if (ev.eventType != HyperlinkEvent.EventType.ACTIVATED || ev.url == null) {
                return@addHyperlinkListener
            }
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                return@addHyperlinkListener
            }
            try {
                Desktop.getDesktop().browse(ev.url.toURI())
            } catch (_: Exception) {
                // ohne Browser keine Aktion
            }
        }

        val scroll = JScrollPane(pane)
        scroll.border = null
        scroll.viewport.isOpaque = false
        scroll.preferredSize = Dimension(440, 260)

        val title = "Information - " + GlobalSettings.APP_DISPLAY_NAME + " " + GlobalSettings.versionsString
        JOptionPane.showMessageDialog(gui, scroll, title, JOptionPane.INFORMATION_MESSAGE)
    }

    private fun letzteElementeStreckenGeklickt(source: Any) {
        GlobalSettings.setzeLetzteElementeStrecken((source as JCheckBoxMenuItem).isSelected)
        GlobalSettings.saveSettings()
        gibAktuellesStruktogramm()!!.zeichenbereichAktualisieren()
        gibAktuellesStruktogramm()!!.zeichne()
    }

    private fun elementEinfuegenShortcutsVerwendenGeklickt(source: Any) {
        val einOderAus = (source as JCheckBoxMenuItem).isSelected
        GlobalSettings.setElementShortcutsVerwenden(einOderAus)
        GlobalSettings.saveSettings()
    }

    fun getGUI(): GUI = gui

    /** Wechsel des Diagramm-Tabs beendet die Simulation (analog iOS-Moduswechsel). */
    fun onStruktogrammTabChanged() {
        if (simulationMode) {
            endSimulationInternal()
        }
    }

    /** Beendet die Simulation (Menü, Tab-Wechsel, Panel-Button „Zurück“). */
    fun leaveSimulationMode() {
        if (simulationMode) {
            endSimulationInternal()
        }
    }

    fun isSimulationMode(): Boolean = simulationMode

    /** Gleiche Aktion wie Bearbeiten → Simulation… / Diagramm bearbeiten… (Paletten-Button). */
    fun toggleSimulationFromUi() {
        onSimulationToggle()
    }

    private fun onSimulationToggle() {
        if (simulationMode) {
            endSimulationInternal()
        } else {
            enterSimulation()
        }
    }

    private fun enterSimulation() {
        if (gui.gibTabbedpane().tabCount <= 0) {
            return
        }
        flushPendingDiagramEdits()
        val str = gibAktuellesStruktogramm() ?: return
        try {
            val doc = str.toSimulationDocument()
            val eng = SimulationEngine(doc)
            simulationMode = true
            gui.getSimulationPanel().setEngine(eng)
            gui.showSimulationCard()
            gui.setEditSimulationMenuText(I18n.tr("menu.edit.diagramMode"))
            aktualisierePalettenBeschriftungen()
        } catch (ex: XmlDecodeException) {
            JOptionPane.showMessageDialog(gui, ex.message, I18n.tr("simulation.error.title"), JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun endSimulationInternal() {
        simulationMode = false
        gui.showEditorCard()
        gui.getSimulationPanel().clearEngine()
        gui.setEditSimulationMenuText(I18n.tr("menu.edit.simulation"))
        val str = gibAktuellesStruktogramm()
        str?.setzeSimulationSpotlightPfad(null)
        aktualisierePalettenBeschriftungen()
    }

    fun programmBeendenGeklickt(): Boolean {
        leaveSimulationMode()
        if (gui.gibTabbedpane().einOderMehrereStruktogrammeNichtGespeichert()) {
            val options = arrayOf(
                I18n.tr("dialog.exitUnsaved.quit"),
                I18n.tr("dialog.exitUnsaved.stay"),
            )
            val r = JOptionPane.showOptionDialog(
                gui,
                I18n.tr("dialog.exitUnsaved.message"),
                I18n.tr("dialog.exitUnsaved.title"),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1],
            )
            if (r == 0) {
                System.exit(0)
                return true
            }
            return false
        }
        System.exit(0)
        return true
    }

    override fun windowOpened(e: WindowEvent) {}

    override fun windowClosing(e: WindowEvent) {
        programmBeendenGeklickt()
    }

    override fun windowClosed(e: WindowEvent) {}

    override fun windowIconified(e: WindowEvent) {}

    override fun windowDeiconified(e: WindowEvent) {}

    override fun windowActivated(e: WindowEvent) {}

    override fun windowDeactivated(e: WindowEvent) {}

    override fun keyTyped(e: KeyEvent) {}

    override fun keyPressed(e: KeyEvent) {}

    override fun keyReleased(e: KeyEvent) {
        if (e.source == gui.gibTabbedpane() && (e.modifiersEx and GlobalSettings.strgOderApfelMask) == 0) {
            when (e.keyCode) {
                KeyEvent.VK_A ->
                    if (GlobalSettings.isElementShortcutsVerwenden()) {
                        gibAktuellesStruktogramm()!!
                            .neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typAnweisung)
                    }

                KeyEvent.VK_I ->
                    if (GlobalSettings.isElementShortcutsVerwenden()) {
                        gibAktuellesStruktogramm()!!
                            .neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typVerzweigung)
                    }

                KeyEvent.VK_S ->
                    if (GlobalSettings.isElementShortcutsVerwenden()) {
                        gibAktuellesStruktogramm()!!
                            .neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typFallauswahl)
                    }

                KeyEvent.VK_F ->
                    if (GlobalSettings.isElementShortcutsVerwenden()) {
                        gibAktuellesStruktogramm()!!
                            .neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typForSchleife)
                    }

                KeyEvent.VK_W ->
                    if (GlobalSettings.isElementShortcutsVerwenden()) {
                        gibAktuellesStruktogramm()!!
                            .neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typWhileSchleife)
                    }

                KeyEvent.VK_D ->
                    if (GlobalSettings.isElementShortcutsVerwenden()) {
                        gibAktuellesStruktogramm()!!
                            .neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typDoUntilSchleife)
                    }

                KeyEvent.VK_E ->
                    if (GlobalSettings.isElementShortcutsVerwenden()) {
                        gibAktuellesStruktogramm()!!
                            .neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typEndlosschleife)
                    }

                KeyEvent.VK_B ->
                    if (GlobalSettings.isElementShortcutsVerwenden()) {
                        gibAktuellesStruktogramm()!!
                            .neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typAussprung)
                    }

                KeyEvent.VK_M ->
                    if (GlobalSettings.isElementShortcutsVerwenden()) {
                        gibAktuellesStruktogramm()!!
                            .neuesElementAnAktuellerStelleEinfuegen(Struktogramm.typAufruf)
                    }

                KeyEvent.VK_DELETE ->
                    gibAktuellesStruktogramm()!!.elementAnAktuellerStelleLoeschen()
            }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(Controlling::class.java.name)

        // http://stackoverflow.com/questions/4552045/copy-bufferedimage-to-clipboard
        @JvmStatic
        fun copyImagetoClipBoard(image: BufferedImage?) {
            val transferable =
                object : Transferable {
                    @Throws(UnsupportedFlavorException::class, IOException::class)
                    override fun getTransferData(flavor: DataFlavor): Any {
                        if (flavor == DataFlavor.imageFlavor && image != null) {
                            return image
                        }
                        throw UnsupportedFlavorException(flavor)
                    }

                    override fun getTransferDataFlavors(): Array<DataFlavor> =
                        arrayOf(DataFlavor.imageFlavor)

                    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
                        val flavors = transferDataFlavors
                        for (f in flavors) {
                            if (flavor == f) {
                                return true
                            }
                        }
                        return false
                    }
                }

            val clipboardOwner =
                ClipboardOwner { _: Clipboard?, _: Transferable? -> }

            Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, clipboardOwner)
        }
    }
}

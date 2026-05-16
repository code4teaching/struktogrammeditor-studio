package de.visustruct.view

import de.visustruct.control.Controlling
import de.visustruct.control.GlobalSettings
import de.visustruct.control.Konstanten
import de.visustruct.control.Struktogramm
import de.visustruct.i18n.I18n
import de.visustruct.other.XActionCommands
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.ItemEvent
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.AbstractAction
import javax.swing.ActionMap
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.ImageIcon
import javax.swing.InputMap
import javax.swing.JCheckBoxMenuItem
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JRadioButtonMenuItem
import javax.swing.JRootPane
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JSplitPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.WindowConstants

/** Hauptfenster von VisuStruct (basiert auf Struktogrammeditor). */
class GUI(private val controlling: Controlling) : JFrame(GlobalSettings.guiTitel), Konstanten {

    private companion object {
        private const val serialVersionUID = -3526840402506170333L
    }

    private val auswahlPanel: AuswahlPanel
    private val tabbedpane: StrTabbedPane
    private val elementEditorPanel: ElementEditorPanel
    private val simulationPanel: SimulationPanel
    private val editorSplit: JSplitPane
    /** Während Simulation: Platzhalter im oberen Teil (TabbedPane liegt im Simulations-Split). */
    private val editorDiagramPlaceholder = JPanel()
    /** Während Simulation: unterer Teil des Editor-Splits (Inspektor ist ausgeparkt). */
    private val editorInspectorPlaceholder = JPanel()
    /** Hält den [ElementEditorPanel] ohne Anzeige im Fenster, solange die Simulation aktiv ist. */
    private val inspectorParking = JPanel(BorderLayout())
    /** Linker Bereich der Simulations-Ansicht (Diagramm). */
    private val simulationLeftHost = JPanel(BorderLayout())
    private lateinit var simulationDiagramSplit: JSplitPane
    private lateinit var simulationCard: JPanel
    private lateinit var centerStack: JPanel
    private val centerCardLayout = CardLayout()
    private var editSimulationMenuItem: JMenuItem? = null
    private val menubar: JMenuBar

    init {
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        val frameWidth = 1180
        val frameHeight = 700
        setSize(frameWidth, frameHeight)
        val d = Toolkit.getDefaultToolkit().screenSize
        val x = (d.width - size.width) / 2
        val y = (d.height - size.height) / 2
        setLocation(x, y)

        layout = BorderLayout()

        val logoUrl = javaClass.getResource(GlobalSettings.logoName)
        if (logoUrl != null) {
            iconImage = ImageIcon(logoUrl).image
        }

        tabbedpane = StrTabbedPane(controlling)
        elementEditorPanel = ElementEditorPanel()

        auswahlPanel = AuswahlPanel(controlling)

        val paletteScroll = JScrollPane(auswahlPanel)
        paletteScroll.border = BorderFactory.createEmptyBorder()
        val vp = UIManager.getColor(VisuStructTheme.KEY_PALETTE_BACKGROUND)
        paletteScroll.viewport.background = vp ?: UIManager.getColor("Panel.background")
        editorSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedpane, elementEditorPanel)
        editorSplit.resizeWeight = 1.0
        editorSplit.dividerLocation = 460
        editorSplit.dividerSize = 5
        editorSplit.isContinuousLayout = true
        editorSplit.isOneTouchExpandable = true
        editorSplit.border = BorderFactory.createEmptyBorder()

        val editorCard = JPanel(BorderLayout())
        editorCard.add(editorSplit, BorderLayout.CENTER)

        simulationPanel = SimulationPanel(controlling)
        simulationDiagramSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, simulationLeftHost, simulationPanel)
        simulationDiagramSplit.resizeWeight = 0.68
        simulationDiagramSplit.dividerSize = 6
        simulationDiagramSplit.isContinuousLayout = true
        simulationDiagramSplit.isOneTouchExpandable = true
        simulationDiagramSplit.border = BorderFactory.createEmptyBorder()

        simulationCard = JPanel(BorderLayout())
        simulationCard.add(simulationDiagramSplit, BorderLayout.CENTER)

        centerStack = JPanel(centerCardLayout)
        centerStack.add(editorCard, "editor")
        centerStack.add(simulationCard, "simulation")

        val splitpane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, paletteScroll, centerStack)
        splitpane.isOneTouchExpandable = true
        splitpane.dividerLocation = 288
        splitpane.dividerSize = 5
        splitpane.isContinuousLayout = true
        splitpane.border = BorderFactory.createEmptyBorder()
        add(splitpane, BorderLayout.CENTER)

        menubar = JMenuBar()
        buildMenuBar()
        jMenuBar = menubar

        installRootPaneCanvasZoomShortcuts()

        addWindowListener(controlling)

        isResizable = true
        isVisible = true
    }

    /** Baut die Menüleiste aus den aktuellen I18n-Texten (z. B. nach Sprachwechsel). */
    fun rebuildMenuBar() {
        menubar.removeAll()
        buildMenuBar()
        menubar.revalidate()
        simulationPanel.refreshLocalizedTexts()
        elementEditorPanel.refreshLocalizedTexts()
    }

    private fun buildMenuBar() {
        var menu = createMenu(I18n.tr("menu.file"), KeyEvent.VK_F)
        menu.add(createMenuItem(I18n.tr("menu.file.new"), XActionCommands.neu, KeyEvent.VK_N, KeyEvent.VK_N))
        menu.add(createMenuItem(I18n.tr("menu.file.open"), XActionCommands.oeffnen, KeyEvent.VK_O, KeyEvent.VK_O))
        appendRecentDiagramSubmenu(menu)
        menu.add(JSeparator())
        menu.add(createMenuItem(I18n.tr("menu.file.save"), XActionCommands.speichern, KeyEvent.VK_S, KeyEvent.VK_S))
        menu.add(createMenuItem(I18n.tr("menu.file.saveAs"), XActionCommands.speicherUnter, KeyEvent.VK_A))
        menu.add(JSeparator())
        menu.add(createMenuItem(I18n.tr("menu.file.saveImage"), XActionCommands.bildSpeichern, KeyEvent.VK_I))
        menu.add(createMenuItem(I18n.tr("menu.file.print"), XActionCommands.bildDrucken, KeyEvent.VK_P, KeyEvent.VK_P))
        menu.add(
            createMenuItem(
                I18n.tr("menu.file.copyImage"),
                XActionCommands.bildInZwischenAblage,
                KeyEvent.VK_B,
                KeyEvent.VK_K,
            ),
        )
        menu.add(JSeparator())
        menu.add(
            createMenuItem(I18n.tr("menu.file.generateCode"), XActionCommands.quellcodeErzeugen, KeyEvent.VK_G),
        )
        menu.add(JSeparator())
        menu.add(
            createMenuItem(
                I18n.tr("menu.file.closeDiagram"),
                XActionCommands.struktogrammSchliessen,
                KeyEvent.VK_C,
                KeyEvent.VK_W,
            ),
        )
        menu.add(JSeparator())
        menu.add(createMenuItem(I18n.tr("menu.file.about"), XActionCommands.info, KeyEvent.VK_B))
        menu.add(JSeparator())
        menu.add(createMenuItem(I18n.tr("menu.file.exit"), XActionCommands.programmBeenden, KeyEvent.VK_X))
        menubar.add(menu)

        menu = createMenu(I18n.tr("menu.edit"), KeyEvent.VK_E)
        menu.add(createMenuItem(I18n.tr("menu.edit.undo"), XActionCommands.rueckgaengig, KeyEvent.VK_U, KeyEvent.VK_Z))
        menu.add(createMenuItem(I18n.tr("menu.edit.redo"), XActionCommands.widerrufen, KeyEvent.VK_R, KeyEvent.VK_Y))
        menu.add(JSeparator())
        menu.add(
            createMenuItem(
                I18n.tr("menu.edit.caption"),
                XActionCommands.struktogrammbeschreibungHinzufuegen,
                KeyEvent.VK_T,
            ),
        )
        menu.add(JSeparator())
        menu.add(
            createMenuItem(I18n.tr("menu.edit.copyDiagram"), XActionCommands.ganzesStruktogrammKopieren, KeyEvent.VK_Y),
        )
        menu.add(JSeparator())
        val simLabel =
            if (controlling.isSimulationMode()) I18n.tr("menu.edit.diagramMode") else I18n.tr("menu.edit.simulation")
        editSimulationMenuItem = createMenuItem(simLabel, XActionCommands.simulationToggle, KeyEvent.VK_I)
        menu.add(editSimulationMenuItem)
        menubar.add(menu)

        menu = createMenu(I18n.tr("menu.settings"), KeyEvent.VK_S)
        menu.add(
            createMenuItem(
                I18n.tr("menu.settings.stretch"),
                XActionCommands.letztesElementStrecken,
                KeyEvent.VK_L,
                GlobalSettings.gibLetzteElementeStrecken(),
            ),
        )
        menu.add(JSeparator())

        var menu2 = createMenu(I18n.tr("menu.settings.theme"), KeyEvent.VK_T)
        run {
            val group = ButtonGroup()

            var radioMenuitem = JRadioButtonMenuItem(I18n.tr("menu.settings.theme.modernLight"))
            radioMenuitem.addActionListener(controlling)
            radioMenuitem.actionCommand = XActionCommands.lookAndFeelFlatLight.toString()
            radioMenuitem.isSelected = GlobalSettings.getLookAndFeelAktuell() == Konstanten.lookAndFeelFlatLight
            group.add(radioMenuitem)
            menu2.add(radioMenuitem)

            radioMenuitem = JRadioButtonMenuItem(I18n.tr("menu.settings.theme.modernDark"))
            radioMenuitem.addActionListener(controlling)
            radioMenuitem.actionCommand = XActionCommands.lookAndFeelFlatDark.toString()
            radioMenuitem.isSelected = GlobalSettings.getLookAndFeelAktuell() == Konstanten.lookAndFeelFlatDark
            group.add(radioMenuitem)
            menu2.add(radioMenuitem)

            menu2.add(JSeparator())

            radioMenuitem = JRadioButtonMenuItem(I18n.tr("menu.settings.theme.osDefault"))
            radioMenuitem.addActionListener(controlling)
            radioMenuitem.actionCommand = XActionCommands.lookAndFeelOSStandard.toString()
            radioMenuitem.isSelected = GlobalSettings.getLookAndFeelAktuell() == Konstanten.lookAndFeelOSStandard
            group.add(radioMenuitem)
            menu2.add(radioMenuitem)

            radioMenuitem = JRadioButtonMenuItem(I18n.tr("menu.settings.theme.swingDefault"))
            radioMenuitem.addActionListener(controlling)
            radioMenuitem.actionCommand = XActionCommands.lookAndFeelSwingStandard.toString()
            radioMenuitem.isSelected = GlobalSettings.getLookAndFeelAktuell() == Konstanten.lookAndFeelSwingStandard
            group.add(radioMenuitem)
            menu2.add(radioMenuitem)

            radioMenuitem = JRadioButtonMenuItem(I18n.tr("menu.settings.theme.nimbus"))
            radioMenuitem.addActionListener(controlling)
            radioMenuitem.actionCommand = XActionCommands.lookAndFeelNimbus.toString()
            radioMenuitem.isSelected = GlobalSettings.getLookAndFeelAktuell() == Konstanten.lookAndFeelNimbus
            group.add(radioMenuitem)
            menu2.add(radioMenuitem)
        }
        menu.add(menu2)

        val langMenu = createMenu(I18n.tr("menu.settings.languages"), KeyEvent.VK_I)
        run {
            val langGroup = ButtonGroup()
            val langItems = mutableListOf<JRadioButtonMenuItem>()
            for (opt in GlobalSettings.UI_LANGUAGE_OPTIONS) {
                val item = JRadioButtonMenuItem(I18n.tr(opt.menuLabelKey))
                wireUiLanguageMenuItem(item, opt.tag)
                langGroup.add(item)
                langMenu.add(item)
                langItems.add(item)
            }
            selectCurrentUiLanguageMenuItem(langItems)
        }
        menu.add(langMenu)

        menu.add(
            createMenuItem(
                I18n.tr("menu.settings.labelsStruktogramm"),
                XActionCommands.elementBeschriftungEinstellen,
                KeyEvent.VK_B,
            ),
        )
        menu.add(
            createMenuItem(
                I18n.tr("menu.settings.simulation"),
                XActionCommands.simulationEinstellen,
                KeyEvent.VK_M,
            ),
        )
        menu.add(
            createMenuItem(I18n.tr("menu.settings.changeFont"), XActionCommands.schriftartAendern, KeyEvent.VK_F),
        )
        menu.add(JSeparator())
        menu.add(createMenuItem(I18n.tr("menu.settings.zoom"), XActionCommands.zoomeinstellungen, KeyEvent.VK_Z))
        menu.add(
            createMenuItem(
                I18n.tr("menu.settings.resetSizes"),
                XActionCommands.vergroesserungenRuckgaengigMachen,
                KeyEvent.VK_R,
            ),
        )
        menu.add(JSeparator())
        menu.add(
            createMenuItem(
                I18n.tr("menu.settings.shortcuts"),
                XActionCommands.elementShortcutsVerwenden,
                KeyEvent.VK_K,
                GlobalSettings.isElementShortcutsVerwenden(),
            ),
        )
        menubar.add(menu)
    }

    private fun selectCurrentUiLanguageMenuItem(items: List<JRadioButtonMenuItem>) {
        val tag = GlobalSettings.getUiLanguageTag()
        for (i in items.indices) {
            if (i < GlobalSettings.UI_LANGUAGE_OPTIONS.size &&
                GlobalSettings.UI_LANGUAGE_OPTIONS[i].tag == tag
            ) {
                items[i].isSelected = true
                return
            }
        }
        if (items.isNotEmpty()) {
            items[0].isSelected = true
        }
    }

    /** macOS-Screen-Menü: [JRadioButtonMenuItem] löst oft kein [ActionEvent] aus — [ItemEvent.SELECTED] nutzen. */
    private fun wireUiLanguageMenuItem(item: JRadioButtonMenuItem, uiLanguageTag: String) {
        item.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                controlling.applyUiLanguageChange(uiLanguageTag)
            }
        }
    }

    private fun createMenu(name: String, auswahlBuchstabe: Int): JMenu {
        val neuMenu = JMenu(name)
        neuMenu.setMnemonic(auswahlBuchstabe)
        return neuMenu
    }

    private fun createMenuItem(name: String, actionCommand: XActionCommands, auswahlBuchstabe: Int): JMenuItem =
        createMenuItem(name, actionCommand, auswahlBuchstabe, -1, -1)

    private fun createMenuItem(
        name: String,
        actionCommand: XActionCommands,
        auswahlBuchstabe: Int,
        shortcutBuchstabe: Int,
    ): JMenuItem =
        createMenuItem(
            name,
            actionCommand,
            auswahlBuchstabe,
            shortcutBuchstabe,
            GlobalSettings.strgOderApfelMask,
            false,
            false,
        )

    private fun createMenuItem(
        name: String,
        actionCommand: XActionCommands,
        auswahlBuchstabe: Int,
        shortcutBuchstabe: Int,
        shortcutMask: Int,
    ): JMenuItem =
        createMenuItem(name, actionCommand, auswahlBuchstabe, shortcutBuchstabe, shortcutMask, false, false)

    private fun createMenuItem(
        name: String,
        actionCommand: XActionCommands,
        auswahlBuchstabe: Int,
        isChecked: Boolean,
    ): JMenuItem =
        createMenuItem(name, actionCommand, auswahlBuchstabe, -1, -1, true, isChecked)

    private fun createMenuItem(
        name: String,
        actionCommand: XActionCommands,
        auswahlBuchstabe: Int,
        shortcutBuchstabe: Int,
        shortcutMask: Int,
        isCheckBox: Boolean,
        isChecked: Boolean,
    ): JMenuItem {
        val menuitem: JMenuItem =
            if (isCheckBox) {
                JCheckBoxMenuItem(name).also { it.isSelected = isChecked }
            } else {
                JMenuItem(name)
            }

        menuitem.actionCommand = actionCommand.toString()

        if (auswahlBuchstabe > -1) {
            menuitem.setMnemonic(auswahlBuchstabe)
        }

        if (shortcutBuchstabe > -1) {
            menuitem.accelerator = KeyStroke.getKeyStroke(shortcutBuchstabe, shortcutMask)
        }

        menuitem.addActionListener(controlling)

        return menuitem
    }

    /** Untermenü „Zuletzt geöffnet“ unter Datei. */
    private fun appendRecentDiagramSubmenu(fileMenu: JMenu) {
        val recent = createMenu(I18n.tr("menu.file.openRecent"), KeyEvent.VK_T)
        val paths = GlobalSettings.getRecentDiagramPaths()
        if (paths.isEmpty()) {
            val empty = JMenuItem(I18n.tr("menu.file.openRecent.empty"))
            empty.isEnabled = false
            recent.add(empty)
        } else {
            for (fullPath in paths) {
                val it = JMenuItem(formatRecentMenuLabel(fullPath))
                it.toolTipText = fullPath
                val fp = fullPath
                it.addActionListener { controlling.oeffneStruktogrammAusZuletztListe(fp) }
                recent.add(it)
            }
        }
        fileMenu.add(recent)
    }

    private fun formatRecentMenuLabel(fullPath: String): String {
        val f = File(fullPath)
        var name = f.name
        if (name.isEmpty()) return fullPath
        var par = f.parent
        if (par.isNullOrEmpty()) return name
        val maxParent = 48
        if (par.length > maxParent) {
            par = "\u2026" + par.substring(par.length - (maxParent - 1))
        }
        return "$name \u2014 $par"
    }

    fun gibTabbedpane(): StrTabbedPane = tabbedpane

    fun gibElementEditorPanel(): ElementEditorPanel = elementEditorPanel

    fun gibAuswahlPanel(): AuswahlPanel = auswahlPanel

    fun getSimulationPanel(): SimulationPanel = simulationPanel

    /** Text des Menüeintrags „Simulation“ / „Diagramm …“. */
    fun setEditSimulationMenuText(text: String) {
        editSimulationMenuItem?.text = text
    }

    fun showEditorCard() {
        if (tabbedpane.parent == simulationLeftHost) {
            simulationLeftHost.remove(tabbedpane)
            editorSplit.topComponent = tabbedpane
        }
        restoreInspectorAfterSimulation()
        centerCardLayout.show(centerStack, "editor")
        editorSplit.revalidate()
        tabbedpane.revalidate()
    }

    fun showSimulationCard() {
        if (tabbedpane.parent == editorSplit) {
            editorSplit.topComponent = editorDiagramPlaceholder
            parkInspectorForSimulation()
            simulationLeftHost.add(tabbedpane, BorderLayout.CENTER)
            simulationLeftHost.revalidate()
        }
        centerCardLayout.show(centerStack, "simulation")
        simulationDiagramSplit.revalidate()
        tabbedpane.revalidate()
        SwingUtilities.invokeLater { simulationDiagramSplit.setDividerLocation(0.68) }
    }

    /** Inspektor aus dem Editor-Split nehmen (kein Platz / keine Anzeige im Simulationsmodus). */
    private fun parkInspectorForSimulation() {
        if (elementEditorPanel.parent == editorSplit) {
            editorSplit.bottomComponent = editorInspectorPlaceholder
            inspectorParking.removeAll()
            inspectorParking.add(elementEditorPanel, BorderLayout.CENTER)
        }
    }

    private fun restoreInspectorAfterSimulation() {
        if (elementEditorPanel.parent == inspectorParking) {
            inspectorParking.remove(elementEditorPanel)
            editorSplit.bottomComponent = elementEditorPanel
        }
    }

    fun getMenubar(): JMenuBar = menubar

    /**
     * Ansichts-Zoom für das aktive Diagramm: ⌘+ / ⌘− (macOS) bzw. Strg+ / Strg− (Windows/Linux),
     * solange das Hauptfenster fokussiert ist.
     */
    private fun installRootPaneCanvasZoomShortcuts() {
        val m = GlobalSettings.strgOderApfelMask
        val root: JRootPane = rootPane
        val im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val am: ActionMap = root.actionMap
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, m), "visuCanvasZoomIn")
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, m), "visuCanvasZoomIn")
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, m), "visuCanvasZoomIn")
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, m or InputEvent.SHIFT_DOWN_MASK), "visuCanvasZoomIn")
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, m), "visuCanvasZoomOut")
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, m), "visuCanvasZoomOut")
        am.put(
            "visuCanvasZoomIn",
            object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val s: Struktogramm? = controlling.gibAktuellesStruktogramm()
                    s?.canvasZoomIn()
                }
            },
        )
        am.put(
            "visuCanvasZoomOut",
            object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    val s: Struktogramm? = controlling.gibAktuellesStruktogramm()
                    s?.canvasZoomOut()
                }
            },
        )
    }
}

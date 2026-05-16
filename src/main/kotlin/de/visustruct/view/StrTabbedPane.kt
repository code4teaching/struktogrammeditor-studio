package de.visustruct.view

import de.visustruct.control.Controlling
import de.visustruct.control.Struktogramm
import de.visustruct.i18n.I18n
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JViewport
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class StrTabbedPane(private val controlling: Controlling) :
    JTabbedPane(TOP, SCROLL_TAB_LAYOUT),
    ChangeListener {

    private companion object {
        private const val serialVersionUID = 1L
        private const val CLIENT_TAB_TITLE_LABEL = "visustruct.tabTitleLabel"
        private const val CLIENT_TAB_DIRTY_LABEL = "visustruct.tabDirtyLabel"
    }

    init {
        border = BorderFactory.createEmptyBorder(8, 6, 10, 10)
        addChangeListener(this)
        addKeyListener(controlling)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val idx = indexAtLocation(e.x, e.y)
                if (idx < 0) return
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    schliesseTab(idx)
                    return
                }
                if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    renameTabAt(idx)
                }
            }

            override fun mousePressed(e: MouseEvent) {
                maybeShowTabPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                maybeShowTabPopup(e)
            }

            private fun maybeShowTabPopup(e: MouseEvent) {
                if (!e.isPopupTrigger) return
                val idx = indexAtLocation(e.x, e.y)
                if (idx < 0) return
                val menu = JPopupMenu()
                val closeItem = JMenuItem(I18n.tr("menu.file.closeDiagram"))
                closeItem.addActionListener { schliesseTab(idx) }
                menu.add(closeItem)
                menu.show(this@StrTabbedPane, e.x, e.y)
            }
        })
    }

    /**
     * Doppelklick auf den Reiter: Namen ändern. Ungespeichert-Kennzeichen (Punkt) bleibt erhalten.
     * Ohne gespeicherte Datei: danach Speicherdialog (Ordner und Dateiname), damit der Speicherort gleich gewählt werden kann.
     */
    private fun renameTabAt(index: Int) {
        val current = getTitleAt(index)
        val dirty = isTabTitleDirty(index)

        var neu = JOptionPane.showInputDialog(
            controlling.getGUI(),
            I18n.tr("dialog.renameTab.message"),
            I18n.tr("dialog.renameTab.title"),
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            current
        ) as? String ?: return
        neu = neu.trim()
        if (neu.isEmpty()) return
        if (neu.length > 200) {
            neu = neu.substring(0, 200)
        }

        setTitleAt(index, neu)
        setTabDirtyUI(index, dirty)
        val str = gibStruktogrammAt(index)
        if (str != null && str.gibAktuellenSpeicherpfad().isEmpty()) {
            str.setVorgeschlagenenSpeicherBasisnamen(neu)
        }
        controlling.titelleisteAktualisieren()

        selectedIndex = index
        if (str != null && str.gibAktuellenSpeicherpfad().isEmpty()) {
            SwingUtilities.invokeLater { controlling.speichern(false) }
        }
    }

    private fun gibStruktogrammAt(index: Int): Struktogramm? {
        if (index < 0 || index >= tabCount) return null
        val c = getComponentAt(index)
        if (c !is JScrollPane) return null
        val view = c.viewport.view
        return view as? Struktogramm
    }

    /** Tab-Index des gegebenen Struktogramms (nicht abhängig vom ausgewählten Reiter). */
    fun indexOfStruktogramm(str: Struktogramm?): Int {
        if (str == null) return -1
        for (i in 0 until tabCount) {
            if (gibStruktogrammAt(i) === str) return i
        }
        return -1
    }

    fun titelFuerStruktogrammSetzen(str: Struktogramm, titel: String?) {
        val i = indexOfStruktogramm(str)
        if (i >= 0 && titel != null) {
            val dirty = isTabTitleDirty(i)
            setTitleAt(i, titel)
            setTabDirtyUI(i, dirty)
        }
    }

    fun titelFuerStruktogrammBearbeitetMarkieren(str: Struktogramm, bearbeitet: Boolean) {
        val i = indexOfStruktogramm(str)
        if (i < 0) return
        var titel = getTitleAt(i)
        if (titel.endsWith("*")) {
            titel = titel.substring(0, titel.length - 1)
            setTitleAt(i, titel)
        }
        if (bearbeitet) {
            if (titel.isEmpty()) {
                setTitleAt(i, I18n.tr("tab.untitled"))
            }
            setTabDirtyUI(i, true)
        } else {
            setTabDirtyUI(i, false)
        }
    }

    fun gibGUI(): GUI = controlling.getGUI()

    fun struktogrammHinzufuegen(): Struktogramm {
        val str = Struktogramm(this)
        val scroll = JScrollPane(str)
        var bc: Color? = UIManager.getColor("Component.borderColor")
        if (bc == null) bc = UIManager.getColor("controlShadow")
        if (bc == null) bc = Color.LIGHT_GRAY
        scroll.border = BorderFactory.createLineBorder(bc, 1, true)
        add(I18n.tr("tab.untitled"), scroll)
        installClosableTabHeader(tabCount - 1)
        selectedIndex = tabCount - 1
        return str
    }

    override fun setTitleAt(index: Int, title: String?) {
        var t = title ?: ""
        if (t.endsWith("*")) {
            t = t.substring(0, t.length - 1)
        }
        super.setTitleAt(index, t)
        val tc = getTabComponentAt(index)
        if (tc is JPanel) {
            val o = tc.getClientProperty(CLIENT_TAB_TITLE_LABEL)
            if (o is JLabel) {
                o.text = t
            }
        }
    }

    /**
     * Reiter mit sichtbarem Titel ([JLabel]) und Schließen-Button (×); Titel bleibt über
     * [setTitleAt] synchron.
     */
    private fun installClosableTabHeader(index: Int) {
        if (index < 0 || index >= tabCount) return
        val tabBar = JPanel(BorderLayout(4, 0))
        tabBar.isOpaque = false

        val dirtyLabel = JLabel("")
        dirtyLabel.isOpaque = false
        dirtyLabel.isVisible = false
        dirtyLabel.border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
        dirtyLabel.font = dirtyLabel.font.deriveFont(Font.BOLD, 12f)
        tabBar.putClientProperty(CLIENT_TAB_DIRTY_LABEL, dirtyLabel)

        val titleLabel = JLabel(getTitleAt(index))
        titleLabel.isOpaque = false
        tabBar.putClientProperty(CLIENT_TAB_TITLE_LABEL, titleLabel)

        val closeLbl = JLabel("\u00D7")
        closeLbl.font = closeLbl.font.deriveFont(Font.BOLD, 15f)
        closeLbl.toolTipText = I18n.tr("tab.close.tooltip")
        closeLbl.accessibleContext.accessibleName = I18n.tr("tab.close.a11y")
        closeLbl.border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
        closeLbl.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        closeLbl.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!SwingUtilities.isLeftMouseButton(e)) return
                e.consume()
                val i = indexOfTabComponent(tabBar)
                if (i >= 0) schliesseTab(i)
            }
        })

        tabBar.add(dirtyLabel, BorderLayout.WEST)
        tabBar.add(titleLabel, BorderLayout.CENTER)
        tabBar.add(closeLbl, BorderLayout.EAST)

        val headerMouse = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger) {
                    val i = indexOfTabComponent(tabBar)
                    if (i >= 0 && selectedIndex != i) {
                        selectedIndex = i
                    }
                }
                maybeTabHeaderPopup(e, tabBar)
            }

            override fun mouseReleased(e: MouseEvent) {
                maybeTabHeaderPopup(e, tabBar)
            }

            override fun mouseClicked(e: MouseEvent) {
                val i = indexOfTabComponent(tabBar)
                if (i < 0) return
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    schliesseTab(i)
                    e.consume()
                    return
                }
                if (e.clickCount == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    renameTabAt(i)
                    e.consume()
                }
            }
        }
        tabBar.addMouseListener(headerMouse)
        titleLabel.addMouseListener(headerMouse)
        dirtyLabel.addMouseListener(headerMouse)

        setTabComponentAt(index, tabBar)
    }

    private fun setTabDirtyUI(index: Int, dirty: Boolean) {
        if (index < 0 || index >= tabCount) return
        val tc = getTabComponentAt(index)
        if (tc !is JPanel) return
        val o = tc.getClientProperty(CLIENT_TAB_DIRTY_LABEL)
        if (o !is JLabel) return
        val dirtyLab = o
        if (dirty) {
            dirtyLab.text = "\u25CF"
            applyDirtyDotForeground(dirtyLab)
            dirtyLab.isVisible = true
            dirtyLab.accessibleContext.accessibleName = I18n.tr("tab.unsaved.a11y")
        } else {
            dirtyLab.text = ""
            dirtyLab.isVisible = false
            dirtyLab.accessibleContext.accessibleName = ""
        }
        tc.revalidate()
    }

    /** Nach Theme-Wechsel: Punktfarbe an aktuelles UI anpassen. */
    fun refreshTabHeaderDirtyAppearance() {
        for (i in 0 until tabCount) {
            if (isTabTitleDirty(i)) {
                val tc = getTabComponentAt(i)
                if (tc is JPanel) {
                    val o = tc.getClientProperty(CLIENT_TAB_DIRTY_LABEL)
                    if (o is JLabel) {
                        applyDirtyDotForeground(o)
                    }
                }
            }
        }
    }

    private fun maybeTabHeaderPopup(e: MouseEvent, tabBar: JPanel) {
        if (!e.isPopupTrigger) return
        val idx = indexOfTabComponent(tabBar)
        if (idx < 0) return
        val menu = JPopupMenu()
        val closeItem = JMenuItem(I18n.tr("menu.file.closeDiagram"))
        closeItem.addActionListener { schliesseTab(idx) }
        menu.add(closeItem)
        menu.show(e.component, e.x, e.y)
    }

    fun titelDerAktuellenSeiteSetzen(titel: String?) {
        val s = gibAktuellesStruktogramm()
        if (s != null) {
            titelFuerStruktogrammSetzen(s, titel)
        }
    }

    fun titelDerAktuellenSeiteAlsBearbeitetOderAlsGespespeichertMarkieren(bearbeitet: Boolean) {
        val dokumentStr = gibAktuellesStruktogramm()
        if (dokumentStr != null) {
            titelFuerStruktogrammBearbeitetMarkieren(dokumentStr, bearbeitet)
        }
    }

    /**
     * Aktuellen Reiter schließen (Menü „Diagramm schließen“) — siehe [schliesseTab].
     */
    fun aktuellesStruktogrammschliessen() {
        schliesseTab(selectedIndex)
    }

    /**
     * Reiter an `index` schließen. Ohne ungespeicherte Änderungen sofort entfernen; sonst wie bisher
     * nachfragen. Nach dem letzten Reiter wird automatisch ein neues Diagramm angelegt.
     *
     * Zusätzlich: Mittelklick auf Reiter, Kontextmenü (rechte Maustaste) → „Diagramm schließen“.
     */
    fun schliesseTab(index: Int) {
        if (index < 0 || index >= tabCount) return
        controlling.leaveSimulationMode()
        if (!isTabTitleDirty(index)) {
            removeTabEnsureMinimum(index)
            return
        }
        selectedIndex = index
        val opts = arrayOf(
            I18n.tr("dialog.saveBeforeClose.save"),
            I18n.tr("dialog.saveBeforeClose.dontSave"),
            I18n.tr("dialog.saveBeforeClose.cancel"),
        )
        val r = JOptionPane.showOptionDialog(
            controlling.getGUI(),
            I18n.tr("dialog.saveBeforeClose.message"),
            I18n.tr("dialog.saveBeforeClose.title"),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            opts,
            opts[2]
        )
        if (r == JOptionPane.CLOSED_OPTION || r == 2) return
        if (r == 0) {
            controlling.speichern(false)
            if (isTabTitleDirty(index)) return
        }
        removeTabEnsureMinimum(index)
    }

    private fun isTabTitleDirty(index: Int): Boolean {
        if (index < 0 || index >= tabCount) return false
        val tc = getTabComponentAt(index)
        if (tc is JPanel) {
            val o = tc.getClientProperty(CLIENT_TAB_DIRTY_LABEL)
            if (o is JLabel && o.isVisible && o.text.isNotEmpty()) return true
        }
        val t = getTitleAt(index)
        return t.isNotEmpty() && t.endsWith("*")
    }

    private fun removeTabEnsureMinimum(index: Int) {
        if (index < 0 || index >= tabCount) return
        removeTabAt(index)
        if (tabCount == 0) {
            controlling.neuesStruktogramm()
        }
        controlling.titelleisteAktualisieren()
    }

    /** Alle geöffneten Diagramme nach Theme-Wechsel neu einfärben und zeichnen. */
    fun refreshAllStruktogrammeNachThemeWechsel() {
        for (i in 0 until tabCount) {
            val str = gibStruktogrammAt(i)
            str?.refreshAfterThemeChange()
        }
        refreshTabHeaderDirtyAppearance()
    }

    fun gibAktuellesStruktogramm(): Struktogramm? {
        if (tabCount > 0) {
            return ((((selectedComponent as JScrollPane).components[0] as JViewport).components[0]) as Struktogramm)
        }
        return null
    }

    override fun stateChanged(e: ChangeEvent) {
        controlling.onStruktogrammTabChanged()
        controlling.titelleisteAktualisieren()
        SwingUtilities.invokeLater { controlling.getGUI().gibAuswahlPanel().aktualisiereBeschriftungen() }
    }

    fun einOderMehrereStruktogrammeNichtGespeichert(): Boolean {
        for (i in 0 until tabCount) {
            if (isTabTitleDirty(i)) return true
        }
        return false
    }

    private fun applyDirtyDotForeground(dirtyLab: JLabel) {
        var c: Color? = UIManager.getColor("Component.accentColor")
        if (c == null) c = UIManager.getColor("TabbedPane.focusColor")
        if (c == null) c = UIManager.getColor("TabbedPane.selectedForeground")
        if (c == null) c = UIManager.getColor("Label.foreground")
        if (c == null) c = Color(0x33, 0x66, 0xCC)
        dirtyLab.foreground = c
    }
}

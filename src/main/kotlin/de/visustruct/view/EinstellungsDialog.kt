package de.visustruct.view

import de.visustruct.control.GlobalSettings
import de.visustruct.i18n.I18n
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Toolkit
import java.text.NumberFormat
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.ListCellRenderer
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/** Dialog „Beschriftung (Struktogramm)“ — nur Textvorlagen für neu eingefügte Blöcke. */
class EinstellungsDialog(gui: GUI, modal: Boolean) :
    JDialog(gui, I18n.tr("menu.settings.labelsStruktogramm"), modal) {

    private val hostGui: GUI = gui

    init {
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        setSize(520, 480)
        val d: Dimension = Toolkit.getDefaultToolkit().screenSize
        val sz = size
        setLocation((d.width - sz.width) / 2, (d.height - sz.height) / 2)
        layout = BorderLayout(10, 10)

        val kopf = JLabel("<html><div style='width:420px'>" + I18n.tr("dialog.elementText.intro") + "</div></html>")
        kopf.border = BorderFactory.createEmptyBorder(4, 8, 0, 8)
        add(kopf, BorderLayout.NORTH)

        var startIdx = GlobalSettings.getElementBeschriftungPresetIndex()
        if (startIdx < 0 || startIdx >= ElementBeschriftungPresets.ANZAHL_PRESETS) {
            startIdx = ElementBeschriftungPresets.PRESET_ENGLISH_JAVA
        }

        val radioPanel = JPanel()
        radioPanel.layout = BoxLayout(radioPanel, BoxLayout.Y_AXIS)
        radioPanel.border = BorderFactory.createTitledBorder(I18n.tr("dialog.elementText.presetSection"))
        val gruppe = ButtonGroup()
        val startDialogPlatz = ElementBeschriftungPresets.dialogPlatzFuerPreset(startIdx)
        val radios = Array(ElementBeschriftungPresets.ANZAHL_PRESETS) { u ->
            val preset = ElementBeschriftungPresets.PRESET_DIALOG_REIHENFOLGE[u]
            val rb = JRadioButton(ElementBeschriftungPresets.getPresetAnzeigename(preset)).apply {
                actionCommand = Integer.toString(u)
                isSelected = (u == startDialogPlatz)
            }
            gruppe.add(rb)
            radioPanel.add(rb)
            radioPanel.add(Box.createVerticalStrut(2))
            rb
        }

        val vorschau = JTextArea()
        vorschau.isEditable = false
        vorschau.lineWrap = true
        vorschau.wrapStyleWord = true
        vorschau.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        vorschau.border = BorderFactory.createEmptyBorder(6, 8, 6, 8)
        vorschau.text = ElementBeschriftungPresets.alsVorschauText(startIdx)

        val vorschauAktualisieren = Runnable {
            var sel = ElementBeschriftungPresets.PRESET_ENGLISH_JAVA
            for (u in radios.indices) {
                if (radios[u].isSelected) {
                    sel = ElementBeschriftungPresets.presetIndexAtDialogPlatz(u)
                    break
                }
            }
            vorschau.text = ElementBeschriftungPresets.alsVorschauText(sel)
        }

        for (rb in radios) {
            rb.addActionListener { vorschauAktualisieren.run() }
        }

        val scroll = JScrollPane(vorschau)
        scroll.border = BorderFactory.createTitledBorder(I18n.tr("dialog.elementText.previewTitle"))

        val mitte = JPanel(BorderLayout(0, 8))
        mitte.border = BorderFactory.createEmptyBorder(0, 8, 0, 8)
        mitte.add(radioPanel, BorderLayout.NORTH)
        mitte.add(scroll, BorderLayout.CENTER)
        add(mitte, BorderLayout.CENTER)

        val unten = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8))
        val abbrechen = JButton(I18n.tr("dialog.elementText.cancel"))
        abbrechen.addActionListener { isVisible = false }
        val ok = JButton(I18n.tr("dialog.elementText.ok"))
        ok.addActionListener { applyAndClose(radios) }
        unten.add(abbrechen)
        unten.add(ok)
        add(unten, BorderLayout.SOUTH)

        isVisible = true
    }

    private fun applyAndClose(radios: Array<JRadioButton>) {
        var sel = ElementBeschriftungPresets.PRESET_ENGLISH_JAVA
        for (u in radios.indices) {
            if (radios[u].isSelected) {
                sel = ElementBeschriftungPresets.presetIndexAtDialogPlatz(u)
                break
            }
        }
        GlobalSettings.wendeElementBeschriftungsPresetAn(sel)
        GlobalSettings.saveSettings()
        SwingUtilities.invokeLater {
            hostGui.gibAuswahlPanel().aktualisiereBeschriftungen()
            hostGui.gibAuswahlPanel().revalidate()
            hostGui.gibAuswahlPanel().repaint()
            SwingUtilities.invokeLater { hostGui.gibAuswahlPanel().aktualisiereBeschriftungen() }
        }
        isVisible = false
    }

    class SimulationSpeedListRenderer : JLabel(), ListCellRenderer<Double> {

        init {
            isOpaque = true
        }

        override fun getListCellRendererComponent(
            list: JList<out Double>,
            value: Double?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            if (isSelected) {
                background = list.selectionBackground
                foreground = list.selectionForeground
            } else {
                background = list.background
                foreground = list.foreground
            }
            text = if (value == null) "" else formatSimulationSpeedLabel(value)
            return this
        }
    }

    companion object {
        private const val serialVersionUID: Long = -6402017961524470279L

        @JvmField
        val anzahlStruktogrammElemente: Int = 10
    }
}

private fun formatSimulationSpeedLabel(seconds: Double): String {
    val nf = NumberFormat.getNumberInstance(GlobalSettings.getUiLocale())
    nf.maximumFractionDigits = 2
    nf.minimumFractionDigits = 0
    return nf.format(seconds) + " " + I18n.tr("dialog.elementText.simulationSpeedUnit")
}

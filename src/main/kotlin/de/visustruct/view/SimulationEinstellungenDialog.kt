package de.visustruct.view

import de.visustruct.control.GlobalSettings
import de.visustruct.i18n.I18n
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Toolkit
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

/** Einstellungen zur Simulation (Geschwindigkeit, Diagramm-Markierung). */
class SimulationEinstellungenDialog(gui: GUI, modal: Boolean) :
    JDialog(gui, I18n.tr("menu.settings.simulation"), modal) {

    private val hostGui: GUI = gui
    private val simulationSpeedCombo: JComboBox<Double>
    private val simulationHighlightLastRadio = JRadioButton()
    private val simulationHighlightNextRadio = JRadioButton()

    init {
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        setSize(440, 220)
        val d = Toolkit.getDefaultToolkit().screenSize
        setLocation((d.width - size.width) / 2, (d.height - size.height) / 2)
        layout = BorderLayout(10, 10)

        simulationSpeedCombo = JComboBox<Double>().apply {
            for (sec in GlobalSettings.SIMULATION_PLAY_DELAY_SECONDS) {
                addItem(sec)
            }
            renderer = EinstellungsDialog.SimulationSpeedListRenderer()
            selectedIndex = GlobalSettings.getSimulationPlayDelayIndex()
        }

        val simulationPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(8, 12, 8, 12)
        }

        val speedRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            add(JLabel(I18n.tr("dialog.elementText.simulationSpeedLabel")))
            add(simulationSpeedCombo)
        }
        simulationPanel.add(speedRow)

        val highlightBlock = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(8, 0, 0, 0)
            add(JLabel(I18n.tr("dialog.elementText.simulationHighlightLabel")))
        }
        ButtonGroup().apply {
            add(simulationHighlightLastRadio)
            add(simulationHighlightNextRadio)
        }
        simulationHighlightLastRadio.text = I18n.tr("dialog.elementText.simulationHighlightLast")
        simulationHighlightNextRadio.text = I18n.tr("dialog.elementText.simulationHighlightNext")
        if (GlobalSettings.isSimulationHighlightNextStep()) {
            simulationHighlightNextRadio.isSelected = true
        } else {
            simulationHighlightLastRadio.isSelected = true
        }
        val highlightRadios = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(simulationHighlightLastRadio)
            add(simulationHighlightNextRadio)
        }
        highlightBlock.add(highlightRadios)
        simulationPanel.add(Box.createVerticalStrut(6))
        simulationPanel.add(highlightBlock)

        add(simulationPanel, BorderLayout.CENTER)

        val unten = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8))
        val abbrechen = JButton(I18n.tr("dialog.elementText.cancel"))
        abbrechen.addActionListener { isVisible = false }
        val ok = JButton(I18n.tr("dialog.elementText.ok"))
        ok.addActionListener { applyAndClose() }
        unten.add(abbrechen)
        unten.add(ok)
        add(unten, BorderLayout.SOUTH)

        isVisible = true
    }

    private fun applyAndClose() {
        val speed = simulationSpeedCombo.selectedItem as? Double
        if (speed != null) {
            GlobalSettings.setSimulationPlayDelaySec(speed)
        }
        GlobalSettings.setSimulationHighlightMode(
            if (simulationHighlightNextRadio.isSelected) {
                GlobalSettings.SimulationHighlightMode.NEXT_STEP
            } else {
                GlobalSettings.SimulationHighlightMode.LAST_EXECUTED
            },
        )
        GlobalSettings.saveSettings()
        SwingUtilities.invokeLater { hostGui.getSimulationPanel().onSimulationSettingsChanged() }
        isVisible = false
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

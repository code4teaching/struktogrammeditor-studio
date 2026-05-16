package de.visustruct.view

import de.visustruct.control.GlobalSettings
import de.visustruct.i18n.I18n
import de.visustruct.other.JNumberField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.WindowConstants

class ZoomEinstellungen(gui: GUI) : JDialog(gui, I18n.tr("dialog.zoom.title"), true) {

    private val numberfieldX: JNumberField
    private val numberfieldY: JNumberField

    init {
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        layout = BorderLayout()

        val shell = JPanel(BorderLayout())
        shell.border = BorderFactory.createEmptyBorder(20, 28, 20, 28)
        add(shell, BorderLayout.CENTER)

        val header = JLabel(I18n.tr("dialog.zoom.intro"))
        header.border = BorderFactory.createEmptyBorder(6, 0, 14, 0)
        shell.add(header, BorderLayout.NORTH)

        val form = JPanel(GridBagLayout())
        var gbc = GridBagConstraints()
        gbc.anchor = GridBagConstraints.LINE_START
        gbc.insets = Insets(8, 0, 8, 12)

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        form.add(JLabel(I18n.tr("dialog.zoom.horizontalStep")), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        numberfieldX = JNumberField().apply {
            columns = 6
            setInt(GlobalSettings.getXZoomProSchritt())
        }
        form.add(numberfieldX, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        form.add(JLabel(I18n.tr("dialog.zoom.verticalStep")), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        numberfieldY = JNumberField().apply {
            columns = 6
            setInt(GlobalSettings.getYZoomProSchritt())
        }
        form.add(numberfieldY, gbc)

        shell.add(form, BorderLayout.CENTER)

        val buttonRow = JPanel(FlowLayout(FlowLayout.CENTER, 12, 0))
        buttonRow.border = BorderFactory.createEmptyBorder(18, 0, 0, 0)
        val ok = JButton(I18n.tr("dialog.common.ok"))
        ok.addActionListener { okGeklickt() }
        val cancel = JButton(I18n.tr("dialog.common.cancel"))
        cancel.addActionListener { abbrechenGeklickt() }
        buttonRow.add(ok)
        buttonRow.add(cancel)
        shell.add(buttonRow, BorderLayout.SOUTH)

        pack()
        minimumSize = Dimension(320, preferredSize.height)
        setLocationRelativeTo(gui)
        isVisible = true
    }

    private fun okGeklickt() {
        if (numberfieldX.isNumeric() && numberfieldY.isNumeric()) {
            GlobalSettings.setXZoomProSchritt(numberfieldX.getInt())
            GlobalSettings.setYZoomProSchritt(numberfieldY.getInt())
            GlobalSettings.saveSettings()
            dispose()
        }
    }

    private fun abbrechenGeklickt() {
        dispose()
    }

    companion object {
        private const val serialVersionUID = -4780523744293396039L
    }
}

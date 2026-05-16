package de.visustruct.view

import de.visustruct.i18n.I18n
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dialog
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JScrollPane

class ColorDialog private constructor(owner: Dialog?) : JDialog(owner, I18n.tr("dialog.color.title"), true) {

    private var okGeklickt = false

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        add(JScrollPane(colorChooser), BorderLayout.CENTER)

        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        var button = JButton(I18n.tr("dialog.common.ok"))
        button.addActionListener { okButtonGeklickt() }
        panel.add(button)

        button = JButton(I18n.tr("dialog.common.cancel"))
        button.addActionListener { abbrechenButtonGeklickt() }
        panel.add(button)

        button = JButton(I18n.tr("dialog.color.black"))
        button.addActionListener { colorChooser.color = Color.BLACK }
        panel.add(button)

        button = JButton(I18n.tr("dialog.color.white"))
        button.addActionListener { colorChooser.color = Color.WHITE }
        panel.add(button)

        add(panel, BorderLayout.SOUTH)
        setSize(600, 470)
        setLocationRelativeTo(owner)
        isVisible = true
    }

    private fun okButtonGeklickt() {
        okGeklickt = true
        dispose()
    }

    private fun abbrechenButtonGeklickt() {
        dispose()
    }

    fun isOkGeklickt(): Boolean = okGeklickt

    companion object {
        private const val serialVersionUID = 5666531548056491207L
        private val colorChooser = JColorChooser()

        @JvmStatic
        fun showColorChooser(owner: Dialog?, startFarbe: Color?): Color? {
            if (startFarbe != null) {
                colorChooser.color = startFarbe
            }
            val colorDialog = ColorDialog(owner)
            return if (colorDialog.isOkGeklickt()) colorChooser.color else startFarbe
        }
    }
}

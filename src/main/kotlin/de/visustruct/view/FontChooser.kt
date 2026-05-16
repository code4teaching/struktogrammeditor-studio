package de.visustruct.view

import de.visustruct.control.Controlling
import de.visustruct.control.GlobalSettings
import de.visustruct.i18n.I18n
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.WindowConstants

class FontChooser(controlling: Controlling, modal: Boolean) :
    JDialog(controlling.getGUI(), I18n.tr("dialog.font.title"), modal) {

    private val schriftarten: Array<String>
    private val schriftartenChooser: JComboBox<String>
    private val styleChooser: JComboBox<String>
    private val sizeChooser: JComboBox<String>
    private val buttonOK = JButton()
    private val buttonAbbrechen = JButton()
    private val buttonZuruecksetzen = JButton()
    private val controlling: Controlling = controlling

    init {
        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        setSize(465, 120)
        val d = Toolkit.getDefaultToolkit().screenSize
        setLocation((d.width - size.width) / 2, (d.height - size.height) / 2)
        layout = BorderLayout()

        schriftarten = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames

        var panel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            schriftartenChooser = JComboBox(schriftarten).apply {
                setBounds(10, 10, 250, 20)
            }
            add(schriftartenChooser)
            styleChooser = JComboBox(styleNames()).apply {
                setBounds(270, 10, 120, 20)
            }
            add(styleChooser)
            sizeChooser = JComboBox(sizeNames).apply {
                setBounds(400, 10, 50, 20)
            }
            add(sizeChooser)
        }
        add(panel, BorderLayout.NORTH)

        fontAufChooser(controlling.gibAktuellesStruktogramm()!!.getFontStr())

        panel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            buttonOK.apply {
                setBounds(10, 40, 91, 25)
                text = I18n.tr("dialog.common.ok")
                margin = Insets(2, 2, 2, 2)
                addActionListener { evt -> buttonOK_ActionPerformed(evt) }
            }
            add(buttonOK)

            buttonAbbrechen.apply {
                setBounds(115, 40, 91, 25)
                text = I18n.tr("dialog.common.cancel")
                margin = Insets(2, 2, 2, 2)
                addActionListener { evt -> buttonAbbrechen_ActionPerformed(evt) }
            }
            add(buttonAbbrechen)

            buttonZuruecksetzen.apply {
                setBounds(220, 40, 91, 25)
                text = I18n.tr("dialog.font.reset")
                margin = Insets(2, 2, 2, 2)
                addActionListener { evt -> buttonZuruecksetzen_ActionPerformed(evt) }
            }
            add(buttonZuruecksetzen)
        }
        add(panel, BorderLayout.SOUTH)

        isVisible = true
    }

    private fun fontAufChooser(font: Font) {
        var pos = 0
        val schriftart = font.family
        for (i in schriftarten.indices) {
            if (schriftarten[i].equals(schriftart, ignoreCase = true)) {
                pos = i
                break
            }
        }

        schriftartenChooser.selectedIndex = pos

        var a = font.style
        for (i in styleValues.indices) {
            if (styleValues[i] == a) {
                pos = i
                break
            }
        }

        styleChooser.selectedIndex = pos

        a = font.size
        for (i in sizeNames.indices) {
            if (sizeNames[i] == "$a") {
                pos = i
                break
            }
        }

        sizeChooser.selectedIndex = pos
    }

    fun buttonOK_ActionPerformed(evt: ActionEvent) {
        val str = controlling.gibAktuellesStruktogramm()!!
        val fi = schriftartenChooser.selectedIndex
        val fam = if (fi >= 0 && fi < schriftarten.size) schriftarten[fi] else schriftarten[0]
        str.setFontStr(
            Font(
                fam,
                styleValues[styleChooser.selectedIndex],
                sizeNames[sizeChooser.selectedIndex].toInt(),
            ),
        )
        str.rueckgaengigPunktSetzen(true)
        str.graphicsInitialisieren()
        str.zeichenbereichAktualisieren()
        str.zeichne()
        isVisible = false
    }

    fun buttonAbbrechen_ActionPerformed(evt: ActionEvent) {
        isVisible = false
    }

    fun buttonZuruecksetzen_ActionPerformed(evt: ActionEvent) {
        fontAufChooser(GlobalSettings.fontStandard)
    }

    companion object {
        private const val serialVersionUID = -7360108534182191037L

        private fun styleNames(): Array<String> = arrayOf(
            I18n.tr("dialog.font.style.plain"),
            I18n.tr("dialog.font.style.italic"),
            I18n.tr("dialog.font.style.bold"),
            I18n.tr("dialog.font.style.boldItalic"),
        )

        private val styleValues = arrayOf(Font.PLAIN, Font.ITALIC, Font.BOLD, Font.BOLD + Font.ITALIC)

        private val sizeNames = arrayOf(
            "8", "10", "12", "14", "15",
            "18", "20", "24", "28", "32", "40", "48", "56", "64", "72",
        )
    }
}

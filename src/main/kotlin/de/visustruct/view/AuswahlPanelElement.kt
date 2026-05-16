package de.visustruct.view

import com.formdev.flatlaf.extras.FlatSVGIcon
import de.visustruct.control.GlobalSettings
import de.visustruct.i18n.StructureElementI18n
import java.awt.Font
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/** Klickbare Kachel zum Ziehen eines neuen Struktogramm-Elements (FlatLaf-Button-Look). */
class AuswahlPanelElement(private val typ: Int) : JButton() {

    init {
        isFocusable = false
        isRequestFocusEnabled = false
        isRolloverEnabled = true
        isDefaultCapable = false
        icon = erzeugeIcon(typ)
        verticalTextPosition = SwingConstants.CENTER
        horizontalTextPosition = SwingConstants.RIGHT
        horizontalAlignment = SwingConstants.LEFT
        iconTextGap = 10
        margin = Insets(4, 10, 4, 10)
        aktualisiereBeschriftung()
        PaletteButtonStyle.apply(this)
        // Ohne released-Event (typisch nach DnD) bleibt das Button-Model „pressed“ → grauer Kasten
        addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                SwingUtilities.invokeLater { PaletteButtonStyle.clearPressedArmedState(this@AuswahlPanelElement) }
            }
        })
    }

    fun aktualisiereBeschriftung() {
        val kurz = GlobalSettings.gibPaletteButtonBeschriftung(typ)
        font = Font(Font.SANS_SERIF, Font.BOLD, textFontSizeFuerZeile(kurz))
        text = kurz
        // Didaktischer Blockname in der UI-Sprache (z. B. Kopf- vs. fußgesteuerte Schleife trotz gleichem Java-Wort auf zwei Buttons).
        toolTipText = StructureElementI18n.paletteTooltip(typ)
        invalidate()
        revalidate()
        repaint()
    }

    fun gibTyp(): Int = typ

    private fun textFontSizeFuerZeile(s: String?): Int =
        when {
            s == null || s.length <= 12 -> 12
            s.length <= 22 -> 11
            else -> 10
        }

    private fun erzeugeIcon(typ: Int): FlatSVGIcon? {
        val name = iconNameFuerTyp(typ)
        return if (name.isEmpty()) null else FlatSVGIcon("icons/lucide/$name.svg", 18, 18)
    }

    private fun iconNameFuerTyp(typ: Int): String =
        when (typ) {
            0 -> "square"
            1 -> "split"
            2 -> "list-tree"
            3 -> "repeat"
            4 -> "refresh-cw"
            5 -> "rotate-ccw"
            6 -> "infinity"
            7 -> "corner-down-left"
            8 -> "log-in"
            else -> ""
        }

    companion object {
        private const val serialVersionUID = 5455270690460661892L

        @JvmField
        val iconOrdner: String = "/icons/"
    }
}

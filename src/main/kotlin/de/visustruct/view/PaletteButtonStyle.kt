package de.visustruct.view

import com.formdev.flatlaf.FlatClientProperties
import java.awt.Color
import java.awt.Component
import javax.swing.JButton
import javax.swing.SwingUtilities
import javax.swing.UIManager

/** Gleiche Darstellung aller Schaltflächen in der linken Palette (Farbe, Form, kein „Default“-Button). */
object PaletteButtonStyle {

    private const val MIN_HEIGHT = 32

    @JvmStatic
    fun apply(b: JButton) {
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT)
        b.putClientProperty(FlatClientProperties.MINIMUM_HEIGHT, MIN_HEIGHT)
        b.isDefaultCapable = false
        b.isRolloverEnabled = true
        b.isOpaque = true
        b.isContentAreaFilled = true
        b.isBorderPainted = true
        val bg = UIManager.getColor("Button.background")
        val fg = UIManager.getColor("Button.foreground")
        if (bg != null) {
            b.background = bg
        }
        if (fg != null) {
            b.foreground = fg
        }
    }

    @JvmStatic
    fun clearPressedArmedState(c: Component?) {
        if (c !is JButton) {
            return
        }
        val reset = Runnable {
            c.model.isPressed = false
            c.model.isArmed = false
            c.repaint()
        }
        if (SwingUtilities.isEventDispatchThread()) {
            reset.run()
        } else {
            SwingUtilities.invokeLater(reset)
        }
    }
}

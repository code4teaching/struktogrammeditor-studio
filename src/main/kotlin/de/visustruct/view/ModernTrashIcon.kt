package de.visustruct.view

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.Icon
import javax.swing.UIManager

/** Flaches Papierkorbsymbol (ohne Pixelgrafik), passt zu FlatLaf / hellem und dunklem Thema. */
class ModernTrashIcon(private val zielAktiv: Boolean) : Icon {

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.translate(x, y)
        g2.scale(0.77, 0.77)

        var fg = UIManager.getColor("Label.foreground") ?: Color(0x37, 0x41, 0x51)
        var akzent = UIManager.getColor("Component.accentColor") ?: Color(0x25, 0x63, 0xEB)

        val stroke = 1.35f
        g2.stroke = BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        val kontur = if (zielAktiv) akzent else fg
        val fillAlpha = if (zielAktiv) 110 else 70

        val lidDy = if (zielAktiv) -2f else 0f
        val deckel = RoundRectangle2D.Float(6f, 7f + lidDy, 32f, 9f, 5f, 5f)
        g2.color = mitAlpha(fg, if (zielAktiv) 90 else 55)
        g2.fill(deckel)
        g2.color = kontur
        g2.draw(deckel)

        g2.color = kontur
        g2.drawRoundRect(17, 4 + lidDy.toInt(), 10, 5, 3, 3)

        val korpus = RoundRectangle2D.Float(9f, 16f + lidDy * 0.4f, 26f, 28f, 7f, 7f)
        g2.color = mitAlpha(fg, fillAlpha)
        g2.fill(korpus)
        g2.color = kontur
        g2.draw(korpus)

        val streifen = mitAlpha(fg, 140)
        g2.color = streifen
        g2.stroke = BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val mx = 22f
        val top = 22f + lidDy * 0.4f
        val bottom = 38f + lidDy * 0.4f
        g2.drawLine((mx - 5.5f).toInt(), top.toInt(), (mx - 5.5f).toInt(), bottom.toInt())
        g2.drawLine(mx.toInt(), top.toInt(), mx.toInt(), bottom.toInt())
        g2.drawLine((mx + 5.5f).toInt(), top.toInt(), (mx + 5.5f).toInt(), bottom.toInt())

        g2.dispose()
    }

    override fun getIconWidth(): Int = W

    override fun getIconHeight(): Int = H

    companion object {
        private const val W = 34
        private const val H = 37

        private fun mitAlpha(c: Color, alpha: Int): Color {
            val a = alpha.coerceIn(0, 255)
            return Color(c.red, c.green, c.blue, a)
        }
    }
}

package de.visustruct.control

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import java.awt.Color
import javax.swing.LookAndFeel
import javax.swing.UIManager

/**
 * Farben der Struktogramm-Zeichenfläche (nicht die Swing-Fenster).
 */
object CanvasStyle {

    const val DIAGRAM_LINE_WIDTH: Float = 2f
    const val SELECTION_LINE_WIDTH: Float = 3.5f

    private var background: Color = Color(0xFAFAFA)
    private var diagramFrame: Color = Color.BLACK
    private var titleText: Color = Color(0x111827)
    private var elementBorder: Color = Color.BLACK
    private var elementText: Color = Color(0x111827)
    private var elementSelectedFill: Color = Color(0xDBEAFE)
    private var selectionStroke: Color = Color(0f, 0.48f, 1f, 0.9f)
    private var dropPreview: Color = Color(0x803B82F6.toInt(), true)
    private var dragFrame: Color = Color(0x3B82F6)
    private var elementFill: Color = Color.WHITE
    private var simulationStepHighlightFill: Color = Color(0xBFDBFE)

    init {
        applyLightPalette()
    }

    private fun applyLightPalette() {
        background = Color(0xFAFAFA)
        diagramFrame = Color.BLACK
        titleText = Color(0x111827)
        elementBorder = Color.BLACK
        elementText = Color(0x111827)
        elementSelectedFill = Color(0xDBEAFE)
        selectionStroke = Color(0f, 0.48f, 1f, 0.9f)
        dropPreview = Color(0x803B82F6.toInt(), true)
        dragFrame = Color(0x3B82F6)
        elementFill = Color.WHITE
        simulationStepHighlightFill = Color(0xBFDBFE)
    }

    private fun applyDarkPalette() {
        background = Color(0x1E1E22)
        diagramFrame = Color(0xB4B4BA)
        titleText = Color(0xF4F4F5)
        elementBorder = Color(0xB4B4BA)
        elementText = Color(0x18181B)
        elementSelectedFill = Color(0xA7C7FF)
        selectionStroke = Color(0.35f, 0.63f, 1f, 0.92f)
        dropPreview = Color(0x805B9FFF.toInt(), true)
        dragFrame = Color(0x5B9FFF)
        elementFill = Color(0xECECF0)
        simulationStepHighlightFill = Color(0x1E3A5F)
    }

    private fun isFlatDarkTheme(): Boolean {
        val laf: LookAndFeel = UIManager.getLookAndFeel()
        return laf is FlatDarkLaf || laf is FlatMacDarkLaf
    }

    @JvmStatic
    fun syncToTheme() {
        if (isFlatDarkTheme()) {
            applyDarkPalette()
        } else {
            applyLightPalette()
        }
    }

    @JvmStatic
    fun getBackground(): Color = background

    @JvmStatic
    fun getDiagramFrame(): Color = diagramFrame

    @JvmStatic
    fun getTitleText(): Color = titleText

    @JvmStatic
    fun getElementBorder(): Color = elementBorder

    @JvmStatic
    fun getElementText(): Color = elementText

    @JvmStatic
    fun getElementSelectedFill(): Color = elementSelectedFill

    @JvmStatic
    fun getSelectionStroke(): Color = selectionStroke

    @JvmStatic
    fun getDropPreview(): Color = dropPreview

    @JvmStatic
    fun getDragFrame(): Color = dragFrame

    @JvmStatic
    fun getElementFill(): Color = elementFill

    @JvmStatic
    fun getSimulationStepHighlightFill(): Color = simulationStepHighlightFill
}

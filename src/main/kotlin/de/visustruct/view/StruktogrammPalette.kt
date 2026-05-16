package de.visustruct.view

/** Reihenfolge und feste Beschriftungen der linken Palette (englisch). */
object StruktogrammPalette {

    /** Indizes entsprechen [de.visustruct.control.Struktogramm]-Konstanten. */
    @JvmField
    val TYPEN_REIHENFOLGE = intArrayOf(0, 1, 4, 5, 3, 2, 6, 7, 8)

    @JvmStatic
    fun getPaletteButtonLabel(typ: Int): String = getPaletteButtonKurzEnglish(typ)

    @JvmStatic
    fun getPaletteButtonKurzEnglish(typ: Int): String =
        when (typ) {
            0 -> "Statement"
            1 -> "If"
            2 -> "Switch"
            3 -> "For loop"
            4 -> "While loop"
            5 -> "Do-while loop"
            6 -> "Endless loop"
            7 -> "Break"
            8 -> "Call"
            else -> ""
        }

    @JvmStatic
    fun getPaletteElementTooltipEnglish(typ: Int): String =
        when (typ) {
            0 -> "Statement — simple action or assignment"
            1 -> "If / else — decision"
            2 -> "Switch — multi-way selection"
            3 -> "For — counter-controlled loop"
            4 -> "While — pre-test loop"
            5 -> "Do-While — post-test loop"
            6 -> "Infinite loop"
            7 -> "Break — exit from a loop or switch"
            8 -> "Call — procedure or method call"
            9 -> "Empty placeholder block"
            else -> ""
        }

    @JvmStatic
    fun getPaletteLabelLeerElement(): String = "Empty"

    @JvmStatic
    fun getDefaultTextForNewElement(typ: Int): String =
        when (typ) {
            0 -> "Statement"
            1 -> "condition"
            2 -> "selector"
            3 -> "i = 0; i < n; i++"
            4 -> "condition"
            5 -> "condition"
            6 -> "\u221e"
            7 -> "break"
            8 -> "method()"
            9 -> "\u00f8"
            else -> ""
        }
}

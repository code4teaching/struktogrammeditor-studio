package de.visustruct.i18n

import de.visustruct.control.Struktogramm

/**
 * I18n für Strukturblöcke: didaktische Namen und Paletten-Kurzlabels.
 */
object StructureElementI18n {

    private val ELEMENT_KEYS = arrayOf(
        "structure.element.statement",
        "structure.element.decision",
        "structure.element.multiway",
        "structure.element.forLoop",
        "structure.element.whileLoop",
        "structure.element.doWhileLoop",
        "structure.element.infiniteLoop",
        "structure.element.breakExit",
        "structure.element.call",
        "structure.element.empty",
    )

    private val PALETTE_KEYS = arrayOf(
        "structure.palette.statement",
        "structure.palette.decision",
        "structure.palette.multiway",
        "structure.palette.forLoop",
        "structure.palette.whileLoop",
        "structure.palette.doWhileLoop",
        "structure.palette.infiniteLoop",
        "structure.palette.breakExit",
        "structure.palette.call",
        "structure.palette.empty",
    )

    /** Standardtexte für neu eingefügte Elemente (Typ 0–9), gemäß aktueller UI-Sprache. */
    @JvmStatic
    fun didacticDefaultTexts(): Array<String> =
        ELEMENT_KEYS.map(I18n::tr).toTypedArray()

    /** Kurztext auf der linken Palette für Strukturtyp [typ] ([Struktogramm.typAnweisung] …). */
    @JvmStatic
    fun paletteShortLabel(typ: Int): String =
        PALETTE_KEYS.getOrNull(typ)?.let(I18n::tr).orEmpty()

    /** Tooltip: ausführliche didaktische Bezeichnung. */
    @JvmStatic
    fun paletteTooltip(typ: Int): String =
        ELEMENT_KEYS.getOrNull(typ)?.let(I18n::tr).orEmpty()

    /**
     * Linker Teil einer Vorschau-Zeile im Dialog „Beschriftung (Struktogramm)“ — immer der
     * didaktische Name, nicht das kompakte Paletten-Symbol.
     */
    @JvmStatic
    fun previewRowLabel(slotIndex: Int): String =
        ELEMENT_KEYS.getOrNull(slotIndex)?.let(I18n::tr).orEmpty()
}

package de.visustruct.simulation.model

/**
 * Entspricht Swift `VisuStructElementType` / Java `Struktogramm.typ*`.
 */
enum class SimulationElementType(val typ: Int) {
    ANWEISUNG(0),
    VERZWEIGUNG(1),
    FALLAUSWAHL(2),
    FOR_SCHLEIFE(3),
    WHILE_SCHLEIFE(4),
    DO_UNTIL_SCHLEIFE(5),
    ENDLOSSCHLEIFE(6),
    AUSSPRUNG(7),
    AUFRUF(8),
    LEER_ELEMENT(9),
    ;

    companion object {
        @JvmStatic
        fun fromTyp(raw: Int): SimulationElementType? =
            entries.find { it.typ == raw }
    }
}

package de.visustruct.simulation.model

/** Entspricht Swift `VisuStructDocument` (Root `<struktogramm>`). */
data class SimulationDocument(
    val fontFamily: String? = null,
    val fontSize: Int? = null,
    val fontStyle: Int? = null,
    val caption: String = "",
    val elements: List<SimulationElement> = emptyList(),
)

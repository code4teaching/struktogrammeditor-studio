package de.visustruct.simulation.model

/** Entspricht Swift `VisuStructElement` (`<strelem>`). */
data class SimulationElement(
    val type: SimulationElementType,
    val zoomX: Int = 0,
    val zoomY: Int = 0,
    val textColor: SimulationRgba? = null,
    val backgroundColor: SimulationRgba? = null,
    val textLines: List<String> = listOf(""),
    val loopBody: List<SimulationElement>? = null,
    val cases: List<SimulationCase>? = null,
)

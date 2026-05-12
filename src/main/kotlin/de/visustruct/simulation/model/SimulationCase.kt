package de.visustruct.simulation.model

/** Entspricht Swift `VisuStructCase` (`fall`-Knoten). */
data class SimulationCase(
    val name: String,
    val elements: List<SimulationElement>,
)

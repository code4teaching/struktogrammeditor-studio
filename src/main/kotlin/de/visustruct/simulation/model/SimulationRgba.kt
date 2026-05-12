package de.visustruct.simulation.model

/** Entspricht Swift `VisuStructRGBA` (optional in `SimulationElement`). Werte 0–255, Java-freundlich. */
data class SimulationRgba(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int = 255,
)

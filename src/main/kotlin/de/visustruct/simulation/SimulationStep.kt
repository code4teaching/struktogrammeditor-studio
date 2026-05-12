package de.visustruct.simulation

import de.visustruct.simulation.model.SimulationCase
import de.visustruct.simulation.model.SimulationElement

/** Entspricht Swift `VisuStructSimulationStepKind`. */
enum class SimulationStepKind {
    STATEMENT,
    BRANCH,
    SWITCH_SELECTION,
    FOR_LOOP_START,
    FOR_LOOP_CONDITION,
    FOR_LOOP_INCREMENT,
    WHILE_LOOP,
    DO_UNTIL_LOOP,
    DO_UNTIL_CONDITION,
    INFINITE_LOOP,
}

/** Entspricht Swift `VisuStructSimulationStep`. */
data class SimulationStep(
    val path: List<Int>,
    val text: String,
    val kind: SimulationStepKind = SimulationStepKind.STATEMENT,
    val cases: List<SimulationCase>? = null,
    val loopBody: List<SimulationElement>? = null,
    val forParts: List<String>? = null,
)

/** Entspricht Swift `VisuStructSimulationInputRequest`. */
data class SimulationInputRequest(
    val variableName: String,
    val valueType: SimulationValueType,
    val prompt: String,
)

/** Entspricht Swift `VisuStructSimulationState`. */
class SimulationState {
    var stepIndex: Int = 0
    val variables: MutableMap<String, SimulationValue> = linkedMapOf()
    val outputLines: MutableList<String> = mutableListOf()
    var inputRequest: SimulationInputRequest? = null
    var inputError: String? = null
    var lastTrace: String? = null
    var message: String? = null
}

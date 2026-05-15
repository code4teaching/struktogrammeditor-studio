package de.visustruct.simulation

import de.visustruct.simulation.model.SimulationCase
import de.visustruct.simulation.model.SimulationDocument
import de.visustruct.simulation.model.SimulationElement
import de.visustruct.simulation.model.SimulationElementType
import kotlin.jvm.JvmOverloads

internal fun buildSteps(
    elements: List<SimulationElement>,
    pathPrefix: List<Int> = emptyList(),
): List<SimulationStep> =
    elements.mapIndexedNotNull { index, element ->
        val path = pathPrefix + index
        val text =
            element.textLines.joinToString(" ").trim()
        when (element.type) {
            SimulationElementType.ANWEISUNG -> {
                if (text.isEmpty()) null
                else SimulationStep(path = path, text = text)
            }
            SimulationElementType.VERZWEIGUNG ->
                SimulationStep(
                    path = path,
                    text = if (text.isEmpty()) "true" else text,
                    kind = SimulationStepKind.BRANCH,
                    cases = element.cases,
                )
            SimulationElementType.FALLAUSWAHL ->
                SimulationStep(
                    path = path,
                    text = if (text.isEmpty()) "0" else text,
                    kind = SimulationStepKind.SWITCH_SELECTION,
                    cases = element.cases,
                )
            SimulationElementType.FOR_SCHLEIFE ->
                SimulationStep(
                    path = path,
                    text = text,
                    kind = SimulationStepKind.FOR_LOOP_START,
                    loopBody = element.loopBody,
                    forParts = normalizedForParts(element.textLines),
                )
            SimulationElementType.WHILE_SCHLEIFE ->
                SimulationStep(
                    path = path,
                    text = if (text.isEmpty()) "true" else text,
                    kind = SimulationStepKind.WHILE_LOOP,
                    loopBody = element.loopBody,
                )
            SimulationElementType.DO_UNTIL_SCHLEIFE ->
                SimulationStep(
                    path = path,
                    text = if (text.isEmpty()) "true" else text,
                    kind = SimulationStepKind.DO_UNTIL_LOOP,
                    loopBody = element.loopBody,
                )
            SimulationElementType.ENDLOSSCHLEIFE ->
                SimulationStep(
                    path = path,
                    text = if (text.isEmpty()) "true" else text,
                    kind = SimulationStepKind.INFINITE_LOOP,
                    loopBody = element.loopBody,
                )
            else -> null
        }
    }

internal fun normalizedForParts(rawParts: List<String>): List<String> {
    val stripped =
        rawParts.map { stripTrailingSemicolon(it).trim() }
    if (stripped.size == 1 && stripped[0].contains(';')) {
        val split =
            stripped[0].split(';').map { it.trim() }
        return paddedForParts(split)
    }
    return paddedForParts(stripped)
}

private fun paddedForParts(parts: List<String>): List<String> {
    val result = parts.take(3).toMutableList()
    while (result.size < 3) result.add("")
    return result
}

private fun stripTrailingSemicolon(text: String): String {
    var result = text.trim()
    while (result.endsWith(';')) {
        result = result.dropLast(1).trim()
    }
    return result
}

/**
 * Verzweigung: XML-Fallreihenfolge kann von der visuellen „true/false“-Zuordnung abweichen
 * (z. B. nach „Zweige vertauschen“ und Speichern ohne persistiertes Vertausch-Flag).
 */
internal fun branchCaseIndex(cases: List<SimulationCase>, result: Boolean): Int {
    if (cases.size == 2) {
        val trueIdx = cases.indexOfFirst { it.name.trim().equals("true", ignoreCase = true) }
        val falseIdx = cases.indexOfFirst { it.name.trim().equals("false", ignoreCase = true) }
        if (trueIdx >= 0 && falseIdx >= 0) {
            return if (result) trueIdx else falseIdx
        }
    }
    return if (result) 0 else 1
}

/**
 * Port von Swift `VisuStructSimulationEngine`: Schritt-für-Schritt-Ausführung eines
 * [SimulationDocument] (gleiche Konzepte wie iOS).
 */
class SimulationEngine
@JvmOverloads
constructor(document: SimulationDocument) {
    private var sourceDocument: SimulationDocument = document
    internal val stepsMutable: MutableList<SimulationStep> =
        buildSteps(document.elements).toMutableList()

    val steps: List<SimulationStep>
        get() = stepsMutable.toList()

    internal var state: SimulationState = SimulationState()

    val currentStepPath: List<Int>?
        get() =
            if (state.stepIndex < stepsMutable.size) {
                stepsMutable[state.stepIndex].path
            } else {
                null
            }

    val canStep: Boolean
        get() = state.inputRequest == null && state.stepIndex < stepsMutable.size

    fun reset(document: SimulationDocument? = null) {
        document?.let { sourceDocument = it }
        stepsMutable.clear()
        stepsMutable.addAll(buildSteps(sourceDocument.elements))
        state = SimulationState()
    }

    fun step() {
        if (!canStep) {
            if (state.inputRequest == null) {
                state.message = "Simulation finished."
            }
            return
        }
        val st = stepsMutable[state.stepIndex]
        try {
            when (st.kind) {
                SimulationStepKind.STATEMENT -> executeStatement(st.text)
                SimulationStepKind.BRANCH -> {
                    executeBranch(st)
                    return
                }
                SimulationStepKind.SWITCH_SELECTION -> {
                    executeSwitchSelection(st)
                    return
                }
                SimulationStepKind.FOR_LOOP_START -> {
                    executeForLoopStart(st)
                    return
                }
                SimulationStepKind.FOR_LOOP_CONDITION -> {
                    executeForLoopCondition(st)
                    return
                }
                SimulationStepKind.FOR_LOOP_INCREMENT -> {
                    executeForLoopIncrement(st)
                    return
                }
                SimulationStepKind.WHILE_LOOP -> {
                    executeWhileLoop(st)
                    return
                }
                SimulationStepKind.DO_UNTIL_LOOP -> {
                    enterDoUntilLoop(st)
                    return
                }
                SimulationStepKind.DO_UNTIL_CONDITION -> {
                    executeDoUntilCondition(st)
                    return
                }
                SimulationStepKind.INFINITE_LOOP -> {
                    enterInfiniteLoop(st)
                    return
                }
            }
            if (state.inputRequest != null) {
                state.message = null
                return
            }
            state.stepIndex++
            state.message =
                if (state.stepIndex >= stepsMutable.size) {
                    "Simulation finished."
                } else {
                    null
                }
        } catch (e: SimulationExprException) {
            state.message = e.message
            state.stepIndex++
        } catch (e: Exception) {
            state.message = e.message ?: e.toString()
            state.stepIndex++
        }
    }

    fun provideInput(rawValue: String) {
        val request = state.inputRequest ?: return
        try {
            val value = parseInputValue(rawValue, request.valueType)
            assignValue(value, request.variableName)
            state.lastTrace = "${request.variableName} = input -> ${value.display}"
        } catch (e: SimulationExprException) {
            state.inputError = e.message
            return
        } catch (e: Exception) {
            state.inputError = e.message
            return
        }
        state.inputRequest = null
        state.inputError = null
        state.stepIndex++
        state.message =
            if (state.stepIndex >= stepsMutable.size) "Simulation finished." else null
    }

    private fun parseInputValue(raw: String, valueType: SimulationValueType): SimulationValue {
        val trimmed = raw.trim().replace(',', '.')
        return when (valueType) {
            SimulationValueType.INT -> {
                val v = trimmed.toIntOrNull()
                    ?: throw SimulationExprException("Please enter a whole number.")
                SimulationValue.VInt(v)
            }
            SimulationValueType.DOUBLE -> {
                val v = trimmed.toDoubleOrNull()
                    ?: throw SimulationExprException("Please enter a number.")
                SimulationValue.VDouble(v)
            }
            SimulationValueType.STRING -> SimulationValue.VString(raw)
        }
    }

    private fun executeBranch(step: SimulationStep) {
        val cases = step.cases ?: throw SimulationExprException("Unsupported statement: ${step.text}")
        if (cases.size < 2) throw SimulationExprException("Unsupported statement: ${step.text}")
        val condition = stripConditionKeyword(step.text)
        val result = evaluateBoolExpression(condition)
        val selectedCaseIndex = branchCaseIndex(cases, result)
        val branchSteps =
            buildSteps(cases[selectedCaseIndex].elements, step.path + selectedCaseIndex)
        stepsMutable.removeAt(state.stepIndex)
        stepsMutable.addAll(state.stepIndex, branchSteps)
        state.lastTrace = "$condition -> ${if (result) "true" else "false"}"
        state.message =
            if (branchSteps.isNotEmpty()) {
                null
            } else if (state.stepIndex >= stepsMutable.size) {
                "Simulation finished."
            } else {
                null
            }
    }

    private fun executeSwitchSelection(step: SimulationStep) {
        val cases = step.cases ?: throw SimulationExprException("Unsupported statement: ${step.text}")
        if (cases.isEmpty()) throw SimulationExprException("Unsupported statement: ${step.text}")
        val selector = stripConditionKeyword(step.text)
        val selectorValue = evaluateSwitchSelector(selector)
        val selectedCaseIndex =
            matchingCaseIndex(selectorValue, cases) ?: (cases.size - 1)
        val selectedCase = cases[selectedCaseIndex]
        val caseSteps =
            buildSteps(selectedCase.elements, step.path + selectedCaseIndex)
        stepsMutable.removeAt(state.stepIndex)
        stepsMutable.addAll(state.stepIndex, caseSteps)
        state.lastTrace = "$selector -> ${selectedCase.name}"
        state.message =
            if (caseSteps.isNotEmpty()) {
                null
            } else if (state.stepIndex >= stepsMutable.size) {
                "Simulation finished."
            } else {
                null
            }
    }

    private fun executeForLoopStart(step: SimulationStep) {
        val parts = normalizedForParts(step.forParts ?: listOf(step.text))
        if (parts.size < 3) throw SimulationExprException("Unsupported statement: ${step.text}")
        if (parts[0].isNotEmpty()) {
            executeStatement(parts[0])
        }
        val conditionStep =
            SimulationStep(
                path = step.path,
                text = parts[1],
                kind = SimulationStepKind.FOR_LOOP_CONDITION,
                loopBody = step.loopBody,
                forParts = parts,
            )
        stepsMutable.removeAt(state.stepIndex)
        stepsMutable.add(state.stepIndex, conditionStep)
    }

    private fun executeForLoopCondition(step: SimulationStep) {
        val parts = normalizedForParts(step.forParts ?: listOf(step.text))
        if (parts.size < 3) throw SimulationExprException("Unsupported statement: ${step.text}")
        val condition = if (parts[1].isEmpty()) "true" else parts[1]
        val shouldEnter = evaluateBoolExpression(condition)
        state.lastTrace = "$condition -> ${if (shouldEnter) "true" else "false"}"
        if (shouldEnter) {
            val bodySteps =
                buildSteps(step.loopBody ?: emptyList(), step.path + 0)
            val incrementStep =
                SimulationStep(
                    path = step.path,
                    text = parts[2],
                    kind = SimulationStepKind.FOR_LOOP_INCREMENT,
                    loopBody = step.loopBody,
                    forParts = parts,
                )
            val conditionStep =
                SimulationStep(
                    path = step.path,
                    text = parts[1],
                    kind = SimulationStepKind.FOR_LOOP_CONDITION,
                    loopBody = step.loopBody,
                    forParts = parts,
                )
            stepsMutable.removeAt(state.stepIndex)
            stepsMutable.addAll(state.stepIndex, bodySteps + incrementStep + conditionStep)
        } else {
            stepsMutable.removeAt(state.stepIndex)
            state.message =
                if (state.stepIndex >= stepsMutable.size) "Simulation finished." else null
        }
    }

    private fun executeForLoopIncrement(step: SimulationStep) {
        val increment = step.text.trim()
        if (increment.isNotEmpty()) {
            executeIncrementStatement(increment)
        }
        stepsMutable.removeAt(state.stepIndex)
    }

    private fun executeWhileLoop(step: SimulationStep) {
        val condition = stripConditionKeyword(step.text)
        val shouldEnter = evaluateBoolExpression(condition)
        state.lastTrace = "$condition -> ${if (shouldEnter) "true" else "false"}"
        if (shouldEnter) {
            val bodySteps =
                buildSteps(step.loopBody ?: emptyList(), step.path + 0)
            val conditionStep =
                SimulationStep(
                    path = step.path,
                    text = step.text,
                    kind = SimulationStepKind.WHILE_LOOP,
                    loopBody = step.loopBody,
                )
            stepsMutable.removeAt(state.stepIndex)
            stepsMutable.addAll(state.stepIndex, bodySteps + conditionStep)
            state.message =
                if (bodySteps.isNotEmpty()) {
                    null
                } else if (state.stepIndex >= stepsMutable.size) {
                    "Simulation finished."
                } else {
                    null
                }
        } else {
            stepsMutable.removeAt(state.stepIndex)
            state.message =
                if (state.stepIndex >= stepsMutable.size) "Simulation finished." else null
        }
    }

    private fun enterDoUntilLoop(step: SimulationStep) {
        val bodySteps =
            buildSteps(step.loopBody ?: emptyList(), step.path + 0)
        val conditionStep =
            SimulationStep(
                path = step.path,
                text = step.text,
                kind = SimulationStepKind.DO_UNTIL_CONDITION,
                loopBody = step.loopBody,
            )
        stepsMutable.removeAt(state.stepIndex)
        stepsMutable.addAll(state.stepIndex, bodySteps + conditionStep)
        state.message =
            if (bodySteps.isNotEmpty()) {
                null
            } else if (state.stepIndex >= stepsMutable.size) {
                "Simulation finished."
            } else {
                null
            }
    }

    private fun executeDoUntilCondition(step: SimulationStep) {
        val condition = stripConditionKeyword(step.text)
        val exitNow = evaluateBoolExpression(condition)
        val shouldRepeat = !exitNow
        state.lastTrace = "until $condition -> ${if (exitNow) "exit" else "repeat"}"
        if (shouldRepeat) {
            val bodySteps =
                buildSteps(step.loopBody ?: emptyList(), step.path + 0)
            val conditionStep =
                SimulationStep(
                    path = step.path,
                    text = step.text,
                    kind = SimulationStepKind.DO_UNTIL_CONDITION,
                    loopBody = step.loopBody,
                )
            stepsMutable.removeAt(state.stepIndex)
            stepsMutable.addAll(state.stepIndex, bodySteps + conditionStep)
        } else {
            stepsMutable.removeAt(state.stepIndex)
            state.message =
                if (state.stepIndex >= stepsMutable.size) "Simulation finished." else null
        }
    }

    private fun enterInfiniteLoop(step: SimulationStep) {
        val bodySteps =
            buildSteps(step.loopBody ?: emptyList(), step.path + 0)
        val loopStep =
            SimulationStep(
                path = step.path,
                text = step.text,
                kind = SimulationStepKind.INFINITE_LOOP,
                loopBody = step.loopBody,
            )
        stepsMutable.removeAt(state.stepIndex)
        stepsMutable.addAll(state.stepIndex, bodySteps + loopStep)
        state.lastTrace = "true -> true"
        state.message = if (bodySteps.isNotEmpty()) null else "Infinite loop has no body."
    }

    /** Nur für Swing/Java: lesende Schnappschüsse (keine Mutation des Zustands von außen). */
    fun getOutputLinesSnapshot(): List<String> = state.outputLines.toList()

    fun getVariablesSnapshot(): Map<String, String> =
        state.variables.mapValues { (_, v) -> v.display }

    fun getUiMessage(): String? = state.message
    fun getUiLastTrace(): String? = state.lastTrace
    fun getInputRequestForUi(): SimulationInputRequest? = state.inputRequest
    fun getInputErrorForUi(): String? = state.inputError
    fun getStepIndexForUi(): Int = state.stepIndex

    /** Text des Simulationsschritts zum Diagramm-Pfad (für die Anzeige „Aktueller Block“). */
    fun findStepTextByPath(path: List<Int>?): String? {
        if (path == null || path.isEmpty()) {
            return null
        }
        return stepsMutable.firstOrNull { it.path == path }?.text
    }

    companion object {
        const val ENGINE_VERSION: String = "simulation-1"
    }
}

package de.visustruct.simulation

import de.visustruct.simulation.model.SimulationCase
import de.visustruct.simulation.model.SimulationDocument
import de.visustruct.simulation.model.SimulationElement
import de.visustruct.simulation.model.SimulationElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SimulationEngineTest {

    @Test
    fun assignmentAndOutput() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("x = 2")),
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("output: x")),
                    ),
            )
        val eng = SimulationEngine(doc)
        assertTrue(eng.canStep)
        eng.step()
        assertEquals(1, eng.state.stepIndex)
        val x = eng.state.variables["x"] as SimulationValue.VInt
        assertEquals(2, x.value)
        eng.step()
        assertEquals(listOf("2"), eng.state.outputLines)
    }

    @Test
    fun branchSelectsTrueArm() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(
                            type = SimulationElementType.VERZWEIGUNG,
                            textLines = listOf("1 > 0"),
                            cases =
                                listOf(
                                    SimulationCase("ja", listOf(SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("a = 1")))),
                                    SimulationCase("nein", listOf(SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("b = 2")))),
                                ),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
        assertEquals(1, eng.stepsMutable.size)
        assertEquals("a = 1", eng.stepsMutable[0].text)
        eng.step()
        assertEquals(1, (eng.state.variables["a"] as SimulationValue.VInt).value)
    }

    @Test
    fun whileLoopDoesNotEnterWhenFalse() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(
                            type = SimulationElementType.WHILE_SCHLEIFE,
                            textLines = listOf("false"),
                            loopBody =
                                listOf(SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("x = 1"))),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
        assertTrue(eng.stepsMutable.isEmpty())
        assertFalse(eng.canStep)
    }

    @Test
    fun doUntilWithTrueConditionAndEmptyBodyExits() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(
                            type = SimulationElementType.DO_UNTIL_SCHLEIFE,
                            textLines = listOf("true"),
                            loopBody = emptyList(),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
        assertFalse(eng.canStep)
        assertTrue(eng.stepsMutable.isEmpty())
    }

    @Test
    fun doUntilRepeatsUntilConditionBecomesTrue() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("x = 0")),
                        SimulationElement(
                            type = SimulationElementType.DO_UNTIL_SCHLEIFE,
                            textLines = listOf("x >= 2"),
                            loopBody =
                                listOf(SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("x = x + 1"))),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
        assertEquals(0, (eng.state.variables["x"] as SimulationValue.VInt).value)
        eng.step()
        assertEquals(1, (eng.state.variables["x"] as SimulationValue.VInt).value)
        eng.step()
        assertEquals(2, (eng.state.variables["x"] as SimulationValue.VInt).value)
        assertFalse(eng.canStep)
    }

    @Test
    fun letAssignmentCreatesVariable() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("let x = 7")),
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("output: x")),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
        assertEquals(7, (eng.state.variables["x"] as SimulationValue.VInt).value)
        eng.step()
        assertEquals(listOf("7"), eng.state.outputLines)
    }

    @Test
    fun bareExpressionEvaluatesWithoutAssignment() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("x = 10")),
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("x / 2")),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
        eng.step()
        assertEquals(10, (eng.state.variables["x"] as SimulationValue.VInt).value)
        assertTrue(eng.state.lastTrace!!.contains("no assignment"))
    }
}

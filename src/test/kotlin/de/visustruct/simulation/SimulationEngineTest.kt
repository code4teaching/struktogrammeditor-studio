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
    fun factorialBranchEntersTrueArmAfterIntAssignment() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("int n = 4")),
                        SimulationElement(
                            type = SimulationElementType.VERZWEIGUNG,
                            textLines = listOf("if n >= 0"),
                            cases =
                                listOf(
                                    SimulationCase(
                                        "true",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("int fakultaet = 1"),
                                            ),
                                            SimulationElement(
                                                type = SimulationElementType.WHILE_SCHLEIFE,
                                                textLines = listOf("n > 1"),
                                                loopBody =
                                                    listOf(
                                                        SimulationElement(
                                                            type = SimulationElementType.ANWEISUNG,
                                                            textLines = listOf("fakultaet = fakultaet * n"),
                                                        ),
                                                        SimulationElement(
                                                            type = SimulationElementType.ANWEISUNG,
                                                            textLines = listOf("n = n - 1"),
                                                        ),
                                                    ),
                                            ),
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("output: fakultaet"),
                                            ),
                                        ),
                                    ),
                                    SimulationCase(
                                        "false",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("output: \"Fehler\""),
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
        assertEquals(4, (eng.state.variables["n"] as SimulationValue.VInt).value)
        assertTrue(eng.canStep)
        eng.step()
        assertTrue(eng.state.lastTrace!!.contains("true"), eng.state.lastTrace)
        assertTrue(eng.canStep, "nach true-Verzweigung müssen Schritte im true-Zweig folgen")
        assertEquals("int fakultaet = 1", eng.stepsMutable[eng.state.stepIndex].text)
    }

    @Test
    fun factorialCompletesWithOutput24() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("int n = 4")),
                        SimulationElement(
                            type = SimulationElementType.VERZWEIGUNG,
                            textLines = listOf("if n >= 0"),
                            cases =
                                listOf(
                                    SimulationCase(
                                        "true",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("int fakultaet = 1"),
                                            ),
                                            SimulationElement(
                                                type = SimulationElementType.WHILE_SCHLEIFE,
                                                textLines = listOf("n > 1"),
                                                loopBody =
                                                    listOf(
                                                        SimulationElement(
                                                            type = SimulationElementType.ANWEISUNG,
                                                            textLines = listOf("fakultaet = fakultaet * n"),
                                                        ),
                                                        SimulationElement(
                                                            type = SimulationElementType.ANWEISUNG,
                                                            textLines = listOf("n = n - 1"),
                                                        ),
                                                    ),
                                            ),
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("output: fakultaet"),
                                            ),
                                        ),
                                    ),
                                    SimulationCase(
                                        "false",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("output: \"Fehler\""),
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        var guard = 0
        while (eng.canStep && guard++ < 200) {
            eng.step()
        }
        assertFalse(eng.canStep, "Simulation sollte enden; message=${eng.state.message}")
        assertEquals(listOf("24"), eng.state.outputLines, eng.state.lastTrace)
        assertNull(eng.state.message, eng.state.message)
    }

    @Test
    fun resetRebuildsStepsAndAcceptsNewInput() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(
                            type = SimulationElementType.ANWEISUNG,
                            textLines = listOf("input: int zahl \"?\""),
                        ),
                        SimulationElement(
                            type = SimulationElementType.VERZWEIGUNG,
                            textLines = listOf("zahl > 5"),
                            cases =
                                listOf(
                                    SimulationCase(
                                        "true",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("output: \"groß\""),
                                            ),
                                        ),
                                    ),
                                    SimulationCase(
                                        "false",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("output: \"klein\""),
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        val initialSteps = eng.steps.size
        eng.step()
        eng.provideInput("6")
        eng.step()
        eng.step()
        assertEquals(listOf("groß"), eng.state.outputLines)
        assertTrue(eng.steps.size > initialSteps, "Verzweigung erweitert die Schrittliste")
        eng.reset(null)
        assertEquals(initialSteps, eng.steps.size)
        assertTrue(eng.state.variables.isEmpty())
        assertTrue(eng.state.outputLines.isEmpty())
        eng.step()
        eng.provideInput("3")
        eng.step()
        eng.step()
        assertEquals(listOf("klein"), eng.state.outputLines)
    }

    @Test
    fun evenInputSelectsTrueBranchForModuloCondition() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(
                            type = SimulationElementType.ANWEISUNG,
                            textLines = listOf("input: int zahl \"Zahl eingeben:\""),
                        ),
                        SimulationElement(
                            type = SimulationElementType.ANWEISUNG,
                            textLines = listOf("output: \"Zahl: \" + zahl"),
                        ),
                        SimulationElement(
                            type = SimulationElementType.VERZWEIGUNG,
                            textLines = listOf("if zahl % 2 == 0"),
                            cases =
                                listOf(
                                    SimulationCase(
                                        "true",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("gerade = 1"),
                                            ),
                                        ),
                                    ),
                                    SimulationCase(
                                        "false",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("output: \"Ungerade\""),
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
        eng.provideInput("6")
        eng.step()
        eng.step()
        assertTrue(eng.state.lastTrace!!.contains("true"), eng.state.lastTrace)
        assertEquals("gerade = 1", eng.stepsMutable[eng.state.stepIndex].text)
    }

    @Test
    fun evenInputUsesTrueBranchWhenCasesAreSwappedInXmlOrder() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(
                            type = SimulationElementType.ANWEISUNG,
                            textLines = listOf("input: int zahl \"Zahl eingeben:\""),
                        ),
                        SimulationElement(
                            type = SimulationElementType.VERZWEIGUNG,
                            textLines = listOf("if zahl % 2 == 0"),
                            cases =
                                listOf(
                                    SimulationCase(
                                        "false",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("output: \"Ungerade\""),
                                            ),
                                        ),
                                    ),
                                    SimulationCase(
                                        "true",
                                        listOf(
                                            SimulationElement(
                                                type = SimulationElementType.ANWEISUNG,
                                                textLines = listOf("gerade = 1"),
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
        eng.provideInput("6")
        eng.step()
        assertTrue(eng.state.lastTrace!!.contains("true"), eng.state.lastTrace)
        assertEquals("gerade = 1", eng.stepsMutable[eng.state.stepIndex].text)
    }

    @Test
    fun systemOutPrintlnAddsToOutputLines() {
        val doc =
            SimulationDocument(
                elements =
                    listOf(
                        SimulationElement(type = SimulationElementType.ANWEISUNG, textLines = listOf("int x = 7")),
                        SimulationElement(
                            type = SimulationElementType.ANWEISUNG,
                            textLines = listOf("System.out.println(x);"),
                        ),
                    ),
            )
        val eng = SimulationEngine(doc)
        eng.step()
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

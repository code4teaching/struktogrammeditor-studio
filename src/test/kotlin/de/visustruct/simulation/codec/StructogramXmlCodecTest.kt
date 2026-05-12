package de.visustruct.simulation.codec

import de.visustruct.simulation.model.SimulationElementType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class StructogramXmlCodecTest {

    @Test
    fun decodeRootCaptionAndSimpleStrelem() {
        val xml =
            """<?xml version="1.0" encoding="UTF-8"?>
<struktogramm fontfamily="-1;" fontsize="12" fontstyle="0" caption="-1;">
  <strelem typ="0" zx="0" zy="0" textcolor="-16777216" bgcolor="-1">
    <text>72;105;</text>
  </strelem>
</struktogramm>
""".trimIndent()

        val doc = StructogramXmlCodec.decode(xml)
        assertEquals("", doc.caption)
        assertEquals("", doc.fontFamily)
        assertEquals(12, doc.fontSize)
        assertEquals(0, doc.fontStyle)
        assertEquals(1, doc.elements.size)
        val e = doc.elements[0]
        assertEquals(SimulationElementType.ANWEISUNG, e.type)
        assertEquals(listOf("Hi"), e.textLines)
        assertNotNull(e.textColor)
        assertEquals(0, e.textColor!!.r)
        assertEquals(0, e.textColor.g)
        assertEquals(0, e.textColor.b)
        assertEquals(255, e.textColor.a)
        assertNotNull(e.backgroundColor)
        assertEquals(255, e.backgroundColor!!.r)
        assertEquals(255, e.backgroundColor.g)
        assertEquals(255, e.backgroundColor.b)
        assertNull(e.loopBody)
        assertNull(e.cases)
    }

    @Test
    fun decodeLoopAndCases() {
        val xml =
            """<?xml version="1.0" encoding="UTF-8"?>
<struktogramm caption="-1;">
  <strelem typ="3" zx="0" zy="0">
    <text>83;99;104;108;101;105;102;101;</text>
    <schleifeninhalt>
      <strelem typ="0" zx="0" zy="0">
        <text>105;110;110;101;114;</text>
      </strelem>
    </schleifeninhalt>
  </strelem>
  <strelem typ="1" zx="0" zy="0">
    <text>105;102;</text>
    <fall fallname="106;97;">
      <strelem typ="0" zx="0" zy="0">
        <text>97;</text>
      </strelem>
    </fall>
    <fall fallname="110;101;105;110;">
      <strelem typ="0" zx="0" zy="0">
        <text>98;</text>
      </strelem>
    </fall>
  </strelem>
</struktogramm>
""".trimIndent()

        val doc = StructogramXmlCodec.decode(xml)
        assertEquals(2, doc.elements.size)
        val loop = doc.elements[0]
        assertEquals(SimulationElementType.FOR_SCHLEIFE, loop.type)
        assertEquals("Schleife", loop.textLines.single())
        assertNotNull(loop.loopBody)
        assertEquals(1, loop.loopBody!!.size)
        assertEquals("inner", loop.loopBody[0].textLines.single())

        val branch = doc.elements[1]
        assertEquals(SimulationElementType.VERZWEIGUNG, branch.type)
        assertNotNull(branch.cases)
        assertEquals(2, branch.cases!!.size)
        assertEquals("ja", branch.cases[0].name)
        assertEquals("a", branch.cases[0].elements.single().textLines.single())
        assertEquals("nein", branch.cases[1].name)
        assertEquals("b", branch.cases[1].elements.single().textLines.single())
    }
}

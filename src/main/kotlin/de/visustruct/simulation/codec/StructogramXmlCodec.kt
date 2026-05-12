package de.visustruct.simulation.codec

import de.visustruct.simulation.model.SimulationCase
import de.visustruct.simulation.model.SimulationDocument
import de.visustruct.simulation.model.SimulationElement
import de.visustruct.simulation.model.SimulationElementType
import de.visustruct.simulation.model.SimulationRgba
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.io.StringReader
import javax.xml.XMLConstants
import kotlin.jvm.Throws

/**
 * Dekodiert VisuStruct-XML wie Swift `VisuStructXMLCodec` / Java `XMLLeser`/`Struktogramm.xmlErstellen`.
 */
object StructogramXmlCodec {

    @Throws(XmlDecodeException::class)
    @JvmStatic
    fun decode(xml: String): SimulationDocument {
        val builder = SAXBuilder()
        try {
            builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        } catch (_: Exception) {
            // ältere Parser: best effort
        }
        val jdom: org.jdom2.Document = try {
            builder.build(StringReader(xml))
        } catch (e: Exception) {
            throw XmlDecodeException("XML konnte nicht gelesen werden: ${e.message}", e)
        }
        val root = jdom.rootElement
        if (root.name != "struktogramm") {
            throw XmlDecodeException("Ungültiges Root-Element: erwartet <struktogramm>, war <${root.name}>.")
        }
        val fontFamily = root.getAttributeValue("fontfamily")?.let { JavaStringCoding.decode(it) }
        val fontSize = root.getAttributeValue("fontsize")?.toIntOrNull()
        val fontStyle = root.getAttributeValue("fontstyle")?.toIntOrNull()
        val caption = root.getAttributeValue("caption")?.let { JavaStringCoding.decode(it) } ?: ""
        val elements = root.children
            .filter { it.name == "strelem" }
            .map { parseStrelem(it) }
        return SimulationDocument(
            fontFamily = fontFamily,
            fontSize = fontSize,
            fontStyle = fontStyle,
            caption = caption,
            elements = elements,
        )
    }

    private fun parseStrelem(el: Element): SimulationElement {
        val typRaw = el.getAttributeValue("typ")?.toIntOrNull() ?: 0
        val type = SimulationElementType.fromTyp(typRaw) ?: SimulationElementType.LEER_ELEMENT
        val zoomX = el.getAttributeValue("zx")?.toIntOrNull() ?: 0
        val zoomY = el.getAttributeValue("zy")?.toIntOrNull() ?: 0
        val textColor = el.getAttributeValue("textcolor")?.let { parseJavaArgbIntString(it) }
        val backgroundColor = el.getAttributeValue("bgcolor")?.let { parseJavaArgbIntString(it) }

        val textLines = mutableListOf<String>()
        var loopBody: List<SimulationElement>? = null
        val cases = mutableListOf<SimulationCase>()

        for (child in el.children) {
            when (child.name) {
                "text" -> {
                    val coded = child.textTrim
                    textLines.add(JavaStringCoding.decode(coded))
                }
                "schleifeninhalt" -> {
                    val inner = child.getChildren("strelem").map { parseStrelem(it) }
                    loopBody = if (inner.isEmpty()) null else inner
                }
                "fall" -> {
                    val nameCoded = child.getAttributeValue("fallname") ?: ""
                    val name = JavaStringCoding.decode(nameCoded)
                    val inner = child.getChildren("strelem").map { parseStrelem(it) }
                    cases.add(SimulationCase(name = name, elements = inner))
                }
            }
        }

        if (textLines.isEmpty()) {
            textLines.add("")
        }

        return SimulationElement(
            type = type,
            zoomX = zoomX,
            zoomY = zoomY,
            textColor = textColor,
            backgroundColor = backgroundColor,
            textLines = textLines.toList(),
            loopBody = loopBody,
            cases = if (cases.isEmpty()) null else cases.toList(),
        )
    }

    /** Java `Color.getRGB()` als dezimaler (ggf. negativer) 32-bit-ARGB-String. */
    private fun parseJavaArgbIntString(s: String): SimulationRgba? {
        val i32 = s.toIntOrNull() ?: return null
        val u = i32.toUInt()
        val a = ((u shr 24) and 0xFFu).toInt()
        val r = ((u shr 16) and 0xFFu).toInt()
        val g = ((u shr 8) and 0xFFu).toInt()
        val b = (u and 0xFFu).toInt()
        return SimulationRgba(r = r, g = g, b = b, a = a)
    }
}

/**
 * JVM-Einstieg für Java-Code: `StructogramXml.decode(xml)`.
 */
object StructogramXml {
    @Throws(XmlDecodeException::class)
    @JvmStatic
    fun decode(xml: String): SimulationDocument = StructogramXmlCodec.decode(xml)
}

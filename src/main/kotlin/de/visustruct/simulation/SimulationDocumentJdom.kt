package de.visustruct.simulation

import de.visustruct.simulation.codec.StructogramXml
import de.visustruct.simulation.codec.XmlDecodeException
import de.visustruct.simulation.model.SimulationDocument
import org.jdom2.Document
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter

/**
 * Brücke vom Editor-JDOM (wie Speichern / Rückgängig) zum Kotlin-Simulationsmodell.
 */
object SimulationDocumentJdom {

    /**
     * Wandelt ein JDOM-[Document] mit Root `struktogramm` in ein [SimulationDocument] um.
     * Encoding UTF-8; gleiche logische Daten wie beim Kotlin-XML-Decoder aus einer Datei.
     */
    @JvmStatic
    @Throws(XmlDecodeException::class)
    fun fromStruktogrammDocument(document: Document?): SimulationDocument {
        require(document != null) { "document is null" }
        val format = Format.getRawFormat().apply { encoding = "UTF-8" }
        val out = XMLOutputter(format)
        val xml = out.outputString(document)
        return StructogramXml.decode(xml)
    }
}

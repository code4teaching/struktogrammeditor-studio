package de.visustruct.simulation;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import de.visustruct.simulation.codec.StructogramXml;
import de.visustruct.simulation.codec.XmlDecodeException;
import de.visustruct.simulation.model.SimulationDocument;

/**
 * Brücke vom Editor-JDOM (wie Speichern / Rückgängig) zum Kotlin-Simulationsmodell.
 */
public final class SimulationDocumentJdom {

	private SimulationDocumentJdom() {
	}

	/**
	 * Wandelt ein JDOM-{@link Document} mit Root {@code struktogramm} in ein {@link SimulationDocument} um.
	 * Encoding UTF-8; gleiche logische Daten wie beim Kotlin-XML-Decoder aus einer Datei.
	 */
	public static SimulationDocument fromStruktogrammDocument(Document document) throws XmlDecodeException {
		if (document == null) {
			throw new IllegalArgumentException("document is null");
		}
		Format format = Format.getRawFormat();
		format.setEncoding("UTF-8");
		XMLOutputter out = new XMLOutputter(format);
		String xml = out.outputString(document);
		return StructogramXml.decode(xml);
	}
}

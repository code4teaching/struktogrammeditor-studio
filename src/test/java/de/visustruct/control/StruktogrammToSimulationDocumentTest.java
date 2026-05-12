package de.visustruct.control;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.visustruct.simulation.codec.XmlDecodeException;
import de.visustruct.simulation.model.SimulationDocument;
import de.visustruct.simulation.model.SimulationElementType;
import org.junit.jupiter.api.Test;

/**
 * Editor → Kotlin-Modell über {@link Struktogramm#toSimulationDocument()} (ohne GUI-Interaktion).
 */
class StruktogrammToSimulationDocumentTest {

	@Test
	void emptyStruktogrammRoundtripsToSimulationDocument() throws XmlDecodeException {
		Struktogramm s = new Struktogramm(null);
		SimulationDocument doc = s.toSimulationDocument();
		// Neue Diagramme enthalten ein Platzhalter-LeerElement („ø“), das als strelem serialisiert wird.
		assertEquals(1, doc.getElements().size());
		assertEquals(SimulationElementType.LEER_ELEMENT, doc.getElements().get(0).getType());
		assertEquals("ø", doc.getElements().get(0).getTextLines().get(0));
		assertEquals("", doc.getCaption());
	}
}

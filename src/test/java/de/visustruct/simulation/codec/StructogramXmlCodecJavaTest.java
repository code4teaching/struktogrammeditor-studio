package de.visustruct.simulation.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.visustruct.simulation.codec.XmlDecodeException;
import de.visustruct.simulation.model.SimulationDocument;
import de.visustruct.simulation.model.SimulationElementType;
import org.junit.jupiter.api.Test;

class StructogramXmlCodecJavaTest {

	@Test
	void decodeFromJavaViaJvmStatic() throws XmlDecodeException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<struktogramm caption=\"-1;\">\n"
				+ "  <strelem typ=\"0\" zx=\"0\" zy=\"0\">\n"
				+ "    <text>88;</text>\n"
				+ "  </strelem>\n"
				+ "</struktogramm>\n";

		SimulationDocument doc = StructogramXml.decode(xml);
		assertEquals("", doc.getCaption());
		assertEquals(1, doc.getElements().size());
		assertEquals(SimulationElementType.ANWEISUNG, doc.getElements().get(0).getType());
		assertEquals("X", doc.getElements().get(0).getTextLines().get(0));
	}
}

package de.visustruct.control;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import de.visustruct.simulation.SimulationEngine;
import de.visustruct.simulation.codec.StructogramXml;
import de.visustruct.struktogrammelemente.Anweisung;
import de.visustruct.struktogrammelemente.Schleife;
import de.visustruct.struktogrammelemente.StruktogrammElement;

/**
 * Simulations-Pfade müssen dieselben Diagramm-Elemente finden wie {@link SimulationEngine}.
 */
class StruktogrammSimulationPfadTest {

	private static final String WHILE_WITH_BODY =
			"""
			<?xml version="1.0" encoding="UTF-8"?>
			<struktogramm fontfamily="-1;" fontsize="12" fontstyle="0" caption="-1;">
			  <strelem typ="4" zx="0" zy="0">
			    <text>116;114;117;101;</text>
			    <schleifeninhalt>
			      <strelem typ="0" zx="0" zy="0">
			        <text>120;32;61;32;49;</text>
			      </strelem>
			    </schleifeninhalt>
			  </strelem>
			</struktogramm>
			""";

	@Test
	void loopBodyStepPathFindsInnerAnweisung() throws Exception {
		Struktogramm struktogramm = new Struktogramm(null);
		var jdom = new SAXBuilder().build(new java.io.StringReader(WHILE_WITH_BODY));
		new XMLLeser().ladeXML(jdom, struktogramm);

		var simDoc = StructogramXml.decode(WHILE_WITH_BODY);
		SimulationEngine eng = new SimulationEngine(simDoc);
		eng.step(); // while-Bedingung
		assertTrue(eng.getCanStep());
		List<Integer> bodyStepPath = eng.getCurrentStepPath();
		assertNotNull(bodyStepPath);

		StruktogrammElement el = struktogramm.elementFuerSimulationPfadSuchen(bodyStepPath);
		assertNotNull(el, "Pfad " + bodyStepPath + " muss ein Diagramm-Element liefern");
		assertTrue(el instanceof Anweisung, "erwartet Anweisung im Schleifeninhalt");
	}

	@Test
	void loopHeaderPathFindsSchleife() throws Exception {
		Struktogramm struktogramm = new Struktogramm(null);
		var jdom = new SAXBuilder().build(new java.io.StringReader(WHILE_WITH_BODY));
		new XMLLeser().ladeXML(jdom, struktogramm);

		StruktogrammElement el = struktogramm.elementFuerSimulationPfadSuchen(List.of(0));
		assertNotNull(el);
		assertTrue(el instanceof Schleife);
	}
}

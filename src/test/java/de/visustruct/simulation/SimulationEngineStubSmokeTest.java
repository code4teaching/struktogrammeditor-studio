package de.visustruct.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SimulationEngineStubSmokeTest {

	@Test
	void kotlinStubIsReachableFromJava() {
		assertEquals(SimulationEngine.ENGINE_VERSION, SimulationEngineStub.version());
		assertEquals(SimulationEngineStub.STUB_VERSION, SimulationEngineStub.version());
	}
}

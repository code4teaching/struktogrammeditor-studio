package de.visustruct.simulation

/**
 * Rückwärtskompatibler Kurzname; Version folgt der echten [SimulationEngine].
 */
object SimulationEngineStub {

    const val STUB_VERSION: String = SimulationEngine.ENGINE_VERSION

    @JvmStatic
    fun version(): String = STUB_VERSION
}

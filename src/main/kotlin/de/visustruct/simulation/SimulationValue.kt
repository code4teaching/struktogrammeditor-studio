package de.visustruct.simulation

import kotlin.math.abs
import kotlin.math.roundToLong
import java.util.Locale

/** Entspricht Swift `VisuStructSimulationValue`. */
sealed class SimulationValue {
    abstract val display: String

    data class VInt(val value: Int) : SimulationValue() {
        override val display: String get() = value.toString()
    }

    data class VDouble(val value: Double) : SimulationValue() {
        override val display: String get() = formatDouble(value)
    }

    data class VString(val value: String) : SimulationValue() {
        override val display: String get() = value
    }

    data class VIntArray(val values: MutableList<Int>) : SimulationValue() {
        override val display: String get() = "[${values.joinToString(", ")}]"
    }

    data class VDoubleArray(val values: MutableList<Double>) : SimulationValue() {
        override val display: String get() = "[${values.joinToString(", ") { formatDouble(it) }}]"
    }

    data class VStringArray(val values: MutableList<String>) : SimulationValue() {
        override val display: String get() = "[${values.joinToString(", ")}]"
    }

    internal fun asDouble(): Double =
        when (this) {
            is VInt -> value.toDouble()
            is VDouble -> value
            else -> Double.NaN
        }

    companion object {
        @JvmStatic
        fun formatDouble(value: Double): String {
            if (value.isFinite() && abs(value - value.roundToLong()) < 1e-12) {
                return String.format(Locale.US, "%.1f", value)
            }
            return String.format(Locale.US, "%.12g", value)
        }
    }
}

/** Entspricht Swift `VisuStructSimulationValueType`. */
enum class SimulationValueType {
    INT,
    DOUBLE,
    STRING,
}

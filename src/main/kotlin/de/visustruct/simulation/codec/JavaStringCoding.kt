package de.visustruct.simulation.codec

/**
 * Entspricht Java [de.visustruct.control.XMLLeser.encodeS]/`decodeS` und Swift `VisuStructJavaStringCoding`:
 * Unicode-UTF-16-Code-Einheiten als `;`-getrennte Zahlen (pro Java `charAt`).
 */
object JavaStringCoding {

    @JvmStatic
    fun encode(s: String): String {
        if (s.isEmpty()) {
            return "-1;"
        }
        return buildString(s.length * 4) {
            for (i in s.indices) {
                append(s[i].code)
                append(';')
            }
        }
    }

    @JvmStatic
    fun decode(coded: String): String {
        val parts = coded.split(';')
        if (parts.size == 1 && parts[0] == "-1") {
            return ""
        }
        return buildString {
            for (p in parts) {
                if (p.isEmpty()) {
                    continue
                }
                val zeichenNummer = p.toIntOrNull() ?: continue
                if (zeichenNummer == -1) {
                    continue
                }
                append(zeichenNummer.toChar())
            }
        }
    }
}

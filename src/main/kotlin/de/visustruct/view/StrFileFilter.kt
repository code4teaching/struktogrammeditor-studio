package de.visustruct.view

import java.io.File
import javax.swing.filechooser.FileFilter

class StrFileFilter(private val filtertyp: Int) : FileFilter() {

    override fun accept(f: File): Boolean = f.isDirectory || dateiAkzeptiert(f.absolutePath)

    override fun getDescription(): String =
        when (filtertyp) {
            filterStruktogrammStudio -> "VisuStruct (*$EXTENSION_VISUSTRUCT)"
            filterAlleSpeicherdateien -> "Diagram files (*$EXTENSION_VISUSTRUCT, *.xml, legacy *.strk)"
            filterLegacyStrk -> "Legacy .strk (original editor)"
            2 -> "XML (*.xml)"
            filterAlleBilddateien -> "Image files"
            4 -> "BMP images"
            5 -> "GIF images"
            6 -> "JPEG images"
            7 -> "PNG images"
            else -> ""
        }

    private fun gibAktuelleErweiterung(): String =
        when (filtertyp) {
            filterStruktogrammStudio, filterAlleSpeicherdateien -> EXTENSION_VISUSTRUCT
            filterLegacyStrk -> ".strk"
            2 -> ".xml"
            filterAlleBilddateien -> ".png"
            4 -> ".bmp"
            5 -> ".gif"
            6 -> ".jpg"
            7 -> ".png"
            else -> ""
        }

    fun erweiterungBeiBedarfAnhaengen(pfad: String): String =
        if (dateiAkzeptiert(pfad)) pfad else pfad + gibAktuelleErweiterung()

    private fun dateiAkzeptiert(pfad: String): Boolean {
        val p = pfad.lowercase()
        return when (filtertyp) {
            filterAlleSpeicherdateien ->
                p.endsWith(EXTENSION_VISUSTRUCT) || p.endsWith(".xml") || p.endsWith(".strk")
            filterAlleBilddateien ->
                p.endsWith(".bmp") || p.endsWith(".gif") || p.endsWith(".jpg") ||
                    p.endsWith(".jpeg") || p.endsWith(".png")
            else -> p.endsWith(gibAktuelleErweiterung())
        }
    }

    companion object {
        const val filterAlleSpeicherdateien = 0
        const val filterStruktogrammStudio = 8
        const val EXTENSION_VISUSTRUCT = ".visustruct"
        const val filterLegacyStrk = 1
        private const val filterAlleBilddateien = 3

        @JvmStatic
        fun haengeStandardSpeicherendungAnFallsNoetig(pfad: String): String {
            val s = pfad.lowercase()
            if (s.endsWith(EXTENSION_VISUSTRUCT) || s.endsWith(".xml") || s.endsWith(".strk")) {
                return pfad
            }
            return pfad + EXTENSION_VISUSTRUCT
        }
    }
}

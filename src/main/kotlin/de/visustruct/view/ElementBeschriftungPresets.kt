package de.visustruct.view

import de.visustruct.i18n.I18n
import de.visustruct.i18n.StructureElementI18n

/**
 * Fest vorgegebene Textvorlagen für neu eingefügte Elemente (keine freie Eingabe).
 * Zwei Pakete — wie VisuStruct-SwiftUI: syntaxnahe Java-Vorgaben oder Begriffe gemäß Oberflächensprache.
 * Muss dieselbe Länge haben wie [EinstellungsDialog.anzahlStruktogrammElemente].
 */
object ElementBeschriftungPresets {

    const val PRESET_ENGLISH_JAVA = 0
    const val PRESET_DIDACTIC_I18N = 1
    const val ANZAHL_PRESETS = 2

    @JvmField
    val PRESET_DIALOG_REIHENFOLGE = intArrayOf(
        PRESET_ENGLISH_JAVA,
        PRESET_DIDACTIC_I18N,
    )

    private const val N = 10

    private val ENGLISH_JAVA_ROW = arrayOf(
        "Statement", "condition", "selector", "i = 0; i < n; i++",
        "condition", "condition", "\u221e", "break", "method()", "\u00f8",
    )

    private val JAVA_STANDARD_PALETTE_LABELS = arrayOf(
        "Statement",
        "if",
        "switch",
        "for",
        "while",
        "do",
        "while(true)",
        "break",
        "method()",
        "\u00f8",
    )

    init {
        require(PRESET_DIALOG_REIHENFOLGE.size == ANZAHL_PRESETS) { "Preset-Dialog-Reihenfolge" }
        require(JAVA_STANDARD_PALETTE_LABELS.size == N) { "Java-Palette-Länge" }
        require(ENGLISH_JAVA_ROW.size == N) { "English-Java-Preset-Länge" }
    }

    @JvmStatic
    fun migrateLegacyPresetIndex(legacy: Int): Int =
        when (legacy) {
            0, 1, 4 -> PRESET_DIDACTIC_I18N
            2, 3 -> PRESET_ENGLISH_JAVA
            else -> PRESET_ENGLISH_JAVA
        }

    @JvmStatic
    fun dialogPlatzFuerPreset(presetIndex: Int): Int {
        for (u in PRESET_DIALOG_REIHENFOLGE.indices) {
            if (PRESET_DIALOG_REIHENFOLGE[u] == presetIndex) {
                return u
            }
        }
        return 0
    }

    @JvmStatic
    fun presetIndexAtDialogPlatz(dialogPlatz: Int): Int {
        if (dialogPlatz < 0 || dialogPlatz >= PRESET_DIALOG_REIHENFOLGE.size) {
            return PRESET_ENGLISH_JAVA
        }
        return PRESET_DIALOG_REIHENFOLGE[dialogPlatz]
    }

    @JvmStatic
    fun javaStandardPaletteButtonLabel(typ: Int): String {
        if (typ < 0 || typ >= JAVA_STANDARD_PALETTE_LABELS.size) {
            return ""
        }
        return JAVA_STANDARD_PALETTE_LABELS[typ]
    }

    @JvmStatic
    fun getPresetAnzeigename(index: Int): String =
        when (index) {
            PRESET_ENGLISH_JAVA -> I18n.tr("elementPreset.englishJava")
            PRESET_DIDACTIC_I18N -> I18n.tr("elementPreset.didacticUiLanguage")
            else -> I18n.tr("elementPreset.englishJava")
        }

    @JvmStatic
    fun gibPresetZeile(index: Int): Array<String> {
        if (index == PRESET_DIDACTIC_I18N) {
            return StructureElementI18n.didacticDefaultTexts()
        }
        if (index < 0 || index >= ANZAHL_PRESETS) {
            return ENGLISH_JAVA_ROW.copyOf()
        }
        return ENGLISH_JAVA_ROW.copyOf()
    }

    @JvmStatic
    fun kopierePreset(index: Int): Array<String> = gibPresetZeile(index).copyOf()

    @JvmStatic
    fun kopierePresetIn(ziel: Array<String>?, index: Int) {
        require(ziel != null && ziel.size == N) { "ziel" }
        System.arraycopy(gibPresetZeile(index), 0, ziel, 0, N)
    }

    @JvmStatic
    fun findePresetIndex(aktuell: Array<String>?): Int {
        if (aktuell == null || aktuell.size != N) {
            return -1
        }
        for (p in 0 until ANZAHL_PRESETS) {
            val ref = gibPresetZeile(p)
            var match = true
            for (i in 0 until N) {
                val a = aktuell[i]?.trim() ?: ""
                if (ref[i] != a) {
                    match = false
                    break
                }
            }
            if (match) {
                return p
            }
        }
        return -1
    }

    @JvmStatic
    fun alsVorschauText(index: Int): String {
        val row = gibPresetZeile(index)
        val sb = StringBuilder()
        for (i in 0 until N) {
            if (i > 0) {
                sb.append('\n')
            }
            sb.append(StructureElementI18n.previewRowLabel(i)).append(": ").append(row[i])
        }
        return sb.toString()
    }
}

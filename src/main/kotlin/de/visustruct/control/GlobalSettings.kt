package de.visustruct.control

import de.visustruct.i18n.StructureElementI18n
import de.visustruct.view.CodeErzeuger
import de.visustruct.view.ElementBeschriftungPresets
import de.visustruct.view.StruktogrammPalette
import java.awt.Font
import java.awt.Font.PLAIN
import java.awt.GraphicsEnvironment
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Date
import java.util.HashSet
import java.util.Locale
import java.util.Properties
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.abs

object GlobalSettings : Konstanten {

    private val LOG: Logger = Logger.getLogger(GlobalSettings::class.java.name)

    /** Anzeigename in Titelleiste, Dock und Infodialog (unabhängig vom ursprünglichen Projektautor). */
    const val APP_DISPLAY_NAME: String = "VisuStruct"

    const val updateNummer: Int = 9

    @JvmField
    var versionsString: String = ""

    @JvmField
    var guiTitel: String = ""

    @JvmField
    val updateDaten: Array<String> = arrayOf(
        "30.05.2011", "31.05.2011", "05.06.2011", "11.09.2011", "18.01.2012", "17.02.2012",
        "02.05.2012", "16.08.2012", "13.05.2014", "10.07.2014",
    )

    @JvmField
    val logoName: String = "/icons/logostr.png"

    /** Vorgeschlagener Dateiname beim ersten Speichern (Endung `.visustruct`). */
    @JvmField
    val STANDARD_SPEICHERDATEI: String = "visustruct.visustruct"

    @JvmField
    val BUILDINFO_FILE: String = "/build.properties"

    @JvmField
    var buildInfoGitHash: String = ""

    @JvmField
    var buildInfoBuildTime: String = ""

    /**
     * Gewähltes UI-Theme (Menü „Settings → Theme“); technisch der Swing-LaF-Index.
     * Persistenzschlüssel in der Properties-Datei bleibt `lookandfeel` (Abwärtskompatibilität).
     */
    private var lookAndFeelAktuell: Int = 4

    /** UI-Sprache (Menüleiste, I18n); siehe [UI_LANGUAGE_OPTIONS]. */
    private var uiLanguageTag: String = "en"

    /** Tag und Menü-Label-Key für „Einstellungen → Sprachen“ (wie VisuStruct-swift). */
    @JvmRecord
    data class UiLanguageOption(val tag: String, val menuLabelKey: String)

    @JvmField
    val UI_LANGUAGE_OPTIONS: List<UiLanguageOption> = listOf(
        UiLanguageOption("en", "menu.settings.language.en"),
        UiLanguageOption("de", "menu.settings.language.de"),
        UiLanguageOption("es", "menu.settings.language.es"),
        UiLanguageOption("fr", "menu.settings.language.fr"),
        UiLanguageOption("it", "menu.settings.language.it"),
        UiLanguageOption("pl", "menu.settings.language.pl"),
        UiLanguageOption("tr", "menu.settings.language.tr"),
        UiLanguageOption("ko", "menu.settings.language.ko"),
        UiLanguageOption("ja", "menu.settings.language.ja"),
        UiLanguageOption("el", "menu.settings.language.el"),
        UiLanguageOption("uk", "menu.settings.language.uk"),
        UiLanguageOption("ar", "menu.settings.language.ar"),
        UiLanguageOption("ru", "menu.settings.language.ru"),
        UiLanguageOption("he", "menu.settings.language.he"),
        UiLanguageOption("nl", "menu.settings.language.nl"),
        UiLanguageOption("sv", "menu.settings.language.sv"),
        UiLanguageOption("da", "menu.settings.language.da"),
        UiLanguageOption("nb", "menu.settings.language.nb"),
        UiLanguageOption("zh_Hans", "menu.settings.language.zh_Hans"),
        UiLanguageOption("zh_Hant", "menu.settings.language.zh_Hant"),
        UiLanguageOption("pt_PT", "menu.settings.language.pt"),
    )

    /** `true`, wenn `uilanguage` in der geladenen Properties-Datei stand (sonst JVM-Locale als Vorgabe). */
    private var uiLanguageFromPropertiesFile: Boolean = false

    private var zuletztGenutzterSpeicherpfad: String = ""
    private var zuletztGenutzterPfadFuerBild: String = ""

    private const val MAX_RECENT_DIAGRAM_FILES: Int = 10
    private val recentDiagramPaths: MutableList<String> = ArrayList()

    /** Standard an: Spalten bei Verzweigung/Fallauswahl unten bündig (letztes Element wird bei Bedarf gestreckt). */
    private var letzteElementeStrecken: Boolean = true

    /**
     * Standardschrift für das Struktogramm-Canvas: **Sans-Serif** (näher an VisuStruct-SwiftUI / System-UI),
     * mit plattformüblicher Prioritätsliste, sonst [Font.SANS_SERIF].
     */
    @JvmField
    val fontStandard: Font = createPreferredDiagramSansFont(17)

    private fun createPreferredDiagramSansFont(size: Int): Font {
        val preferred = arrayOf(
            "Segoe UI",
            "SF Pro Text",
            "Helvetica Neue",
            "Lucida Grande",
            "Roboto",
            "Ubuntu",
            "Noto Sans",
            "DejaVu Sans",
            "Arial",
        )
        try {
            val installed =
                HashSet(Arrays.asList(*GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames))
            for (family in preferred) {
                if (installed.contains(family)) {
                    return Font(family, Font.PLAIN, size)
                }
            }
        } catch (_: RuntimeException) {
            // Headless o. Ä. → Fallback
        }
        return Font(Font.SANS_SERIF, PLAIN, size)
    }

    private const val einstellungsDateiPfad: String = "visustruct.properties"
    private const val einstellungsDateiPfadLegacy: String = "struktogrammeditor.properties"
    private const val einstellungsDateiPfadBisVersion1Punkt4: String = "StruktogrammeditorEinstellungen.txt"

    private var codeErzeugerEinrueckungGesamt: Int = 3
    private var codeErzeugerEinrueckungProStufe: Int = 3

    /** [CodeErzeuger.typJava], [CodeErzeuger.typPython] oder [CodeErzeuger.typJavaScript]. */
    private var codeErzeugerProgrammiersprache: Int = CodeErzeuger.typJava
    private var codeErzeugerAlsKommentar: Boolean = false

    private var elementShortcutsVerwenden: Boolean = true

    private var xZoomProSchritt: Int = 10
    private var yZoomProSchritt: Int = 10

    /** Textpaket für neu eingefügte Elemente (siehe [ElementBeschriftungPresets]). */
    private var elementBeschriftungPresetIndex: Int = ElementBeschriftungPresets.PRESET_ENGLISH_JAVA

    /** Erlaubte Pausen (Sekunden) zwischen zwei Schritten bei Simulations-Wiedergabe. */
    @JvmField
    val SIMULATION_PLAY_DELAY_SECONDS: DoubleArray = doubleArrayOf(0.2, 0.5, 0.75, 1.0, 1.5, 2.0)

    private const val DEFAULT_SIMULATION_PLAY_DELAY_SEC: Double = 0.5

    private var simulationPlayDelaySec: Double = DEFAULT_SIMULATION_PLAY_DELAY_SEC

    /** Welcher Block im Diagramm während der Simulation hervorgehoben wird. */
    enum class SimulationHighlightMode {
        /** Zuletzt ausgeführter Schritt (Standard). */
        LAST_EXECUTED,

        /** Nächster auszuführender Schritt. */
        NEXT_STEP,
    }

    private var simulationHighlightMode: SimulationHighlightMode = SimulationHighlightMode.LAST_EXECUTED

    /**
     * Extended-Modifier-Maske für Menü-Kurzbefehle (Strg bzw. ⌘), z. B. für
     * [javax.swing.KeyStroke.getKeyStroke] und `KeyEvent.getModifiersEx()`.
     * Ohne Anzeige (Headless) plattformtypischer Ersatz.
     */
    private fun initStrgOderApfelMask(): Int =
        try {
            if (GraphicsEnvironment.isHeadless()) {
                fallbackStrgOderApfelMask()
            } else {
                Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
            }
        } catch (_: HeadlessException) {
            fallbackStrgOderApfelMask()
        }

    private fun fallbackStrgOderApfelMask(): Int {
        val os = System.getProperty("os.name", "").lowercase(Locale.getDefault())
        return if (os.contains("mac")) InputEvent.META_DOWN_MASK else InputEvent.CTRL_DOWN_MASK
    }

    @JvmField
    val strgOderApfelMask: Int = initStrgOderApfelMask()

    init {
        readBuildInfoFile()
    }

    @JvmStatic
    fun init() {
        uiLanguageFromPropertiesFile = false
        loadSettings()
        if (!uiLanguageFromPropertiesFile) {
            applyDefaultUiLanguageFromRuntimeLocale()
        }
    }

    private fun readBuildInfoFile() {
        try {
            val pr = Properties()
            try {
                val `in` = BufferedInputStream(GlobalSettings::class.java.getResourceAsStream(BUILDINFO_FILE))
                pr.load(`in`)
                `in`.close()
            } catch (e: IOException) {
                LOG.log(Level.WARNING, "build.properties konnte nicht gelesen werden", e)
            }

            var s = pr.getProperty("version")
            if (!s.isNullOrBlank()) {
                versionsString = s.trim()
                guiTitel = APP_DISPLAY_NAME + " " + versionsString
            } else {
                guiTitel = APP_DISPLAY_NAME
            }

            s = pr.getProperty("revision")
            if (!s.isNullOrBlank()) {
                buildInfoGitHash = s.trim()
            }

            s = pr.getProperty("timestamp")
            if (!s.isNullOrBlank()) {
                try {
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                    buildInfoBuildTime = sdf.format(Date(s.trim().toLong()))
                } catch (_: NumberFormatException) {
                    // optional build.properties from filtered POM — ignore invalid timestamp
                }
            }
        } catch (e: RuntimeException) {
            LOG.log(Level.WARNING, "build.properties: unerwarteter Fehler", e)
        }
    }

    private fun loadSettings() {
        // Alte Textdatei (bis Version 1.4) entfernen — Startbeschriftungen sind fest englisch.
        val f = File(einstellungsDateiPfadBisVersion1Punkt4)
        if (f.exists()) {
            if (!f.delete()) {
                f.deleteOnExit()
            }
        }

        val primary = File(einstellungsDateiPfad)
        var toLoad: File = primary
        if (!primary.exists()) {
            val legacy = File(einstellungsDateiPfadLegacy)
            if (legacy.exists()) {
                toLoad = legacy
            }
        }

        if (!toLoad.exists()) {
            return
        }

        val pr = Properties()
        try {
            BufferedInputStream(FileInputStream(toLoad)).use {
                pr.load(it)
            }
        } catch (e: IOException) {
            LOG.log(Level.WARNING, "Einstellungsdatei konnte nicht gelesen werden: $toLoad", e)
            return
        }

        applySettingsFromProperties(pr)
    }

    private fun applySettingsFromProperties(pr: Properties) {
        var s: String?

        s = pr.getProperty("stretchlast")
        if (s != null) {
            letzteElementeStrecken = s == "1"
        }

        s = pr.getProperty("cespaces")
        if (s != null) {
            codeErzeugerEinrueckungGesamt = Integer.parseInt(s)
        }

        s = pr.getProperty("cespacesperstep")
        if (s != null) {
            codeErzeugerEinrueckungProStufe = Integer.parseInt(s)
        }

        s = pr.getProperty("celanguage")
        if (s != null) {
            try {
                val lang = Integer.parseInt(s.trim())
                codeErzeugerProgrammiersprache = when (lang) {
                    CodeErzeuger.typPython -> CodeErzeuger.typPython
                    CodeErzeuger.typJavaScript -> CodeErzeuger.typJavaScript
                    else -> CodeErzeuger.typJava
                }
            } catch (_: NumberFormatException) {
                codeErzeugerProgrammiersprache = CodeErzeuger.typJava
            }
        }

        s = pr.getProperty("cecomments")
        if (s != null) {
            codeErzeugerAlsKommentar = s == "1"
        }

        s = pr.getProperty("useelementshortcuts")
        if (s != null) {
            elementShortcutsVerwenden = s == "1"
        }

        s = pr.getProperty("pathfiles")
        if (s != null) {
            zuletztGenutzterSpeicherpfad = s
        }

        s = pr.getProperty("pathpictures")
        if (s != null) {
            zuletztGenutzterPfadFuerBild = s
        }

        s = pr.getProperty("zoomx")
        if (s != null) {
            xZoomProSchritt = Integer.parseInt(s)
        }

        s = pr.getProperty("zoomy")
        if (s != null) {
            yZoomProSchritt = Integer.parseInt(s)
        }

        s = pr.getProperty("lookandfeel")
        if (s != null) {
            lookAndFeelAktuell = Integer.parseInt(s)
            // Früher Index 3 = „Metal (classic)“ (entfernt) → Modern · light
            if (lookAndFeelAktuell == 3) {
                lookAndFeelAktuell = Konstanten.lookAndFeelFlatLight
            }
            if (lookAndFeelAktuell < Konstanten.lookAndFeelOSStandard ||
                lookAndFeelAktuell > Konstanten.lookAndFeelFlatDark ||
                (lookAndFeelAktuell > Konstanten.lookAndFeelNimbus && lookAndFeelAktuell < Konstanten.lookAndFeelFlatLight)
            ) {
                lookAndFeelAktuell = Konstanten.lookAndFeelFlatLight
            }
        }

        /* Neu: elementlabelpreset = 0 (Java) | 1 (didaktisch/UI-Sprache). Legacy: elementbeschriftungpreset 0–4. */
        s = pr.getProperty("elementlabelpreset")
        if (!s.isNullOrBlank()) {
            try {
                val p = Integer.parseInt(s.trim())
                if (p == ElementBeschriftungPresets.PRESET_ENGLISH_JAVA ||
                    p == ElementBeschriftungPresets.PRESET_DIDACTIC_I18N
                ) {
                    elementBeschriftungPresetIndex = p
                }
            } catch (_: NumberFormatException) {
                // ungültig → Standard beibehalten
            }
        } else {
            s = pr.getProperty("elementbeschriftungpreset")
            if (!s.isNullOrBlank()) {
                try {
                    val p = Integer.parseInt(s.trim())
                    if (p in 0..4) {
                        elementBeschriftungPresetIndex = ElementBeschriftungPresets.migrateLegacyPresetIndex(p)
                    }
                } catch (_: NumberFormatException) {
                    // ungültig → Standard beibehalten
                }
            }
        }

        s = pr.getProperty("uilanguage")
        if (s != null && s.isNotEmpty()) {
            setUiLanguageTag(s)
            uiLanguageFromPropertiesFile = true
        }

        s = pr.getProperty("simulationplaydelay")
        if (!s.isNullOrBlank()) {
            try {
                setSimulationPlayDelaySec(s.trim().toDouble())
            } catch (_: NumberFormatException) {
                simulationPlayDelaySec = DEFAULT_SIMULATION_PLAY_DELAY_SEC
            }
        }

        s = pr.getProperty("simulationhighlight")
        if (!s.isNullOrBlank()) {
            setSimulationHighlightMode(parseSimulationHighlightMode(s.trim()))
        }

        recentDiagramPaths.clear()
        for (i in 0 until MAX_RECENT_DIAGRAM_FILES) {
            s = pr.getProperty("recentdiagram$i")
            if (s.isNullOrBlank()) {
                break
            }
            recentDiagramPaths.add(s.trim())
        }
    }

    /** Wenn keine `uilanguage` in den Einstellungen: an JVM-Locale anlehnen (z. B. deutsch → `de`). */
    private fun applyDefaultUiLanguageFromRuntimeLocale() {
        val def = Locale.getDefault()
        val lang = def.language
        when {
            lang == "de" -> {
                setUiLanguageTag("de")
                return
            }
            lang == "pt" && "PT".equals(def.country, ignoreCase = true) -> {
                setUiLanguageTag("pt_PT")
                return
            }
            lang == "es" -> {
                setUiLanguageTag("es")
                return
            }
            lang == "fr" -> {
                setUiLanguageTag("fr")
                return
            }
            lang == "it" -> {
                setUiLanguageTag("it")
                return
            }
            lang == "nl" -> {
                setUiLanguageTag("nl")
                return
            }
            lang == "pl" -> {
                setUiLanguageTag("pl")
                return
            }
            lang == "tr" -> {
                setUiLanguageTag("tr")
                return
            }
            lang == "ru" -> {
                setUiLanguageTag("ru")
                return
            }
            lang == "ko" -> {
                setUiLanguageTag("ko")
                return
            }
            lang == "ja" -> {
                setUiLanguageTag("ja")
                return
            }
            lang == "el" -> {
                setUiLanguageTag("el")
                return
            }
            lang == "uk" -> {
                setUiLanguageTag("uk")
                return
            }
            lang == "ar" -> {
                setUiLanguageTag("ar")
                return
            }
            lang == "he" -> {
                setUiLanguageTag("he")
                return
            }
            lang == "sv" -> {
                setUiLanguageTag("sv")
                return
            }
            lang == "da" -> {
                setUiLanguageTag("da")
                return
            }
            lang == "nb" -> {
                setUiLanguageTag("nb")
                return
            }
            lang == "zh" -> {
                val country = def.country
                if ("TW".equals(country, ignoreCase = true) ||
                    "HK".equals(country, ignoreCase = true) ||
                    "MO".equals(country, ignoreCase = true)
                ) {
                    setUiLanguageTag("zh_Hant")
                } else {
                    setUiLanguageTag("zh_Hans")
                }
                return
            }
        }
        setUiLanguageTag("en")
    }

    @JvmStatic
    fun saveSettings() {
        val properties = Properties()

        properties.setProperty("stretchlast", if (letzteElementeStrecken) "1" else "0")

        properties.setProperty("cespaces", "" + codeErzeugerEinrueckungGesamt)
        properties.setProperty("cespacesperstep", "" + codeErzeugerEinrueckungProStufe)
        properties.setProperty("celanguage", "" + codeErzeugerProgrammiersprache)
        properties.setProperty("cecomments", if (codeErzeugerAlsKommentar) "1" else "0")

        properties.setProperty("useelementshortcuts", if (elementShortcutsVerwenden) "1" else "0")

        properties.setProperty("pathfiles", zuletztGenutzterSpeicherpfad)
        properties.setProperty("pathpictures", zuletztGenutzterPfadFuerBild)

        properties.setProperty("zoomx", "" + xZoomProSchritt)
        properties.setProperty("zoomy", "" + yZoomProSchritt)

        properties.setProperty("lookandfeel", "" + lookAndFeelAktuell)

        properties.setProperty("elementlabelpreset", "" + elementBeschriftungPresetIndex)

        properties.setProperty("uilanguage", uiLanguageTag)

        properties.setProperty("simulationplaydelay", simulationPlayDelaySec.toString())
        properties.setProperty(
            "simulationhighlight",
            if (simulationHighlightMode == SimulationHighlightMode.NEXT_STEP) "next" else "last",
        )

        for (i in 0 until MAX_RECENT_DIAGRAM_FILES) {
            if (i < recentDiagramPaths.size) {
                properties.setProperty("recentdiagram$i", recentDiagramPaths[i])
            } else {
                properties.setProperty("recentdiagram$i", "")
            }
        }

        try {
            val out = BufferedOutputStream(FileOutputStream(File(einstellungsDateiPfad)))
            out.use {
                properties.store(it, "$APP_DISPLAY_NAME Properties")
            }

            val legacy = File(einstellungsDateiPfadLegacy)
            if (legacy.exists() && !legacy.delete()) {
                legacy.deleteOnExit()
            }
        } catch (e: FileNotFoundException) {
            LOG.log(Level.SEVERE, "Einstellungen: Datei nicht gefunden beim Speichern", e)
        } catch (e: IOException) {
            LOG.log(Level.SEVERE, "Einstellungen konnten nicht gespeichert werden", e)
        }
    }

    @JvmStatic
    fun setzeSpeicherpfad(pfad: String) {
        if (pfad != "") {
            zuletztGenutzterSpeicherpfad = pfad // letzter richtiger Pfad wird gespeichert
        }
    }

    @JvmStatic
    fun setzeBildSpeicherpfad(pfad: String) {
        if (pfad != "") {
            zuletztGenutzterPfadFuerBild = pfad
        }
    }

    @JvmStatic
    fun getZuletztGenutzterSpeicherpfad(): String = zuletztGenutzterSpeicherpfad

    @JvmStatic
    fun getZuletztGenutzterPfadFuerBild(): String = zuletztGenutzterPfadFuerBild

    /** Unveränderliche Liste zuletzt geöffneter oder gespeicherter Struktogramm-Dateien (maximal zehn Einträge). */
    @JvmStatic
    fun getRecentDiagramPaths(): List<String> = recentDiagramPaths.toList()

    /** Pfad in die MRU-Liste (vorne); Duplikate werden zusammengeführt. */
    @JvmStatic
    fun rememberRecentStruktogrammPath(path: String?) {
        if (path == null) {
            return
        }
        var p = path.trim()
        if (p.isEmpty()) {
            return
        }
        try {
            p = File(p).canonicalPath
        } catch (_: IOException) {
            p = File(p).absolutePath
        }
        recentDiagramPaths.remove(p)
        recentDiagramPaths.add(0, p)
        while (recentDiagramPaths.size > MAX_RECENT_DIAGRAM_FILES) {
            recentDiagramPaths.removeAt(recentDiagramPaths.size - 1)
        }
    }

    /** Entfernt einen Eintrag (z. B. Datei existiert nicht mehr). */
    @JvmStatic
    fun removeRecentStruktogrammPath(path: String?) {
        if (path == null) {
            return
        }
        var p = path.trim()
        if (p.isEmpty()) {
            return
        }
        try {
            p = File(p).canonicalPath
        } catch (_: IOException) {
            p = File(p).absolutePath
        }
        recentDiagramPaths.remove(p)
    }

    @JvmStatic
    fun setzeLetzteElementeStrecken(strecken: Boolean) {
        letzteElementeStrecken = strecken
    }

    @JvmStatic
    fun gibLetzteElementeStrecken(): Boolean = letzteElementeStrecken

    /** Text für neu erzeugte Struktogramm-Elemente (gewähltes Textpaket). */
    @JvmStatic
    fun gibElementBeschriftung(typNummer: Int): String {
        val row = ElementBeschriftungPresets.gibPresetZeile(elementBeschriftungPresetIndex)
        if (typNummer >= 0 && typNummer < row.size) {
            return row[typNummer]
        }
        return StruktogrammPalette.getDefaultTextForNewElement(typNummer)
    }

    /**
     * Text auf der linken Palette: bei Preset **Java (Standard)** syntaxnahe Kurzformen (`if`, `while`, …),
     * sonst kurze didaktische Namen gemäß **UI-Sprache** (`structure.palette.*`).
     */
    @JvmStatic
    fun gibPaletteButtonBeschriftung(typNummer: Int): String =
        if (elementBeschriftungPresetIndex == ElementBeschriftungPresets.PRESET_ENGLISH_JAVA) {
            ElementBeschriftungPresets.javaStandardPaletteButtonLabel(typNummer)
        } else {
            StructureElementI18n.paletteShortLabel(typNummer)
        }

    @JvmStatic
    fun getElementBeschriftungPresetIndex(): Int = elementBeschriftungPresetIndex

    /**
     * Wendet ein Textpaket an (nur für **neu** eingefügte Elemente; bestehende Blöcke bleiben unverändert).
     */
    @JvmStatic
    fun wendeElementBeschriftungsPresetAn(presetIndex: Int) {
        elementBeschriftungPresetIndex =
            if (presetIndex < 0 || presetIndex >= ElementBeschriftungPresets.ANZAHL_PRESETS) {
                ElementBeschriftungPresets.PRESET_ENGLISH_JAVA
            } else {
                presetIndex
            }
    }

    @JvmStatic
    fun setCodeErzeugerEinrueckungGesamt(codeErzeugerEinrueckungGesamt: Int) {
        GlobalSettings.codeErzeugerEinrueckungGesamt = codeErzeugerEinrueckungGesamt
    }

    @JvmStatic
    fun getCodeErzeugerEinrueckungGesamt(): Int = codeErzeugerEinrueckungGesamt

    @JvmStatic
    fun setCodeErzeugerEinrueckungProStufe(codeErzeugerEinrueckungProStufe: Int) {
        GlobalSettings.codeErzeugerEinrueckungProStufe = codeErzeugerEinrueckungProStufe
    }

    @JvmStatic
    fun getCodeErzeugerEinrueckungProStufe(): Int = codeErzeugerEinrueckungProStufe

    @JvmStatic
    fun getCodeErzeugerProgrammiersprache(): Int = codeErzeugerProgrammiersprache

    @JvmStatic
    fun setCodeErzeugerProgrammiersprache(codeErzeugerProgrammiersprache: Int) {
        GlobalSettings.codeErzeugerProgrammiersprache = when (codeErzeugerProgrammiersprache) {
            CodeErzeuger.typPython -> CodeErzeuger.typPython
            CodeErzeuger.typJavaScript -> CodeErzeuger.typJavaScript
            else -> CodeErzeuger.typJava
        }
    }

    @JvmStatic
    fun isCodeErzeugerAlsKommentar(): Boolean = codeErzeugerAlsKommentar

    @JvmStatic
    fun setCodeErzeugerAlsKommentar(codeErzeugerAlsKommentar: Boolean) {
        GlobalSettings.codeErzeugerAlsKommentar = codeErzeugerAlsKommentar
    }

    @JvmStatic
    fun getXZoomProSchritt(): Int = xZoomProSchritt

    @JvmStatic
    fun setXZoomProSchritt(xZoomProSchritt: Int) {
        GlobalSettings.xZoomProSchritt = xZoomProSchritt
    }

    @JvmStatic
    fun getYZoomProSchritt(): Int = yZoomProSchritt

    @JvmStatic
    fun setYZoomProSchritt(yZoomProSchritt: Int) {
        GlobalSettings.yZoomProSchritt = yZoomProSchritt
    }

    @JvmStatic
    fun isElementShortcutsVerwenden(): Boolean = elementShortcutsVerwenden

    @JvmStatic
    fun setElementShortcutsVerwenden(elementShortcutsVerwenden: Boolean) {
        GlobalSettings.elementShortcutsVerwenden = elementShortcutsVerwenden
    }

    /** @return Index des gewählten Themes (siehe [Konstanten]). */
    @JvmStatic
    fun getLookAndFeelAktuell(): Int = lookAndFeelAktuell

    @JvmStatic
    fun setLookAndFeelAktuell(themeIndex: Int) {
        lookAndFeelAktuell = themeIndex
    }

    /** Locale für UI-Texte. */
    @JvmStatic
    fun getUiLocale(): Locale =
        when (uiLanguageTag) {
            "de" -> Locale.GERMAN
            "pt_PT" -> Locale.forLanguageTag("pt-PT")
            "es" -> Locale.forLanguageTag("es")
            "fr" -> Locale.FRENCH
            "it" -> Locale.ITALIAN
            "nl" -> Locale.forLanguageTag("nl")
            "pl" -> Locale.forLanguageTag("pl")
            "tr" -> Locale.forLanguageTag("tr")
            "ru" -> Locale.forLanguageTag("ru")
            "ko" -> Locale.KOREAN
            "ja" -> Locale.JAPANESE
            "el" -> Locale.forLanguageTag("el")
            "uk" -> Locale.forLanguageTag("uk")
            "ar" -> Locale.forLanguageTag("ar")
            "he" -> Locale.forLanguageTag("he")
            "sv" -> Locale.forLanguageTag("sv")
            "da" -> Locale.forLanguageTag("da")
            "nb" -> Locale.forLanguageTag("nb")
            "zh_Hans" -> Locale.forLanguageTag("zh-Hans")
            "zh_Hant" -> Locale.forLanguageTag("zh-Hant")
            else -> Locale.ENGLISH
        }

    @JvmStatic
    fun getUiLanguageTag(): String = uiLanguageTag

    /** Kanonischer Tag aus [UI_LANGUAGE_OPTIONS]; unbekannt → `en`. */
    @JvmStatic
    fun normalizeUiLanguageTag(tag: String?): String {
        if (tag.isNullOrBlank()) {
            return "en"
        }
        val t = tag.trim()
        for (opt in UI_LANGUAGE_OPTIONS) {
            if (opt.tag.equals(t, ignoreCase = true)) {
                return opt.tag
            }
        }
        val hy = t.replace('_', '-').lowercase(Locale.ROOT)
        for (opt in UI_LANGUAGE_OPTIONS) {
            val optHy = opt.tag.replace('_', '-').lowercase(Locale.ROOT)
            if (hy == optHy || hy.startsWith("$optHy-")) {
                return opt.tag
            }
        }
        if (t.equals("pt", ignoreCase = true) || hy == "pt-pt") {
            return "pt_PT"
        }
        if (hy == "zh-cn" || hy == "zh-hans") {
            return "zh_Hans"
        }
        if (hy == "zh-tw" || hy == "zh-hant" || hy == "zh-hk") {
            return "zh_Hant"
        }
        return "en"
    }

    /**
     * Setzt die UI-Sprache; unbekannte Werte werden wie `en` behandelt.
     */
    @JvmStatic
    fun setUiLanguageTag(tag: String) {
        uiLanguageTag = normalizeUiLanguageTag(tag)
    }

    @JvmStatic
    fun isUiGerman(): Boolean = uiLanguageTag == "de"

    @JvmStatic
    fun isUiPortuguesePortugal(): Boolean = uiLanguageTag == "pt_PT"

    @JvmStatic
    fun isUiSpanish(): Boolean = uiLanguageTag == "es"

    @JvmStatic
    fun isUiFrench(): Boolean = uiLanguageTag == "fr"

    @JvmStatic
    fun isUiItalian(): Boolean = uiLanguageTag == "it"

    @JvmStatic
    fun isUiDutch(): Boolean = uiLanguageTag == "nl"

    @JvmStatic
    fun isUiPolish(): Boolean = uiLanguageTag == "pl"

    @JvmStatic
    fun isUiTurkish(): Boolean = uiLanguageTag == "tr"

    @JvmStatic
    fun isUiRussian(): Boolean = uiLanguageTag == "ru"

    @JvmStatic
    fun getSimulationPlayDelaySec(): Double = simulationPlayDelaySec

    @JvmStatic
    fun getSimulationPlayDelayMs(): Int =
        maxOf(50, kotlin.math.round(simulationPlayDelaySec * 1000.0).toInt())

    @JvmStatic
    fun setSimulationPlayDelaySec(seconds: Double) {
        for (opt in SIMULATION_PLAY_DELAY_SECONDS) {
            if (abs(opt - seconds) < 1e-9) {
                simulationPlayDelaySec = opt
                return
            }
        }
        simulationPlayDelaySec = DEFAULT_SIMULATION_PLAY_DELAY_SEC
    }

    @JvmStatic
    fun getSimulationPlayDelayIndex(): Int {
        for (i in SIMULATION_PLAY_DELAY_SECONDS.indices) {
            if (abs(SIMULATION_PLAY_DELAY_SECONDS[i] - simulationPlayDelaySec) < 1e-9) {
                return i
            }
        }
        return 1
    }

    @JvmStatic
    fun getSimulationHighlightMode(): SimulationHighlightMode = simulationHighlightMode

    @JvmStatic
    fun isSimulationHighlightNextStep(): Boolean =
        simulationHighlightMode == SimulationHighlightMode.NEXT_STEP

    @JvmStatic
    fun setSimulationHighlightMode(mode: SimulationHighlightMode) {
        simulationHighlightMode =
            if (mode == SimulationHighlightMode.NEXT_STEP) {
                SimulationHighlightMode.NEXT_STEP
            } else {
                SimulationHighlightMode.LAST_EXECUTED
            }
    }

    private fun parseSimulationHighlightMode(raw: String): SimulationHighlightMode =
        if (raw.equals("next", ignoreCase = true)) {
            SimulationHighlightMode.NEXT_STEP
        } else {
            SimulationHighlightMode.LAST_EXECUTED
        }
}
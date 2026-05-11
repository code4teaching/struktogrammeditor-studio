package de.visustruct.control;

import java.awt.Font;
import static java.awt.Font.PLAIN;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import de.visustruct.i18n.StructureElementI18n;
import de.visustruct.view.CodeErzeuger;
import de.visustruct.view.ElementBeschriftungPresets;
import de.visustruct.view.StruktogrammPalette;

public class GlobalSettings implements Konstanten{

	/** Anzeigename in Titelleiste, Dock und Infodialog (unabhängig vom ursprünglichen Projektautor). */
	public static final String APP_DISPLAY_NAME = "VisuStruct";

	public static final int updateNummer = 9;
	public static String versionsString = "";
	public static String guiTitel = "";
	public static final String[] updateDaten = {"30.05.2011", "31.05.2011", "05.06.2011", "11.09.2011", "18.01.2012", "17.02.2012", "02.05.2012", "16.08.2012", "13.05.2014", "10.07.2014"};
	
	public static final String logoName = "/icons/logostr.png";

	/** Vorgeschlagener Dateiname beim ersten Speichern (Endung {@code .visustruct}). */
	public static final String STANDARD_SPEICHERDATEI = "visustruct.visustruct";
	
	public static final String BUILDINFO_FILE = "/build.properties";
	public static String buildInfoGitHash = "";
	public static String buildInfoBuildTime = "";
	
	/**
	 * Gewähltes UI-Theme (Menü „Settings → Theme“); technisch der Swing-LaF-Index.
	 * Persistenzschlüssel in der Properties-Datei bleibt {@code lookandfeel} (Abwärtskompatibilität).
	 */
	private static int lookAndFeelAktuell = 4;

	/** UI-Sprache: {@code en}, {@code de} oder {@code pt_PT} (Portugal; Menüleiste, I18n). */
	private static String uiLanguageTag = "en";

	/** {@code true}, wenn {@code uilanguage} in der geladenen Properties-Datei stand (sonst JVM-Locale als Vorgabe). */
	private static boolean uiLanguageFromPropertiesFile = false;

	private static String zuletztGenutzterSpeicherpfad = "";
	private static String zuletztGenutzterPfadFuerBild = "";
	/** Standard an: Spalten bei Verzweigung/Fallauswahl unten bündig (letztes Element wird bei Bedarf gestreckt). */
	private static boolean letzteElementeStrecken = true;
	/**
	 * Standardschrift für das Struktogramm-Canvas: **Sans-Serif** (näher an VisuStruct-SwiftUI / System-UI),
	 * mit plattformüblicher Prioritätsliste, sonst {@link Font#SANS_SERIF}.
	 */
	public static final Font fontStandard = createPreferredDiagramSansFont(17);

	private static Font createPreferredDiagramSansFont(int size) {
		String[] preferred = {
			"Segoe UI",
			"SF Pro Text",
			"Helvetica Neue",
			"Lucida Grande",
			"Roboto",
			"Ubuntu",
			"Noto Sans",
			"DejaVu Sans",
			"Arial",
		};
		try {
			Set<String> installed = new HashSet<>(Arrays.asList(
					GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
			for (String family : preferred) {
				if (installed.contains(family)) {
					return new Font(family, Font.PLAIN, size);
				}
			}
		} catch (RuntimeException ignored) {
			// Headless o. Ä. → Fallback
		}
		return new Font(Font.SANS_SERIF, PLAIN, size);
	}
	private static final String einstellungsDateiPfad = "visustruct.properties";
	private static final String einstellungsDateiPfadLegacy = "struktogrammeditor.properties";
	private static final String einstellungsDateiPfadBisVersion1Punkt4 = "StruktogrammeditorEinstellungen.txt";
	
	private static int codeErzeugerEinrueckungGesamt = 3;
	private static int codeErzeugerEinrueckungProStufe = 3;
	/** {@link CodeErzeuger#typJava}, {@link CodeErzeuger#typPython} oder {@link CodeErzeuger#typJavaScript}. */
	private static int codeErzeugerProgrammiersprache = CodeErzeuger.typJava;
	private static boolean codeErzeugerAlsKommentar = true;
	
	private static boolean beiMausradGroesseAendern = false;
	private static boolean elementShortcutsVerwenden = true;
	
	private static int xZoomProSchritt = 10;
	private static int yZoomProSchritt = 10;

	/** Textpaket für neu eingefügte Elemente (siehe {@link ElementBeschriftungPresets}). */
	private static int elementBeschriftungPresetIndex = ElementBeschriftungPresets.PRESET_ENGLISH_JAVA;

	/**
	 * Extended-Modifier-Maske für Menü-Kurzbefehle (Strg bzw. ⌘), z. B. für
	 * {@link javax.swing.KeyStroke#getKeyStroke(int, int)} und {@code KeyEvent#getModifiersEx()}.
	 * Ohne Anzeige (Headless) plattformtypischer Ersatz.
	 */
	private static int initStrgOderApfelMask(){
		try {
			if (GraphicsEnvironment.isHeadless()){
				return fallbackStrgOderApfelMask();
			}
			return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		}catch (HeadlessException e){
			return fallbackStrgOderApfelMask();
		}
	}

	private static int fallbackStrgOderApfelMask(){
		String os = System.getProperty("os.name", "").toLowerCase();
		return os.contains("mac") ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
	}

	public static final int strgOderApfelMask = initStrgOderApfelMask();

	static {
		readBuildInfoFile();
	}

	public static void init(){
		uiLanguageFromPropertiesFile = false;
		loadSettings();
		if (!uiLanguageFromPropertiesFile) {
			applyDefaultUiLanguageFromRuntimeLocale();
		}
	}
	
	private static void readBuildInfoFile(){

		try {
	
			Properties pr = new Properties();

			InputStream in = null;
			try {
				in = new BufferedInputStream(GlobalSettings.class.getResourceAsStream(BUILDINFO_FILE));
				pr.load(in);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			String s;
			
			s = pr.getProperty("version");
			if (s != null && !s.isBlank()) {
				versionsString = s.trim();
				guiTitel = APP_DISPLAY_NAME + " " + versionsString;
			} else {
				guiTitel = APP_DISPLAY_NAME;
			}

			s = pr.getProperty("revision");
			if (s != null && !s.isBlank()) {
				buildInfoGitHash = s.trim();
			}

			s = pr.getProperty("timestamp");
			if (s != null && !s.isBlank()) {
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
					buildInfoBuildTime = sdf.format(new Date(Long.parseLong(s.trim())));
				} catch (NumberFormatException ignored) {
					// optional build.properties from filtered POM — ignore invalid timestamp
				}
			}

		} catch (RuntimeException e){
			e.printStackTrace();
		}
	}
	
	private static void loadSettings(){
		// Alte Textdatei (bis Version 1.4) entfernen — Startbeschriftungen sind fest englisch.
		File f = new File(einstellungsDateiPfadBisVersion1Punkt4);
		if(f.exists()){
			if(!f.delete()){
				f.deleteOnExit();
			}
		}

		File primary = new File(einstellungsDateiPfad);
		File toLoad = primary;
		if (!primary.exists()) {
			File legacy = new File(einstellungsDateiPfadLegacy);
			if (legacy.exists()) {
				toLoad = legacy;
			}
		}

		if (!toLoad.exists()) {
			return;
		}

		Properties pr = new Properties();
		try {
			try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(toLoad))) {
				pr.load(in);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		applySettingsFromProperties(pr);
	}

	private static void applySettingsFromProperties(Properties pr) {
		String s;

		s = pr.getProperty("stretchlast");
		if(s != null){
			letzteElementeStrecken = s.equals("1");
		}

		s = pr.getProperty("cespaces");
		if(s != null){
			codeErzeugerEinrueckungGesamt = Integer.parseInt(s);
		}

		s = pr.getProperty("cespacesperstep");
		if(s != null){
			codeErzeugerEinrueckungProStufe = Integer.parseInt(s);
		}

		s = pr.getProperty("celanguage");
		if (s != null) {
			try {
				int lang = Integer.parseInt(s.trim());
				if (lang == CodeErzeuger.typPython) {
					codeErzeugerProgrammiersprache = CodeErzeuger.typPython;
				} else if (lang == CodeErzeuger.typJavaScript) {
					codeErzeugerProgrammiersprache = CodeErzeuger.typJavaScript;
				} else {
					codeErzeugerProgrammiersprache = CodeErzeuger.typJava;
				}
			} catch (NumberFormatException ignored) {
				codeErzeugerProgrammiersprache = CodeErzeuger.typJava;
			}
		}

		s = pr.getProperty("cecomments");
		if(s != null){
			codeErzeugerAlsKommentar = s.equals("1");
		}

		s = pr.getProperty("mousewheelresize");
		if(s != null){
			beiMausradGroesseAendern = s.equals("1");
		}

		s = pr.getProperty("useelementshortcuts");
		if(s != null){
			elementShortcutsVerwenden = s.equals("1");
		}

		s = pr.getProperty("pathfiles");
		if(s != null){
			zuletztGenutzterSpeicherpfad = s;
		}

		s = pr.getProperty("pathpictures");
		if(s != null){
			zuletztGenutzterPfadFuerBild = s;
		}

		s = pr.getProperty("zoomx");
		if(s != null){
			xZoomProSchritt = Integer.parseInt(s);
		}

		s = pr.getProperty("zoomy");
		if(s != null){
			yZoomProSchritt = Integer.parseInt(s);
		}

		s = pr.getProperty("lookandfeel");
		if(s != null){
			lookAndFeelAktuell = Integer.parseInt(s);
			// Früher Index 3 = „Metal (classic)“ (entfernt) → Modern · light
			if (lookAndFeelAktuell == 3) {
				lookAndFeelAktuell = lookAndFeelFlatLight;
			}
			if (lookAndFeelAktuell < lookAndFeelOSStandard || lookAndFeelAktuell > lookAndFeelFlatDark
					|| (lookAndFeelAktuell > lookAndFeelNimbus && lookAndFeelAktuell < lookAndFeelFlatLight)) {
				lookAndFeelAktuell = lookAndFeelFlatLight;
			}
		}

		s = pr.getProperty("elementbeschriftungpreset");
		if (s != null) {
			try {
				int p = Integer.parseInt(s.trim());
				if (p >= 0 && p < ElementBeschriftungPresets.ANZAHL_PRESETS) {
					elementBeschriftungPresetIndex = p;
				}
			} catch (NumberFormatException ignored) {
				// ungültig → Standard beibehalten
			}
		}

		s = pr.getProperty("uilanguage");
		if (s != null && !s.isEmpty()) {
			setUiLanguageTag(s);
			uiLanguageFromPropertiesFile = true;
		}
	}

	/** Wenn keine {@code uilanguage} in den Einstellungen: an JVM-Locale anlehnen (z. B. deutsch → {@code de}). */
	private static void applyDefaultUiLanguageFromRuntimeLocale() {
		Locale def = Locale.getDefault();
		String lang = def.getLanguage();
		if ("de".equals(lang)) {
			setUiLanguageTag("de");
			return;
		}
		if ("pt".equals(lang) && "PT".equalsIgnoreCase(def.getCountry())) {
			setUiLanguageTag("pt_PT");
			return;
		}
		setUiLanguageTag("en");
	}
	
	
	public static void saveSettings(){
		
		Properties properties = new Properties();
		
		properties.setProperty("stretchlast", letzteElementeStrecken ? "1" : "0");
		
		properties.setProperty("cespaces", ""+codeErzeugerEinrueckungGesamt);
		properties.setProperty("cespacesperstep", ""+codeErzeugerEinrueckungProStufe);
		properties.setProperty("celanguage", ""+codeErzeugerProgrammiersprache);
		properties.setProperty("cecomments", codeErzeugerAlsKommentar ? "1" : "0");
		
		properties.setProperty("mousewheelresize", beiMausradGroesseAendern ? "1" : "0");
		properties.setProperty("useelementshortcuts", elementShortcutsVerwenden ? "1" : "0");
		
		properties.setProperty("pathfiles", zuletztGenutzterSpeicherpfad);
		properties.setProperty("pathpictures", zuletztGenutzterPfadFuerBild);
		
		properties.setProperty("zoomx", ""+xZoomProSchritt);
		properties.setProperty("zoomy", ""+yZoomProSchritt);
		
		properties.setProperty("lookandfeel", ""+lookAndFeelAktuell);

		properties.setProperty("elementbeschriftungpreset", "" + elementBeschriftungPresetIndex);

		properties.setProperty("uilanguage", uiLanguageTag);

		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(einstellungsDateiPfad)));

			properties.store(out, APP_DISPLAY_NAME + " Properties");
			out.close();

			File legacy = new File(einstellungsDateiPfadLegacy);
			if (legacy.exists() && !legacy.delete()) {
				legacy.deleteOnExit();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void setzeSpeicherpfad(String pfad){
		if(!pfad.equals("")){
			zuletztGenutzterSpeicherpfad = pfad; //letzter richtiger Pfad wird gespeichert
		}
	}

	public static void setzeBildSpeicherpfad(String pfad){
		if(!pfad.equals("")){
			zuletztGenutzterPfadFuerBild = pfad;
		}
	}


	public static String getZuletztGenutzterSpeicherpfad() {
		return zuletztGenutzterSpeicherpfad;
	}


	public static String getZuletztGenutzterPfadFuerBild() {
		return zuletztGenutzterPfadFuerBild;
	}

	public static void setzeLetzteElementeStrecken(boolean strecken){
		letzteElementeStrecken = strecken;
	}

	public static boolean gibLetzteElementeStrecken(){
		return letzteElementeStrecken;
	}

	/** Text für neu erzeugte Struktogramm-Elemente (gewähltes Textpaket). */
	public static String gibElementBeschriftung(int typNummer){
		String[] row = ElementBeschriftungPresets.gibPresetZeile(elementBeschriftungPresetIndex);
		if (typNummer >= 0 && typNummer < row.length) {
			return row[typNummer];
		}
		return StruktogrammPalette.getDefaultTextForNewElement(typNummer);
	}

	/**
	 * Text auf der linken Palette: bei Preset <b>Java (Standard)</b> syntaxnahe Kurzformen ({@code if}, {@code while}, …),
	 * sonst kurze didaktische Namen gemäß <b>UI-Sprache</b> ({@code structure.palette.*}).
	 */
	public static String gibPaletteButtonBeschriftung(int typNummer) {
		if (elementBeschriftungPresetIndex == ElementBeschriftungPresets.PRESET_ENGLISH_JAVA) {
			return ElementBeschriftungPresets.javaStandardPaletteButtonLabel(typNummer);
		}
		return StructureElementI18n.paletteShortLabel(typNummer);
	}

	public static int getElementBeschriftungPresetIndex() {
		return elementBeschriftungPresetIndex;
	}

	/**
	 * Wendet ein Textpaket an (nur für <b>neu</b> eingefügte Elemente; bestehende Blöcke bleiben unverändert).
	 */
	public static void wendeElementBeschriftungsPresetAn(int presetIndex) {
		if (presetIndex < 0 || presetIndex >= ElementBeschriftungPresets.ANZAHL_PRESETS) {
			elementBeschriftungPresetIndex = ElementBeschriftungPresets.PRESET_ENGLISH_JAVA;
		} else {
			elementBeschriftungPresetIndex = presetIndex;
		}
	}

	public static void setCodeErzeugerEinrueckungGesamt(
			int codeErzeugerEinrueckungGesamt) {
		GlobalSettings.codeErzeugerEinrueckungGesamt = codeErzeugerEinrueckungGesamt;
	}


	public static int getCodeErzeugerEinrueckungGesamt() {
		return codeErzeugerEinrueckungGesamt;
	}


	public static void setCodeErzeugerEinrueckungProStufe(
			int codeErzeugerEinrueckungProStufe) {
		GlobalSettings.codeErzeugerEinrueckungProStufe = codeErzeugerEinrueckungProStufe;
	}


	public static int getCodeErzeugerEinrueckungProStufe() {
		return codeErzeugerEinrueckungProStufe;
	}

	public static int getCodeErzeugerProgrammiersprache() {
		return codeErzeugerProgrammiersprache;
	}

	public static void setCodeErzeugerProgrammiersprache(int codeErzeugerProgrammiersprache) {
		if (codeErzeugerProgrammiersprache == CodeErzeuger.typPython) {
			GlobalSettings.codeErzeugerProgrammiersprache = CodeErzeuger.typPython;
		} else if (codeErzeugerProgrammiersprache == CodeErzeuger.typJavaScript) {
			GlobalSettings.codeErzeugerProgrammiersprache = CodeErzeuger.typJavaScript;
		} else {
			GlobalSettings.codeErzeugerProgrammiersprache = CodeErzeuger.typJava;
		}
	}

	public static boolean isCodeErzeugerAlsKommentar() {
		return codeErzeugerAlsKommentar;
	}


	public static void setCodeErzeugerAlsKommentar(boolean codeErzeugerAlsKommentar) {
		GlobalSettings.codeErzeugerAlsKommentar = codeErzeugerAlsKommentar;
	}


	public static void setBeiMausradGroesseAendern(boolean beiMausradGroesseAendern) {
		GlobalSettings.beiMausradGroesseAendern = beiMausradGroesseAendern;
	}


	public static boolean isBeiMausradGroesseAendern() {
		return beiMausradGroesseAendern;
	}


	public static int getXZoomProSchritt() {
		return xZoomProSchritt;
	}


	public static void setXZoomProSchritt(int xZoomProSchritt) {
		GlobalSettings.xZoomProSchritt = xZoomProSchritt;
	}


	public static int getYZoomProSchritt() {
		return yZoomProSchritt;
	}


	public static void setYZoomProSchritt(int yZoomProSchritt) {
		GlobalSettings.yZoomProSchritt = yZoomProSchritt;
	}


	public static boolean isElementShortcutsVerwenden() {
		return elementShortcutsVerwenden;
	}


	public static void setElementShortcutsVerwenden(boolean elementShortcutsVerwenden) {
		GlobalSettings.elementShortcutsVerwenden = elementShortcutsVerwenden;
	}


	/** @return Index des gewählten Themes (siehe {@link Konstanten}). */
	public static int getLookAndFeelAktuell() {
		return lookAndFeelAktuell;
	}


	public static void setLookAndFeelAktuell(int themeIndex) {
		GlobalSettings.lookAndFeelAktuell = themeIndex;
	}

	/** Locale für die UI-Texte: Englisch, Deutsch oder Português (Portugal, {@code pt-PT}). */
	public static Locale getUiLocale() {
		if ("de".equals(uiLanguageTag)) {
			return Locale.GERMAN;
		}
		if ("pt_PT".equals(uiLanguageTag)) {
			return Locale.forLanguageTag("pt-PT");
		}
		return Locale.ENGLISH;
	}

	public static String getUiLanguageTag() {
		return uiLanguageTag;
	}

	/** Kanonische Tags: {@code en}, {@code de}, {@code pt_PT}. */
	public static String normalizeUiLanguageTag(String tag) {
		if (tag == null || tag.isBlank()) {
			return "en";
		}
		String t = tag.trim();
		if (t.equalsIgnoreCase("de")) {
			return "de";
		}
		String hy = t.replace('_', '-').toLowerCase(Locale.ROOT);
		if ("pt-pt".equals(hy)) {
			return "pt_PT";
		}
		return "en";
	}

	/**
	 * Setzt die UI-Sprache; unbekannte Werte werden wie {@code en} behandelt.
	 */
	public static void setUiLanguageTag(String tag) {
		uiLanguageTag = normalizeUiLanguageTag(tag);
	}

	public static boolean isUiGerman() {
		return "de".equals(uiLanguageTag);
	}

	public static boolean isUiPortuguesePortugal() {
		return "pt_PT".equals(uiLanguageTag);
	}
}

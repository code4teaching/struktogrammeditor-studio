package de.visustruct.view;

import java.util.Arrays;

import de.visustruct.i18n.I18n;
import de.visustruct.i18n.StructureElementI18n;

/**
 * Fest vorgegebene Textvorlagen für neu eingefügte Elemente (keine freie Eingabe).
 * Zwei Pakete — wie VisuStruct-SwiftUI: syntaxnahe Java-Vorgaben oder Begriffe gemäß Oberflächensprache.
 * Muss dieselbe Länge haben wie {@link EinstellungsDialog#anzahlStruktogrammElemente}.
 */
public final class ElementBeschriftungPresets {

	/** Syntaxnahe Java-Vorgaben; Palette mit {@link #javaStandardPaletteButtonLabel(int)}. */
	public static final int PRESET_ENGLISH_JAVA = 0;
	/** Begriffe und Platzhalter gemäß gewählter Oberflächensprache ({@link StructureElementI18n}). */
	public static final int PRESET_DIDACTIC_I18N = 1;
	public static final int ANZAHL_PRESETS = 2;

	public static final int[] PRESET_DIALOG_REIHENFOLGE = {
			PRESET_ENGLISH_JAVA,
			PRESET_DIDACTIC_I18N,
	};

	private static final int N = 10;

	/** Standardtexte für {@link #PRESET_ENGLISH_JAVA} (Platzhalter in neuen Blöcken). */
	private static final String[] ENGLISH_JAVA_ROW = {
			"Statement", "condition", "selector", "i = 0; i < n; i++",
			"condition", "condition", "\u221e", "break", "method()", "\u00f8",
	};

	/**
	 * Nur Palette bei {@link #PRESET_ENGLISH_JAVA}: echte Java-Schlüsselwörter / übliche Kurzformen
	 * (nicht {@code condition} für alles).
	 */
	private static final String[] JAVA_STANDARD_PALETTE_LABELS = {
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
	};

	static {
		if (PRESET_DIALOG_REIHENFOLGE.length != ANZAHL_PRESETS) {
			throw new IllegalStateException("Preset-Dialog-Reihenfolge");
		}
		if (JAVA_STANDARD_PALETTE_LABELS.length != N) {
			throw new IllegalStateException("Java-Palette-L\u00e4nge");
		}
		if (ENGLISH_JAVA_ROW.length != N) {
			throw new IllegalStateException("English-Java-Preset-L\u00e4nge");
		}
	}

	private ElementBeschriftungPresets() {
	}

	/**
	 * Alte Einstellungen (0–4) aus {@code elementbeschriftungpreset} auf die zwei verbleibenden
	 * Presets abbilden.
	 */
	public static int migrateLegacyPresetIndex(int legacy) {
		return switch (legacy) {
			case 0, 1, 4 -> PRESET_DIDACTIC_I18N;
			case 2, 3 -> PRESET_ENGLISH_JAVA;
			default -> PRESET_ENGLISH_JAVA;
		};
	}

	/** Gespeichertes Preset → Index der zugehörigen Radio-Option im Dialog. */
	public static int dialogPlatzFuerPreset(int presetIndex) {
		for (int u = 0; u < PRESET_DIALOG_REIHENFOLGE.length; u++) {
			if (PRESET_DIALOG_REIHENFOLGE[u] == presetIndex) {
				return u;
			}
		}
		return 0;
	}

	/** Radio-Position im Dialog → interner Preset-Index. */
	public static int presetIndexAtDialogPlatz(int dialogPlatz) {
		if (dialogPlatz < 0 || dialogPlatz >= PRESET_DIALOG_REIHENFOLGE.length) {
			return PRESET_ENGLISH_JAVA;
		}
		return PRESET_DIALOG_REIHENFOLGE[dialogPlatz];
	}

	/** Paletten-Button bei gewähltem Java-Standard-Preset ({@link #PRESET_ENGLISH_JAVA}). */
	public static String javaStandardPaletteButtonLabel(int typ) {
		if (typ < 0 || typ >= JAVA_STANDARD_PALETTE_LABELS.length) {
			return "";
		}
		return JAVA_STANDARD_PALETTE_LABELS[typ];
	}

	public static String getPresetAnzeigename(int index) {
		return switch (index) {
			case PRESET_ENGLISH_JAVA -> I18n.tr("elementPreset.englishJava");
			case PRESET_DIDACTIC_I18N -> I18n.tr("elementPreset.didacticUiLanguage");
			default -> I18n.tr("elementPreset.englishJava");
		};
	}

	public static String[] gibPresetZeile(int index) {
		if (index == PRESET_DIDACTIC_I18N) {
			return StructureElementI18n.didacticDefaultTexts();
		}
		if (index < 0 || index >= ANZAHL_PRESETS) {
			return Arrays.copyOf(ENGLISH_JAVA_ROW, N);
		}
		return Arrays.copyOf(ENGLISH_JAVA_ROW, N);
	}

	public static String[] kopierePreset(int index) {
		return Arrays.copyOf(gibPresetZeile(index), N);
	}

	public static void kopierePresetIn(String[] ziel, int index) {
		if (ziel == null || ziel.length != N) {
			throw new IllegalArgumentException("ziel");
		}
		System.arraycopy(gibPresetZeile(index), 0, ziel, 0, N);
	}

	public static int findePresetIndex(String[] aktuell) {
		if (aktuell == null || aktuell.length != N) {
			return -1;
		}
		for (int p = 0; p < ANZAHL_PRESETS; p++) {
			String[] ref = gibPresetZeile(p);
			boolean match = true;
			for (int i = 0; i < N; i++) {
				String a = aktuell[i] != null ? aktuell[i].trim() : "";
				if (!ref[i].equals(a)) {
					match = false;
					break;
				}
			}
			if (match) {
				return p;
			}
		}
		return -1;
	}

	public static String alsVorschauText(int index) {
		String[] row = gibPresetZeile(index);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < N; i++) {
			if (i > 0) {
				sb.append('\n');
			}
			sb.append(StructureElementI18n.previewRowLabel(i)).append(": ").append(row[i]);
		}
		return sb.toString();
	}
}

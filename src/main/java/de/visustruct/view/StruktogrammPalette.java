package de.visustruct.view;

/**
 * Reihenfolge und feste Beschriftungen der linken Palette (englisch).
 */
public final class StruktogrammPalette {

	/** Indizes entsprechen {@link de.visustruct.control.Struktogramm}-Konstanten. */
	public static final int[] TYPEN_REIHENFOLGE = { 0, 1, 4, 5, 3, 2, 6, 7, 8 };

	/**
	 * Kurze englische Paletten-Texte; ausführliche Benennung steht im Tooltip.
	 */
	public static String getPaletteButtonLabel(int typ) {
		return getPaletteButtonKurzEnglish(typ);
	}

	/** Kompakte englische Beschriftung der Palette. */
	public static String getPaletteButtonKurzEnglish(int typ) {
		switch (typ) {
		case 0:
			return "Statement";
		case 1:
			return "If";
		case 2:
			return "Switch";
		case 3:
			return "For loop";
		case 4:
			return "While loop";
		case 5:
			return "Do-while loop";
		case 6:
			return "Endless loop";
		case 7:
			return "Break";
		case 8:
			return "Call";
		default:
			return "";
		}
	}

	/** Ausführliche englische Palette-Tooltips. */
	public static String getPaletteElementTooltipEnglish(int typ) {
		switch (typ) {
		case 0:
			return "Statement — simple action or assignment";
		case 1:
			return "If / else — decision";
		case 2:
			return "Switch — multi-way selection";
		case 3:
			return "For — counter-controlled loop";
		case 4:
			return "While — pre-test loop";
		case 5:
			return "Do-While — post-test loop";
		case 6:
			return "Infinite loop";
		case 7:
			return "Break — exit from a loop or switch";
		case 8:
			return "Call — procedure or method call";
		case 9:
			return "Empty placeholder block";
		default:
			return "";
		}
	}

	/** Bezeichnung für Leer-Element (Palette hat kein Kachel-Typ 9). */
	public static String getPaletteLabelLeerElement() {
		return "Empty";
	}

	/**
	 * Standardtext für neu eingefügte Blöcke (englisch / code-nah).
	 */
	public static String getDefaultTextForNewElement(int typ) {
		switch (typ) {
		case 0:
			return "Statement";
		case 1:
			return "condition";
		case 2:
			return "selector";
		case 3:
			return "i = 0; i < n; i++";
		case 4:
			return "condition";
		case 5:
			return "condition";
		case 6:
			return "\u221e";
		case 7:
			return "break";
		case 8:
			return "method()";
		case 9:
			return "\u00f8";
		default:
			return "";
		}
	}

	private StruktogrammPalette() {
	}
}

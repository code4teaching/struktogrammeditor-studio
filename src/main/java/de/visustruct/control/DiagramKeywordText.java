package de.visustruct.control;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Locale;
import java.util.Optional;

/**
 * Fachliche Diagramm-Textdarstellung: Schlüsselwörter fett, Rest normal (Monospace),
 * angelehnt an {@code DiagramCanvasView.keywordSplit} / {@code drawKeywordLine} in VisuStruct-SwiftUI.
 */
public final class DiagramKeywordText {

	private DiagramKeywordText() {
	}

	/** Ergebnis von {@link #splitKeywordLine(String)}. */
	public static final class KeywordParts {
		public final String keyword;
		public final String rest;

		public KeywordParts(String keyword, String rest) {
			this.keyword = keyword;
			this.rest = rest;
		}
	}

	/**
	 * Wie Swift {@code normalizedDoUntilConditionLines}: führendes {@code until} in der
	 * Fußbedingung für die Anzeige zu {@code while} umformulieren (Speicher bleibt unverändert).
	 */
	public static String normalizeDoUntilDisplayLine(String line) {
		if (line == null) {
			return "";
		}
		String t = line.trim();
		String lower = t.toLowerCase(Locale.ROOT);
		if ("until".equals(lower)) {
			return "";
		}
		if (lower.startsWith("until ")) {
			return t.substring(5).trim();
		}
		if (lower.startsWith("until(")) {
			return t.substring(5);
		}
		return line;
	}

	/**
	 * Erste Zeile mit führendem Schlüsselwort versehen (Swift {@code withLeadingKeyword}),
	 * wenn noch nicht vorhanden.
	 */
	public static String ensureLeadingKeyword(String keyword, String line) {
		if (line == null) {
			return keyword + " ";
		}
		String trimmed = line.trim();
		if (trimmed.isEmpty()) {
			return keyword + " ";
		}
		String lower = trimmed.toLowerCase(Locale.ROOT);
		String k = keyword.toLowerCase(Locale.ROOT);
		if (lower.equals(k) || lower.startsWith(k + " ")) {
			return line;
		}
		return keyword + " " + trimmed;
	}

	/**
	 * Zeile für die Diagramm-Anzeige aufbereiten (Do-Until-Normalisierung + ggf. führendes Keyword).
	 *
	 * @param struktogrammTyp {@link Struktogramm#typAnweisung} …
	 * @param lineIndex       0-basiert; führendes Keyword nur bei Zeile 0
	 */
	public static String lineForDisplay(int struktogrammTyp, int lineIndex, String raw) {
		if (raw == null) {
			return "";
		}
		String line = raw;
		if (struktogrammTyp == Struktogramm.typDoUntilSchleife) {
			line = normalizeDoUntilDisplayLine(line);
		}
		if (lineIndex != 0) {
			return line;
		}
		return switch (struktogrammTyp) {
			case Struktogramm.typVerzweigung -> ensureLeadingKeyword("if", line);
			case Struktogramm.typWhileSchleife -> ensureLeadingKeyword("while", line);
			case Struktogramm.typForSchleife -> ensureLeadingKeyword("for", line);
			case Struktogramm.typFallauswahl -> ensureLeadingKeyword("switch", line);
			case Struktogramm.typDoUntilSchleife -> ensureLeadingKeyword("while", line);
			default -> line;
		};
	}

	/**
	 * Zerlegt eine Zeile in fett zu zeichnendes Schlüsselwort und Rest (Swift {@code keywordSplit}).
	 */
	public static Optional<KeywordParts> splitKeywordLine(String line) {
		if (line == null || line.isEmpty()) {
			return Optional.empty();
		}
		String trimmed = line.trim();
		String lower = trimmed.toLowerCase(Locale.ROOT);
		for (String prefix : new String[] { "input:", "output:", "print:" }) {
			if (lower.equals(prefix)) {
				return Optional.of(new KeywordParts(trimmed.substring(0, prefix.length()), ""));
			}
			if (lower.startsWith(prefix + " ")) {
				String kw = trimmed.substring(0, prefix.length());
				String rest = trimmed.substring(prefix.length()).trim();
				return Optional.of(new KeywordParts(kw, rest));
			}
		}
		int sp = indexOfFirstSpace(trimmed);
		if (sp < 0) {
			String w = trimmed;
			if (isBareKeyword(w)) {
				return Optional.of(new KeywordParts(w, ""));
			}
			return Optional.empty();
		}
		String head = trimmed.substring(0, sp);
		if (!isBareKeyword(head)) {
			return Optional.empty();
		}
		String rest = trimmed.substring(sp + 1).trim();
		return Optional.of(new KeywordParts(head, rest));
	}

	private static int indexOfFirstSpace(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isBareKeyword(String w) {
		String lower = w.toLowerCase(Locale.ROOT);
		return lower.equals("if") || lower.equals("for") || lower.equals("while")
				|| lower.equals("until") || lower.equals("switch") || lower.equals("do")
				|| lower.equals("true") || lower.equals("false") || lower.equals("default");
	}

	/** Gesamtbreite der Zeile (fett + normal) in px. */
	public static int measureLineWidth(Graphics2D g, String line) {
		Optional<KeywordParts> parts = splitKeywordLine(line);
		if (parts.isEmpty()) {
			return (int) Math.ceil(stringWidth(g, g.getFont(), line));
		}
		KeywordParts p = parts.get();
		Font plain = stripBold(g.getFont());
		Font bold = plain.deriveFont(Font.BOLD);
		double wKw = stringWidth(g, bold, p.keyword);
		double wRest = p.rest.isEmpty() ? 0 : stringWidth(g, plain, " " + p.rest);
		return (int) Math.ceil(wKw + wRest);
	}

	/**
	 * Zeichnet eine Zeile mit optional fettem Schlüsselwort; {@code y} ist Basislinie wie bei {@link Graphics2D#drawString}.
	 */
	public static void drawKeywordAwareLine(Graphics2D g, Color color, int x, int y, String line) {
		g.setColor(color);
		Optional<KeywordParts> parts = splitKeywordLine(line);
		if (parts.isEmpty()) {
			g.drawString(line, x, y);
			return;
		}
		KeywordParts p = parts.get();
		Font plain = stripBold(g.getFont());
		Font bold = plain.deriveFont(Font.BOLD);
		g.setFont(bold);
		g.drawString(p.keyword, x, y);
		int x2 = x + (int) Math.ceil(stringWidth(g, bold, p.keyword));
		if (!p.rest.isEmpty()) {
			g.setFont(plain);
			g.drawString(" " + p.rest, x2, y);
		} else {
			g.setFont(plain);
		}
	}

	private static Font stripBold(Font f) {
		return f.deriveFont(f.getStyle() & ~Font.BOLD);
	}

	private static double stringWidth(Graphics2D g, Font font, String s) {
		FontRenderContext frc = g.getFontRenderContext();
		Rectangle2D r = font.getStringBounds(s, frc);
		return r.getWidth();
	}
}

package de.visustruct.control;

import java.awt.Color;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;

/**
 * Farben der Struktogramm-Zeichenfläche (nicht die Swing-Fenster).
 * Bei <b>Modern · dark</b> (FlatDark) wird eine hellere graue Fläche genutzt, passend zum dunklen UI.
 * <p>
 * Helles Schema: an {@code VisuStruct-SwiftUI} / {@code DiagramCanvasView} angelehnt (weiße Zellen,
 * schwarze Kanten 2 px, blauer Auswahlrahmen).
 */
public final class CanvasStyle {

	/** Kanten von Blöcken und Hilfslinien — entspricht Swift {@code DiagramCanvasInk.diagramLineWidth}. */
	public static final float DIAGRAM_LINE_WIDTH = 2f;
	/** Auswahlrahmen um die aktive Kachel — entspricht Swift {@code DiagramCanvasInk.selectionLineWidth}. */
	public static final float SELECTION_LINE_WIDTH = 3.5f;

	private static Color background;
	private static Color diagramFrame;
	private static Color titleText;
	private static Color elementBorder;
	private static Color elementText;
	private static Color elementSelectedFill;
	private static Color selectionStroke;
	private static Color dropPreview;
	private static Color dragFrame;
	private static Color elementFill;
	private static Color simulationStepHighlightFill;

	private CanvasStyle() {
	}

	static {
		applyLightPalette();
	}

	private static void applyLightPalette() {
		// Canvas ~ Swift Color(white: 0.98); Rahmen/Kanten schwarz wie im Swift-Diagramm-Canvas.
		background = new Color(0xFAFAFA);
		diagramFrame = Color.BLACK;
		titleText = new Color(0x111827);
		elementBorder = Color.BLACK;
		elementText = new Color(0x111827);
		// Swift: Auswahl = weiße Füllung + blauer Rand; Füllung kommt aus {@code elementFill}, Rahmen aus {@code selectionStroke}.
		elementSelectedFill = new Color(0xDBEAFE);
		selectionStroke = new Color(0f, 0.48f, 1f, 0.9f);
		// Drop-Vorschau: kräftiger + halbtransparent, damit sie über Diagrammflächen nicht "klebt".
		dropPreview = new Color(0x803B82F6, true);
		dragFrame = new Color(0x3B82F6);
		elementFill = Color.WHITE;
		simulationStepHighlightFill = new Color(0xBFDBFE);
	}

	/** Zeichenfläche spürbar dunkler; Blöcke bleiben hell genug für Lesbarkeit. */
	private static void applyDarkPalette() {
		background = new Color(0x1E1E22);
		diagramFrame = new Color(0xB4B4BA);
		titleText = new Color(0xF4F4F5);
		elementBorder = new Color(0xB4B4BA);
		elementText = new Color(0x18181B);
		elementSelectedFill = new Color(0xA7C7FF);
		selectionStroke = new Color(0.35f, 0.63f, 1f, 0.92f);
		// Drop-Vorschau: stärker + halbtransparent.
		dropPreview = new Color(0x805B9FFF, true);
		dragFrame = new Color(0x5B9FFF);
		elementFill = new Color(0xECECF0);
		simulationStepHighlightFill = new Color(0x1E3A5F);
	}

	private static boolean isFlatDarkTheme() {
		LookAndFeel laf = UIManager.getLookAndFeel();
		return laf instanceof FlatDarkLaf || laf instanceof FlatMacDarkLaf;
	}

	/**
	 * Nach Theme-Wechsel ({@link javax.swing.UIManager#setLookAndFeel}) aufrufen, z. B. aus {@link de.visustruct.view.UiTheme#applyAfterTheme()}.
	 */
	public static void syncToTheme() {
		if (isFlatDarkTheme()) {
			applyDarkPalette();
		} else {
			applyLightPalette();
		}
	}

	public static Color getBackground() {
		return background;
	}

	public static Color getDiagramFrame() {
		return diagramFrame;
	}

	public static Color getTitleText() {
		return titleText;
	}

	public static Color getElementBorder() {
		return elementBorder;
	}

	public static Color getElementText() {
		return elementText;
	}

	public static Color getElementSelectedFill() {
		return elementSelectedFill;
	}

	/** Randfarbe für die aktuell ausgewählte Kachel (Swift: {@code selectionStrokeColor}). */
	public static Color getSelectionStroke() {
		return selectionStroke;
	}

	public static Color getDropPreview() {
		return dropPreview;
	}

	public static Color getDragFrame() {
		return dragFrame;
	}

	/** Füllfarbe der Blöcke (nicht markiert). */
	public static Color getElementFill() {
		return elementFill;
	}

	/** Hintergrund für den aktuellen Simulations-Schritt im Diagramm. */
	public static Color getSimulationStepHighlightFill() {
		return simulationStepHighlightFill;
	}
}
